# FE-ADR-010: Containerization — Multi-Stage Build, Additive Compose Change Only

## Status
Accepted (2026-07-23). Image tags pinned per FE-ADR-002 §4.

## Context
`infra/docker-compose.yml` currently runs eight services and carries a
substantial amount of hard-won operational knowledge in its comments: the
build-context-is-repo-root rule, the healthcheck rationale (PROJECTBRAIN §5.10
records a real failure where `depends_on` alone let services boot against
stale defaults), the `bash /dev/tcp` probe workaround for images without
curl/wget, the `KC_HOSTNAME` issuer pinning, and the `keycloak-init` one-shot
container.

ADR-009 established that customer-service, lookup-service and mernis-stub
publish **no host port** — all client traffic enters through the gateway BFF.

## Decision

### 1. Multi-stage Dockerfile at `frontend/Dockerfile`
```
Stage 1 (build):   node:22.23.1-alpine   → npm ci && npm run build
Stage 2 (runtime): nginx:1.30.4-alpine   → static assets + nginx.conf
```
The runtime image contains no Node, no `node_modules` and no sources — only
built assets and an nginx config.

**Why multi-stage:** a single-stage Node image would ship the entire toolchain
and dependency tree to run a static file server, and would keep a Node process
alive in production for no reason.

**Why nginx and not `ng serve`:** the dev server is explicitly not a production
server — no compression, no cache headers, unnecessary rebuild machinery, and a
different code path from what tests exercise.

**Why both tags are exact:** `node:22-alpine` and `nginx:alpine` are moving
targets; a rebuild three months later would silently produce a different image.
`node:22.23.1-alpine` matches the pinned Node exactly (FE-ADR-002 §4), so the
container build cannot diverge from local development. `nginx:1.30.4-alpine` is
on the **stable** line (even minor number; 1.31.x is mainline).

### 2. Build context is the repository root
```
build:
  context: ..
  dockerfile: frontend/Dockerfile
```
This matches every existing service. The comment at the top of the compose file
explains the backend's reason (parent POM inheritance); the frontend follows the
same shape for consistency, and `.dockerignore` already excludes `target`,
`.git` and `.idea` — it gains `node_modules` and `dist`.

### 3. nginx serves the SPA and reverse-proxies the four gateway prefixes
Implementing FE-ADR-004 §3:

| Location | Behaviour |
|---|---|
| `/api`, `/oauth2`, `/login`, `/logout` | `proxy_pass http://api-gateway:8080` |
| everything else | static files, `try_files $uri $uri/ /index.html` |

The SPA fallback is required so a deep link (`/customers/1001`) reloads
correctly instead of 404-ing. Proxy locations are declared **before** the
fallback so they are never swallowed by it.

`proxy_pass` targets the compose service name `api-gateway`, resolved on
`crm-net` — the gateway's published host port is irrelevant to this hop.

### 4. Compose is changed ADDITIVELY — nothing existing is touched
> 🔴 **Binding rule.** Adding the frontend means appending **one new service
> block** to `infra/docker-compose.yml`. No existing service's lines are
> edited, reordered, reformatted or "tidied". No existing `depends_on`,
> healthcheck, environment variable, port mapping or comment is modified.

**Why this is a rule and not etiquette:** the file encodes failure modes that
were diagnosed the hard way and documented in PROJECTBRAIN §5.10 and §4.7. A
reformat or an innocent-looking reordering can reintroduce a boot-order bug that
manifests only intermittently. A frontend change must never be able to break the
backend stack.

The new service:
- builds from the repo root (§2),
- `depends_on: api-gateway: condition: service_healthy` — the proxy target must
  be answering,
- carries its own healthcheck in the established style,
- joins `crm-net`,
- publishes exactly one host port.

### 5. Host port is **4200**
Decided 2026-07-23. Previously published ports are `8888`, `8761`, `8080`,
`5432`, `8180`; `4200` is free and familiar from `ng serve`, so the port is the
same in development and in the container.

The redirect-origin problem this port choice used to be blocked on is **resolved**
— see FE-ADR-004 §Addendum. In short: nginx forwards the browser's real origin,
the gateway honours it via `forward-headers-strategy: framework`, and both
`:4200` and `:8080` are registered on the `crm-bff` client and re-applied on
every `up` by `keycloak-init`.

### 5b. Required nginx proxy headers
The reverse-proxy locations must forward the browser-facing origin, otherwise
the gateway builds an OAuth `redirect_uri` pointing at the internal container
name and Keycloak rejects it:

```nginx
proxy_set_header Host              $host;
proxy_set_header X-Forwarded-Host  $host;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Port  $server_port;
proxy_set_header X-Real-IP         $remote_addr;
```

`$host` is passed through rather than hardcoded so the image is portable across
hostnames.

### 5c. Scoped exception to §4 (recorded, not silent)
§4 forbids editing existing compose services. Enabling the frontend origin
required **one** such edit: extending the `keycloak-init` command so it also
reconciles the `crm-bff` client's redirect URIs, exactly as it already
reconciles `loginTheme`.

This is a deliberate, reviewed exception rather than a loosening of the rule.
It qualifies because it (a) uses the mechanism the repository already
established for this precise class of problem, (b) adds behaviour without
altering any existing setting, (c) touches a container nothing `depends_on`, so
a failure cannot affect stack startup, and (d) was verified on the running stack
to preserve `pkce.code.challenge.method: S256` and
`directAccessGrantsEnabled: false`. The rule stands for every other service.

### 6. No environment-variable API URL is injected
Consistent with FE-ADR-004 §2 there is nothing to inject: the frontend calls
relative paths and nginx decides the upstream. No entrypoint script rewrites a
config file at container start, and no runtime `env.js` is generated.

## Consequences
- The production artifact is a small static image; the Node toolchain never
  reaches a running environment.
- `docker compose up --build` brings up a stack that behaves like production,
  including the reverse proxy — so proxy-related bugs surface locally rather
  than after deployment.
- Compose grows from 8 to 9 services. PROJECTBRAIN §10's note ("Compose 8
  servis") will need updating when this is implemented.
- The frontend build runs inside Docker for the container image and on the host
  for development. Both use the same pinned Node, so behaviour matches.
- Rebuilds are slower than backend rebuilds on cold cache (`npm ci`); layer
  ordering (copy `package*.json`, `npm ci`, then copy sources) keeps the warm
  path fast.
