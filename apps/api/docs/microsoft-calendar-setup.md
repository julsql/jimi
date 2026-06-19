# Connecting JIMI to Outlook / Microsoft 365 — setup guide

JIMI acts on the user's own Outlook/Microsoft 365 calendar via Microsoft Graph
(OAuth 2.0, PKCE). One-time setup in **Microsoft Entra ID** (formerly Azure AD).
Mirrors the Google setup; Claude can't do this part (needs your Microsoft
account), but the code is already wired to the env vars below.

You'll end up with: `MICROSOFT_CLIENT_ID`, `MICROSOFT_CLIENT_SECRET` (and reuse
the existing `TOKEN_ENCRYPTION_KEY`).

---

## 1. Register the application

1. Go to <https://entra.microsoft.com> → **Identity → Applications → App registrations → New registration**.
2. Name: `JIMI`.
3. **Supported account types**: "Accounts in any organizational directory and personal Microsoft accounts" (matches `MICROSOFT_TENANT=common`). Pick a single tenant instead if you only want your org.
4. **Redirect URI**: platform **Web**, value exactly:
   - Production: `https://jimi-api.julsql.fr/oauth/microsoft/callback`
   - Local dev:  `http://localhost:8080/oauth/microsoft/callback`
   (Must match `MICROSOFT_REDIRECT_URI` exactly. Add both if you test locally.)
5. Register → copy the **Application (client) ID** → `MICROSOFT_CLIENT_ID`.

## 2. Client secret

**Certificates & secrets → New client secret** → copy the secret **Value**
(not the Id) → `MICROSOFT_CLIENT_SECRET`. Note its expiry.

## 3. API permissions (scope)

**API permissions → Add a permission → Microsoft Graph → Delegated permissions**
→ add **`Calendars.ReadWrite`** → Add. (We also request `offline_access` at
runtime to get a refresh token; it doesn't need to be added here.) Minimal scope
— no mail or contacts access.

## 4. Fill `.env`

```dotenv
MICROSOFT_CLIENT_ID=<application-client-id>
MICROSOFT_CLIENT_SECRET=<secret-value>
MICROSOFT_REDIRECT_URI=https://jimi-api.julsql.fr/oauth/microsoft/callback
MICROSOFT_TENANT=common
# TOKEN_ENCRYPTION_KEY is shared with Google (already set).
```

Restart the API. Blank client id/secret → Microsoft stays disabled (the chat
replies `NEEDS_CONNECTION`), nothing crashes.

## How to test

Same shape as Google:
1. Browser → `GET /connect/microsoft?userId=test-1` → Microsoft consent.
2. Approve → callback stores tokens → 302 to `jimi://connected` (browser can't
   open that scheme — expected).
3. `GET /connections?userId=test-1` → `{"providers":["microsoft"]}`.
4. `POST /chat {"userId":"test-1","message":"Lunch tomorrow 1pm"}` creates a real
   Outlook event.
5. `DELETE /connect/microsoft?userId=test-1` to unlink.

## Notes / limitations

- Recurring events are **not** mapped for Microsoft yet: Graph uses a structured
  `patternedRecurrence` object instead of an iCal RRULE, so JIMI handles single
  events on this provider for now.
- Tokens are AES-256-GCM encrypted at rest, same as Google.
- `calendarView` times are requested in the server's timezone (`Prefer:
  outlook.timezone`) so the hour shown to the user is correct.
