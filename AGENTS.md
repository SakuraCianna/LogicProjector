# AGENTS.md

## Boundaries
- This repo has 3 standalone apps and no root task runner: `SpringBackend/`, `VueFrontend/`, `FastBackend/`.
- Real flow is `VueFrontend -> SpringBackend -> FastBackend`; the Vue app should only call Spring.
- Trace cross-app behavior from these files first:
  - Spring HTTP: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskController.java`, `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskController.java`
  - Spring async workers: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskListener.java`, `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskListener.java`
  - Spring -> worker bridge: `SpringBackend/src/main/java/com/LogicProjector/exporttask/worker/HttpMediaExportWorkerClient.java`
  - Vue workspace shell and history behavior: `VueFrontend/src/App.vue`
  - Vue API wiring and token storage: `VueFrontend/src/api/pasApi.ts`
  - Fast worker HTTP and pipeline: `FastBackend/app/main.py`, `FastBackend/app/services/export_pipeline.py`

## Commands
- Spring dev: `mvn spring-boot:run` from `SpringBackend`
- Spring compile without MySQL tests: `mvn -DskipTests package` from `SpringBackend`
- Spring tests: `mvn test` from `SpringBackend` only when a MySQL test database is available
- Single Spring test: `mvn -q -Dtest=ExportTaskServiceTest test` from `SpringBackend`
- Focused Spring business-flow check: `mvn -q "-Dtest=GenerationTaskControllerTest,ExportTaskControllerTest,ExportTaskServiceTest" test` from `SpringBackend`
- Vue dev: `npm run dev` from `VueFrontend`
- Vue tests: `npm run test -- --run` from `VueFrontend`
- Single Vue spec: `npm run test -- --run src/App.spec.ts` from `VueFrontend`
- Vue build: `npm run build` from `VueFrontend` (`vue-tsc -b` is part of this; there is no separate typecheck script)
- Fast worker dev: `D:\anaconda3\envs\logicprojector-fast\python.exe -m uvicorn app.main:app --reload --port 8000` from `FastBackend`
- Fast worker tests: `D:\anaconda3\envs\logicprojector-fast\python.exe -m pytest tests/test_frame_renderer.py tests/test_video_compositor.py tests/test_export_pipeline.py -v` from `FastBackend`
- Single Fast worker test: `D:\anaconda3\envs\logicprojector-fast\python.exe -m pytest tests/test_export_pipeline.py -v` from `FastBackend`
- Always use the dedicated `logicprojector-fast` conda env for `FastBackend`; do not install project dependencies into Anaconda `base`, and do not assume global `python` or `pytest`.

## Runtime Truths
- `SpringBackend/src/main/resources/application.yml` is the source of truth for local wiring, with environment variable overrides for Docker. RabbitMQ, MySQL, JWT, worker URL, download root, and AI settings are configured there.
- Do not commit real `.env` files, real DeepSeek keys, or production JWT secrets. Use `.env.example` as the safe template.
- Spring protects everything except `/api/auth/**` and `/health/**`; Vue stores the JWT in localStorage key `pas_token`.
- `README.md`, the listed demo account, and `scripts/local-smoke-test.ps1` still contain pre-auth/demo-flow assumptions in places (for example posting `userId`, calling protected routes without a Bearer token, or implying a seeded `teacher@example.com` account). Trust controllers and tests over those docs/scripts.
- Runtime generation/export depends on RabbitMQ. Local default is `localhost:5672`; Docker uses service name `rabbitmq`. Spring publishes to AMQP and `@RabbitListener`s do the work; polling status does not trigger processing.
- Spring uses MySQL only. Local default is database `logic_projector` with `root / 123456`; Docker uses the `mysql` service and persistent volume.
- Database schema is managed manually through `create.sql` or explicit SQL changes; Spring does not auto-create or auto-update tables.
- Vue API calls use same-origin `/api` paths by default. Vite dev proxies `/api` and `/health` to `http://localhost:8080`; Docker Nginx proxies them to Spring.
- The authenticated Vue home screen is now a sidebar workspace, not a simple single-form page. Recent activity is loaded from Spring via `/api/generation-tasks/recent` and `/api/export-tasks/recent`, and reopening a history item relies on `GenerationTaskResponse.sourceCode` being present.
- Spring download resolves files from `pas.export.download-root` first, then falls back to the raw worker-returned path.
- Export billing in Spring settles against worker-reported `tokenUsage + renderSeconds + concurrencyUnits`.
- Fast export needs `ffmpeg` on `PATH`. The ffmpeg-backed pytest modules skip when it is missing, and TTS tests monkeypatch `edge-tts`, so green tests do not prove live media generation end-to-end.

## Verification
- For cross-app export changes without a MySQL test database, verify Spring with `mvn -DskipTests package`, Vue `npm run test -- --run` and `npm run build`, Fast worker pytest command above.
- Do not run local Docker image builds when the user explicitly says not to; inspect Docker files statically instead.
- For auth/history/sidebar or ownership bugfixes, the highest-signal checks are Spring `GenerationTaskControllerTest`, `ExportTaskControllerTest`, `ExportTaskServiceTest` and Vue `src/App.spec.ts`.
- Queue health endpoint: `GET http://localhost:8080/health/queues`.

## Gotchas
- `FastBackend/outputs/` is ignored by `.gitignore`; do not commit generated media.
- If you change `UserAccount` constructor or fields, update Spring tests that instantiate `new UserAccount(...)` directly.
