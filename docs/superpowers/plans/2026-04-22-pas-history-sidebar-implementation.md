# Pas History Sidebar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a full authenticated sidebar with recent generation/export history and make the main workspace reopen those records.

**Architecture:** Extend Spring Boot with fixed-size recent history endpoints and enrich generation detail with source code. Extend the Vue shell so the authenticated layout becomes a sidebar workspace that can load recent generation/export records into the existing submission, status, and player views.

**Tech Stack:** Spring Boot 3.5, Java 21, Spring MVC, Spring Data JPA, Vue 3, TypeScript, Vitest, Vue Test Utils

---

## File Structure

- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskRepository.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskRepository.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/api/GenerationTaskResponse.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/generation/api/GenerationTaskListItemResponse.java`
- Create: `SpringBackend/src/main/java/com/LogicProjector/exporttask/api/ExportTaskListItemResponse.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskService.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskService.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/generation/GenerationTaskController.java`
- Modify: `SpringBackend/src/main/java/com/LogicProjector/exporttask/ExportTaskController.java`
- Modify: `SpringBackend/src/test/java/com/LogicProjector/generation/GenerationTaskControllerTest.java`
- Modify: `SpringBackend/src/test/java/com/LogicProjector/exporttask/ExportTaskControllerTest.java`
- Modify: `VueFrontend/src/types/pas.ts`
- Modify: `VueFrontend/src/api/pasApi.ts`
- Modify: `VueFrontend/src/App.vue`
- Modify: `VueFrontend/src/App.spec.ts`
- Modify: `VueFrontend/src/style.css`

## Tasks

### Task 1
- [ ] Add failing Spring controller tests for `GET /api/generation-tasks/recent` and `GET /api/export-tasks/recent`.
- [ ] Run the focused Spring controller tests and confirm they fail.
- [ ] Add recent-item DTOs, repository queries, service methods, and controller endpoints.
- [ ] Extend `GenerationTaskResponse` to carry `sourceCode`.
- [ ] Re-run focused Spring controller tests and make them pass.

### Task 2
- [ ] Add failing Vue tests for recent lists loading after auth restore and for reopening a recent generation/export from the sidebar.
- [ ] Run focused Vue tests and confirm they fail.
- [ ] Extend frontend types and API helpers with `getRecentGenerationTasks()` and `getRecentExportTasks()`.
- [ ] Refactor `App.vue` into an authenticated sidebar shell with history loading and selection handlers.
- [ ] Re-run focused Vue tests and make them pass.

### Task 3
- [ ] Add or extend Vue tests for `New walkthrough` from a selected history item and empty recent-list states.
- [ ] Run focused Vue tests and confirm they fail.
- [ ] Add sidebar styling, active-row styling, and responsive stacking behavior.
- [ ] Re-run the full Vue test suite and build.
- [ ] Run Spring tests for the touched controller surface.

## Verification

- `mvn -q -Dtest=GenerationTaskControllerTest,ExportTaskControllerTest test` in `SpringBackend`
- `npm run test -- --run src/App.spec.ts` in `VueFrontend`
- `npm run test -- --run` in `VueFrontend`
- `npm run build` in `VueFrontend`
