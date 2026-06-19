# CLAUDE.md

Pointers for future Claude sessions on this repo. Keep it short — code is the
source of truth, this is just orientation.

## Project in one paragraph

JIMI API is a Spring Boot 3 / Java 17 chatbot backend. The user posts a
natural-language message; the API asks an LLM (default: **Mistral AI**, free
tier, OpenAI-compatible) to extract structured intent and acts on the user's
**own connected calendar** (Google primary, plus CalDAV and Microsoft) through
the `CalendarProvider` abstraction. JIMI keeps **no copy of calendar events** —
the calendar is the single source of truth. Multi-turn flows (missing info, or
a pending edit/delete awaiting confirmation) are persisted server-side in a
`conversation` table and resumed via `conversationId`.

**Safety model (non-negotiable):** the LLM never executes a destructive action.
CREATE is written directly (low-risk, reversible). EDIT/DELETE return
`AWAITING_CONFIRMATION` with the resolved target event(s); the calendar is only
touched after the user confirms via `POST /chat/confirm`, which acts on the
recorded event ids **without re-consulting the LLM**. See `ChatService`.

> Migration in progress: PR #1 landed the provider abstraction + confirmation
> flow. PR #2 (branch `feat/google-oauth-provider`) adds OAuth account linking
> (encrypted tokens, PKCE) + `GoogleCalendarProvider`, so Google users get a
> live calendar. CalDAV (PR #3) and Microsoft (PR #4) come next, then the app.
>
> **OAuth/Google:** see `docs/google-calendar-setup.md`. Tokens are stored
> AES-256-GCM encrypted (`TokenCipher`, key `TOKEN_ENCRYPTION_KEY`); only the
> `calendar.events` scope is requested; no calendar content is persisted.
> Endpoints: `GET /connect/google`, `GET /oauth/google/callback`,
> `GET /connections`, `DELETE /connect/google`.

## Build / run

```shell
./mvnw -DskipTests compile        # quick check
./mvnw -DskipTests package        # produces target/jimi-api.jar
./run.sh                          # local run — sources .env then mvnw spring-boot:run
docker compose up -d --build      # full stack (api + MariaDB) — see Dockerfile + docker-compose.yml
```

Required env vars to actually start: `DB_PASSWORD`, `MISTRAL_API_KEY` (or
`LLM_API_KEY`). They live in `.env` at project root (gitignored, template in
`.env.example`). Defaults for everything else live in `application.yml`.

`mvn` and `./mvnw` are interchangeable; the wrapper just pins a Maven version
for reproducibility.

## Deployment model (2-stack, registry-free)

The api is deployed **independently from the web frontend** — the
Android client and the browser both consume `jimi-api.julsql.fr`, so
the API stack must stand on its own.

`docker-compose.yml` here runs `api` + `db`:

- The api binds to `127.0.0.1:${API_PORT:-8102}:8080` — never public.
  The host's nginx reverse-proxies `jimi-api.julsql.fr` onto that
  loopback port (drop-in vhost in `../jimi_app/infra/nginx/`).
- The db has no host port mapping at all — only `api` reaches it via
  the docker bridge network. Use `docker-compose.db.yml` for ad-hoc
  dev access.

**No registry pulls of JIMI-specific images.** Only generic upstream
images are fetched (`mariadb:11`, `maven:3.9-eclipse-temurin-17`,
`eclipse-temurin:17-jre`). The api image is rebuilt locally from
source on every deploy (`docker compose up -d --build`).

## Architecture (modernised 2026-04)

Logic lives in services; the controller only validates and delegates.

```
ChatController  →  ChatService  →  LlmClient (interface)
                                    └─ OpenAiCompatibleLlmClient (impl)
                              →  ConversationService  →  ConversationRepository
                              →  AgendaService        →  AgendaRepository
```

- **`services/llm/LlmClient`** — provider-agnostic chat-completion interface.
  Swap providers by wiring a different bean. The default impl
  (`OpenAiCompatibleLlmClient`) talks to any OpenAI-compatible
  `/chat/completions` endpoint and is configured via `LlmProperties`
  (`llm.url`, `llm.model`, `llm.api-key`). Default points at Mistral; works
  unchanged for Groq, OpenRouter, Ollama, etc.
- **`services/ChatService`** — one HTTP turn:
  1. resume an in-progress `Conversation` if `conversationId` is given,
  2. append the new user message to history,
  3. call the LLM with `Prompts.extraction()`,
  4. if `EventExtraction` is incomplete → persist a draft, return
     `AWAITING_INFO` with `conversationId` + `missingFields`,
  5. otherwise dispatch to `AgendaService` and return `COMPLETED`.
- **`global/Prompts`** — LLM system prompts. The extraction prompt requires
  the LLM to output a strict JSON object including a `missing_fields` array,
  which the server uses (alongside its own check) to decide whether to ask
  for more.

## API contract

`POST /chat` body:

```json
{ "userId": "u1", "message": "...", "conversationId": null }
```

Response:

```json
{
  "conversationId": "<uuid|null>",
  "status": "AWAITING_INFO" | "COMPLETED",
  "message": "assistant reply for the user",
  "missingFields": ["date", "begin_time"]
}
```

`GET /` — health check returning the logo PNG.

## Swagger / OpenAPI

- UI: `http://localhost:8080/swagger-ui.html`
- Spec: `http://localhost:8080/v3/api-docs`
- Generated by **springdoc-openapi** 2.1.0 (the only Swagger lib — Springfox
  has been removed because it is incompatible with Spring Boot 3).

If you add an endpoint, annotate it with `@Operation` and `@ApiResponses`
following `ChatController` as the reference.

## Database

Two tables, both defined in `src/main/resources/create.sql`:

- `agenda` — calendar events (`id`, `date`, `type`, `begin_time`, `end_time`,
  `title`, `user_id`).
- `conversation` — in-progress LLM extraction drafts keyed by UUID. Stores
  the partial draft JSON, the full chat history JSON, status
  (`AWAITING_INFO` / `COMPLETED`), and timestamps.

`spring.jpa.hibernate.ddl-auto=update` will keep the schema in sync at boot.

## Conventions to keep

- Controllers stay thin — never put LLM/DB logic back in there.
- New LLM providers: implement `LlmClient` and register the bean. Don't add
  provider-specific code to `ChatService`.
- DTOs in `records/` use `org.json` (`JSONObject`/`JSONArray`) for
  serialization — match that style rather than introducing Jackson ad hoc.
- Secrets only via env vars, loaded from `.env` (gitignored). Never paste
  a real key into `application.yml`, code, or commit messages.
- Prompts go in `global/Prompts.java`, not back into `Shared.java`.

## Common gotchas

- The LLM must return strict JSON. We turn on `response_format: json_object`
  via `llm.json-mode: true` (Groq, OpenAI, OpenRouter support it; Ollama in
  newer versions too). If you switch to a provider that doesn't, set
  `llm.json-mode: false` and tighten the prompt instead.
- `EventExtraction` falls back to `category=OTHER` when JSON parsing fails,
  so a malformed LLM reply still produces a usable response — but it means
  silent failures look like off-topic answers. Log the raw content if
  debugging extraction issues.
- The legacy multi-turn pattern (frontend re-sending full message history)
  is **gone**. Server-side state via `conversationId` replaces it. Don't
  resurrect the old `UserMessages`/`UserMessage`/`UserAnswer` records.

## Files most likely relevant when changing things

| Change                              | Touch                                                 |
|-------------------------------------|-------------------------------------------------------|
| Tweak how the LLM is prompted       | `global/Prompts.java`                                 |
| Add a new LLM provider              | new impl of `services/llm/LlmClient`                  |
| Change request/response shape       | `records/ChatApiRequest.java`, `records/ChatApiResponse.java`, `controllers/ChatController.java` |
| Change how events are stored        | `entities/Agenda.java`, `services/AgendaService.java`, `resources/create.sql` |
| Change multi-turn behaviour         | `services/ConversationService.java`                   |
| Add a new endpoint                  | new `@RestController` + service; mirror `ChatController` swagger annotations |
