# Connecting JIMI to a CalDAV calendar — setup guide

JIMI can act on any **CalDAV** calendar (Apple iCloud, Fastmail, Nextcloud,
mailbox.org, …). Unlike Google there is **no OAuth flow**: the user provides a
calendar **collection URL** plus HTTP Basic credentials (username + an
app-specific password). JIMI encrypts these at rest with the same
`TOKEN_ENCRYPTION_KEY` it uses for Google tokens (`TokenCipher`,
AES-256-GCM) — make sure that env var is set.

JIMI never discovers your principal or calendar-home: you give it the exact
collection URL of the single calendar it should manage.

---

## What you need

1. The **collection URL** of the calendar (ends in a `/`, points at one
   calendar collection, not the account root).
2. Your **username** (usually your email / Apple ID).
3. An **app-specific password** (most providers refuse your normal password
   for CalDAV).

---

## Apple iCloud

1. Create an app-specific password: sign in at
   <https://account.apple.com/> → **Sign-In and Security → App-Specific
   Passwords** → generate one, label it "JIMI". Copy it.
2. Find your collection URL. The host is `https://caldav.icloud.com`. The path
   contains your numeric DSID and the calendar name, e.g.

   ```
   https://caldav.icloud.com/<dsid>/calendars/home/
   ```

   The easiest way to get the exact URL is a CalDAV client (e.g. Thunderbird,
   or `curl` a `PROPFIND` against `https://caldav.icloud.com/.well-known/caldav`
   following the redirects). The `home` collection is the default calendar.
3. Username = your Apple ID email. Password = the app-specific password.

## Fastmail

1. **Settings → Privacy & Security → Integrations → App passwords** → create
   one scoped to **CalDAV/CardDAV**.
2. Collection URL pattern:

   ```
   https://caldav.fastmail.com/dav/calendars/user/<you@fastmail.com>/<calendar-id>/
   ```

   Your default calendar id is often `Default`.
3. Username = your Fastmail address. Password = the app password.

## Nextcloud

1. In Nextcloud: **Settings → Security → Devices & sessions → Create new app
   password**.
2. Collection URL pattern (visible in **Calendar → … → Copy private link**,
   then keep the collection path):

   ```
   https://<your-nextcloud>/remote.php/dav/calendars/<username>/<calendar-name>/
   ```
3. Username = your Nextcloud username. Password = the app password.

---

## Connecting via the API

`POST /connect/caldav` with a JSON body:

```json
{
  "userId": "u1",
  "serverUrl": "https://caldav.icloud.com/<dsid>/calendars/home/",
  "username": "me@icloud.com",
  "password": "<app-specific-password>"
}
```

JIMI issues a `PROPFIND` (Depth 0) against the collection URL to validate the
credentials before storing them:

- **success** → `200 {"connected": true}` (credentials encrypted + stored).
- **failure** → `400 {"error": "Connect failed.", "reason": "..."}` (nothing
  is stored).

To disconnect (there is no token to revoke, the row is simply deleted):

```
DELETE /connect/caldav?userId=u1   →   200 {"disconnected": true}
```

`GET /connections?userId=u1` lists every linked provider, including `caldav`.

---

## Troubleshooting

- **400 on connect** — most often a wrong collection URL (pointing at the
  account root instead of a single calendar) or the normal account password
  instead of an app-specific one. Confirm the URL ends with the calendar's
  collection path and a trailing `/`.
- **Encryption errors** — `TOKEN_ENCRYPTION_KEY` is not set. Generate one with
  `openssl rand -base64 32`.
