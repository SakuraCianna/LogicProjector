# Pas History Sidebar Design

## Goal

Upgrade the authenticated Pas home screen from a single-run flow into a working dashboard with a full left sidebar. The sidebar should let a teacher start a new walkthrough, reopen recent generation tasks, and reopen recent export tasks without losing the existing playback-oriented main workspace.

## Scope

- Add a persistent authenticated sidebar on desktop layouts.
- Show `New walkthrough`, `Recent generations`, and `Recent exports` in the sidebar.
- Load recent activity from Spring Boot, not local-only browser state.
- Let users click a recent generation to reopen its current detail state.
- Let users click a recent export to reopen the export detail together with its parent generation detail.
- Keep the existing main workspace model: code submission, generation status, completed walkthrough player, export status, and recovery flows.

## Out Of Scope

- Search, filtering, or pagination.
- A dedicated history route.
- Multi-user admin views.
- Real task cancellation.
- Mobile-specific navigation drawers beyond responsive stacking.

## Backend Design

Spring Boot should expose two authenticated recent-activity endpoints:

- `GET /api/generation-tasks/recent`
- `GET /api/export-tasks/recent`

Each endpoint returns a short list of the current user's most recently updated records, newest first. The initial limit is fixed in the backend to keep the contract simple.

Generation recent items should include enough data to label the sidebar row clearly:

- task id
- status
- detected algorithm
- summary
- source preview
- created/updated timestamps

Export recent items should include:

- export id
- generation task id
- status
- detected algorithm from the linked generation task when available
- created/updated timestamps

To support reopening an existing generation inside the current player, `GenerationTaskResponse` should also include `sourceCode` so the frontend can render code highlight without needing a second special-purpose detail endpoint.

## Frontend Design

The authenticated shell becomes a two-column workspace:

- left: full-height sidebar
- right: current main workspace

The sidebar contains:

- a primary `New walkthrough` action
- a `Recent generations` list
- a `Recent exports` list
- current user summary and logout action

Click behavior:

- `New walkthrough`: clear selected history item, clear current task/export state, return to editable source view while preserving the default/new editor behavior.
- recent generation item: load `getGenerationTask(id)`, set it as the active task, set `sourceCode` from the returned detail, and display the correct status/player view.
- recent export item: load `getExportTask(id)`, then load the parent generation via `getGenerationTask(generationTaskId)`, and show both in the main workspace.

## UX Rules

- Sidebar rows must show selection state.
- Empty recent lists should show a short empty-state line instead of blank space.
- The sidebar should remain visible while generation or export is in progress.
- Existing recovery behavior must continue to work even when the current task came from history instead of a fresh submission.

## Testing

Backend tests should cover the new recent endpoints and ensure results are filtered by user and sorted newest first.

Frontend tests should cover:

- recent lists loading after login restore
- selecting a recent generation
- selecting a recent export
- `New walkthrough` returning to editable mode from a history-selected task
