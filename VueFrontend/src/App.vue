<template>
  <main class="app-shell" :class="{ 'app-shell-auth': viewState === 'auth' }">
    <AuthPanel
      v-if="viewState === 'auth'"
      :busy="authBusy"
      :error-message="authErrorMessage"
      :success-message="authMessage"
      @login="handleLogin"
      @register="handleRegister"
    />

    <section v-else class="workspace-shell">
      <aside class="workspace-sidebar">
        <section class="sidebar-brand">
          <p class="panel-kicker">算法演示工作室</p>
          <h1>教师工作台</h1>
          <p>恢复最近的讲解任务，导出完整课程视频，专注完成一条教学流程。</p>
        </section>

        <button class="primary-button sidebar-primary-action" data-new-walkthrough-button type="button" @click="openNewWalkthrough">
          新建讲解
        </button>

        <section class="sidebar-section">
          <p class="panel-kicker">最近生成</p>
          <p v-if="recentGenerationTasks.length === 0" class="sidebar-empty">暂无生成记录。</p>
          <button
            v-for="item in recentGenerationTasks"
            :key="`generation-${item.id}`"
            class="sidebar-history-item"
            :class="{ active: selectedHistoryKind === 'generation' && selectedHistoryId === item.id }"
            :data-generation-history-item="item.id"
            type="button"
            @click="handleSelectGeneration(item.id)"
          >
            <strong>{{ item.detectedAlgorithm ? formatAlgorithmName(item.detectedAlgorithm) : item.sourcePreview }}</strong>
            <span>{{ formatGenerationStatus(item.status) }}</span>
          </button>
        </section>

        <section class="sidebar-section">
          <p class="panel-kicker">最近导出</p>
          <p v-if="recentExportTasks.length === 0" class="sidebar-empty">暂无导出记录。</p>
          <button
            v-for="item in recentExportTasks"
            :key="`export-${item.id}`"
            class="sidebar-history-item"
            :class="{ active: selectedHistoryKind === 'export' && selectedHistoryId === item.id }"
            :data-export-history-item="item.id"
            type="button"
            @click="handleSelectExport(item.id)"
          >
            <strong>{{ item.detectedAlgorithm ? `${formatAlgorithmName(item.detectedAlgorithm)} 导出` : `导出 #${item.id}` }}</strong>
            <span>{{ formatExportStatus(item.status) }}</span>
          </button>
        </section>

        <section class="sidebar-footer">
          <strong>{{ currentUser?.username }}</strong>
          <span>可用额度：{{ currentUser?.creditsBalance ?? 0 }}</span>
          <button type="button" @click="handleLogout">退出登录</button>
        </section>
      </aside>

      <section class="workspace-main">
        <section class="workspace-hero">
          <div>
            <p class="panel-kicker">智能教学工作台</p>
            <h2>{{ workspaceHeadline }}</h2>
            <p class="workspace-copy">{{ workspaceDescription }}</p>
          </div>

          <div class="workspace-meta">
            <span class="workspace-pill">{{ workspaceStatus }}</span>
            <span v-if="workspaceSecondaryMeta" class="workspace-meta-text">{{ workspaceSecondaryMeta }}</span>
          </div>
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
            <button class="secondary-button" data-start-over-button type="button" @click="startOver">重新提交代码</button>
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
            <button class="secondary-button" data-start-over-button type="button" @click="startOver">重新开始</button>
          </template>
        </GenerationStatusCard>

        <p v-if="bannerMessage" class="error-banner workspace-banner">{{ bannerMessage }}</p>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

import {
  clearStoredToken,
  createExportTask,
  createGenerationTask,
  getStoredToken,
  getExportTask,
  getGenerationTask,
  getRecentExportTasks,
  getRecentGenerationTasks,
  login,
  me,
  register,
  setStoredToken,
} from './api/pasApi'
import AuthPanel from './components/AuthPanel.vue'
import CodeHighlightPanel from './components/CodeHighlightPanel.vue'
import CodeSubmissionPanel from './components/CodeSubmissionPanel.vue'
import ExplanationPanel from './components/ExplanationPanel.vue'
import ExportStatusCard from './components/ExportStatusCard.vue'
import GenerationStatusCard from './components/GenerationStatusCard.vue'
import PlaybackControls from './components/PlaybackControls.vue'
import TaskSummaryCard from './components/TaskSummaryCard.vue'
import VisualizationStage from './components/VisualizationStage.vue'
import type {
  CreateExportTaskResponse,
  ExportTaskListItemResponse,
  ExportTaskResponse,
  GenerationTaskListItemResponse,
  GenerationTaskResponse,
  UserProfile,
} from './types/pas'

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
const recentGenerationTasks = ref<GenerationTaskListItemResponse[]>([])
const recentExportTasks = ref<ExportTaskListItemResponse[]>([])
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
const activityErrorMessage = ref('')
const selectedHistoryKind = ref<'new' | 'generation' | 'export'>('new')
const selectedHistoryId = ref<number | null>(null)
let exportPollHandle: ReturnType<typeof setInterval> | null = null
let generationPollHandle: ReturnType<typeof setInterval> | null = null
let playbackHandle: ReturnType<typeof setInterval> | null = null

const currentStep = computed(() => task.value?.visualizationPayload?.steps[activeIndex.value] ?? {
  title: '暂无步骤',
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

const bannerMessage = computed(() => exportErrorMessage.value || activityErrorMessage.value)

const workspaceHeadline = computed(() => {
  if (!task.value) {
    return '选择历史讲解，或开始新的教学演示。'
  }

  return '当前讲解'
})

const workspaceDescription = computed(() => {
  if (!task.value) {
    return '从左侧选择最近记录，或新建一次算法讲解，继续推进你的备课流程。'
  }

  if (task.value.status === 'FAILED') {
    return task.value.errorMessage ?? '这次讲解生成失败，请调整代码后重试。'
  }

  if (exportTask.value?.status === 'COMPLETED') {
    return '讲解已可播放，最新导出视频也已准备好下载。'
  }

  if (exportTask.value) {
    return '你可以继续查看讲解，系统会在后台完成视频导出。'
  }

  if (task.value.summary) {
    return task.value.summary
  }

  return '逐步查看讲解内容，确认每个教学步骤是否清晰。'
})

const workspaceStatus = computed(() => {
  if (!task.value) {
    return '准备新课程'
  }

  if (exportTask.value?.status === 'COMPLETED') {
    return '导出已完成'
  }

  if (exportTask.value) {
    return `导出${formatExportStatus(exportTask.value.status)}`
  }

  return `任务${formatGenerationStatus(task.value.status)}`
})

const workspaceSecondaryMeta = computed(() => {
  if (!task.value) {
    return selectedHistoryKind.value === 'new' ? '面向课堂的算法讲解工作台' : null
  }

  if (task.value.detectedAlgorithm) {
    return formatAlgorithmName(task.value.detectedAlgorithm)
  }

  return task.value.language.toUpperCase()
})

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}

function formatGenerationStatus(status: string) {
  switch (status) {
    case 'PENDING':
      return '排队中'
    case 'ANALYZING':
      return '分析中'
    case 'COMPLETED':
      return '已完成'
    case 'FAILED':
      return '失败'
    case 'EXPORTING':
      return '导出中'
    default:
      return status
  }
}

function formatExportStatus(status: string) {
  switch (status) {
    case 'PENDING':
      return '排队中'
    case 'PROCESSING':
      return '处理中'
    case 'COMPLETED':
      return '已完成'
    case 'FAILED':
      return '失败'
    default:
      return status
  }
}

function formatAlgorithmName(algorithm: string) {
  switch (algorithm) {
    case 'BUBBLE_SORT':
      return '冒泡排序'
    case 'SELECTION_SORT':
      return '选择排序'
    case 'INSERTION_SORT':
      return '插入排序'
    case 'QUICK_SORT':
      return '快速排序'
    case 'MERGE_SORT':
      return '归并排序'
    case 'BINARY_SEARCH':
      return '二分查找'
    default:
      return algorithm.replaceAll('_', ' ')
  }
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

function clearHistorySelection() {
  selectedHistoryKind.value = 'new'
  selectedHistoryId.value = null
}

async function refreshRecentActivity() {
  try {
    const [generationItems, exportItems] = await Promise.all([
      getRecentGenerationTasks(),
      getRecentExportTasks(),
    ])
    recentGenerationTasks.value = generationItems
    recentExportTasks.value = exportItems
    activityErrorMessage.value = ''
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录。'))
      return
    }
    activityErrorMessage.value = '无法加载最近记录。'
  }
}

function handleAuthExpired(_message = '登录已过期，请重新登录。') {
  clearStoredToken()
  currentUser.value = null
  task.value = null
  recentGenerationTasks.value = []
  recentExportTasks.value = []
  clearHistorySelection()
  authMessage.value = ''
  authErrorMessage.value = '登录已过期，请重新登录。'
  submissionErrorMessage.value = ''
  resetExportState()
  stopGenerationPolling()
  stopPlayback()
}

function startOver() {
  task.value = null
  submissionErrorMessage.value = ''
  clearHistorySelection()
  resetExportState()
  stopGenerationPolling()
  stopPlayback()
}

function openNewWalkthrough() {
  clearHistorySelection()
  sourceCode.value = defaultSourceCode
  task.value = null
  submissionErrorMessage.value = ''
  activityErrorMessage.value = ''
  resetExportState()
  stopGenerationPolling()
  stopPlayback()
}

async function handleSelectGeneration(taskId: number) {
  stopGenerationPolling()
  stopExportPolling()
  stopPlayback()
  resetExportState()
  exportErrorMessage.value = ''
  submissionErrorMessage.value = ''

  try {
    const loadedTask = await getGenerationTask(taskId)
    task.value = loadedTask
    sourceCode.value = loadedTask.sourceCode ?? defaultSourceCode
    selectedHistoryKind.value = 'generation'
    selectedHistoryId.value = taskId
    activeIndex.value = 0
    activityErrorMessage.value = ''

    if (loadedTask.status === 'FAILED' && loadedTask.errorMessage) {
      submissionErrorMessage.value = loadedTask.errorMessage
    }

    if (loadedTask.status !== 'COMPLETED' && loadedTask.status !== 'FAILED') {
      startGenerationPolling(taskId)
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录。'))
      return
    }
    activityErrorMessage.value = getErrorMessage(error, '加载生成历史失败')
  }
}

async function handleSelectExport(exportTaskId: number) {
  stopGenerationPolling()
  stopExportPolling()
  stopPlayback()
  submissionErrorMessage.value = ''
  exportErrorMessage.value = ''

  try {
    const loadedExportTask = await getExportTask(exportTaskId)
    const loadedTask = await getGenerationTask(loadedExportTask.generationTaskId)
    exportTask.value = loadedExportTask
    task.value = loadedTask
    sourceCode.value = loadedTask.sourceCode ?? defaultSourceCode
    selectedHistoryKind.value = 'export'
    selectedHistoryId.value = exportTaskId
    activeIndex.value = 0
    activityErrorMessage.value = ''

    if (loadedExportTask.status !== 'COMPLETED' && loadedExportTask.status !== 'FAILED') {
      startExportPolling(exportTaskId)
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录。'))
      return
    }
    activityErrorMessage.value = getErrorMessage(error, '加载导出历史失败')
  }
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
    selectedHistoryKind.value = 'generation'
    selectedHistoryId.value = task.value.id
    await refreshRecentActivity()
    if (task.value.status !== 'COMPLETED') {
      await refreshGenerationTask(task.value.id)
      if (task.value.status !== 'COMPLETED' && task.value.status !== 'FAILED') {
        startGenerationPolling(task.value.id)
      }
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录。'))
      return
    }
    submissionErrorMessage.value = getErrorMessage(error, '生成失败')
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
    authMessage.value = '注册成功，请登录。'
  } catch (error) {
    authErrorMessage.value = getErrorMessage(error, '注册失败')
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
    await refreshRecentActivity()
  } catch (error) {
    authErrorMessage.value = getErrorMessage(error, '登录失败')
  } finally {
    authBusy.value = false
  }
}

function handleLogout() {
  clearStoredToken()
  currentUser.value = null
  recentGenerationTasks.value = []
  recentExportTasks.value = []
  clearHistorySelection()
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
        handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录。'))
        return
      }
      submissionErrorMessage.value = getErrorMessage(error, '刷新生成状态失败')
    }
  }, 3000)
}

async function refreshGenerationTask(taskId: number) {
  task.value = await getGenerationTask(taskId)
  if (task.value.sourceCode) {
    sourceCode.value = task.value.sourceCode
  }
  if (task.value.status === 'FAILED' && task.value.errorMessage) {
    submissionErrorMessage.value = task.value.errorMessage
    await refreshRecentActivity()
  }
  if (task.value.status === 'COMPLETED') {
    await refreshRecentActivity()
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
    selectedHistoryKind.value = 'export'
    selectedHistoryId.value = exportMeta.value.id
    await refreshRecentActivity()
    await refreshExportTask(exportMeta.value.id)

    if (exportTask.value && exportTask.value.status !== 'COMPLETED' && exportTask.value.status !== 'FAILED') {
      startExportPolling(exportMeta.value.id)
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录。'))
      return
    }
    exportErrorMessage.value = getErrorMessage(error, '创建导出任务失败')
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
        handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录。'))
        return
      }
      exportErrorMessage.value = getErrorMessage(error, '刷新导出状态失败')
    }
  }, 3000)
}

async function refreshExportTask(exportTaskId: number) {
  exportTask.value = await getExportTask(exportTaskId)
  if (exportTask.value.status === 'FAILED' && exportTask.value.errorMessage) {
    exportErrorMessage.value = exportTask.value.errorMessage
    await refreshRecentActivity()
  }
  if (exportTask.value.status === 'COMPLETED') {
    await refreshRecentActivity()
  }
}

onBeforeUnmount(() => {
  stopExportPolling()
  stopGenerationPolling()
  stopPlayback()
})

onMounted(async () => {
  if (!getStoredToken()) {
    return
  }

  try {
    currentUser.value = await me()
    await refreshRecentActivity()
  } catch (error) {
    if (isAuthExpiredError(error)) {
      clearStoredToken()
      authErrorMessage.value = '登录已过期，请重新登录。'
    } else {
      authErrorMessage.value = '无法恢复登录状态，请重新登录。'
    }
    currentUser.value = null
  }
})
</script>
