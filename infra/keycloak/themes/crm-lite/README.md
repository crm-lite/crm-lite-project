# CRM Lite — Keycloak `crm-lite` login theme

Reproduces the analyst **"Login v2"** mock (Etiya EDS Lite design system) as a
Keycloak **login** theme. This is the correct home for the FR-AUTH-01 login UI:
by architecture decision the login page is Keycloak's, not an Angular/in-app form
(**ADR-006**: credentials only on the Keycloak page; ROPC/Direct Grant disabled;
no token ever reaches the browser — ADR-007/008). The open item recorded in
`docs/requirements/document-delta.md` **#6** ("future Keycloak project theme") is
what this folder delivers.

## Layout

```
themes/crm-lite/
  README.md
  login/
    theme.properties            # parent=keycloak, locales=en,tr
    login.ftl                   # standalone template matching the mock
    messages/
      messages_en.properties    # theme copy (English, default)
      messages_tr.properties    # theme copy (Turkish)  — UTF-8
    resources/
      css/login.css             # reconstructed EDS Lite styling
      js/login.js               # password toggle + disable-until-filled (UX only)
      img/etiya-logo.svg        # exact logo extracted from the mock
      img/favicon.svg
```

Only `login.ftl` is overridden; every other page (error, etc.) falls back to the
stock `keycloak` theme. Registration/reset are disabled in the realm, so those
pages never render.

## How it maps the mock to real authentication

| Mock (prototype) | This theme (real Keycloak) |
|---|---|
| Hardcoded `salesperson/etiya2026`, JS view-swap on submit | Form posts to `${url.loginAction}`; Keycloak authenticates (Auth Code + PKCE) |
| Demo 3-attempt lockout | Keycloak's own brute-force settings (realm) |
| "Demo credentials" hint line | **Removed** — never print credentials on a real login page |
| Inline error text | Keycloak's localized, non-revealing message (`message.summary`, AC-AUTH-01-03/04/05) |
| Username / Password fields | `name="username"` / `name="password"` (the names Keycloak expects) |

FR-AUTH-01 UI criteria carried here: password masking (default) + reveal toggle,
`maxlength="64"` on both fields, login button disabled while a field is empty
(progressive enhancement — server still validates), EN/TR language switch.

## Applying the theme (IMPORTANT — realm import runs only once)

`docker-compose.yml` mounts `./keycloak/themes` into the Keycloak container and the
realm import sets `"loginTheme": "crm-lite"`. **But `--import-realm` only imports on
first start** (when the realm is absent from `keycloak_db`). If your `keycloak_db`
already has the `crm-lite` realm, the new `loginTheme` will NOT auto-apply. Pick one:

- **Existing realm (fastest):** Admin Console → `http://localhost:8180` → realm
  `crm-lite` → **Realm settings → Themes → Login theme → `crm-lite`** → Save.
- **Fresh apply from the committed JSON:** drop and re-import the realm (e.g. delete
  the `crm-lite` realm in the console, or recreate `keycloak_db`), then restart
  Keycloak so the import runs again. Do **not** `podman compose down -v` (destroys all
  DBs — see `docs/runbooks/auth-testing.md`).

## Iterating against the mock

`start-dev` disables theme caching, so the loop is fast:

1. Start the stack (see `docs/runbooks/auth-testing.md` §2) — or just `postgres` +
   `keycloak`.
2. Open `http://localhost:8080/oauth2/authorization/keycloak` (or, to see the page
   directly, the account/login URL for the `crm-lite` realm).
3. Edit files under `login/` → **refresh the browser** (no restart).
4. Compare to the mock's Login v2 screen; adjust `login.css`.

## Reconstruction notes (verify against the mock)

The mock is fully tokenized; most values were recovered exactly from the design's
inline token fallbacks and the logo asset. A few values could not be extracted from
the tokenized bundle and are best-fit — all marked `(assumed)` in `login.css`:
primary-button hover/active shades, focus-ring color, placeholder color, hover
border, and disabled colors. The primary button base color is the Etiya brand orange
`#F58220` (from the logo). The typeface is **Inter**; this theme uses an Inter →
system fallback stack rather than bundling Inter's woff2 files — say the word to
self-host the exact Inter weights for full typographic fidelity.
