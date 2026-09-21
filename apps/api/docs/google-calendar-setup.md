# Connecting JIMI to Google Calendar — setup guide

JIMI acts on the user's **own** Google Calendar via OAuth 2.0. This is the
one-time setup you (the operator) must do in your Google Cloud project. Claude
cannot do this part — it needs your Google account — but everything else is
already coded against the env vars below.

You'll end up with three secrets to drop into `.env`:
`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `TOKEN_ENCRYPTION_KEY`.

---

## 1. Enable the Google Calendar API

1. Go to <https://console.cloud.google.com/> and select your **JIMI** project
   (top-left project picker).
2. **APIs & Services → Library** → search **"Google Calendar API"** → **Enable**.

## 2. Configure the OAuth consent screen

1. **APIs & Services → OAuth consent screen**.
2. User type: **External** (unless you have a Workspace org) → Create.
3. Fill the required fields: app name (`JIMI`), user support email, developer
   contact email. Logo/links optional.
4. **Scopes** → *Add or remove scopes* → add **only**:
   `https://www.googleapis.com/auth/calendar.events`
   (read/write events — NOT full calendar, NOT contacts.)
5. **Test users**: while the app is in "Testing" status, only listed Google
   accounts can connect. **Add your own Gmail here** so you can test. Save.

> Staying in "Testing" is fine for you + a few testers. To open it to everyone,
> submit for verification later (required because `calendar.events` is a
> sensitive scope).

## 3. Create the OAuth client ID

1. **APIs & Services → Credentials → Create credentials → OAuth client ID**.
2. Application type: **Web application** (the token exchange happens
   server-side, with the client secret — even though the app opens the browser).
3. **Authorized redirect URIs** → add the callback exactly as the server will
   send it:
   - Production: `https://jimi-api.julsql.fr/oauth/google/callback`
   - Local dev:  `http://localhost:8080/oauth/google/callback`
   (Add both if you test locally. They must match `GOOGLE_REDIRECT_URI`
   character-for-character — trailing slash included.)
4. Create → copy the **Client ID** and **Client secret**.

## 4. Generate the token-encryption key

```bash
openssl rand -base64 32
```

## 5. Fill `.env`

```dotenv
TOKEN_ENCRYPTION_KEY=<output of openssl rand -base64 32>
GOOGLE_CLIENT_ID=<...>.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=<...>
GOOGLE_REDIRECT_URI=https://jimi-api.julsql.fr/oauth/google/callback
APP_RETURN_URL=jimi://connected
```

Restart the API (`./run.sh` or `docker compose up -d --build`). If the three
Google vars are blank, calendar features simply stay disabled (the chat replies
`NEEDS_CONNECTION`) — nothing crashes.

---

## How to test the flow

`APP_RETURN_URL` is a mobile deep link, so the very end of the flow only fully
closes inside the app. To test the **server** half from a browser/curl:

1. Open `GET https://jimi-api.julsql.fr/connect/google?userId=test-1` in a
   browser. You're redirected to Google's consent screen.
2. Approve with a **test user** account.
3. Google redirects to `/oauth/google/callback`, the API stores the tokens, then
   issues a 302 to `jimi://connected?status=connected` (the browser can't open
   that scheme — that's expected; the link succeeded if no error is shown).
4. Verify the link was recorded:
   `GET /connections?userId=test-1` → `{"providers":["google"]}`.
5. Now `POST /chat` with `{"userId":"test-1","message":"Lunch tomorrow 1pm"}`
   creates a real event on that Google account.
6. Clean up: `DELETE /connect/google?userId=test-1` (revokes + unlinks).

## Privacy / security notes

- Only the `calendar.events` scope is requested — JIMI cannot read your email,
  contacts, or other calendars' metadata.
- Access & refresh tokens are stored **AES-256-GCM encrypted** (`TokenCipher`),
  keyed by `TOKEN_ENCRYPTION_KEY`. A DB leak alone exposes no usable token.
- No calendar event content is ever persisted by JIMI.
- OAuth uses PKCE (S256) and an encrypted, time-limited `state` (CSRF-safe).
- Deleting a user (`DELETE /user?userId=`) revokes tokens at Google and removes
  the stored account.
