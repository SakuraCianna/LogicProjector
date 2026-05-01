# MySQL Docker Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make LogicProjector deployable as a full Docker Compose stack with MySQL-only Spring configuration and targeted transaction hardening.

**Architecture:** Docker Compose runs MySQL, RabbitMQ, FastBackend, SpringBackend, and VueFrontend on one internal network. Vue is served by Nginx and proxies API calls to Spring; Spring talks to MySQL, RabbitMQ, and FastBackend by Compose service name. Spring runtime configuration is environment-variable-driven and no longer depends on H2.

**Tech Stack:** Spring Boot 3.5, MySQL Connector/J, Vue/Vite, Nginx, FastAPI/Uvicorn, Docker Compose, RabbitMQ, Maven, npm, pytest.

---

## File Map

- Create `docker-compose.yml`: full deployment graph, volumes, ports, service environment.
- Create `.env.example`: deploy-time environment template without real DeepSeek key or production JWT secret.
- Modify `.gitignore`: ignore `.env`, FastBackend outputs, Docker/local generated files.
- Create `SpringBackend/Dockerfile`: multi-stage Maven build and Java runtime image.
- Modify `SpringBackend/pom.xml`: remove H2 dependency, add MySQL runtime driver.
- Modify `SpringBackend/src/main/resources/application.yml`: MySQL-only, environment-variable-driven configuration.
- Modify `SpringBackend/src/main/java/com/LogicProjector/account/UserAccountRepository.java`: add pessimistic lock query.
- Modify Spring services/processors: use read-only transactions for reads and locked account loads for balance mutation.
- Create `FastBackend/Dockerfile`: Python image with ffmpeg and requirements installed.
- Modify FastBackend output config if needed: keep `/app/outputs` compatible with shared volume.
- Create `VueFrontend/Dockerfile`: build Vue assets and serve with Nginx.
- Create `VueFrontend/nginx.conf`: SPA fallback and API proxy to Spring.
- Modify `VueFrontend/src/api/pasApi.ts`: same-origin API base by default with optional Vite override.
- Modify `VueFrontend/vite.config.ts`: local dev proxy for `/api` and `/health` if present.
- Modify `README.md`: Docker deployment instructions and MySQL-only local notes.
- Modify `AGENTS.md`: update source-of-truth runtime notes and verification commands.

## Tasks

### Task 1: Docker Compose And Ignore Rules

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`
- Modify: `.gitignore`

- [ ] Add Compose services for `mysql`, `rabbitmq`, `fast-backend`, `spring-backend`, and `vue-frontend`.
- [ ] Add persistent volumes for MySQL, RabbitMQ, and FastBackend/Spring shared outputs.
- [ ] Add `.env.example` with `MYSQL_ROOT_PASSWORD=123456`, `MYSQL_DATABASE=logic_projector`, placeholder `PAS_AI_DEEPSEEK_API_KEY`, and placeholder `PAS_AUTH_JWT_SECRET`.
- [ ] Ignore `.env`, `FastBackend/outputs/`, build output directories, and Python caches.

### Task 2: Spring MySQL Runtime Configuration

**Files:**
- Modify: `SpringBackend/pom.xml`
- Modify: `SpringBackend/src/main/resources/application.yml`

- [ ] Remove the H2 runtime dependency.
- [ ] Add `com.mysql:mysql-connector-j` as a runtime dependency.
- [ ] Replace H2 datasource with MySQL datasource defaults: database `logic_projector`, user `root`, password `123456`.
- [ ] Remove H2 console configuration.
- [ ] Convert RabbitMQ, worker URL, download root, DeepSeek key, and JWT secret to environment-variable-driven values.
- [ ] Keep `spring.jpa.hibernate.ddl-auto=update` and `spring.jpa.open-in-view=false`.

### Task 3: Transaction And Account Locking Hardening

**Files:**
- Modify: `SpringBackend/src/main/java/com/LogicProjector/account/UserAccountRepository.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskProcessor.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskService.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskProcessor.java`

- [ ] Add `findByIdForUpdate(Long id)` with `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- [ ] Use the locked query in generation creation before balance checks.
- [ ] Use the locked query in export creation before freezing credits.
- [ ] Use the locked query in generation completion billing.
- [ ] Use the locked query in export settlement and refund paths.
- [ ] Add `@Transactional(readOnly = true)` to read-only service methods.
- [ ] Keep dispatch compensation and processor state transitions in existing transaction boundaries.

### Task 4: FastBackend Container

**Files:**
- Create: `FastBackend/Dockerfile`
- Optionally modify: `FastBackend/app/services/export_pipeline.py` if output path needs environment support.

- [ ] Build from a slim Python image.
- [ ] Install `ffmpeg` through apt.
- [ ] Install `FastBackend/requirements.txt`.
- [ ] Run `uvicorn app.main:app --host 0.0.0.0 --port 8000`.
- [ ] Ensure `/app/outputs` exists and is writable.

### Task 5: Vue Container, Nginx Proxy, And API Base

**Files:**
- Create: `VueFrontend/Dockerfile`
- Create: `VueFrontend/nginx.conf`
- Modify: `VueFrontend/src/api/pasApi.ts`
- Modify: `VueFrontend/vite.config.ts`

- [ ] Build Vue with Node and serve `dist/` from Nginx.
- [ ] Proxy `/api/` and `/health/` to `spring-backend:8080`.
- [ ] Use same-origin API paths in browser by default.
- [ ] Preserve local dev by configuring Vite proxy to `http://localhost:8080`.

### Task 6: Documentation Updates

**Files:**
- Modify: `README.md`
- Modify: `AGENTS.md`

- [ ] Document Docker production deployment with `docker compose up -d --build`, while noting this task does not run Docker builds locally.
- [ ] Document `.env` creation from `.env.example`.
- [ ] Document MySQL-only Spring configuration and `root / 123456` initial value.
- [ ] Update local verification commands to exclude MySQL tests unless MySQL is available.

### Task 7: Verification And Commit

**Commands:**
- `mvn -DskipTests package` from `SpringBackend`
- `npm run test -- --run` from `VueFrontend`
- `npm run build` from `VueFrontend`
- `D:\anaconda3\envs\logicprojector-fast\python.exe -m pytest tests -v` from `FastBackend`

- [ ] Do not run `docker compose build` locally.
- [ ] Do not run MySQL-dependent Spring tests locally.
- [ ] Run allowed verification commands.
- [ ] Commit implementation changes with a concise feature message.
- [ ] Push commits to `origin/main` if the working tree only contains intentionally uncommitted local files.

## Self-Review

- Spec coverage: Docker services, MySQL-only Spring config, no local Docker builds, no MySQL tests, shared export volume, same-origin frontend API, transaction locking, and docs are covered.
- Placeholder scan: no TBD/TODO implementation placeholders are present.
- Type consistency: repository method name `findByIdForUpdate(Long id)` is used consistently in transaction tasks.
