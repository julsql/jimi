# JIMI API

A Spring Boot 3 backend for the JIMI calendar assistant. The user
sends a natural-language message ("Plan a meeting tomorrow at 2pm");
the API asks an LLM to extract structured event data and CRUDs it on
a MariaDB `agenda` table. When the LLM doesn't have enough info, the
API parks a draft conversation server-side and asks the frontend for
the missing fields — multi-turn extraction without any client-side
state machine.

The web/Android client lives in the sibling
[`jimi_app`](../jimi_app) repo.

## Table of contents

- [How it works](#how-it-works)
- [Stack](#stack)
- [Quickstart](#quickstart)
- [Configuration](#configuration)
- [API contract](#api-contract)
- [Architecture](#architecture)
- [Database](#database)
- [Swapping LLM provider](#swapping-llm-provider)
- [Deployment](#deployment)
- [License](#license)
- [Authors](#authors)

## How it works

```
                   ┌──────────────────────────────────────┐
 user message      │  ChatController                      │
─────────────────▶ │     ↓                                │
                   │  ChatService                         │
                   │     ├─── LlmClient (Mistral default) │
                   │     │      "extract structured JSON" │
                   │     │                                │
                   │     ├─── if missing info             │
                   │     │      → save draft + return     │
                   │     │        conversationId          │
                   │     │                                │
                   │     └─── else → AgendaService.crud() │
                   │              → MariaDB               │
                   └──────────────────────────────────────┘
```

1. **First turn** — frontend posts `{ userId, message, conversationId: null }`.
2. **Extraction** — the LLM returns a strict JSON object: category
   (`CREATE`/`EDIT`/`DELETE`/`GET`/`OTHER`), event fields, missing
   fields, and a friendly reply.
3. **If incomplete** — the API persists a draft `conversation` row
   and returns `status: AWAITING_INFO` with the `conversationId` and
   the list of `missingFields`. The frontend echoes the
   `conversationId` with the user's next message.
4. **If complete** — `AgendaService` applies the action (insert,
   update, delete, or fetch) and returns `status: COMPLETED`.
5. **Schedule queries** — bypass the LLM entirely:
   `GET /agenda?userId=...` reads straight from MariaDB.

The extraction prompt is written to **never hallucinate**: when the
agenda is empty or info is missing, the LLM asks rather than
inventing. See [`global/Prompts.java`](src/main/java/com/tsp/jimi_api/global/Prompts.java).

## Stack

- **Spring Boot 3** + **Java 17** (built with Maven)
- **MariaDB 11** (via Hibernate / Spring Data JPA, `ddl-auto=update`)
- **Mistral AI** by default — any OpenAI-compatible
  `/chat/completions` endpoint works (Groq, Ollama, OpenRouter…)
- **springdoc-openapi 2.1** for the live OpenAPI / Swagger UI
- Docker Compose for the runtime stack

## Quickstart

### Full stack (API + DB) with Docker

```bash
git clone <this-repo>
cd jimi_api

cp .env.example .env
# edit .env — at minimum set MISTRAL_API_KEY=...

docker compose up -d --build
```

Open Swagger UI at <http://localhost:8102/swagger-ui.html>.

What runs:

| Service | Image                | Host port             | Notes                                  |
|---------|----------------------|-----------------------|----------------------------------------|
| `api`   | built locally        | `127.0.0.1:8102→8080` | Spring Boot backend                    |
| `db`    | `mariadb:11`         | (internal only)       | data persisted in volume `jimi-db-data` |

Wipe and restart from scratch: `docker compose down -v`.

### DB-only (run the JAR on your host)

If you'd rather run the backend with `./run.sh` and only need
MariaDB in a container:

```bash
docker compose -f docker-compose.db.yml up -d
./run.sh
```

(Add `DB_URL=jdbc:mariadb://localhost:3306/JIMI?useSSL=false&serverTimezone=Europe/Paris`
to `.env` first.)

### No Docker at all

```bash
./mvnw clean install
./run.sh                  # auto-loads .env then mvnw spring-boot:run
```

## Configuration

`application.yml` reads everything from environment variables with
safe defaults. Secrets live in `.env` (gitignored). **Never commit
credentials.**

| Variable             | Purpose                              | Default                                                |
|----------------------|--------------------------------------|--------------------------------------------------------|
| `MISTRAL_API_KEY`    | LLM API key — **required**           | _(empty — required)_                                   |
| `LLM_API_KEY`        | Fallback if `MISTRAL_API_KEY` unset  | _(empty)_                                              |
| `LLM_URL`            | Chat-completions endpoint            | `https://api.mistral.ai/v1/chat/completions`           |
| `LLM_MODEL`          | Model name                           | `mistral-small-latest`                                 |
| `DB_URL`             | JDBC URL                             | _(prod URL — override locally)_                        |
| `DB_USERNAME`        | DB user                              | `julsql`                                               |
| `DB_PASSWORD`        | DB password                          | _(empty — set it)_                                     |
| `API_PORT`           | Host port for the api container      | `8102`                                                 |
| `SPRING_PROFILES_ACTIVE` | Spring profile                   | `prod`                                                 |

## API contract

### `POST /chat` — natural-language turn (LLM)

**Request**

```json
{
  "userId": "user-123",
  "message": "Plan a meeting tomorrow at 2pm",
  "conversationId": null
}
```

`conversationId` is `null` for a fresh request, or the value
returned by a previous response.

**Response — info still missing (HTTP 200)**

```json
{
  "conversationId": "9f8e7d6c-...",
  "status": "AWAITING_INFO",
  "message": "Sure! When does the meeting end? 🙂",
  "missingFields": ["end_time"]
}
```

The frontend keeps `conversationId` and re-sends it with the user's
next message. The server merges the new info into the stored draft.

**Response — completed (HTTP 200)**

```json
{
  "conversationId": null,
  "status": "COMPLETED",
  "message": "Done! Meeting added on 2026-04-29 from 14:00 to 15:00.",
  "missingFields": []
}
```

`COMPLETED` means the action was applied (or the message was
off-topic). The frontend can drop any cached `conversationId`.

**Response — error (HTTP 400)**

```json
{ "error": "Chat request failed.", "reason": "Incorrect or missing user id." }
```

### `GET /agenda?userId=<id>` — read-only event list (no LLM)

Returns all events for a user, sorted by date then start time.
This is what the frontend's schedule page uses — by design the LLM
is **never** in the loop, so no hallucinated events.

```json
[
  {
    "id": 1,
    "date": "2026-04-29",
    "beginTime": "14:00:00",
    "endTime": "15:00:00",
    "type": "PRO",
    "title": "Meeting"
  }
]
```

### `GET /` — health-check

Returns the JIMI logo as PNG (used by uptime checks).

### Swagger / OpenAPI

- UI: <http://localhost:8102/swagger-ui.html>
- JSON: <http://localhost:8102/v3/api-docs>
- Production: <https://jimi-api.julsql.fr/swagger-ui/index.html>

## Architecture

The controller stays thin; logic lives in services.

```
ChatController  →  ChatService          →  LlmClient (interface)
                                            └─ OpenAiCompatibleLlmClient
                                        →  ConversationService → ConversationRepository
                                        →  AgendaService       → AgendaRepository

AgendaController →  AgendaService       →  AgendaRepository
```

Key files:

| Concern                              | File                                                                                                  |
|--------------------------------------|-------------------------------------------------------------------------------------------------------|
| Tweak the LLM prompt                 | [`global/Prompts.java`](src/main/java/com/tsp/jimi_api/global/Prompts.java)                           |
| Add an LLM provider                  | new impl of [`services/llm/LlmClient`](src/main/java/com/tsp/jimi_api/services/llm/LlmClient.java)   |
| Change request/response shape        | [`records/ChatApiRequest.java`](src/main/java/com/tsp/jimi_api/records/ChatApiRequest.java), [`ChatApiResponse.java`](src/main/java/com/tsp/jimi_api/records/ChatApiResponse.java), [`ChatController.java`](src/main/java/com/tsp/jimi_api/controllers/ChatController.java) |
| Change agenda persistence            | [`entities/Agenda.java`](src/main/java/com/tsp/jimi_api/entities/Agenda.java), [`services/AgendaService.java`](src/main/java/com/tsp/jimi_api/services/AgendaService.java), [`resources/create.sql`](src/main/resources/create.sql) |
| Multi-turn conversation logic        | [`services/ConversationService.java`](src/main/java/com/tsp/jimi_api/services/ConversationService.java) |

## Database

Two tables, defined in
[`src/main/resources/create.sql`](src/main/resources/create.sql)
and auto-applied by Hibernate (`ddl-auto=update`):

- **`agenda`** — calendar events
  (`id`, `date`, `type`, `begin_time`, `end_time`, `title`, `user_id`).
- **`conversation`** — in-progress draft extractions keyed by UUID
  (`status`, partial `draft_json`, full `history_json`, timestamps).
  Drafts are marked `COMPLETED` once the action is applied.

## Swapping LLM provider

The `LlmClient` interface is provider-agnostic. The default
implementation, `OpenAiCompatibleLlmClient`, talks to any
OpenAI-compatible `/chat/completions` endpoint. Swap by overriding
`LLM_URL` / `LLM_MODEL` (and the matching API key):

| Provider   | `LLM_URL`                                         | Sample `LLM_MODEL`                  |
|------------|---------------------------------------------------|-------------------------------------|
| Mistral    | `https://api.mistral.ai/v1/chat/completions`      | `mistral-small-latest`              |
| Groq       | `https://api.groq.com/openai/v1/chat/completions` | `llama-3.3-70b-versatile`           |
| OpenRouter | `https://openrouter.ai/api/v1/chat/completions`   | `meta-llama/llama-3.3-70b-instruct` |
| Ollama     | `http://localhost:11434/v1/chat/completions`      | `llama3.1`                          |

For providers that don't support `response_format: json_object`, set
`llm.json-mode: false` in `application.yml` and rely on the prompt
alone.

## Deployment

The API is deployed independently from the frontend (the Android
client and the browser both hit the same public API URL). Runtime
stack: `docker-compose.yml` (api + db). The api is bound to
`127.0.0.1:${API_PORT:-8102}` and the host's nginx reverse-proxies
`jimi-api.julsql.fr` onto that loopback port. The db has no host
port — it's only reachable through the docker bridge network.

Only generic upstream images are pulled (`mariadb:11`,
`maven:3.9-eclipse-temurin-17`, `eclipse-temurin:17-jre`). The api
image is rebuilt from source on every deploy.

### One-time host setup

```bash
git clone https://github.com/<owner>/jimi_api.git /opt/jimi_api
cd /opt/jimi_api

cat > .env <<EOF
MISTRAL_API_KEY=...
API_PORT=8102
EOF

# Drop the host nginx vhost (lives in the sibling repo)
sudo cp /path/to/jimi_app/infra/nginx/jimi-api.julsql.fr.conf /etc/nginx/conf.d/
sudo certbot --nginx -d jimi-api.julsql.fr
sudo nginx -s reload
```

The user that runs `docker compose` must be in the `docker` group.

### Deploy with GitHub Actions

`.github/workflows/deploy.yml` runs on every push to `main` and on
manual dispatch. Configure these repository secrets
(*Settings → Secrets and variables → Actions*):

| Secret        | Example         |
|---------------|-----------------|
| `SSH_HOST`    | `your.server`   |
| `SSH_USER`    | `deploy`        |
| `SSH_KEY`     | the SSH **private** key authorized on the server |
| `DEPLOY_PATH` | `/opt/jimi_api` |

The workflow SSHes onto the server and runs:

```bash
cd $DEPLOY_PATH
git pull
docker compose down
docker compose build --no-cache
docker compose up -d
docker image prune -f
```

### Deploy manually

Same five commands, packaged in `deploy.sh`:

```bash
DEPLOY_HOST=deploy@your.server \
DEPLOY_PATH=/opt/jimi_api \
./deploy.sh
```

Or by hand:

```bash
ssh deploy@your.server
cd /opt/jimi_api
git pull
docker compose up -d --build
```

## License

MIT — see [LICENSE](LICENSE).

## Authors

- Jul SQL
