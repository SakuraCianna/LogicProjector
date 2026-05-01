# MySQL-Only Docker Deployment Design

Date: 2026-05-01

## Goal

Move LogicProjector from local H2-based Spring runtime toward a cloud-server-friendly Docker deployment. The deployment must run the Vue frontend, Spring backend, FastAPI worker, MySQL, and RabbitMQ through Docker Compose.

## Decisions

- Use MySQL only for Spring runtime configuration.
- Do not keep H2 runtime configuration or H2 Maven dependency.
- Use database name `logic_projector`.
- Use MySQL account `root` with password `123456` for the initial design.
- In non-Docker environments, the user will handle database creation and readiness; implementation should not run MySQL-dependent tests locally.
- Do not run local Docker image builds during this work.
- Do not commit real `.env` files or secret API keys.

## Docker Services

`docker-compose.yml` will define five services:

- `mysql`: MySQL database service with persistent volume. On a fresh Docker volume, Compose initializes database `logic_projector`.
- `rabbitmq`: RabbitMQ broker with management UI for queue inspection.
- `fast-backend`: FastAPI media worker built from `FastBackend/`, including `ffmpeg` in the image.
- `spring-backend`: Spring Boot API built from `SpringBackend/`, connected to MySQL, RabbitMQ, and the FastAPI worker through Compose service names.
- `vue-frontend`: Vue app built into static assets and served by Nginx.

## Networking

Compose service names are the internal DNS contract:

- Spring connects to MySQL at `mysql:3306`.
- Spring connects to RabbitMQ at `rabbitmq:5672`.
- Spring calls FastBackend at `http://fast-backend:8000`.
- Nginx in `vue-frontend` proxies API requests to `http://spring-backend:8080`.

External ports:

- Vue/Nginx: host `80` to container `80`.
- Spring: optional host `8080` to container `8080` for direct API debugging.
- FastBackend: optional host `8000` to container `8000` for direct worker debugging.
- RabbitMQ management: optional host `15672` to container `15672`.
- MySQL: optional host `3306` to container `3306` if direct DB access is needed.

## File Sharing

FastBackend writes generated media into a shared Docker volume. Spring reads downloads from the same mounted volume.

- FastBackend output path in container: `/app/outputs`.
- Spring download root in container: `/app/outputs`.
- Compose volume name: `logicprojector_outputs`.

This preserves the existing Spring download model while making it work across containers.

## Spring Configuration

`SpringBackend/src/main/resources/application.yml` will be changed to environment-variable-driven MySQL configuration:

- `SPRING_DATASOURCE_URL`, defaulting to `jdbc:mysql://localhost:3306/logic_projector?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
- `SPRING_DATASOURCE_USERNAME`, defaulting to `root`
- `SPRING_DATASOURCE_PASSWORD`, defaulting to `123456`
- `SPRING_RABBITMQ_HOST`, defaulting to `localhost`
- `SPRING_RABBITMQ_PORT`, defaulting to `5672`
- `PAS_EXPORT_WORKER_BASE_URL`, defaulting to `http://localhost:8000`
- `PAS_EXPORT_DOWNLOAD_ROOT`, defaulting to the current local FastBackend outputs path for non-Docker development
- `PAS_AI_DEEPSEEK_API_KEY`, with no real secret committed
- `PAS_AUTH_JWT_SECRET`, with a safe local default or `.env` override

The Maven dependency changes are:

- Remove `com.h2database:h2`.
- Add `com.mysql:mysql-connector-j` as runtime dependency.

## Vue Configuration

The frontend should avoid hardcoding `http://localhost:8080` for cloud deployment. The API client should use same-origin paths by default, so browser calls go through Nginx:

- Browser requests `/api/...` and `/health/...`.
- Nginx proxies those paths to Spring.

For local non-Docker dev, the existing Vite dev server can proxy API calls to `http://localhost:8080`, or the API base can be controlled with a Vite environment variable. The implementation should prefer the smallest change that supports both cloud Docker and local dev.

## Nginx

`VueFrontend/nginx.conf` will:

- Serve Vue static files.
- Fall back to `index.html` for SPA routes.
- Proxy `/api/` to `spring-backend:8080/api/`.
- Proxy `/health/` to `spring-backend:8080/health/`.
- Preserve standard proxy headers.

## Transaction Design

Transaction changes should be targeted and minimal:

- Add `@Transactional(readOnly = true)` to read-only service methods.
- Keep generation creation, export creation, generation completion billing, export settlement, and refund paths in write transactions.
- Add pessimistic locking for account rows on balance-changing paths to avoid concurrent debit/freeze races.
- Do not introduce broad refactors or unrelated service splitting.

Likely repository addition:

- `UserAccountRepository.findByIdForUpdate(Long id)` using `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

Balance-changing flows should load the account through that locked method before mutating credits.

## Database Schema Strategy

For this implementation phase, keep Hibernate schema management simple:

- Continue with `spring.jpa.hibernate.ddl-auto=update` for first Docker deployment.
- Do not introduce Flyway or Liquibase yet.

After cloud deployment is stable, a later migration task can replace `ddl-auto=update` with explicit schema migrations.

## Secret Handling

Commit:

- `.env.example` with placeholder values.
- Compose references to environment variables.

Do not commit:

- Real `.env`.
- Real DeepSeek API key.
- Production JWT secret.

The temporary MySQL password `123456` is accepted by the user for initial setup, but README should clearly mark it as a deployment value that should be changed for public production use.

## Verification Plan

Do not run local Docker image builds.

Do not run MySQL-dependent tests locally.

Allowed checks:

- Spring compile/package without tests: `mvn -DskipTests package` from `SpringBackend`.
- Vue unit tests: `npm run test -- --run` from `VueFrontend`.
- Vue production build: `npm run build` from `VueFrontend`.
- FastBackend tests with the existing conda environment: `D:\anaconda3\envs\logicprojector-fast\python.exe -m pytest tests -v` from `FastBackend`.
- Static inspection of Docker files and config references.

If a check cannot run because a local dependency is unavailable, report the exact blocker instead of substituting a different verification.

## Out Of Scope

- Running Docker builds locally.
- Running MySQL integration tests locally.
- Manually creating the MySQL database on the user's machine or server.
- Introducing Flyway/Liquibase migrations.
- Reworking the three-app architecture.
- Moving FastBackend dependencies into Anaconda `base`.
