# CineMind AI — Office Laptop Setup Guide

> Audience: an engineer receiving this project on a **new/corporate laptop**.
> Primary instructions are for **Windows 11 + PowerShell** (the environment the
> project was built and verified on). macOS/Linux differences are called out.
>
> For architecture and design, see [`SYSTEM_DOCUMENT.md`](SYSTEM_DOCUMENT.md).
> For a concise reference, see the root [`README.md`](../README.md).

---

## 0. Read this first — important facts about this project

| Fact | Consequence |
| --- | --- |
| **The project is hosted on GitHub** (private): `https://github.com/ShreeanshSachan/cinemind.git` | On the laptop, `git clone` it (Section 2.1). A ZIP copy is a fallback if GitHub is blocked. |
| **`.env` is gitignored and is NOT in the repo** | Create a fresh `.env` on the laptop from `.env.example`. Never move API keys on USB/email. |
| **Java 17 only** — Java 21/25 will not work | Install exactly JDK 17 and set `JAVA_HOME`. |
| **Docker builds need internet** (Docker Hub, Maven Central, npm) | On a locked-down laptop, see Section 5 (offline transfer). |
| **Ollama runs on the HOST, not in Docker** | Install it and pull `nomic-embed-text` (~274 MB) before starting Compose. |
| **A Gemini API key is optional to run** | Without it the stack starts fully; only `POST /api/recommendations` returns a clean `503`. Everything else works. |
| **Frontend API URL is baked in at image build time** | If you change `GATEWAY_PORT`, you must `docker compose build frontend` again. |
| **Default ports 8080, 8081, 4200, 3306 may be occupied** on corporate laptops (VPN, proxies, other apps) | Override host ports in `.env` (Section 4.3). |

**Estimated disk:** ~2–4 GB for Docker images, ~300 MB Ollama model,
~500 MB Maven/npm caches.

---

## 1. Prerequisites — install these first

| Tool | Required version | Where | Notes |
| --- | --- | --- | --- |
| JDK | **17** (e.g. Eclipse Temurin 17, Oracle JDK 17) | <https://adoptium.net/> | Must be a JDK, not a JRE. Needed for the Maven Wrapper. |
| Node.js | **24 LTS** (npm 11) | <https://nodejs.org/> | Only needed for local frontend dev/tests; Docker builds it inside the image. |
| Docker Desktop | Recent, with **Compose v2** | <https://www.docker.com/products/docker-desktop/> | Corporate machines may require a **paid Docker Desktop licence** — check with IT. |
| Ollama | Recent | <https://ollama.com/download> | Runs on the host at `:11434`. |
| Git | Any | <https://git-scm.com/> | Required to clone the repository. |
| `curl` / `tar` | built into Windows 10+/PowerShell | — | Used for verification commands below. |
| MySQL 8 | — | — | **Not required on the laptop** if you use Docker Compose (it runs MySQL in a container). |

Optional but handy: an IDE (IntelliJ IDEA / VS Code), and
<https://aistudio.google.com/app/apikey> access for a Gemini key.

### 1.1 Verify installs (PowerShell)

```powershell
java -version          # must show 17.x
node -v                # v24.x
npm -v                 # 11.x
docker --version
docker compose version
ollama --version
```

### 1.2 Set `JAVA_HOME` (Windows)

The Maven Wrapper **requires** `JAVA_HOME`. Set it permanently:

```powershell
# Find the JDK path first
Get-ChildItem 'C:\Program Files\Java', 'C:\Program Files\Eclipse Adoptium' -ErrorAction SilentlyContinue

# Set permanently for your user (edit the path to match)
[Environment]::SetEnvironmentVariable('JAVA_HOME','C:\Program Files\Java\jdk-17','User')
[Environment]::SetEnvironmentVariable('Path', $env:Path + ';%JAVA_HOME%\bin','User')
```

**Restart PowerShell** afterwards, then verify:

```powershell
$env:JAVA_HOME
java -version
```

macOS/Linux:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS
# or: export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64   # Linux
echo 'export JAVA_HOME=...' >> ~/.zshrc
```

---

## 2. Get the project on the laptop

### 2.1 Clone from GitHub (recommended)

The repository is private, so authenticate first — use Git Credential Manager
(browser sign-in on first push/pull) or the GitHub CLI:

```powershell
gh auth login          # or let Git Credential Manager prompt on first access

# Choose a path WITHOUT spaces if possible (simplifies Docker/JVM tooling)
New-Item -ItemType Directory -Force -Path 'C:\dev' | Out-Null
cd C:\dev
git clone https://github.com/ShreeanshSachan/cinemind.git cinemind-ai
cd C:\dev\cinemind-ai
git status -sb          # should show "## main...origin/main" and a clean tree
Get-ChildItem -Force
```

Expected top-level: `backend/`, `frontend/`, `database/`, `dataset/`,
`docker-compose.yml`, `.env.example`, `.dockerignore`, `.gitignore`,
`.gitattributes`, `README.md`, `mvnw`, `mvnw.cmd`, `.mvn/`, `docs/`.

> `.gitattributes` pins all text files to LF (only `.cmd`/`.bat` to CRLF).
> This matters on Windows where `core.autocrlf=true` would otherwise convert
> `mvnw` to CRLF and break the Linux-based Docker build. Do not delete it.

### 2.2 Fallback: clean ZIP (if GitHub is blocked on the corporate network)

Do **not** copy build outputs or secrets. Run this on the source machine:

```powershell
$src   = 'C:\Users\Shreeansh Sachan\Desktop\cinemind'
$stage = Join-Path $env:TEMP 'cinemind-ai-stage'
$zip   = Join-Path $env:USERPROFILE 'Desktop\cinemind-ai.zip'

Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
# /XD excludes build dirs anywhere in the tree; /XF excludes secrets & logs.
robocopy $src $stage /E /XD target node_modules dist .angular coverage .git `
         /XF .env *.log *.err /NFL /NDL /NJH /NJS /NP | Out-Null
Compress-Archive -Path $stage -DestinationPath $zip -Force
Write-Output "Created $zip"
```

`robocopy` returning a non-zero "exit code" here is normal (1 = files copied).

> **Check before transferring:** open the ZIP and confirm there is **no** `.env`,
> no `target/`, no `node_modules/`, and no `dist/`. The `.env.example` template
> **should** be there.

### 2.3 Extract the ZIP (only if you used 2.2)

```powershell
New-Item -ItemType Directory -Force -Path 'C:\dev' | Out-Null
Expand-Archive -Path "$env:USERPROFILE\Desktop\cinemind-ai.zip" -DestinationPath 'C:\dev'
Rename-Item 'C:\dev\cinemind-ai-stage' 'cinemind-ai'
cd C:\dev\cinemind-ai
Get-ChildItem -Force
```

### 2.4 Verify the copy (both paths)

```powershell
cd C:\dev\cinemind-ai
Test-Path .env                      # must be False - create it in Section 4
git status --porcelain              # must be empty (clean checkout)
git check-ignore -v .env            # confirms .env is ignored
```

`.gitignore` excludes `.env`, `target/`, `node_modules/`, `dist/`, `.angular/`,
`*.log`, `*.key`, `*.pem`, while keeping `.env.example` and the Maven Wrapper
jar. **No API keys are present in the repository.**

---

## 3. Install Ollama and download the embedding model

Ollama must run on the **host** because Docker Compose does not include it.

```powershell
# Install from https://ollama.com/download, then:
ollama pull nomic-embed-text
ollama list
curl.exe http://localhost:11434/api/tags
```

You should see `nomic-embed-text:latest` with `embedding_length: 768`.

Leave `ollama serve` running (the desktop app does this automatically). The RAG
container reaches it via `http://host.docker.internal:11434`
(`extra_hosts: host.docker.internal:host-gateway` is already in Compose).

**Corporate network:** `ollama pull` needs outbound HTTPS. If blocked, see
Section 5.3.

---

## 4. Configure the environment

```powershell
cd C:\dev\cinemind-ai
Copy-Item .env.example .env
notepad .env            # or: code .env
```

### 4.1 Minimum edits

| Variable | What to set |
| --- | --- |
| `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` | Replace `change-me*` with your own values |
| `GEMINI_API_KEY` | Your key from Google AI Studio — or **leave the placeholder** to run in `DEGRADED` mode |
| `GEMINI_MODEL` | `gemini-2.5-flash` (default) |
| `OLLAMA_BASE_URL` | `http://host.docker.internal:11434` for Docker; `http://localhost:11434` if running Java services directly on the host |
| `OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text` |
| `EMBEDDING_DIMENSION` | `768` |
| `CORS_ALLOWED_ORIGINS` | Must contain the browser origin, e.g. `http://localhost:4200` |

**Do not set `GEMINI_API_KEY=` to an empty string.** Spring AI needs a
non-blank value to build its client at startup. Use the placeholder (or omit
the line) — the app then starts in `DEGRADED` mode and refuses recommendation
requests with a clear `503`, without making any Gemini call.

### 4.2 Security rules

- Never commit or email `.env`.
- The Angular frontend contains **no** API keys; the Gemini key is read only by
  the AI Orchestrator container.
- Do not paste the key into `README`, tickets, screenshots or browser URLs.

### 4.3 Port conflicts (very common on office laptops)

Check before starting:

```powershell
foreach ($p in 8080,8081,8083,8084,8761,4200,3306) {
  $busy = [bool](Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue)
  Write-Output "$p busy=$busy"
}
```

If any are busy, override the **host** ports in `.env` (container-internal ports
stay the same and service discovery is unaffected):

```dotenv
GATEWAY_PORT=19080
MOVIE_SERVICE_PORT=19081
RAG_SERVICE_PORT=19083
ORCHESTRATOR_PORT=19084
DISCOVERY_SERVER_PORT=18761
FRONTEND_PORT=4300
MYSQL_PORT=3309
CORS_ALLOWED_ORIGINS=http://localhost:4300
```

Then note two consequences:

1. `API_BASE_URL` in the frontend image follows `GATEWAY_PORT` at **build time**
   — run `docker compose build frontend` after changing it.
2. `CORS_ALLOWED_ORIGINS` must include the new `FRONTEND_PORT` origin.

---

## 5. Corporate network / offline considerations

This is the section most likely to matter on an office laptop.

### 5.1 If the laptop has normal internet

Nothing special — `docker compose build` pulls base images and dependencies
from Docker Hub, Maven Central and the npm registry. A first build takes
roughly **5–10 minutes**.

### 5.2 If a corporate HTTP proxy is required

- **Docker Desktop:** Settings → *Resources → Proxies*, set HTTP/HTTPS proxy
  and `localhost,127.0.0.1` in the bypass list; restart Docker.
- **npm (local frontend work):**
  ```powershell
  npm config set proxy http://proxy.corp:8080
  npm config set https-proxy http://proxy.corp:8080
  ```
- **Maven (local backend work):** add a `<proxies>` block to
  `%USERPROFILE%\.m2\settings.xml`, or pass
  `MAVEN_OPTS=-Dhttps.proxyHost=proxy.corp -Dhttps.proxyPort=8080`.
- **Ollama:** set `HTTPS_PROXY` / `HTTP_PROXY` in the environment before
  `ollama pull`.
- **Gemini:** outbound access to Google's generative AI endpoint may be
  blocked. In that case the app still runs; recommendations return `503`.

### 5.3 If the laptop has NO internet (air-gapped / restricted)

Prepare on an internet-connected machine, then transfer:

```powershell
# 1) Build and start once on the connected machine
docker compose build
docker compose up -d

# 2) Save every image the stack uses into one tar
docker save (docker compose images -q) -o C:\Temp\cinemind-images.tar

# 3) On the office laptop, load them
docker load -i C:\Temp\cinemind-images.tar
```

Additional items to carry across:

| Item | Source | Destination on laptop |
| --- | --- | --- |
| Ollama model | `%USERPROFILE%\.ollama\models` | same path (or `~/.ollama/models`) |
| Maven cache (for local builds) | `%USERPROFILE%\.m2\repository` | same path |
| npm cache (for local builds) | `%LOCALAPPDATA%\npm-cache` | same path |

Then run `docker compose up -d` **without** `docker compose build` (images are
already loaded). Note the RAG container still needs the host Ollama process
running.

---

## 6. Start the stack (recommended path)

```powershell
cd C:\dev\cinemind-ai

# 0) Make sure Docker Desktop is running and Ollama is up
curl.exe http://localhost:11434/api/tags

# 1) Validate the compose file + your .env
docker compose config

# 2) Build all 7 images (skip if you loaded images offline)
docker compose build

# 3) Start everything
docker compose up -d

# 4) Wait until ALL services report (healthy)
docker compose ps
```

Startup order is enforced by health checks: MySQL → Discovery → Movie (applies
Flyway) → RAG → Orchestrator → Gateway → Frontend. Do not worry if a service
shows `health: starting` for 1–2 minutes.

Useful log commands:

```powershell
docker compose logs -f movie-service
docker compose logs -f rag-service
docker compose ps --format "table {{.Service}}\t{{.Status}}"
```

### 6.1 First-run data indexing

The database is seeded with **26 movies** automatically by Flyway. Embeddings
are **not** created automatically — index once:

```powershell
curl.exe -X POST http://localhost:8080/api/rag/index/all
```

Expected: `{"totalMovies":26,"created":26,"updated":0,"skipped":0,"modelName":"nomic-embed-text"}`.
Running it again should return `"skipped":26` (idempotent).

> If you changed `GATEWAY_PORT`, substitute it in every URL below.

---

## 7. Verify the installation (smoke checklist)

Run these from PowerShell on the laptop:

```powershell
$port = 8080            # change this to GATEWAY_PORT if you overrode it in .env
$B = "http://localhost:$port"

# 1) Eureka dashboard + registered services
curl.exe http://localhost:8761/         # or open in a browser
curl.exe -H "Accept: application/json" http://localhost:8761/eureka/apps

# 2) Gateway health
curl.exe "$B/actuator/health"

# 3) Movies from MySQL
curl.exe "$B/api/movies?page=0&size=2"

# 4) RAG health (Ollama reachable from inside the container)
curl.exe "$B/api/rag/health"

# 5) Semantic search - use a JSON file to avoid PowerShell quote mangling
'{"query":"mind bending psychological thriller on an island","topK":5}' |
  Set-Content -Path .\.search.json -Encoding ascii
curl.exe -X POST "$B/api/rag/search" -H "Content-Type: application/json" `
  --data-binary "@.search.json"
Remove-Item .\.search.json

# 6) Orchestrator health (UP with a key, DEGRADED without one)
curl.exe "$B/api/recommendations/health"
```

Then open the UI: **<http://localhost:4200>**

- Home page shows **Featured movies** with real ratings/genres/years.
- The Genre dropdown is populated from the real catalogue.
- Submitting a search opens the results page.
- The **Agent trace** link shows the real backend trace (empty until a search
  has run).

With a valid `GEMINI_API_KEY`:

```powershell
'{"query":"Suggest dark psychological thrillers after 2010","maxResults":5}' |
  Set-Content -Path .\.rec.json -Encoding ascii
curl.exe -X POST "$B/api/recommendations" -H "Content-Type: application/json" `
  --data-binary "@.rec.json"
Remove-Item .\.rec.json
```

Without a key, expect a clean `503` with
`"GEMINI_API_KEY is not configured. Set it in .env before requesting recommendations."`
— this is expected, not a failure of the setup.

---

## 8. Alternative: run services individually (no Docker)

Useful for debugging. Requires MySQL reachable on the host.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:MYSQL_HOST = 'localhost'
$env:MYSQL_PORT = '3306'
$env:MYSQL_DATABASE = 'cinemind'
$env:MYSQL_USER = 'cinemind'
$env:MYSQL_PASSWORD = 'change-me'
$env:EUREKA_URL = 'http://localhost:8761/eureka/'
$env:OLLAMA_BASE_URL = 'http://localhost:11434'

# MySQL only (if you don't use the Compose mysql service)
docker run -d --name cinemind-mysql -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=change-me-root -e MYSQL_DATABASE=cinemind `
  -e MYSQL_USER=cinemind -e MYSQL_PASSWORD=change-me mysql:8.4
```

Open **one terminal per service**, in this order:

```powershell
.\mvnw.cmd -f backend/discovery-server/pom.xml spring-boot:run
.\mvnw.cmd -f backend/movie-service/pom.xml spring-boot:run
.\mvnw.cmd -f backend/rag-service/pom.xml spring-boot:run
.\mvnw.cmd -f backend/ai-orchestrator/pom.xml spring-boot:run
.\mvnw.cmd -f backend/api-gateway/pom.xml spring-boot:run
```

Frontend (dev server):

```powershell
cd frontend\cinemind-angular
npm ci
npm start -- --port 4200
```

For a local JVM run, `src/environments/environment.ts` must point
`apiBaseUrl` at `http://localhost:8080` (already the default).

---

## 9. Run the tests

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
.\mvnw.cmd -f backend/pom.xml clean package      # 149 backend tests, all 5 modules

cd frontend\cinemind-angular
npm test -- --watch=false                        # 23 Angular tests
npm run build                                   # production bundle
```

All tests mock LLM/embedding clients — **no Gemini credits are consumed**.

> **Windows Smart App Control note:** on some corporate images, `ng test` and
> `ng serve` fail with *"An Application Control policy has blocked this file"*
> for Rollup's native `.node` binary. `ng build` is unaffected. Workaround
> (environment-only, not a project dependency):
> ```powershell
> npm install --no-save @rollup/wasm-node@4.68.2
> Rename-Item node_modules\rollup _rollup_native_backup
> Copy-Item node_modules\@rollup\wasm-node node_modules\rollup -Recurse
> npm test -- --watch=false
> Remove-Item node_modules\rollup -Recurse -Force
> Rename-Item node_modules\_rollup_native_backup rollup
> ```
> Ask IT before changing application-control policy; do not disable it.

---

## 10. Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `JAVA_HOME not found` / `.\mvnw.cmd` fails | JDK 17 not installed or `JAVA_HOME` unset | Section 1.2; restart the terminal |
| `docker compose build` fails downloading | Proxy / no internet | Section 5.2 / 5.3 |
| `Bind for 0.0.0.0:8080 failed: port is already allocated` | Port conflict | Section 4.3, then `docker compose build frontend` |
| A container stays `unhealthy` | Startup still in progress, or DB/Eureka not ready | `docker compose logs -f <service>`; wait 1–2 min |
| Frontend `unhealthy` but page loads in browser | Busybox `wget`/IPv6 `::1` issue | Already fixed in Compose (`127.0.0.1/health`); ensure you use the current `docker-compose.yml` |
| `/api/rag/health` → `ollamaAvailable:false` | Ollama not running, model not pulled, or `host.docker.internal` unreachable | Start Ollama; `ollama pull nomic-embed-text`; verify `curl http://localhost:11434/api/tags`; check Docker Desktop proxy bypass |
| RAG indexing returns `503` "Ollama embedding failed" | Model missing / wrong dimension | `ollama list`; keep `EMBEDDING_DIMENSION=768` |
| `POST /api/recommendations` → `503 GEMINI_API_KEY is not configured` | No key supplied | Expected. Add a real key to `.env` and `docker compose up -d ai-orchestrator` |
| `POST /api/recommendations` → Gemini 401/403 | Invalid key or corporate firewall blocking Google | Verify the key; check proxy/firewall; DEGRADED mode is fine for demos |
| Angular shows "Cannot reach the CineMind API Gateway" | Gateway not up, wrong `GATEWAY_PORT`, or CORS origin mismatch | `docker compose ps`; check `CORS_ALLOWED_ORIGINS`; rebuild frontend image if `GATEWAY_PORT` changed |
| Movie Service fails at startup | MySQL not healthy yet | Wait; Compose already gates on `service_healthy`. Check `docker compose logs mysql` |
| `ng test` / `ng serve` blocked by Application Control | Windows policy | Section 9 workaround |

---

## 11. Stopping / cleanup

```powershell
docker compose down          # stop + remove containers and network (keeps DB volume)
docker compose down -v       # ALSO deletes the MySQL volume (all movies/embeddings)
docker compose logs --tail=50 <service>
```

To start again later: `docker compose up -d`, then re-run
`POST /api/rag/index/all` only if you removed the volume.

---

## 12. Quick reference card

| Thing | Default URL / command |
| --- | --- |
| Git repository (private) | `https://github.com/ShreeanshSachan/cinemind.git` (`main`) |
| Angular UI | <http://localhost:4200> |
| API Gateway | <http://localhost:8080> |
| Eureka dashboard | <http://localhost:8761> |
| Ollama | <http://localhost:11434> |
| Movie list | `GET /api/movies?page=0&size=20` |
| RAG health | `GET /api/rag/health` |
| Index catalogue | `POST /api/rag/index/all` |
| Semantic search | `POST /api/rag/search` |
| Recommendations | `POST /api/recommendations` |
| Orchestrator health | `GET /api/recommendations/health` |
| All tests | `.\mvnw.cmd -f backend/pom.xml clean package` + `npm test -- --watch=false` |
| Config template | `.env.example` → `.env` |
| Full docs | `README.md`, `docs/SYSTEM_DOCUMENT.md` |

---

## 13. Minimum path to a working demo (TL;DR)

1. Install JDK 17 (+ `JAVA_HOME`), Docker Desktop, Node 24, Ollama.
2. `ollama pull nomic-embed-text`.
3. `git clone https://github.com/ShreeanshSachan/cinemind.git C:\dev\cinemind-ai`.
4. `Copy-Item .env.example .env`; set MySQL passwords and (optionally) `GEMINI_API_KEY`.
5. `docker compose build` → `docker compose up -d` → wait for all `healthy`.
6. `curl.exe -X POST http://localhost:8080/api/rag/index/all`.
7. Open <http://localhost:4200>.
8. Optionally run the smoke checklist in Section 7.

**Without a Gemini key**, steps 1–8 still give you a fully working movie
catalogue, semantic RAG search and UI; only the final LLM recommendation step
is unavailable (clean `503`).
