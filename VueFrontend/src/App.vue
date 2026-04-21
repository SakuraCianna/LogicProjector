<template>
  <main class="app-shell">
    <AuthPanel
      v-if="viewState === 'auth'"
      :busy="authBusy"
      :error-message="authErrorMessage"
      :success-message="authMessage"
      @login="handleLogin"
      @register="handleRegister"
    />

    <section v-else class="user-bar">
      <span>{{ currentUser?.username }}</span>
      <span>Credits: {{ currentUser?.creditsBalance ?? 0 }}</span>
      <button type="button" @click="handleLogout">Logout</button>
    </section>

    <CodeSubmissionPanel
      v-if="currentUser && (viewState === 'ready' || viewState === 'error-recoverable')"
      v-model="sourceCode"
      :busy="generationBusy"
      :error-message="submissionErrorMessage"
      @submit="handleSubmit"
    />

    <section v-else-if="currentUser && task && (viewState === 'generated' || viewState === 'exporting' || viewState === 'exported')" class="player-layout">
      <TaskSummaryCard :task="task" :export-busy="exportBusy || viewState === 'exporting'" @export="handleExport" />
      <div class="player-actions">
        <button class="secondary-button" data-start-over-button type="button" @click="startOver">Submit new code</button>
      </div>
      <VisualizationStage :step="currentStep" />
      <ExplanationPanel :step="currentStep" />
      <CodeHighlightPanel :source-code="sourceCode" :highlighted-lines="currentStep.highlightedLines" />
      <ExportStatusCard v-if="exportTask" :export-task="exportTask" @retry="handleExport" />
      <PlaybackControls
        :step-count="task.visualizationPayload?.steps.length ?? 0"
        :active-index="activeIndex"
        :is-playing="isPlaying"
        :playback-speed="playbackSpeed"
        @change="setActiveIndex($event)"
        @change-speed="changePlaybackSpeed"
        @toggle-play="togglePlayback"
      />
    </section>

    <GenerationStatusCard v-else-if="currentUser && task" :task="task">
      <template #actions>
        <button class="secondary-button" data-start-over-button type="button" @click="startOver">Start over</button>
      </template>
    </GenerationStatusCard>

    <p v-if="bannerMessage" class="error-banner">{{ bannerMessage }}</p>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import { clearStoredToken, createExportTask, createGenerationTask, getExportTask, getGenerationTask, login, me, register, setStoredToken } from './api/pasApi'
import AuthPanel from './components/AuthPanel.vue'
import CodeHighlightPanel from './components/CodeHighlightPanel.vue'
import CodeSubmissionPanel from './components/CodeSubmissionPanel.vue'
import ExplanationPanel from './components/ExplanationPanel.vue'
import ExportStatusCard from './components/ExportStatusCard.vue'
import GenerationStatusCard from './components/GenerationStatusCard.vue'
import PlaybackControls from './components/PlaybackControls.vue'
import TaskSummaryCard from './components/TaskSummaryCard.vue'
import VisualizationStage from './components/VisualizationStage.vue'
import type { CreateExportTaskResponse, ExportTaskResponse, GenerationTaskResponse, UserProfile } from './types/pas'

type ViewState = 'auth' | 'ready' | 'generating' | 'generated' | 'exporting' | 'exported' | 'error-recoverable'

const defaultSourceCode = `public class QuickSort {
  void sort(int[] arr, int low, int high) {
    int pivot = arr[high];
    partition(arr, low, high);
  }
}`

const currentUser = ref<UserProfile | null>(null)
const task = ref<GenerationTaskResponse | null>(null)
const exportMeta = ref<CreateExportTaskResponse | null>(null)
const exportTask = ref<ExportTaskResponse | null>(null)
const sourceCode = ref(defaultSourceCode)
const activeIndex = ref(0)
const playbackSpeed = ref(1)
const isPlaying = ref(false)
const authBusy = ref(false)
const generationBusy = ref(false)
const exportBusy = ref(false)
const authMessage = ref('')
const authErrorMessage = ref('')
const submissionErrorMessage = ref('')
const exportErrorMessage = ref('')
let exportPollHandle: ReturnType<typeof setInterval> | null = null
let generationPollHandle: ReturnType<typeof setInterval> | null = null
let playbackHandle: ReturnType<typeof setInterval> | null = null

const currentStep = computed(() => task.value?.visualizationPayload?.steps[activeIndex.value] ?? {
  title: 'No step',
  narration: '',
  arrayState: [],
  activeIndices: [],
  highlightedLines: [],
})

const viewState = computed<ViewState>(() => {
  if (!currentUser.value) {
    return 'auth'
  }

  if (exportTask.value?.status === 'COMPLETED') {
    return 'exported'
  }

  if (exportTask.value && exportTask.value.status !== 'FAILED') {
    return 'exporting'
  }

  if (task.value?.status === 'COMPLETED') {
    return 'generated'
  }

  if (submissionErrorMessage.value) {
    return 'error-recoverable'
  }

  if (generationBusy.value || (task.value && task.value.status !== 'FAILED')) {
    return 'generating'
  }

  if (exportErrorMessage.value) {
    return 'error-recoverable'
  }

  return 'ready'
})

const bannerMessage = computed(() => exportErrorMessage.value)

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}

function isAuthExpiredError(error: unknown) {
  if (!(error instanceof Error)) {
    return false
  }

  const status = Reflect.get(error, 'status')
  return error.name === 'AuthExpiredError' || status === 401 || status === 403
}

function stopGenerationPolling() {
  if (generationPollHandle) {
    clearInterval(generationPollHandle)
    generationPollHandle = null
  }
}

function stopExportPolling() {
  if (exportPollHandle) {
    clearInterval(exportPollHandle)
    exportPollHandle = null
  }
}

function resetExportState() {
  exportMeta.value = null
  exportTask.value = null
  exportBusy.value = false
  exportErrorMessage.value = ''
  stopExportPolling()
}

function handleAuthExpired(message = 'Login expired. Please sign in again.') {
  clearStoredToken()
  currentUser.value = null
  task.value = null
  authMessage.value = ''
  authErrorMessage.value = message
  submissionErrorMessage.value = ''
  resetExportState()
  stopGenerationPolling()
  stopPlayback()
}

function startOver() {
  task.value = null
  submissionErrorMessage.value = ''
  resetExportState()
  stopGenerationPolling()
  stopPlayback()
}

async function handleSubmit(nextSourceCode: string) {
  sourceCode.value = nextSourceCode
  submissionErrorMessage.value = ''
  activeIndex.value = 0
  playbackSpeed.value = 1
  generationBusy.value = true
  authErrorMessage.value = ''
  authMessage.value = ''
  resetExportState()
  stopPlayback()

  try {
    task.value = await createGenerationTask(nextSourceCode)
    if (task.value.status !== 'COMPLETED') {
      await refreshGenerationTask(task.value.id)
      if (task.value.status !== 'COMPLETED' && task.value.status !== 'FAILED') {
        startGenerationPolling(task.value.id)
      }
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, 'Login expired. Please sign in again.'))
      return
    }
    submissionErrorMessage.value = getErrorMessage(error, 'Generation failed')
  } finally {
    generationBusy.value = false
  }
}

async function handleRegister(credentials: { username: string; password: string }) {
  authBusy.value = true
  authMessage.value = ''
  authErrorMessage.value = ''
  try {
    await register(credentials.username, credentials.password)
    authMessage.value = 'Registration submitted. Login after it succeeds.'
  } catch (error) {
    authErrorMessage.value = getErrorMessage(error, 'Registration failed')
  } finally {
    authBusy.value = false
  }
}

async function handleLogin(credentials: { username: string; password: string }) {
  authBusy.value = true
  authMessage.value = ''
  authErrorMessage.value = ''
  try {
    const response = await login(credentials.username, credentials.password)
    setStoredToken(response.token)
    currentUser.value = response.user
  } catch (error) {
    authErrorMessage.value = getErrorMessage(error, 'Login failed')
  } finally {
    authBusy.value = false
  }
}

function handleLogout() {
  clearStoredToken()
  currentUser.value = null
  authMessage.value = ''
  authErrorMessage.value = ''
  task.value = null
  submissionErrorMessage.value = ''
  resetExportState()
  stopGenerationPolling()
  stopPlayback()
}

function startGenerationPolling(taskId: number) {
  stopGenerationPolling()

  generationPollHandle = setInterval(async () => {
    try {
      await refreshGenerationTask(taskId)
      if (task.value && (task.value.status === 'COMPLETED' || task.value.status === 'FAILED')) {
        stopGenerationPolling()
      }
    } catch (error) {
      stopGenerationPolling()
      if (isAuthExpiredError(error)) {
        handleAuthExpired(getErrorMessage(error, 'Login expired. Please sign in again.'))
        return
      }
      submissionErrorMessage.value = getErrorMessage(error, 'Generation polling failed')
    }
  }, 3000)
}

async function refreshGenerationTask(taskId: number) {
  task.value = await getGenerationTask(taskId)
  if (task.value.status === 'FAILED' && task.value.errorMessage) {
    submissionErrorMessage.value = task.value.errorMessage
  }
}

async function handleExport() {
  if (!task.value || exportBusy.value || viewState.value === 'exporting') {
    return
  }

  try {
    exportBusy.value = true
    exportErrorMessage.value = ''
    exportMeta.value = await createExportTask(task.value.id)
    await refreshExportTask(exportMeta.value.id)

    if (exportTask.value && exportTask.value.status !== 'COMPLETED' && exportTask.value.status !== 'FAILED') {
      startExportPolling(exportMeta.value.id)
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, 'Login expired. Please sign in again.'))
      return
    }
    exportErrorMessage.value = getErrorMessage(error, 'Export creation failed')
  } finally {
    exportBusy.value = false
  }
}

function togglePlayback() {
  if (!task.value?.visualizationPayload?.steps.length) {
    return
  }

  if (isPlaying.value) {
    stopPlayback()
    return
  }

  isPlaying.value = true
  playbackHandle = setInterval(() => {
    const stepCount = task.value?.visualizationPayload?.steps.length ?? 0
    if (activeIndex.value >= stepCount - 1) {
      stopPlayback()
      return
    }
    activeIndex.value += 1
    if (activeIndex.value >= stepCount - 1) {
      stopPlayback()
    }
  }, Math.round(1500 / playbackSpeed.value))
}

function stopPlayback() {
  isPlaying.value = false
  if (playbackHandle) {
    clearInterval(playbackHandle)
    playbackHandle = null
  }
}

function setActiveIndex(nextIndex: number) {
  activeIndex.value = nextIndex
  stopPlayback()
}

function changePlaybackSpeed(speed: number) {
  playbackSpeed.value = speed
  if (isPlaying.value) {
    stopPlayback()
    togglePlayback()
  }
}

function startExportPolling(exportTaskId: number) {
  stopExportPolling()

  exportPollHandle = setInterval(async () => {
    try {
      await refreshExportTask(exportTaskId)
      if (exportTask.value && (exportTask.value.status === 'COMPLETED' || exportTask.value.status === 'FAILED')) {
        stopExportPolling()
      }
    } catch (error) {
      stopExportPolling()
      if (isAuthExpiredError(error)) {
        handleAuthExpired(getErrorMessage(error, 'Login expired. Please sign in again.'))
        return
      }
      exportErrorMessage.value = getErrorMessage(error, 'Export polling failed')
    }
  }, 3000)
}

async function refreshExportTask(exportTaskId: number) {
  exportTask.value = await getExportTask(exportTaskId)
  if (exportTask.value.status === 'FAILED' && exportTask.value.errorMessage) {
    exportErrorMessage.value = exportTask.value.errorMessage
  }
}

onBeforeUnmount(() => {
  stopExportPolling()
  stopGenerationPolling()
  stopPlayback()
})

onMounted(async () => {
  try {
    currentUser.value = await me()
  } catch (error) {
    if (isAuthExpiredError(error)) {
      clearStoredToken()
      authErrorMessage.value = getErrorMessage(error, 'Login expired. Please sign in again.')
    } else {
      authErrorMessage.value = 'Unable to restore your session. Please try signing in again.'
    }
    currentUser.value = null
  }
})
</script>
