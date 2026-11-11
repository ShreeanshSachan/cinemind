# CineMind AI

A microservices-based AI movie recommendation system. Describe a film in plain
language and get explained recommendations backed by semantic search over a
real movie catalogue — with a visible, honest execution trace.

> **Status:** all five backend services and the Angular frontend are implemented
> and built. Docker Compose runs the full stack. See
> [Integration verification](#10-integration-verification) for exactly what was
> tested end-to-end and what was not.

**Documentation**

| Document | Purpose |
| --- | --- |
| [`docs/SETUP.md`](docs/SETUP.md) | Step-by-step setup on a new/office laptop (prerequisites, transfer, env config, Docker Compose, troubleshooting) |
| [`docs/SYSTEM_DOCUMENT.md`](docs/SYSTEM_DOCUMENT.md) | Full system design & specification (architecture, requirements, data model, flows, API, config, limitations) |
| `README.md` (this file) | Quick reference, commands and API examples |

---

## 1. Project overview

CineMind AI combines:

- **Java Spring Boot microservices** with Eureka discovery and a Spring Cloud
  Gateway single entry point.
- **Two Gemini Flash agents** coordinated by a deterministic orchestrator
  (Preference Analyst → RAG retrieval → Recommendation Specialist).
- **Retrieval-Augmented Generation** using local Ollama embeddings stored in
  MySQL as JSON text and compared with **Java cosine similarity** — no vector
  database.
- **Angular 21** frontend with reactive forms, real backend data and a live
  agent execution trace.

A normal recommendation makes **at most two Gemini calls** (one per agent) and
**one Ollama embedding call** (the query). Movie embeddings are generated only
during ingestion.

---

## 2. Architecture

```
Browser (Angular :4200)
        |
        v
API Gateway :8080  ──routes──>  /api/movies/**          -> MOVIE-SERVICE  :8081
        |                        /api/rag/**             -> RAG-SERVICE    :8083
        |                        /api/recommendations/** -> AI-ORCHESTRATOR :8084
        |                              |        ^              |
        |                              |        |              +-- 1 Gemini call (Agent 1)
        v                              |        +----------------- 1 Gemini call (Agent 2)
Eureka :8761  <--- all services register / resolve lb:// names
        |
   MySQL 8 :3306  (cinemind)
     movies, movie_embeddings
        ^
        +-- Ollama (host, :11434)  <-- RAG container via host.docker.internal
```

| Service | Port | Responsibility |
| --- | --- | --- |
| Discovery Server | 8761 | Eureka registry + dashboard |
| API Gateway | 8080 | CORS, Eureka-based routing, JSON 503 on downstream failure |
| Movie Service | 8081 | Movie CRUD/search/filter, owns Flyway schema |
| RAG Service | 8083 | Ollama embeddings, MySQL persistence, cosine similarity retrieval |
| AI Orchestrator | 8084 | Two Gemini agents, Java hard filters, execution trace |
| Angular frontend | 4200 | Search, results, similar movies, agent trace |

**Data ownership:** `database/migrations/V1__create_schema.sql` creates both
`movies` and `movie_embeddings`; `V2__seed_movies.sql` seeds 26 development
movies. Only the Movie Service runs Flyway; RAG sets
`spring.flyway.enabled: false` and `ddl-auto: none`.

---

## 3. Technology stack

| Component | Version |
| --- | --- |
| Java | **17** |
| Spring Boot | **3.5.15** |
| Spring Cloud | **2025.0.3** |
| Spring AI | **1.1.8** |
| Angular / TypeScript | **21.2** / 5.9 |
| Maven Wrapper | 3.9.11 |
| MySQL | 8.4 |
| Ollama model | `nomic-embed-text` (768 dimensions) |
| Gemini | `gemini-2.5-flash` |

All three framework BOMs were verified against Maven Central to target the same
Spring Boot 3.5.15 baseline. No Spring Framework/Hibernate/Spring Data versions
are overridden manually. No Lombok, PostgreSQL, pgvector, Redis, Chroma,
Pinecone, Kafka or LangChain4j.

---

## 4. Prerequisites

- JDK 17, Node 24 + npm 11, Docker + Docker Compose v2, MySQL 8 (or Docker).
- Ollama installed on the host for embeddings.
- A Google AI Studio API key for Gemini (recommendations only).

### Java 17 setup

```powershell
# Windows PowerShell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
java -version
.\mvnw.cmd -v        # bootstraps Maven 3.9.11 via the committed wrapper
```

```bash
# Linux / macOS
export JAVA_HOME=/path/to/jdk-17
./mvnw -v
```

`JAVA_HOME` must be set — the Maven Wrapper requires it. The wrapper is
jar-based because the workspace path may contain spaces.

### MySQL setup

Docker Compose creates the database automatically. For a local JVM run:

```powershell
docker run -d --name cinemind-mysql -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=change-me-root `
  -e MYSQL_DATABASE=cinemind `
  -e MYSQL_USER=cinemind `
  -e MYSQL_PASSWORD=change-me `
  mysql:8.4
```

The Movie Service applies the Flyway migrations on first start.

### Ollama installation and model download

Ollama runs on the **host**, not in Docker Compose.

```bash
# Install: https://ollama.com/download
ollama serve
ollama pull nomic-embed-text
ollama list        # confirm nomic-embed-text:latest
curl http://localhost:11434/api/tags
```

`OLLAMA_BASE_URL` is `http://localhost:11434` for a local JVM and
`http://host.docker.internal:11434` inside Docker. The RAG container declares
`extra_hosts: ["host.docker.internal:host-gateway"]` so this resolves on
Docker Desktop (Windows/macOS) and Linux.

### Gemini API key configuration

```powershell
Copy-Item .env.example .env
# edit .env: set GEMINI_API_KEY=AIza... (from https://aistudio.google.com/app/apikey)
```

**Never commit `.env`.** `.gitignore` excludes `.env`, `.env.*`, `*.key` and
`*.pem`. The key is read only by the AI Orchestrator; it is never sent to the
browser. Leave `GEMINI_API_KEY` unset (or the `.env.example` placeholder) to
start in `DEGRADED` mode: `/api/recommendations/health` reports `DEGRADED` and
`POST /api/recommendations` returns a clean JSON 503 **without making any
Gemini call**. Do not set it to an empty string — Spring AI needs a non-blank
value at startup.

---

## 5. Environment variables

`.env.example` is the template. Required/new names:

| Variable | Purpose |
| --- | --- |
| `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | MySQL credentials |
| `GEMINI_API_KEY` | Gemini Flash key (orchestrator only) |
| `GEMINI_MODEL` | LLM model, default `gemini-2.5-flash` |
| `OLLAMA_BASE_URL` | Ollama endpoint, `http://host.docker.internal:11434` in Docker |
| `OLLAMA_EMBEDDING_MODEL` | default `nomic-embed-text` |

Extras: `EMBEDDING_DIMENSION` (768), `EUREKA_URL`,
`CORS_ALLOWED_ORIGINS`, `DISCOVERY_SERVER_PORT`, `GATEWAY_PORT`,
`MOVIE_SERVICE_PORT`, `RAG_SERVICE_PORT`, `ORCHESTRATOR_PORT`,
`FRONTEND_PORT`, `MYSQL_PORT`, `MYSQL_HOST`.

The Angular API URL lives in
`frontend/cinemind-angular/src/environments/environment.ts` (`apiBaseUrl`),
swapped for `environment.production.ts` by `angular.json`. The Docker image
injects the browser-visible gateway URL at build time via the
`API_BASE_URL` build arg.

---

## 6. Running individual services

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'

# Build / test everything
.\mvnw.cmd -f backend/pom.xml clean package

# Or one service (each has its own pom.xml and is independently buildable)
.\mvnw.cmd -f backend/discovery-server/pom.xml spring-boot:run
.\mvnw.cmd -f backend/api-gateway/pom.xml spring-boot:run
.\mvnw.cmd -f backend/movie-service/pom.xml spring-boot:run
.\mvnw.cmd -f backend/rag-service/pom.xml spring-boot:run
.\mvnw.cmd -f backend/ai-orchestrator/pom.xml spring-boot:run

# Frontend
cd frontend/cinemind-angular
npm install
npm start -- --port 4200
```

For local JVMs set `MYSQL_HOST=localhost`, `MYSQL_PORT=3306` and
`EUREKA_URL=http://localhost:8761/eureka/`. Start Eureka and MySQL first,
then the other services.

Useful URLs: <http://localhost:8761> (Eureka dashboard),
<http://localhost:8080/api/movies>, <http://localhost:8083/api/rag/health>,
<http://localhost:8084/api/recommendations/health>.

---

## 7. Running with Docker Compose

```powershell
Copy-Item .env.example .env
# edit .env (set GEMINI_API_KEY; adjust CORS_ALLOWED_ORIGINS if you change FRONTEND_PORT)
docker compose config          # validate
docker compose build           # builds 7 images (Maven Wrapper + npm ci inside)
docker compose up -d
docker compose ps              # wait until every service is (healthy)
docker compose logs -f rag-service
docker compose down            # docker compose down -v also removes the DB volume
```

Compose defines health checks for all seven services and uses
`depends_on: condition: service_healthy` so the Movie Service waits for MySQL +
Eureka, RAG waits for the Flyway schema, the Orchestrator waits for RAG, the
Gateway waits for all downstreams, and the frontend waits for the Gateway.
Host port mappings are overridable from `.env` if a port is in use.

The Angular image is built with `API_BASE_URL=http://localhost:${GATEWAY_PORT}`
so the browser always targets the published gateway port. Include the frontend
origin in `CORS_ALLOWED_ORIGINS` (default `http://localhost:4200`).

After start, index the catalogue once:

```bash
curl -X POST http://localhost:8080/api/rag/index/all
```

**Important:** Ollama is deliberately outside Compose. Start it on the host
and pull `nomic-embed-text` before running the stack.

---

## 8. API documentation

All endpoints are reachable through the Gateway (`http://localhost:8080`).

### Movie Service (`/api/movies`)

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/movies` | `201` + `Location` |
| GET | `/api/movies` | paginated: `page`, `size`, `sortBy`, `sortDir` |
| GET | `/api/movies/{id}` | `200` / `404` |
| PUT | `/api/movies/{id}` | full update |
| DELETE | `/api/movies/{id}` | `204` |
| GET | `/api/movies/search?title=` | case-insensitive |
| GET | `/api/movies/filter` | `genre`, `yearFrom`, `yearTo`, `minRuntime`, `maxRuntime`, `minRating` |

### RAG Service (`/api/rag`)

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/rag/index/{movieId}` | `?force=true` re-embeds |
| POST | `/api/rag/index/all` | only new/updated movies |
| POST | `/api/rag/search` | `{query, topK, minScore, filters}` → ranked docs with scores |
| GET | `/api/rag/health` | `UP` / `DEGRADED`, Ollama + model availability, indexed count |

### AI Orchestrator (`/api/recommendations`)

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/api/recommendations` | two Gemini calls max, top 10 retrieved, ≤5 results |
| GET | `/api/recommendations/health` | `UP` / `DEGRADED`, model, credit limit |

### Example recommendation request

```bash
curl -X POST http://localhost:8080/api/recommendations \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Suggest dark psychological thrillers after 2010",
    "maxResults": 5
  }'
```

Successful response (with a valid `GEMINI_API_KEY`):

```json
{
  "query": "Suggest dark psychological thrillers after 2010",
  "maxResults": 5,
  "retrievedDocuments": 10,
  "preferences": {
    "genres": ["Psychological Thriller"], "mood": "Dark",
    "similarTo": [], "releaseYearMin": 2010,
    "maxRuntime": null, "excludedGenres": []
  },
  "recommendations": [
    {
      "movieId": 17, "title": "Shutter Island", "releaseYear": 2010,
      "matchScore": 95, "reason": "Dark psychological tension on an isolated island"
    }
  ],
  "agentTrace": {
    "preferenceAgent": "COMPLETED",
    "ragRetrieval": "COMPLETED",
    "recommendationAgent": "COMPLETED",
    "retrievedDocuments": 10,
    "candidatesAfterFilters": 6
  }
}
```

Errors are structured JSON (`400`, `404`, `409`, `503`) from global exception
handlers. With no key the example above returns `503` with
`"GEMINI_API_KEY is not configured..."`.

---

## 9. Testing

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\mvnw.cmd -f backend/pom.xml clean package     # 149 tests, all 5 modules
cd frontend/cinemind-angular
npm test -- --watch=false                       # 23 tests, 7 spec files
npm run build                                   # production bundle
```

All automated tests mock LLM/embedding interactions — **no Gemini credits are
consumed**. Cosine similarity, embedding persistence, indexing, retrieval and
the orchestrator execution order are unit tested; the orchestrator also has a
full HTTP integration test with a mocked `ChatModel`.

> On this machine Windows Application Control blocks Rollup's native `.node`
> binary, which `ng test` / `ng serve` load (`ng build` is unaffected). A
> reversible workaround is to install
> `npm install --no-save @rollup/wasm-node@<rollup version>` and temporarily
> swap `node_modules/rollup` for `node_modules/@rollup/wasm-node`, then restore
> the native folder. This is environment-only: `package.json` and
> `package-lock.json` contain no WASM entry.

---

## 10. Integration verification

The full stack was actually started with `docker compose up -d` (seven
containers, all reported `healthy`) and exercised through the API Gateway and
a real browser. Because host ports 8080/8081/4200/3306 are occupied by other
projects on this machine, the verification used a temporary `.env` with
alternate published ports; internal container ports and service discovery were
unchanged.

| # | Check | Result |
| --- | --- | --- |
| 1 | Eureka Server starts | **PASS** — `GET /` → 200 HTML dashboard |
| 2 | All microservices register | **PASS** — `API-GATEWAY`, `MOVIE-SERVICE`, `RAG-SERVICE`, `AI-ORCHESTRATOR` all `UP` in `/eureka/apps` |
| 3 | API Gateway routes | **PASS** — `GET /api/movies`, `/api/rag/health`, `/api/recommendations/health` all routed through `:gateway` |
| 4 | Movie Service ↔ MySQL | **PASS** — Flyway V1/V2 applied; 26 movies queryable |
| 5 | Movie CRUD | **PASS** — POST `201`, GET `200`, PUT `200`, DELETE `204`, GET after delete `404` |
| 6 | RAG retrieves movie metadata | **PASS** — `POST /api/rag/index/all` fetched 26 movies from Movie Service via Eureka |
| 7 | Ollama generates embeddings | **PASS** — `/api/rag/health` `ollamaAvailable: true`; 26 real 768-dim vectors produced via `host.docker.internal` |
| 8 | Embeddings persist in MySQL | **PASS** — 26 rows, 26 distinct `movie_id`, all `nomic-embed-text`, 9.4–9.5 KB JSON each |
| 9 | Cosine similarity retrieval | **PASS** — query *"mind bending psychological thriller on an island"* ranked **Shutter Island** first (0.6929) with metadata filters applied |
| 10 | Preference Analyst extracts preferences | **PARTIAL** — see limitations |
| 11 | Recommendation Specialist ranks | **PARTIAL** — see limitations |
| 12 | Orchestrator coordinates both agents | **PARTIAL** — see limitations |
| 13 | Angular displays real backend results | **PASS** — 8 featured movies with ratings/genres/years loaded from the real gateway, zero console errors; search flow surfaced the real backend 503 message |

Additionally verified: CORS preflight from the frontend origin returns `200`
with the expected headers; `POST /api/recommendations` with no key returns a
clean `503` with no outbound Gemini call; `GET /api/recommendations/health`
reports `DEGRADED`.

### Limitations (reported honestly)

- **No Gemini API key was available**, so a **real LLM recommendation was NOT
  executed end-to-end**. Items 10–12 are only verified through automated tests
  with a mocked `ChatModel` (structured-output parsing, one call per agent,
  execution order via Mockito `InOrder`, invented-ID rejection, hard filters,
  trace). With a key set, the workflow is implemented but was not exercised
  against the live Gemini service here.
- The dataset is a **26-movie development seed** (`V2__seed_movies.sql`,
  mirrored in `dataset/movies.csv`), not the optional 100–500 movie dataset.
  The CSV import runner described in the original specification is **not
  implemented**; `dataset/movies.csv` is the canonical seed reference.
- Poster URLs in the seed are `placehold.co` placeholders, not real artwork.
- Authentication/authorisation is intentionally absent (MVP).
- The Docker frontend image injects the gateway URL at **build time**; if you
  change `GATEWAY_PORT` you must `docker compose build frontend` again.
- Docker Compose does not include Ollama; it must run on the host.
- Host ports must be free; override them via `.env` if another service occupies
  8080/8081/4200/3306.

**Full end-to-end recommendation with real Gemini output is therefore NOT
claimed.** Everything else in the table above was tested against the running
stack.

---

## 11. Credit policy

A normal recommendation uses at most **two** Gemini calls (one per agent) and
**one** Ollama embedding call (the query). `spring.ai.retry.max-attempts: 1`
disables Spring AI's default 10 retries. Movie embeddings are generated only
during ingestion/re-indexing, unchanged movies are `SKIPPED`, only the top 10
documents are retrieved, Agent 2 receives compact metadata, and hard filters
are applied in Java. No Gemini call is made when the key is missing. All tests
mock LLM and embedding clients.
