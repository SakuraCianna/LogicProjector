# Pas MVP

## Structure

- `SpringBackend`: business backend for users, tasks, billing, logs, recognition, and visualization payloads
- `VueFrontend`: teacher-facing walkthrough player and code submission UI
- `FastBackend`: future media worker for subtitles, TTS, ffmpeg composition, and export jobs

## Run backend

```bash
cd SpringBackend
mvn spring-boot:run
```

## Run frontend

```bash
cd VueFrontend
npm install
npm run dev
```

## Run Python export worker

```bash
cd FastBackend
venv\Scripts\python.exe -m uvicorn app.main:app --reload --port 8000
```

## RabbitMQ quick check

- RabbitMQ is expected at `localhost:5672`
- Management UI is expected at `http://localhost:15672`
- Queue health API after Spring starts: `GET http://localhost:8080/health/queues`

## End-to-end local checklist

1. Make sure RabbitMQ is running on `localhost:5672`
2. Start Spring Boot:

```bash
cd SpringBackend
mvn spring-boot:run
```

3. Start FastBackend worker:

```bash
cd FastBackend
venv\Scripts\python.exe -m uvicorn app.main:app --reload --port 8000
```

4. Start Vue frontend:

```bash
cd VueFrontend
npm install
npm run dev
```

5. Open the app in the browser and submit a Java algorithm snippet
6. Check generation queue state:

```text
GET http://localhost:8080/health/queues
```

7. Wait for generation status to become `COMPLETED`
8. Click `Export video`
9. Poll export status in the UI until `COMPLETED`
10. Download the final `.mp4`

## Focused verification commands

```bash
cd SpringBackend
mvn test
```

```bash
cd VueFrontend
npm run test -- --run
npm run build
```

```bash
cd FastBackend
venv\Scripts\python.exe -m pytest tests/test_frame_renderer.py tests/test_video_compositor.py tests/test_export_pipeline.py -v
```

## Demo account

- email: `teacher@example.com`
- credits: `300`
