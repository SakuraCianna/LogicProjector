# Pas Product Usability Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Polish the existing Pas MVP flow so login, generation, playback, export, and recovery states feel product-like and recoverable for a first-time teacher user.

**Architecture:** Keep the current three-app architecture and existing API shapes. Concentrate the changes in the Vue frontend by normalizing API error handling, making the page's primary state explicit in `App.vue`, and tightening component contracts so loading, retry, and start-over behavior are driven deliberately instead of inferred from scattered refs.

**Tech Stack:** Vue 3, TypeScript, Vite, Vitest, Vue Test Utils, Spring Boot 3.5, Java 21

---

## File Structure

### Vue frontend files

- Modify: `VueFrontend/src/api/pasApi.ts`
  Normalize HTTP failure parsing and expose a recognizable auth-expired error.
- Create: `VueFrontend/src/api/pasApi.spec.ts`
  Unit tests for API error normalization.
- Modify: `VueFrontend/src/App.vue`
  Own the explicit page state, reset helpers, polling cleanup, retry flow, and auth-expiry recovery.
- Modify: `VueFrontend/src/App.spec.ts`
  Integration tests for the polished login, generation, export, retry, and start-over flows.
- Modify: `VueFrontend/src/components/AuthPanel.vue`
  Accept busy and message props instead of optimistic local success behavior.
- Create: `VueFrontend/src/components/AuthPanel.spec.ts`
  Focused tests for loading and message rendering.
- Modify: `VueFrontend/src/components/CodeSubmissionPanel.vue`
  Move the editor value under parent control so start-over and retry can preserve source code.
- Create: `VueFrontend/src/components/CodeSubmissionPanel.spec.ts`
  Focused tests for disabled submit and empty-input validation.
- Modify: `VueFrontend/src/components/GenerationStatusCard.vue`
  Render clearer plain-language copy and a local recovery slot area.
- Modify: `VueFrontend/src/components/TaskSummaryCard.vue`
  Render export busy state and a second primary action for submitting new code.
- Modify: `VueFrontend/src/components/ExportStatusCard.vue`
  Render distinct processing, completed, and failed messaging with retry and download affordances.
- Modify: `VueFrontend/src/style.css`
  Style loading, disabled, inline success, inline error, action rows, and status emphasis.

### Backend files

- No backend file changes are planned in the first pass.
  The existing Spring endpoints and status model are sufficient for this usability pass. Only revisit backend work if frontend implementation reveals a concrete mismatch in readable error semantics.

## Task 1: Normalize API Errors For UI Recovery

**Files:**
- Modify: `VueFrontend/src/api/pasApi.ts`
- Create: `VueFrontend/src/api/pasApi.spec.ts`

- [ ] **Step 1: Write the failing API error tests**

```ts
import { afterEach, describe, expect, it, vi } from 'vitest'

import { AuthExpiredError, createGenerationTask, login } from './pasApi'

describe('pasApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('throws AuthExpiredError for protected 403 responses', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      json: async () => ({ message: 'Forbidden' }),
    }))

    await expect(createGenerationTask('class Demo {}')).rejects.toBeInstanceOf(AuthExpiredError)
  })

  it('preserves readable backend messages for business failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 422,
      json: async () => ({ message: 'Unsupported algorithm or low confidence' }),
    }))

    await expect(login('teacher', 'bad-pass')).rejects.toThrow('Unsupported algorithm or low confidence')
  })
})
```

- [ ] **Step 2: Run the focused API test and verify it fails**

Run: `npm run test -- --run src/api/pasApi.spec.ts`
Expected: FAIL because `pasApi.ts` currently throws plain `Error` for every non-OK response and does not export `AuthExpiredError`.

- [ ] **Step 3: Implement normalized request helpers in `pasApi.ts`**

```ts
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export class AuthExpiredError extends ApiError {
  constructor(message = 'Login expired. Please sign in again.') {
    super(message, 403)
    this.name = 'AuthExpiredError'
  }
}

async function readErrorMessage(response: Response, fallback: string): Promise<string> {
  const payload = await response.json().catch(() => null)
  return typeof payload?.message === 'string' && payload.message.length > 0
    ? payload.message
    : fallback
}

async function requestJson<T>(url: string, init: RequestInit, fallback: string): Promise<T> {
  const response = await fetch(url, init)
  if (!response.ok) {
    const message = await readErrorMessage(response, fallback)
    if (response.status === 401 || response.status === 403) {
      throw new AuthExpiredError(message)
    }
    throw new ApiError(message, response.status)
  }

  return response.json() as Promise<T>
}
```

```ts
export async function me(): Promise<UserProfile> {
  return requestJson<UserProfile>('http://localhost:8080/api/auth/me', {
    headers: { ...authHeaders() },
  }, 'Auth check failed')
}

export async function createExportTask(taskId: number): Promise<CreateExportTaskResponse> {
  return requestJson<CreateExportTaskResponse>(`http://localhost:8080/api/generation-tasks/${taskId}/exports`, {
    method: 'POST',
    headers: { ...authHeaders() },
  }, 'Export creation failed')
}
```

- [ ] **Step 4: Re-run the focused API test and verify it passes**

Run: `npm run test -- --run src/api/pasApi.spec.ts`
Expected: PASS with both tests green.

- [ ] **Step 5: Commit the API normalization slice**

```bash
git add VueFrontend/src/api/pasApi.ts VueFrontend/src/api/pasApi.spec.ts
git commit -m "feat: normalize frontend api errors"
```

## Task 2: Make Auth And Submission Components Parent-Controlled

**Files:**
- Modify: `VueFrontend/src/components/AuthPanel.vue`
- Create: `VueFrontend/src/components/AuthPanel.spec.ts`
- Modify: `VueFrontend/src/components/CodeSubmissionPanel.vue`
- Create: `VueFrontend/src/components/CodeSubmissionPanel.spec.ts`

- [ ] **Step 1: Write the failing component tests**

```ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import AuthPanel from './AuthPanel.vue'

describe('AuthPanel', () => {
  it('disables submit and shows loading label while busy', () => {
    const wrapper = mount(AuthPanel, {
      props: {
        busy: true,
        errorMessage: '',
        successMessage: '',
      },
    })

    expect(wrapper.find('button[type="submit"]').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('Working...')
  })
})
```

```ts
import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'

import CodeSubmissionPanel from './CodeSubmissionPanel.vue'

describe('CodeSubmissionPanel', () => {
  it('shows inline validation for blank input', async () => {
    const wrapper = mount(CodeSubmissionPanel, {
      props: {
        modelValue: '   ',
        busy: false,
        errorMessage: '',
      },
    })

    await wrapper.find('form').trigger('submit')

    expect(wrapper.text()).toContain('Paste Java code before generating a walkthrough.')
  })
})
```

- [ ] **Step 2: Run the focused component tests and verify they fail**

Run: `npm run test -- --run src/components/AuthPanel.spec.ts src/components/CodeSubmissionPanel.spec.ts`
Expected: FAIL because `AuthPanel.vue` does not accept busy/message props and `CodeSubmissionPanel.vue` does not expose `modelValue`, disabled state, or inline validation.

- [ ] **Step 3: Refactor `AuthPanel.vue` to receive state from the parent**

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  busy: boolean
  errorMessage: string
  successMessage: string
}>()

const submitLabel = computed(() => props.busy ? 'Working...' : mode.value === 'login' ? 'Login' : 'Register')

function submit() {
  if (props.busy) {
    return
  }

  if (mode.value === 'login') {
    emit('login', { username: username.value, password: password.value })
    return
  }

  emit('register', { username: username.value, password: password.value })
}
</script>
```

```vue
<template>
  <section class="auth-panel">
    <form class="auth-form" @submit.prevent="submit">
      <input v-model="username" type="text" placeholder="Username" autocomplete="username">
      <input v-model="password" type="password" placeholder="Password" autocomplete="current-password">
      <button class="primary-button" type="submit" :disabled="busy">{{ submitLabel }}</button>
    </form>
    <p v-if="successMessage" class="auth-message auth-success">{{ successMessage }}</p>
    <p v-if="errorMessage" class="auth-message auth-error">{{ errorMessage }}</p>
  </section>
</template>
```

- [ ] **Step 4: Refactor `CodeSubmissionPanel.vue` to preserve source and validate locally**

```vue
<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  modelValue: string
  busy: boolean
  errorMessage: string
}>()

const emit = defineEmits<{
  submit: [sourceCode: string]
  'update:modelValue': [value: string]
}>()

const inlineMessage = ref('')

const sourceCode = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
})

function submit() {
  if (props.busy) {
    return
  }

  if (!sourceCode.value.trim()) {
    inlineMessage.value = 'Paste Java code before generating a walkthrough.'
    return
  }

  inlineMessage.value = ''
  emit('submit', sourceCode.value)
}
</script>
```

```vue
<template>
  <form class="submission-panel" @submit.prevent="submit">
    <textarea v-model="sourceCode" class="code-input" spellcheck="false" />
    <p v-if="inlineMessage || errorMessage" class="submission-message">{{ inlineMessage || errorMessage }}</p>
    <button class="primary-button" type="submit" :disabled="busy">{{ busy ? 'Generating...' : 'Generate walkthrough' }}</button>
  </form>
</template>
```

- [ ] **Step 5: Re-run the focused component tests and verify they pass**

Run: `npm run test -- --run src/components/AuthPanel.spec.ts src/components/CodeSubmissionPanel.spec.ts`
Expected: PASS with the new parent-controlled props and local validation in place.

- [ ] **Step 6: Commit the component-contract slice**

```bash
git add VueFrontend/src/components/AuthPanel.vue VueFrontend/src/components/AuthPanel.spec.ts VueFrontend/src/components/CodeSubmissionPanel.vue VueFrontend/src/components/CodeSubmissionPanel.spec.ts
git commit -m "feat: add controlled auth and submission states"
```

## Task 3: Make `App.vue` Own Explicit View State And Recovery

**Files:**
- Modify: `VueFrontend/src/App.vue`
- Modify: `VueFrontend/src/App.spec.ts`

- [ ] **Step 1: Write the failing integration tests for auth expiry and start-over**

```ts
it('returns to login when a protected action rejects with AuthExpiredError', async () => {
  vi.mocked(api.createGenerationTask).mockRejectedValue(Object.assign(
    new Error('Login expired. Please sign in again.'),
    { name: 'AuthExpiredError', status: 403 },
  ))

  const wrapper = await mountAuthenticatedApp()
  await wrapper.find('textarea').setValue('class Demo {}')
  await wrapper.find('form').trigger('submit')
  await flushPromises()

  expect(api.clearStoredToken).toHaveBeenCalledTimes(1)
  expect(wrapper.text()).toContain('Login expired. Please sign in again.')
  expect(wrapper.text()).toContain('Login')
})

it('starts over without wiping the editor source', async () => {
  vi.mocked(api.createGenerationTask).mockResolvedValue({
    id: 1,
    status: 'PENDING',
    language: 'java',
    detectedAlgorithm: null,
    summary: null,
    confidenceScore: 0,
    visualizationPayload: null,
    errorMessage: null,
    creditsCharged: 0,
  })
  vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask)

  const wrapper = await mountAuthenticatedApp()
  await wrapper.find('textarea').setValue('public class MergeSort {}')
  await wrapper.find('form').trigger('submit')
  await flushPromises()
  await wrapper.find('[data-start-over-button]').trigger('click')

  expect(wrapper.find('textarea').element.value).toContain('MergeSort')
  expect(wrapper.find('[data-export-button]').exists()).toBe(false)
})
```

- [ ] **Step 2: Run the focused App tests and verify they fail**

Run: `npm run test -- --run src/App.spec.ts`
Expected: FAIL because `App.vue` does not currently model auth-expired failures specially and does not expose a start-over action that preserves editor source.

- [ ] **Step 3: Add explicit view-state derivation and reset helpers in `App.vue`**

```ts
import { clearStoredToken, createExportTask, createGenerationTask, getExportTask, getGenerationTask, login, me, register, setStoredToken } from './api/pasApi'

type ViewState = 'auth' | 'ready' | 'generating' | 'generated' | 'exporting' | 'exported' | 'error-recoverable'

const authMessage = ref('')
const authErrorMessage = ref('')
const submissionErrorMessage = ref('')
const exportErrorMessage = ref('')
const authBusy = ref(false)
const generationBusy = ref(false)
const exportBusy = ref(false)

function isAuthExpiredError(error: unknown): boolean {
  if (!(error instanceof Error)) {
    return false
  }

  const status = Reflect.get(error, 'status')
  return error.name === 'AuthExpiredError' || status === 401 || status === 403
}

const viewState = computed<ViewState>(() => {
  if (!currentUser.value) return 'auth'
  if (exportTask.value?.status === 'COMPLETED') return 'exported'
  if (exportTask.value && exportTask.value.status !== 'FAILED') return 'exporting'
  if (task.value?.status === 'COMPLETED') return exportErrorMessage.value ? 'error-recoverable' : 'generated'
  if (generationBusy.value || (task.value && task.value.status !== 'FAILED')) return 'generating'
  if (submissionErrorMessage.value || exportErrorMessage.value) return 'error-recoverable'
  return 'ready'
})

function resetExportState() {
  exportMeta.value = null
  exportTask.value = null
  exportErrorMessage.value = ''
  exportBusy.value = false
  if (exportPollHandle) {
    clearInterval(exportPollHandle)
    exportPollHandle = null
  }
}

function startOver() {
  task.value = null
  submissionErrorMessage.value = ''
  resetExportState()
  stopPlayback()
}

function handleAuthExpired(message = 'Login expired. Please sign in again.') {
  clearStoredToken()
  currentUser.value = null
  authMessage.value = message
  authErrorMessage.value = ''
  task.value = null
  resetExportState()
  stopPlayback()
}
```

- [ ] **Step 4: Wire the async handlers to use the new helpers**

```ts
async function handleSubmit(nextSourceCode: string) {
  sourceCode.value = nextSourceCode
  submissionErrorMessage.value = ''
  generationBusy.value = true
  resetExportState()
  stopPlayback()

  try {
    task.value = await createGenerationTask(nextSourceCode)
    await refreshGenerationTask(task.value.id)
    if (task.value.status !== 'COMPLETED' && task.value.status !== 'FAILED') {
      startGenerationPolling(task.value.id)
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(error instanceof Error ? error.message : undefined)
      return
    }
    submissionErrorMessage.value = error instanceof Error ? error.message : 'Generation failed'
  } finally {
    generationBusy.value = false
  }
}

async function handleExport() {
  if (!task.value || exportBusy.value) return

  exportErrorMessage.value = ''
  exportBusy.value = true
  try {
    exportMeta.value = await createExportTask(task.value.id)
    await refreshExportTask(exportMeta.value.id)
    if (exportTask.value && exportTask.value.status !== 'COMPLETED' && exportTask.value.status !== 'FAILED') {
      startExportPolling(exportMeta.value.id)
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(error instanceof Error ? error.message : undefined)
      return
    }
    exportErrorMessage.value = error instanceof Error ? error.message : 'Export creation failed'
  } finally {
    exportBusy.value = false
  }
}
```

```vue
<AuthPanel
  v-if="viewState === 'auth'"
  :busy="authBusy"
  :error-message="authErrorMessage"
  :success-message="authMessage"
  @login="handleLogin"
  @register="handleRegister"
/>

<CodeSubmissionPanel
  v-if="currentUser && (viewState === 'ready' || viewState === 'error-recoverable')"
  v-model="sourceCode"
  :busy="generationBusy"
  :error-message="submissionErrorMessage"
  @submit="handleSubmit"
/>
```

- [ ] **Step 5: Re-run the focused App tests and verify they pass**

Run: `npm run test -- --run src/App.spec.ts`
Expected: PASS with the new auth-expiry and start-over tests green, and the existing login, generation, export, and playback assertions still green after updating their mount helpers to pass the new component props and preserved-source behavior.

- [ ] **Step 6: Commit the app-state slice**

```bash
git add VueFrontend/src/App.vue VueFrontend/src/App.spec.ts
git commit -m "feat: add recoverable app states for core flow"
```

## Task 4: Polish Status Cards, Action Rows, And Full Frontend Regression Coverage

**Files:**
- Modify: `VueFrontend/src/components/GenerationStatusCard.vue`
- Modify: `VueFrontend/src/components/TaskSummaryCard.vue`
- Modify: `VueFrontend/src/components/ExportStatusCard.vue`
- Modify: `VueFrontend/src/style.css`
- Modify: `VueFrontend/src/App.spec.ts`

- [ ] **Step 1: Write the failing integration assertions for export busy, retry, and local status messaging**

```ts
it('disables export while an export task is active', async () => {
  vi.mocked(api.createGenerationTask).mockResolvedValue({
    id: 1,
    status: 'PENDING',
    language: 'java',
    detectedAlgorithm: null,
    summary: null,
    confidenceScore: 0,
    visualizationPayload: null,
    errorMessage: null,
    creditsCharged: 0,
  })
  vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask)
  vi.mocked(api.createExportTask).mockResolvedValue({
    id: 101,
    generationTaskId: 1,
    status: 'PENDING',
    progress: 0,
    creditsFrozen: 18,
  })
  vi.mocked(api.getExportTask).mockResolvedValue({
    id: 101,
    generationTaskId: 1,
    status: 'PROCESSING',
    progress: 45,
    videoUrl: null,
    subtitleUrl: null,
    audioUrl: null,
    errorMessage: null,
    creditsFrozen: 18,
    creditsCharged: null,
    createdAt: '2026-04-11T16:00:00Z',
    updatedAt: '2026-04-11T16:00:10Z',
  })

  const wrapper = await mountAuthenticatedApp()
  await wrapper.find('form').trigger('submit')
  await flushPromises()
  await wrapper.find('[data-export-button]').trigger('click')
  await flushPromises()

  expect(wrapper.find('[data-export-button]').attributes('disabled')).toBeDefined()
  expect(wrapper.text()).toContain('Export is running. You can stay on this page while Pas finishes the video.')
})

it('shows retry export action after export failure', async () => {
  vi.mocked(api.getGenerationTask).mockResolvedValue(mockCompletedTask)
  vi.mocked(api.createExportTask)
    .mockResolvedValueOnce({ id: 101, generationTaskId: 1, status: 'PENDING', progress: 0, creditsFrozen: 18 })
    .mockResolvedValueOnce({ id: 102, generationTaskId: 1, status: 'PENDING', progress: 0, creditsFrozen: 18 })
  vi.mocked(api.getExportTask).mockResolvedValue({
    id: 101,
    generationTaskId: 1,
    status: 'FAILED',
    progress: 100,
    videoUrl: null,
    subtitleUrl: null,
    audioUrl: null,
    errorMessage: 'Video composition failed.',
    creditsFrozen: 18,
    creditsCharged: null,
    createdAt: '2026-04-11T16:00:00Z',
    updatedAt: '2026-04-11T16:00:10Z',
  })

  const wrapper = await mountAuthenticatedApp()
  await wrapper.find('form').trigger('submit')
  await flushPromises()
  await wrapper.find('[data-export-button]').trigger('click')
  await flushPromises()

  expect(wrapper.find('[data-retry-export-button]').exists()).toBe(true)
})
```

- [ ] **Step 2: Run the full frontend test suite and verify the new assertions fail**

Run: `npm run test -- --run`
Expected: FAIL because the status cards and action rows do not yet surface disabled export, retry export, or stronger local messaging.

- [ ] **Step 3: Update the status components to render product-facing actions and messages**

```vue
<!-- VueFrontend/src/components/TaskSummaryCard.vue -->
<script setup lang="ts">
defineProps<{
  task: GenerationTaskResponse
  exportBusy: boolean
}>()

defineEmits<{
  export: []
  startOver: []
}>()
</script>

<template>
  <section class="task-summary-card">
    <p class="panel-kicker">Generated result</p>
    <h2>{{ task.detectedAlgorithm }}</h2>
    <p>{{ task.summary }}</p>
    <div class="summary-grid">
      <span>Confidence: {{ task.confidenceScore.toFixed(2) }}</span>
      <span>Credits: {{ task.creditsCharged }}</span>
    </div>
    <div class="action-row">
      <button class="primary-button" data-export-button type="button" :disabled="exportBusy" @click="$emit('export')">
        {{ exportBusy ? 'Exporting...' : 'Export video' }}
      </button>
      <button class="secondary-button" data-start-over-button type="button" @click="$emit('startOver')">
        Submit new code
      </button>
    </div>
  </section>
</template>
```

```vue
<!-- VueFrontend/src/components/ExportStatusCard.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import type { ExportTaskResponse } from '../types/pas'

const props = defineProps<{
  exportTask: ExportTaskResponse
}>()

defineEmits<{ retry: [] }>()

const statusCopy = computed(() => {
  if (props.exportTask.status === 'COMPLETED') return 'Video export is ready. Download the result below.'
  if (props.exportTask.status === 'FAILED') return 'Export failed, but your generated walkthrough is still available.'
  return 'Export is running. You can stay on this page while Pas finishes the video.'
})
</script>

<template>
  <section class="export-status-card">
    <p class="panel-kicker">Export status</p>
    <h3>{{ exportTask.status }}</h3>
    <p>{{ statusCopy }}</p>
    <p>Progress: {{ exportTask.progress }}%</p>
    <p>Frozen credits: {{ exportTask.creditsFrozen ?? 0 }}</p>
    <p v-if="exportTask.creditsCharged !== null">Charged credits: {{ exportTask.creditsCharged }}</p>
    <p v-if="exportTask.errorMessage" class="export-error">{{ exportTask.errorMessage }}</p>
    <button v-if="exportTask.status === 'FAILED'" class="secondary-button" data-retry-export-button type="button" @click="$emit('retry')">Retry export</button>
    <a v-if="exportTask.videoUrl" class="primary-link" :href="exportTask.videoUrl" data-download-link>Download video</a>
  </section>
</template>
```

```vue
<!-- VueFrontend/src/components/GenerationStatusCard.vue -->
<template>
  <section class="generation-status-card">
    <p class="panel-kicker">Generation status</p>
    <h2>{{ task.status }}</h2>
    <p>{{ statusCopy }}</p>
    <slot name="actions" />
    <p v-if="task.errorMessage" class="generation-error">{{ task.errorMessage }}</p>
  </section>
</template>
```

- [ ] **Step 4: Add the corresponding app wiring and styles**

```vue
<!-- VueFrontend/src/App.vue -->
<TaskSummaryCard
  v-if="task?.status === 'COMPLETED'"
  :task="task"
  :export-busy="exportBusy || viewState === 'exporting'"
  @export="handleExport"
  @start-over="startOver"
/>

<ExportStatusCard
  v-if="exportTask"
  :export-task="exportTask"
  @retry="handleExport"
/>

<GenerationStatusCard v-else-if="currentUser && (viewState === 'generating' || (viewState === 'error-recoverable' && task))" :task="task!">
  <template #actions>
    <button class="secondary-button" data-start-over-button type="button" @click="startOver">Start over</button>
  </template>
</GenerationStatusCard>
```

```css
/* VueFrontend/src/style.css */
.action-row {
  display: flex;
  gap: 12px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.secondary-button,
.primary-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  padding: 12px 18px;
}

.secondary-button {
  border: 1px solid rgba(125, 211, 252, 0.35);
  background: rgba(15, 23, 42, 0.92);
  color: #dbeafe;
}

.primary-button:disabled,
.secondary-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.auth-error,
.submission-message,
.export-error,
.generation-error {
  color: #fecaca;
}

.auth-success {
  color: #bfdbfe;
}

.primary-link {
  background: linear-gradient(135deg, #38bdf8, #6366f1);
  color: white;
  text-decoration: none;
}
```

- [ ] **Step 5: Run the full frontend verification and confirm it passes**

Run: `npm run test -- --run`
Expected: PASS for `src/App.spec.ts`, `src/api/pasApi.spec.ts`, `src/components/AuthPanel.spec.ts`, `src/components/CodeSubmissionPanel.spec.ts`, and existing component tests.

Run: `npm run build`
Expected: PASS with `vue-tsc -b && vite build` completing successfully.

- [ ] **Step 6: Commit the UX polish and regression coverage slice**

```bash
git add VueFrontend/src/components/GenerationStatusCard.vue VueFrontend/src/components/TaskSummaryCard.vue VueFrontend/src/components/ExportStatusCard.vue VueFrontend/src/style.css VueFrontend/src/App.spec.ts
git commit -m "feat: polish core walkthrough usability"
```

## Self-Review

- **Spec coverage:** The plan covers auth expiry, controlled login/register feedback, client-side empty submission validation, explicit generating/exporting state handling, retry/start-over paths, export completion messaging, and test coverage for these transitions.
- **Placeholder scan:** No `TODO`, `TBD`, or implicit "handle this later" steps remain. Every task names the exact files, tests, commands, and code shapes.
- **Type consistency:** The plan consistently uses `AuthExpiredError`, `viewState`, `startOver`, `exportBusy`, `errorMessage`, and `successMessage` across the API helper, components, and app shell.
