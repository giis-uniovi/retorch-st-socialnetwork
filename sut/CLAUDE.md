# CLAUDE.md — docker-socialnetwork

This is the **System Under Test (SUT)** for the `retorch-st-socialnetwork` test suite. It is a fork of [DeathStarBench Social Network](https://github.com/delimitrou/DeathStarBench) with local bug fixes applied.

---

## What this project is

A microservices-based social network built on **Apache Thrift RPC**, fronted by an **OpenResty/Nginx gateway** that translates HTTP requests into Thrift calls via Lua scripts. It supports user registration/login, posting, following/unfollowing, and timeline reading (user-timeline and home-timeline).

---

## Architecture overview

### Entry points

| Port | Service | Role |
|------|---------|------|
| 8080 | `nginx-thrift` | Main API gateway + static HTML UI |
| 8081 | `media-frontend` | Media upload/download |
| 16686 | `jaeger-agent` | Distributed tracing UI |

All Thrift microservices listen on port **9090** within the Docker network and are not exposed to the host.

### Microservices

All use the image `deathstarbench/social-network-microservices:latest`.

| Service | Responsibility | Backing stores |
|---------|---------------|----------------|
| `user-service` | Register, login, JWT issuance | `user-mongodb`, `user-memcached` |
| `social-graph-service` | Follow/unfollow, follower/followee queries | `social-graph-mongodb`, `social-graph-redis` |
| `compose-post-service` | Post creation + fan-out orchestration | (calls other services; no own store) |
| `post-storage-service` | Persist post documents | `post-storage-mongodb`, `post-storage-memcached` |
| `user-timeline-service` | Author's own posts | `user-timeline-mongodb`, `user-timeline-redis` |
| `home-timeline-service` | Feed of posts from followed users | `home-timeline-redis` |
| `text-service` | Parse text: extract URLs and @mentions | stateless |
| `user-mention-service` | Resolve `@username` → user id | stateless |
| `url-shorten-service` | Shorten URLs found in post text | `url-shorten-mongodb`, `url-shorten-memcached` |
| `unique-id-service` | Snowflake 64-bit post ID generation | stateless |
| `media-service` | Media storage | `media-mongodb`, `media-memcached` |

### Data stores

- **MongoDB 4.4.6** — 5 instances (user, social-graph, post-storage, url-shorten, media); all on the internal Docker network, ports not exposed
- **Redis** — 3 instances (social-graph, home-timeline, user-timeline)
- **Memcached** — 3 instances (post-storage, url-shorten, user/media)

---

## Core data flows

- **Register** → `user-service` writes user to `user-mongodb`
- **Login** → `user-service` verifies credentials, returns JWT; Nginx sets `login_token` cookie and redirects to `main.html`
- **Follow/Unfollow** → `social-graph-service` updates `social-graph-mongodb` and refreshes `social-graph-redis`
- **Compose post** → `compose-post-service` fans out: `unique-id` → `text`/`url-shorten`/`user-mention` enrichment → `post-storage` persist → `user-timeline` update → `home-timeline` update for each follower (reads follower list from `social-graph-service`)
- **Read user-timeline** → `user-timeline-service` returns author's own posts
- **Read home-timeline** → `home-timeline-service` returns the fan-out feed (never includes the requesting user's own posts)

---

## Nginx gateway (`nginx-web-server/`)

OpenResty (nginx + LuaJIT) listening on port 8080. The nginx.conf defines all routes; each route `content_by_lua_file`s into a Lua script that:
1. Parses/validates the request
2. Checks JWT if required
3. Opens a pooled Thrift TCP connection to the target microservice
4. Makes the Thrift RPC call using generated bindings from `gen-lua/`
5. Serialises the response as JSON and returns it

### Endpoint catalog

Two route trees:

- `/api/*` — browser UI surface (JWT-cookie auth, redirects to HTML)
- `/wrk2-api/*` — unauthenticated load-test surface (accepts `user_id` directly)

| Route | Method | Auth | Notes |
|-------|--------|------|-------|
| `/api/user/register` | POST | none | `first_name,last_name,username,password`; 400 on missing field; 302→index.html |
| `/api/user/login` | POST | none | `username,password`; sets `login_token` cookie; 302→main.html; wrong password→500 |
| `/api/user/follow` | POST | none¹ | `user_name`+`followee_name`; 302→contact.html |
| `/api/user/unfollow` | POST | none¹ | same params; 302→contact.html (see bug fix below) |
| `/api/user/get_follower` | GET | JWT cookie | returns `[{follower_id}]`; 401 without cookie |
| `/api/user/get_followee` | GET | JWT cookie | returns `[{followee_id}]`; 401 without cookie |
| `/api/post/compose` | POST | JWT cookie | `post_type,text[,media_*]`; user taken from JWT |
| `/api/user-timeline/read` | GET | JWT cookie | `start,stop` |
| `/api/home-timeline/read` | GET | JWT cookie | `start,stop` |
| `/wrk2-api/post/compose` | POST | none | `username,user_id,text,media_ids,media_types,post_type` |
| `/wrk2-api/user-timeline/read` | GET | none | `user_id,start,stop` |
| `/wrk2-api/home-timeline/read` | GET | none | `user_id,start,stop` |

¹ JWT check is commented out in the Lua; follow/unfollow accept username params without a session.

### Session management

JWT secret is stored in the Nginx shared dictionary `config` (32 KB). Cookie TTL is 24 hours. Protected endpoints decode the cookie to extract `user_id` and `username`.

---

## Lua scripts (`nginx-web-server/lua-scripts/`)

```
api/user/register.lua
api/user/login.lua
api/user/follow.lua
api/user/unfollow.lua       ← patched in this fork
api/user/get_follower.lua
api/user/get_followee.lua
api/post/compose.lua
api/user-timeline/read.lua
api/home-timeline/read.lua
wrk2-api/                   ← parallel endpoints for load testing
```

After editing any Lua script, restart the gateway so OpenResty reloads it:

```bash
docker restart docker-socialnetwork-nginx-thrift-1
```

---

## Generated Thrift bindings (`gen-lua/`)

Auto-generated from `social_network.thrift`. Do not edit manually.

```
social_network_UserService.lua
social_network_SocialGraphService.lua
social_network_ComposePostService.lua
social_network_PostStorageService.lua
social_network_UserTimelineService.lua
social_network_HomeTimelineService.lua
social_network_TextService.lua
social_network_MediaService.lua
social_network_UrlShortenService.lua
social_network_UserMentionService.lua
social_network_UniqueIdService.lua
social_network_ttypes.lua
social_network_constants.lua
```

---

## HTML pages (`nginx-web-server/pages/`)

| Page | Title | Key element ids |
|------|-------|-----------------|
| `index.html` | Login | form `name=username/password`, "Sign Up" link |
| `signup.html` | Signup | `name=first_name/last_name/username/password`, "Login" link |
| `main.html` | DeathStar | `#show-post`, `#compose`, `#post-content`, `#create-post`, `#card-block` |
| `profile.html` | Profile | `#post-content`, `#card-block`, `#mentioned_user` |
| `contact.html` | Contact | follow/unfollow forms, `#follower-list`, `#followee-list` |

---

## Known bugs & quirks

### Empty-`ZADD` 500

`social-graph-service` caches follower/followee sets in Redis with `ZADD`. When the set is **empty**, Redis rejects the command and the service returns HTTP 500. This affects:
- **Compose post** for an author with zero followers (home-timeline fan-out calls `GetFollowers`)
- **`get_follower`/`get_followee`** when the user has none

Workaround in tests: always arrange ≥1 follower before composing; unfollow tests follow two users and only remove one.

### Unfollow file-download bug — FIXED in this repo

Upstream `unfollow.lua` did not redirect on success, so the response fell through with `Content-Type: application/octet-stream`, causing the browser to open a save-file dialog and the unfollow to appear incomplete in the UI.

**Fix applied:** `nginx-web-server/lua-scripts/api/user/unfollow.lua` now calls `ngx.redirect("../../contact.html")` on success, matching `follow.lua`.

### Home-timeline is asynchronous and excludes own posts

A composed post appears in `user-timeline` immediately but only reaches followers' `home-timeline` after fan-out completes. Tests polling home-timeline must retry.

### Empty home-timeline returns `{}` not `[]`

`home-timeline-service` returns a JSON object instead of an empty array when there are no posts. Clients must tolerate both forms.

### Snowflake user IDs

User IDs are 64-bit integers (e.g. `1199044181654814720`). Lua uses `tonumber` (double) for these consistently, so values remain self-consistent within the gateway even though they exceed double precision exactly.

---

## Configuration

### Runtime service config

`config/service-config.json` — consumed by all microservices at startup.

### Jaeger tracing

`nginx-web-server/conf/jaeger-config.json`:
- Service name: `nginx-web-server`
- Agent: `jaeger-agent:6831`
- Sampler: probabilistic, 20%

---

## Deployment

### Start (single machine)

```bash
docker-compose up -d
```

Wait up to 300 s for the gateway to serve the DeathStar UI at `http://localhost:8080`.

### Alternative compose files

| File | Use |
|------|-----|
| `docker-compose.yml` | Default single-machine |
| `docker-compose-sharding.yml` | Redis sharding |
| `docker-compose-swarm.yml` | Docker Swarm multi-node |
| `docker-compose-tls.yml` | TLS enabled |

### Bootstrap social graph (optional)

```bash
python3 scripts/init_social_graph.py --graph=socfb-Reed98 --ip=localhost --port=8080
```

Registers dataset users and creates follow edges via `/wrk2-api/`.

---

## Media frontend (`media-frontend/`)

Secondary Nginx gateway on port **8081** using image `yg397/media-frontend:xenial`.

- `/upload-media` — handles multipart file uploads (max 100 MB)
- `/get-media` — retrieves media by ID from Memcached/MongoDB

---

## Thrift interface (`social_network.thrift`)

Source of truth for all inter-service contracts. Key structures:

- `User` — credentials and metadata
- `Post` — content, creator, media references, mentions, URLs
- `Media`, `UserMention`, `Url` — embedded structures in `Post`
- `ErrorCode` — standard error enum used by all services

To regenerate bindings after editing the `.thrift` file:

```bash
thrift --gen lua social_network.thrift   # regenerates gen-lua/
thrift --gen cpp social_network.thrift   # regenerates gen-cpp/
```
