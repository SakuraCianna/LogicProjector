# Pas Product Usability Polish Design

## Overview

This document defines the next productization pass for Pas after the MVP flow became functional end to end. The current system can already register and log in a user, submit Java code for walkthrough generation, play back the generated visualization, and export a video asynchronously. The main gap is not feature absence, but product usability: users can still end up in unclear states, see technical status without user guidance, hit failures without a direct recovery path, or lose confidence because the UI does not make the next action obvious.

This pass focuses on one job: make the single-user core journey feel intentional and recoverable without expanding product scope.

## Goal

Turn the current MVP into a more product-like single-session experience for a teacher using Pas for the first time.

Success means:

- the user can always tell which step of the journey they are in
- every major state has a clear next action
- generation and export provide explicit progress and result feedback
- failures are readable and recoverable in place
- auth expiry returns the user to a safe login state instead of leaving the UI half-broken

## In Scope

This design covers the following areas:

- login and registration interaction polish
- code submission validation and generation-state feedback
- generated walkthrough state polish
- export progress and result polish
- recoverable error handling for generation, export, and auth expiry
- a clear "start over" path inside the current session
- frontend tests for the main user-facing flow states

## Out Of Scope

This design does not include:

- task history or a user dashboard
- multi-task management
- route-level frontend restructuring
- backend task cancellation
- new algorithm support
- deployment, secret management, or environment configuration redesign
- websockets or streaming progress
- redesigning the export worker contract beyond what is needed for stable UX messaging

## Current Problem

The current frontend is functionally correct but still behaves like a stitched workflow rather than a finished product:

- the main page state is inferred from several refs and conditional blocks rather than one explicit UI phase
- loading states are incomplete, so users can click actions again while async work is already underway
- failures are shown, but recovery actions are not consistently paired with the failure
- auth expiry clears the token on mount, but protected action failures are not yet treated as a first-class product state
- once a walkthrough is generated, the product does not clearly present "export", "retry", and "start over" as deliberate user choices

The result is a product that works for the developer who knows the flow, but still feels fragile to a first-time user.

## Product Principle

This pass should follow one simple rule:

> The UI must always answer two questions: "What is happening now?" and "What can I do next?"

That principle is more important than adding new controls.

## Main State Model

The frontend should organize the page around one explicit primary view state. This does not require a state-machine library. A single derived `viewState` in `App.vue`, backed by the existing refs, is enough.

The allowed primary states are:

### `auth`

The user is unauthenticated or their session has expired. The page shows the auth panel and one relevant auth message if needed.

### `ready`

The user is logged in and can submit source code. No active generation or export is being presented.

### `generating`

The generation task has been created and the UI is polling for progress. The page shows generation status and blocks duplicate submission.

### `generated`

Generation finished successfully. The page shows the walkthrough player, summary, and export entry point.

### `exporting`

An export task exists and is still pending or processing. The walkthrough remains visible while export status is emphasized.

### `exported`

The export completed successfully. The walkthrough remains visible, and the download action becomes the primary call to action.

### `error-recoverable`

The most recent async action failed, but the current session still has enough context to recover locally. This state is used for generation failure, export failure, and selected network failures where a retry action is meaningful.

## State Transitions

The intended transitions are:

1. `auth -> ready`
   After successful login or successful `me()` restoration.

2. `ready -> generating`
   Immediately after generation task creation succeeds.

3. `generating -> generated`
   When polling returns `COMPLETED`.

4. `generating -> error-recoverable`
   When polling or initial generation request fails in a recoverable way.

5. `generated -> exporting`
   Immediately after export task creation succeeds.

6. `exporting -> exported`
   When export polling returns `COMPLETED`.

7. `exporting -> error-recoverable`
   When export polling or export creation fails in a recoverable way.

8. `generated | exporting | exported | error-recoverable -> ready`
   When the user chooses to start over with a fresh submission while staying logged in.

9. `ready | generating | generated | exporting | exported | error-recoverable -> auth`
   When a protected API call indicates authentication is no longer valid.

## Interaction Design

### Authentication

- Login and registration submit buttons must show a loading state and prevent double submission.
- Registration success should show a plain next-step message: the account was created and the user should log in.
- Login failure should prefer the backend's readable message when available.
- Auth expiry should trigger a one-time message such as "Login expired. Please sign in again." and return the page to the auth state.

### Code Submission

- Empty source code must be rejected on the client before calling the API.
- After submission succeeds, the page should transition immediately into a generation-in-progress state instead of waiting for the first poll result.
- While generating, the submit action must be disabled.
- The source code must remain in memory so the user can edit and resubmit after a failure.

### Generation In Progress

- The UI should show the backend status value and a user-facing explanation line.
- The explanation should translate technical states into user intent, for example: analyzing algorithm, generating explanation, or preparing result.
- The page should offer a local "start over" action that stops polling and returns to editable mode. This does not cancel the backend job; it only resets the frontend session.

### Generated Walkthrough

- The generated state should show the algorithm name, summary, player, explanation panel, code highlight panel, and primary actions.
- The two main actions are `Export video` and `Submit new code`.
- `Submit new code` should clear the current task and export state but preserve the editor content so the user can revise it before submitting again.

### Export In Progress

- Export should disable the export action immediately after task creation succeeds.
- The page should keep the walkthrough visible and add a strong export status card rather than switching to a separate waiting screen.
- The status card should make it clear that the user can stay on the page while export continues.

### Export Completed

- The download button should become the primary action.
- The page should show a concise success summary including export completion and billing facts already returned by the API, such as frozen credits and charged credits.
- The page may continue to expose a repeat export entry point if the current API already supports creating another export from the same generation task.

### Recoverable Failures

Generation failure behavior:

- show the readable failure reason near the generation section
- preserve the source code
- provide a direct retry path by letting the user submit again

Export failure behavior:

- show the readable failure reason near the export section
- preserve the generated walkthrough
- provide a direct retry path by letting the user create a new export task again

Generic network or unknown failures:

- show a safe fallback message
- pair the message with one concrete recovery action instead of only rendering a banner

### Messaging Rules

- Global error banners can remain, but each major module should also provide local feedback near the relevant action.
- The UI should avoid repeating the same error in multiple places at once.
- Backend status codes should not be the only message shown to the user; each state also needs a plain-language explanation.

## Technical Design

### Frontend Structure

This pass should keep the current component structure mostly intact. The main orchestration remains in `VueFrontend/src/App.vue`.

Recommended changes:

- introduce a small derived `viewState`
- centralize reset helpers for generation, export, and auth recovery
- centralize polling start/stop logic so interval cleanup is consistent
- centralize failure handling so auth failures, business failures, and generic network failures can diverge intentionally

This should stay a minimal refactor, not a rewrite. The objective is clarity of behavior, not architectural purity.

### API Client Behavior

`VueFrontend/src/api/pasApi.ts` should become the single place that normalizes HTTP failure handling.

Recommended behavior:

- parse backend JSON messages when available
- throw a recognizable auth-expired error for 401 and 403 responses
- preserve readable business error messages for 4xx and 422 responses
- fall back to stable generic messages for malformed or unavailable responses

This allows the page layer to handle auth expiry as a specific product flow instead of special-casing each API call ad hoc.

### Backend Expectations

Spring Boot should require only small adjustments, if any.

The preferred approach is:

- keep existing endpoints and response shapes
- keep the async generation and export flow unchanged
- only tighten error message consistency where frontend recovery depends on a stable readable message

This pass should not introduce new product concepts at the backend level.

## Testing Strategy

This pass should primarily add or update Vue tests because the behavior change is mostly UX and control-flow logic.

The key frontend cases are:

- login failure shows readable feedback
- registration success shows the expected next-step message
- submitting code moves the page into generating state immediately
- generation failure preserves recovery options
- export action becomes disabled while export is active
- export failure preserves the walkthrough and exposes retry
- auth-expired API failures return the page to auth state
- start-over actions clear the right state without wiping source code unnecessarily

Spring tests should only be expanded when needed to guarantee stable error semantics relied on by the frontend.

Verification commands for this pass:

- `mvn test` in `SpringBackend`
- `npm run test -- --run` in `VueFrontend`
- `npm run build` in `VueFrontend`

## Acceptance Criteria

This design is complete when all of the following are true:

- users can always see a coherent current state and next action
- generation and export actions cannot be double-submitted accidentally
- generation failure does not discard source code
- export failure does not discard the walkthrough result
- auth expiry returns the product to a safe login state with a readable message
- the main flow tests cover the new state transitions and recovery paths

## Rollout Guidance

Implementation should proceed in this order:

1. normalize API error handling
2. make primary page state explicit in `App.vue`
3. add loading, retry, and start-over actions
4. improve local success and failure messaging
5. update and expand frontend tests

This order reduces regressions because it stabilizes error semantics before adjusting the UI flow.
