# Local Acceptance Checklist

## Prerequisites

1. RabbitMQ is running on `localhost:5672`
2. Spring Boot is running on `localhost:8080`
3. FastBackend is running on `localhost:8000`
4. Vue frontend is running in the browser
5. DeepSeek key is configured in `SpringBackend/src/main/resources/application.yml`

## Manual Flow

1. Open the app
2. Paste a supported Java algorithm snippet such as quick sort or binary search
3. Submit generation request
4. Confirm the UI first shows `Generation status`
5. Confirm the UI eventually switches to the walkthrough player
6. Check code highlight, narration, play/pause, timeline, and speed controls
7. Click `Export video`
8. Confirm the UI shows `Export status`
9. Confirm export eventually completes and shows a download link
10. Download the `.mp4` and verify it opens normally

## Queue Checks

1. Open `GET http://localhost:8080/health/queues`
2. Confirm generation and export queues show consumer counts
3. During active work, confirm message counts move back toward `0`
4. Confirm dead-letter queues stay at `0` for a healthy run

## Failure Checks

1. Submit unsupported code and confirm generation eventually fails with a readable message
2. Force an export-side failure and confirm export status shows `FAILED`
3. Confirm failed exports do not leave frozen credits behind

## Expected Product Signals

- AI identifies supported algorithms instead of using hardcoded classification
- Summary and per-step narration are AI-generated but deterministic steps remain stable
- Generation and export both behave asynchronously
- Exported video includes styled frames, subtitles, and audio
