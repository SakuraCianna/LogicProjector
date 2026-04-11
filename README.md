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

## Demo account

- email: `teacher@example.com`
- credits: `300`
