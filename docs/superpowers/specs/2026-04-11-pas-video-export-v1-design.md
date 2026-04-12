# Pas Video Export V1 Design

## Overview

This document defines the first export-oriented video pipeline for Pas. The scope is intentionally narrow: turn an already generated walkthrough into an asynchronous export task that produces a final video with subtitles and TTS voiceover.

The existing system already generates deterministic visualization payloads and stores generation task state in Spring Boot. Video export v1 should build on top of that result instead of re-analyzing source code or re-running algorithm recognition.

## Goal

Allow a teacher to click one export button after a walkthrough has been generated, wait for an asynchronous task to complete, and then download a video artifact containing:

- walkthrough visuals
- subtitles
- TTS voiceover

The user should not need to configure detailed export settings in v1. System defaults should control video style, subtitle style, TTS voice, resolution, and frame rate.

## Out Of Scope

Video export v1 does not include:

- direct frontend access to Python worker APIs
- custom subtitle styling controls
- voice selection controls
- background music
- multiple video resolutions
- websocket progress streaming
- advanced retry orchestration
- asset storage redesign

## Architecture

The export flow uses three layers.

### 1. Vue Frontend

Vue owns the teacher-facing export entry point and status display.

- shows an "Export video" action on completed walkthroughs
- creates export tasks through Spring Boot
- polls export task state
- shows completion or failure
- exposes a final download action

Vue must not call Python directly.

### 2. Spring Boot Business Backend

Spring Boot remains the only public application backend and system of record.

It owns:

- export task creation
- validation that the source generation task is complete
- credit freezing and final settlement
- export task status updates
- billing records
- operational logs
- download authorization
- internal calls to Python worker

### 3. Python Media Worker

Python owns media production work only.

It handles:

- subtitle generation from walkthrough content
- TTS generation from narration content
- ffmpeg composition
- final media artifact paths

Python does not own users, login, billing, permissions, or export task truth.

## Export Model

Video export must use a separate task model from walkthrough generation.

One `generation_task` can produce zero, one, or many `export_task` records. This keeps generation lifecycle separate from export lifecycle and allows future re-export support without mutating the original walkthrough result.

### Export Task States

- `PENDING`: export task created and waiting to start
- `PROCESSING`: Python worker is generating subtitles, audio, or final video
- `COMPLETED`: final video is ready for download
- `FAILED`: export terminated with an error

`CANCELLED` is not required for v1.

## Spring Boot Public API

Video export v1 should expose exactly three public endpoints.

### 1. Create Export Task

`POST /api/generation-tasks/{taskId}/exports`

Purpose:

- create a new export task for an existing completed walkthrough

Request body for v1 can be empty or minimal because all export options use system defaults.

Recommended response:

```json
{
  "id": 101,
  "generationTaskId": 42,
  "status": "PENDING",
  "progress": 0,
  "creditsFrozen": 18
}
```

### 2. Query Export Task

`GET /api/export-tasks/{exportTaskId}`

Recommended response shape:

```json
{
  "id": 101,
  "generationTaskId": 42,
  "status": "PROCESSING",
  "progress": 45,
  "videoUrl": null,
  "subtitleUrl": null,
  "audioUrl": null,
  "errorMessage": null,
  "creditsFrozen": 18,
  "creditsCharged": null,
  "createdAt": "2026-04-11T16:00:00Z",
  "updatedAt": "2026-04-11T16:00:12Z"
}
```

### 3. Download Final Video

`GET /api/export-tasks/{exportTaskId}/download`

Purpose:

- stream the local file or redirect to a stored object URL

For v1, the exact storage backend can remain simple as long as Spring Boot controls access.

## Spring Boot To Python Worker API

The internal worker API should stay intentionally small.

### Worker Entry Point

`POST /exports`

Spring Boot sends a complete export snapshot so the worker can operate without querying business state.

Recommended request payload:

```json
{
  "exportTaskId": 101,
  "generationTaskId": 42,
  "algorithm": "QUICK_SORT",
  "summary": "Quick sort picks a pivot, partitions the array, and recursively sorts both sides.",
  "visualizationPayload": {
    "algorithm": "QUICK_SORT",
    "steps": []
  },
  "sourceCode": "public class QuickSort { ... }",
  "subtitleEnabled": true,
  "ttsEnabled": true
}
```

Recommended worker response:

```json
{
  "status": "COMPLETED",
  "progress": 100,
  "videoPath": "outputs/101.mp4",
  "subtitlePath": "outputs/101.srt",
  "audioPath": "outputs/101.mp3",
  "tokenUsage": 1462,
  "renderSeconds": 38,
  "concurrencyUnits": 1,
  "errorMessage": null
}
```

The worker should return resource-usage facts, not billing decisions.

## Data Model

Add one new table.

### `export_tasks`

Suggested fields:

- `id`
- `generation_task_id`
- `user_id`
- `status`
- `progress`
- `video_path`
- `subtitle_path`
- `audio_path`
- `credits_frozen`
- `credits_charged`
- `error_message`
- `created_at`
- `updated_at`

Supporting tables remain unchanged in role:

- `users` stores balances
- `generation_tasks` stores walkthrough outputs
- `billing_records` stores export freezes and settlement records
- `system_logs` stores worker and export process logs

## Frontend Flow

The frontend flow should remain simple.

1. Teacher opens a completed walkthrough.
2. Teacher clicks `Export video`.
3. Vue calls `POST /api/generation-tasks/{taskId}/exports`.
4. Vue receives `exportTaskId` and starts polling.
5. Vue calls `GET /api/export-tasks/{exportTaskId}` every few seconds.
6. When status becomes `COMPLETED`, Vue shows a download button.
7. If status becomes `FAILED`, Vue shows the readable failure message.

v1 should use polling instead of websocket updates.

## Backend Flow

The backend lifecycle should be:

1. Validate the generation task exists.
2. Validate the generation task status is `COMPLETED`.
3. Estimate export cost and freeze credits.
4. Create `export_task` as `PENDING`.
5. Call Python worker with export snapshot.
6. Update task to `PROCESSING`.
7. Receive worker result.
8. If successful:
   - store output paths
   - set status to `COMPLETED`
   - settle frozen credits against actual cost
9. If failed:
   - set status to `FAILED`
   - release frozen credits
   - store readable error message

## Cost And Billing

The pricing model for video export v1 is fixed conceptually even if the coefficients change later.

Actual cost formula:

`cost = model tokens + render seconds + concurrency occupancy`

Billing strategy:

- freeze estimated credits when export task is created
- calculate actual cost after worker completion
- settle using actual worker-reported usage
- refund any unused frozen amount
- collect any additional amount if estimate was low enough to require it

The Python worker should only return usage facts:

- token usage
- render seconds
- concurrency units

Spring Boot should convert those values into billing entries.

## Failure Modes

Readable failure categories for v1 should include:

- `GENERATION_NOT_READY`
- `INSUFFICIENT_CREDITS`
- `WORKER_UNAVAILABLE`
- `SUBTITLE_FAILED`
- `TTS_FAILED`
- `VIDEO_COMPOSE_FAILED`

Each failure must produce:

- a user-facing short message
- a stored `error_message` on `export_task`
- a detailed `system_logs` entry for diagnosis

## Defaults

To keep v1 simple, all export settings should be fixed system defaults:

- subtitle generation enabled
- TTS enabled
- one default voice
- one default video size
- one default frame rate
- one default subtitle style

No per-export customization is required in v1.

## Testing Strategy

The first export-oriented implementation should verify:

- export task creation only works for completed generation tasks
- export task state transitions are valid
- worker request payload contains the required snapshot fields
- worker result updates paths, progress, and final state correctly
- credit freezing and settlement are correct for success and failure
- polling endpoint returns stable progress states
- download endpoint rejects unfinished tasks

## Risks And Mitigations

### Risk: export logic leaks into frontend

Mitigation: keep Vue limited to create, poll, and download flows.

### Risk: Python worker becomes a second business backend

Mitigation: keep all business truth in Spring Boot and pass only export snapshots to Python.

### Risk: cost drift between estimate and actual charge

Mitigation: use freeze-then-settle instead of fixed upfront billing.

### Risk: media generation failures confuse the user

Mitigation: keep failure categories explicit and expose readable task status.

## Next Step

After review, the next step is to write an implementation plan for export v1 covering:

- `export_tasks` persistence
- Spring Boot export APIs
- worker request and result models
- polling flow in Vue
- freeze and settlement billing logic
- media worker result handling
