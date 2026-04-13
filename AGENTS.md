# AGENTS.md

## Repo Shape
- This repo is split into 3 independent apps. There is no root task runner.
- `SpringBackend/`: Spring Boot 3.5 + Java 21 business backend. Owns users, generation tasks, export tasks, billing, logs, algorithm recognition, and visualization payloads.
- `VueFrontend/`: Vue 3 + Vite SPA. Owns code submission, walkthrough playback, export button, polling UI, and download link rendering.
- `FastBackend/`: FastAPI media worker. Owns frame rendering, subtitle generation, TTS, and ffmpeg video composition.

## Current Product State
- Pas is no longer just a scaffold. The codebase now supports:
  - Java algorithm walkthrough generation in Spring Boot
  - Vue playback UI with code highlight and step controls
  - async-style export task flow in Spring Boot and Vue
  - Python worker media generation for frames, `.srt`, `.mp3`, and `.mp4`
- Export flow is `Vue -> SpringBoot -> FastBackend`; the frontend must not call Python directly.

## Commands
- Backend dev server: `cd SpringBackend && mvn spring-boot:run`
- Backend tests: `cd SpringBackend && mvn test`
- Single backend test: `cd SpringBackend && mvn -q -Dtest=ExportTaskServiceTest test`
- Frontend dev server: `cd VueFrontend && npm install && npm run dev`
- Frontend tests: `cd VueFrontend && npm run test -- --run`
- Single frontend spec: `cd VueFrontend && npm run test -- --run src/App.spec.ts`
- Frontend build: `cd VueFrontend && npm run build`
- Python worker dev server: `cd FastBackend && venv\Scripts\python.exe -m uvicorn app.main:app --reload --port 8000`
- Python worker tests: `cd FastBackend && venv\Scripts\python.exe -m pytest tests/test_frame_renderer.py tests/test_video_compositor.py tests/test_export_pipeline.py -v`

## Workflow Conventions
- User preference: work directly on `main` unless explicitly told otherwise.
- Use `venv\Scripts\python.exe`, not bare `python` or `pytest`, for `FastBackend` commands.
- Verify all 3 apps when changing cross-cutting export flow: Spring tests, Vue tests/build, and FastBackend pytest.

## Non-Obvious Runtime Facts
- There is no real auth yet. Frontend and export controller paths assume the demo user flow (`userId = 1` / `teacher@example.com`). Do not design changes assuming finished auth exists.
- Spring uses a file-backed H2 database: `SpringBackend/src/main/resources/application.yml` points to `jdbc:h2:file:./data/pas-mvp`. Local runs can keep state across restarts.
- Spring export processing is not on a real queue yet. `ExportTaskService.getExportTask(...)` triggers `processExportTask(...)` the first time a pending export is polled.
- Export pricing is currently settled in Spring from worker-reported usage as `tokenUsage + renderSeconds + concurrencyUnits`.
- FastBackend runtime export depends on `ffmpeg` being available on `PATH`.
- `TtsService` uses real `edge-tts` at runtime, but worker tests monkeypatch audio generation for determinism. Passing tests do not prove live network TTS works.

## File Pointers
- Export backend entrypoint: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskService.java`
- Export HTTP API: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskController.java`
- Frontend export wiring: `VueFrontend/src/App.vue`
- Worker HTTP entrypoint: `FastBackend/app/main.py`
- Worker pipeline: `FastBackend/app/services/export_pipeline.py`

## Gotchas
- Generated media goes under `FastBackend/outputs/` by default from the worker path logic, but that directory is not currently ignored in `.gitignore`. Do not commit generated media unless the user asks.
- Spring download logic resolves files from `pas.export.download-root` first, then falls back to the raw worker-returned path.
- If you change constructor signatures on `UserAccount`, update the Spring tests; several tests instantiate it directly.

## Relevant Design Docs
- Walkthrough MVP spec: `docs/superpowers/specs/2026-04-11-pas-mvp-design.md`
- Video export v1 spec: `docs/superpowers/specs/2026-04-11-pas-video-export-v1-design.md`
- Video export v1 plan: `docs/superpowers/plans/2026-04-11-pas-video-export-v1-implementation.md`
