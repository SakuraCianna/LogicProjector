# Pas MVP Design

## Overview

Pas is an AI-assisted algorithm visualization product aimed at teachers. A user uploads Java algorithm code, the system automatically analyzes the code, recognizes the algorithm type, and generates a step-by-step visualization centered on data structure changes with code highlighting as a secondary aid. The long-term direction includes video export and later expansion into business-code visualization, but the MVP is limited to algorithm teaching.

The current workspace only contains early scaffolds: a minimal Vue frontend, a minimal Spring Boot backend, and a FastAPI environment. There is no existing business logic yet, so the MVP should be designed from scratch on top of those foundations rather than constrained by existing implementation details.

## Product Goal

The MVP solves a narrow teaching problem: teachers need a faster way to turn algorithm code into classroom-ready explanatory material. Instead of manually building slides or recording demonstrations, they can upload a Java implementation of a supported algorithm and receive a guided visualization that makes the algorithm process understandable.

The experience should resemble a Ponder-style guided explanation: the primary focus is the changing state of the data structure, while code highlighting, step labels, and playback controls help anchor the explanation. The first output is an interactive animated lesson. Video export is a later milestone layered on top of the same result.

## Target User

The primary user is a teacher preparing algorithm lessons. This user values correctness, replayability, and classroom usability more than novelty. The product should therefore optimize for trustworthy explanations, clean presentation, and low friction input.

Students, interview coaches, and enterprise trainers may become future users, but the MVP should not optimize for their broader needs yet.

## MVP Scope

The MVP includes the following:

- Upload or paste Java algorithm code.
- Fully automatic code analysis from the user's perspective.
- Automatic recognition of supported algorithm types.
- AI-generated explanation text, step labels, and teaching copy.
- Deterministic extraction of state transitions for visualization.
- A guided playback page with animation, step controls, and code highlighting.
- Credit-based billing support at the backend model level.
- Basic task tracking, billing history, and system logs.

The MVP explicitly does not include the following:

- Arbitrary programming language support.
- Full Spring Boot or business-system code visualization.
- Support for all Java coding styles.
- Complex 3D scenes or game-like visuals.
- Multi-user teaching management, course management, or enterprise permissions.
- First-milestone video export as a required launch feature.

## Supported Algorithms

The first release should support a controlled whitelist of foundational algorithms implemented in Java. The initial set should be:

- Bubble sort
- Selection sort
- Insertion sort
- Binary search
- Quick sort
- Merge sort

This set is broad enough to validate real teaching demand while still keeping the deterministic execution layer manageable.

## Core Experience

The core interaction should prioritize visual understanding over code reading.

- The main stage shows data structure changes such as array content, pointer positions, swaps, partitions, and recursive progress.
- A side panel shows the source code with the current relevant lines highlighted.
- A step panel shows a short explanation of what is happening in the current phase.
- Playback controls allow play, pause, step forward, step backward, and jump between steps.

The visual language should borrow the structure of Create's Ponder system, not its literal game style. The product should feel instructional and polished rather than playful or game-like.

## Automation Strategy

The product should appear fully automatic to the user, but the implementation must protect correctness with an internal confidence gate.

Recommended approach:

- The user uploads code without manually selecting an algorithm type.
- The system automatically analyzes the code and predicts the supported algorithm type.
- If recognition confidence is high enough, generation proceeds automatically.
- If recognition confidence is below threshold, the system does not fabricate an explanation and instead returns a clear unsupported-or-uncertain result.

This preserves the all-automatic experience the product wants to project while avoiding incorrect teaching output.

## System Architecture

The MVP should be structured into four main modules.

### 1. Code Input and Analysis

This module accepts Java source code, normalizes it, parses relevant structure, and identifies whether the code likely implements one of the supported algorithms. It produces:

- normalized source input
- detected algorithm type
- confidence score
- parser metadata needed by downstream modules

### 2. Explanation Generation

This module uses an LLM to generate teaching-facing content, including:

- short algorithm summary
- per-step explanation text
- step titles
- optional teaching notes

This module must not be responsible for factual execution state.

### 3. Deterministic Execution and State Extraction

This is the correctness core of the system. For supported algorithms, it maps code into a controlled execution model and extracts the state sequence needed for animation. The output should include:

- timeline of steps
- array or structure snapshots
- active indices or pointers
- compare and swap events
- recursion or merge context when applicable
- source-code highlight mapping for each step

This layer should stay deterministic wherever possible so the user sees stable, reproducible teaching output.

### 4. Visualization Player

This module renders the generated state sequence into a guided learning view. It consumes a unified result payload rather than raw source code analysis details. That separation is important because the same result payload can later feed video export.

## Data Flow

The end-to-end flow should be:

1. User submits Java code.
2. The backend creates a generation task.
3. The analysis module identifies a likely algorithm and computes confidence.
4. If confidence is sufficient, the explanation module and deterministic state extractor run.
5. The backend stores the result payload and updates task status.
6. The frontend loads the generated visualization and lets the teacher review the playback.
7. Billing records are written based on task consumption.

If analysis fails or confidence is too low, the task should move to a failed or unsupported state with a clear reason.

## Error Handling

The MVP should prefer explicit refusal over low-quality output.

Error cases to support:

- code cannot be parsed
- language is not Java
- algorithm is outside whitelist
- confidence below threshold
- LLM generation failure
- visualization payload generation failure

For each case, the user should receive a short readable message, while the system log captures diagnostic detail.

## Billing Model

Pas should use a credit-based billing system.

Internally, credit cost can be influenced by:

- LLM token usage
- rendering complexity
- runtime resource occupancy
- later video export cost

Externally, the user-facing rule should remain simple. The MVP should expose a small number of understandable charge categories instead of raw infrastructure metrics.

## Data Model

The MVP should use four core tables.

### users

Stores account identity, account state, and credit balance.

Suggested fields:

- id
- email or username
- password_hash or third-party auth identifier
- credits_balance
- status
- created_at
- updated_at

### generation_tasks

Tracks the full lifecycle of a single upload and generation attempt.

Suggested fields:

- id
- user_id
- source_code
- language
- detected_algorithm
- confidence_score
- status
- result_payload_path or result_payload_json
- video_path
- error_message
- created_at
- updated_at

### billing_records

Tracks all credit changes.

Suggested fields:

- id
- user_id
- task_id
- change_type
- credits_delta
- balance_after
- description
- created_at

### system_logs

Stores operational and debugging events without becoming the source of truth for task state.

Suggested fields:

- id
- task_id
- user_id
- log_level
- module
- message
- details
- created_at

## Testing Strategy

The MVP should verify correctness at the deterministic layer before worrying about polish.

Minimum testing expectations:

- parser tests for supported Java input shapes
- recognition tests for each supported algorithm
- confidence-gate tests for unsupported or ambiguous input
- deterministic state-sequence tests for each algorithm
- API tests for task creation and result retrieval
- frontend tests for playback of a known result payload

The highest-risk regressions will come from incorrect step extraction and incorrect mapping between execution state and highlighted code.

## Risks and Mitigations

### Risk: overpromising full automation

Mitigation: keep automation external, but gate output with confidence internally.

### Risk: LLM hallucination undermines trust

Mitigation: limit LLM to explanation text and do not let it invent execution state.

### Risk: scope expands into arbitrary code understanding

Mitigation: keep the MVP whitelist explicit and reject out-of-scope input.

### Risk: video generation delays MVP launch

Mitigation: make the interactive player the first deliverable and treat video export as a second milestone.

## Delivery Sequence

The implementation should proceed in this order:

1. Java algorithm input and generation task lifecycle.
2. Algorithm recognition and confidence gating.
3. Deterministic state extraction for the whitelist.
4. Visualization player for a stored result payload.
5. LLM-generated explanation text.
6. Billing and task history support.
7. Video export as a later milestone.

## Open Decisions Resolved In This Spec

The following decisions are now fixed for the MVP:

- target user is teachers
- output priority is interactive guided visualization, then later video export
- first language is Java only
- supported domain is foundational algorithms only
- automation model is full automatic recognition with internal confidence gating
- architecture is hybrid: LLM for explanation, deterministic core for execution state
- billing model is credits
- backend minimum data model is users, generation_tasks, billing_records, and system_logs

## Next Step

The next step after spec approval is to create an implementation plan that breaks the MVP into concrete backend, frontend, parsing, state-engine, and testing milestones.
