<template>
  <main class="app-shell" :class="{ 'app-shell-auth': viewState === 'auth' }">
    <section v-if="viewState === 'auth'" class="auth-page">
      <div class="auth-page__brand">
        <span class="brand-mark" aria-hidden="true">
          <svg class="brand-icon" viewBox="0 0 32 32" role="img">
            <path d="M7 9.5L16 5l9 4.5v13L16 27l-9-4.5v-13Z" />
            <path d="M11.5 12.5L16 10l4.5 2.5v7L16 22l-4.5-2.5v-7Z" />
            <path d="M16 10v12M11.5 12.5 16 15l4.5-2.5" />
          </svg>
        </span>
        <p class="panel-kicker">Logic Projector</p>
        <h1>把代码变成一堂清楚的课</h1>
        <p>粘贴 Java / C / C++ 算法代码，生成步骤讲解、课堂可视化和视频导出</p>
      </div>
      <AuthPanel :busy="authBusy" :error-message="authErrorMessage" :success-message="authMessage" @login="handleLogin"
        @register="handleRegister" />
    </section>

    <section v-else class="workspace-shell">
      <aside class="workspace-sidebar">
        <section class="sidebar-brand">
          <span class="brand-mark" aria-hidden="true">
            <svg class="brand-icon" viewBox="0 0 32 32" role="img">
              <path d="M7 9.5L16 5l9 4.5v13L16 27l-9-4.5v-13Z" />
              <path d="M11.5 12.5L16 10l4.5 2.5v7L16 22l-4.5-2.5v-7Z" />
              <path d="M16 10v12M11.5 12.5 16 15l4.5-2.5" />
            </svg>
          </span>
          <div>
            <p class="panel-kicker">智能教学工作台</p>
            <h1>Logic Projector</h1>
          </div>
        </section>

        <nav class="sidebar-nav" aria-label="工作台导航">
          <button class="sidebar-nav-item" :class="{ active: activePage === 'compose' }" data-new-walkthrough-button
            type="button" @click="openNewWalkthrough">
            <span class="nav-dot"></span>
            <strong>新建讲解</strong>
            <small>粘贴 Java / C / C++ 代码</small>
          </button>
          <button class="sidebar-nav-item" :class="{ active: activePage === 'generations' }" type="button"
            @click="openGenerationsPage">
            <span class="nav-dot"></span>
            <strong>最近生成</strong>
            <small>{{ recentGenerationTasks.length }} 条记录</small>
          </button>
          <button class="sidebar-nav-item" :class="{ active: activePage === 'exports' }" type="button"
            @click="openExportsPage">
            <span class="nav-dot"></span>
            <strong>最近导出</strong>
            <small>{{ recentExportTasks.length }} 条记录</small>
          </button>
          <button class="sidebar-nav-item" :class="{ active: activePage === 'recharge' }" type="button"
            @click="openRechargePage">
            <span class="nav-dot"></span>
            <strong>充值商店</strong>
            <small>购买额度</small>
          </button>
        </nav>

        <section class="sidebar-footer">
          <div class="user-chip">
            <span>{{ currentUser?.username?.slice(0, 1).toUpperCase() }}</span>
            <div>
              <strong>{{ currentUser?.username }}</strong>
              <small>可用额度：{{ currentUser?.creditsBalance ?? 0 }}</small>
            </div>
          </div>
          <button type="button" @click="handleLogout">退出登录</button>
        </section>
      </aside>

      <section class="workspace-main">
        <header class="workspace-topbar">
          <div>
            <p class="panel-kicker">{{ activePage === 'player' ? '步骤讲解' : '工作台' }}</p>
            <h2>{{ activePage === 'player' && task?.detectedAlgorithm ? formatAlgorithmName(task.detectedAlgorithm) :
              workspaceHeadline }}</h2>
          </div>
          <div class="topbar-actions">
            <button v-if="activePage === 'player'" class="secondary-button" data-start-over-button type="button"
              @click="startOver">重新提交代码</button>
            <button class="secondary-button" type="button" @click="openGenerationsPage">最近生成</button>
          </div>
        </header>

        <section v-if="activePage === 'player'" class="lesson-page">
          <section
            v-if="currentUser && task && (viewState === 'generated' || viewState === 'exporting' || viewState === 'exported')"
            class="lesson-layout">
            <section class="lesson-main-card">
              <VisualizationStage :step="currentStep" />
              <PlaybackControls :step-count="task.visualizationPayload?.steps.length ?? 0" :active-index="activeIndex"
                :is-playing="isPlaying" :playback-speed="playbackSpeed" @change="setActiveIndex($event)"
                @change-speed="changePlaybackSpeed" @toggle-play="togglePlayback" />
            </section>
            <aside class="lesson-side-card">
              <TaskSummaryCard :task="task" :export-busy="exportBusy || viewState === 'exporting'"
                @export="handleExport" />
              <ExplanationPanel :step="currentStep" />
              <ExportStatusCard v-if="exportTask" :export-task="exportTask" @retry="handleExport" />
            </aside>
            <CodeHighlightPanel :source-code="sourceCode" :highlighted-lines="currentStep.highlightedLines" />
          </section>

          <GenerationStatusCard v-else-if="currentUser && task" :task="task">
            <template #actions>
              <button class="secondary-button" data-continue-generation-button type="button"
                @click="continueGeneration">继续生成</button>
            </template>
          </GenerationStatusCard>

          <section v-else class="empty-page-card">
            <p class="panel-kicker">步骤讲解</p>
            <h2>还没有可播放的讲解</h2>
            <p>先新建一次讲解，或从最近生成中打开已有任务</p>
          </section>
        </section>

        <section v-else class="workspace-page">
          <CodeSubmissionPanel
            v-if="currentUser && activePage === 'compose' && (viewState === 'ready' || viewState === 'error-recoverable')"
            v-model="sourceCode" :language="sourceLanguage" :busy="generationBusy" :error-message="submissionErrorMessage"
            @update:language="handleSourceLanguageChange"
            @submit="handleSubmit" />

          <GenerationStatusCard v-else-if="currentUser && activePage === 'compose' && task" :task="task">
            <template #actions>
              <button class="secondary-button" data-continue-generation-button type="button"
                @click="continueGeneration">继续生成</button>
            </template>
          </GenerationStatusCard>

          <section v-else-if="currentUser && activePage === 'generations'" class="history-page-card">
            <div class="history-page-header">
              <p class="panel-kicker">最近生成</p>
              <h2>生成记录</h2>
            </div>
            <p v-if="recentGenerationTasks.length === 0" class="history-empty">暂无生成记录</p>
            <button v-for="item in recentGenerationTasks" :key="`generation-${item.id}`" class="history-list-item"
              :data-generation-history-item="item.id" type="button" @click="handleSelectGeneration(item.id)">
              <strong>{{ item.detectedAlgorithm ? formatAlgorithmName(item.detectedAlgorithm) : item.sourcePreview
                }}</strong>
              <span>{{ formatGenerationStatus(item.status) }}</span>
              <small>{{ formatDisplayTime(item.updatedAt) }}</small>
            </button>
          </section>

          <section v-else-if="currentUser && activePage === 'exports'" class="history-page-card">
            <div class="history-page-header">
              <p class="panel-kicker">最近导出</p>
              <h2>导出记录</h2>
            </div>
            <p v-if="recentExportTasks.length === 0" class="history-empty">暂无导出记录</p>
            <button v-for="item in recentExportTasks" :key="`export-${item.id}`" class="history-list-item"
              :data-export-history-item="item.id" type="button" @click="handleSelectExport(item.id)">
              <strong>{{ item.detectedAlgorithm ? `${formatAlgorithmName(item.detectedAlgorithm)} 导出` : `导出 #${item.id}`
                }}</strong>
              <span>{{ formatExportStatus(item.status) }}</span>
              <small>{{ formatDisplayTime(item.updatedAt) }}</small>
            </button>
          </section>

          <section v-else-if="currentUser && activePage === 'recharge'" class="recharge-page-card">
            <div class="history-page-header">
              <p class="panel-kicker">充值商店</p>
              <h2>购买额度</h2>
              <p>当前余额：{{ currentUser.creditsBalance }}</p>
            </div>
            <div class="recharge-package-grid">
              <article v-for="item in rechargePackages" :key="item.code" class="recharge-package-card">
                <strong>{{ item.name }}</strong>
                <span>{{ item.credits }} 额度</span>
                <small>{{ withoutSentencePeriod(item.description) }}</small>
                <b>￥{{ (item.amountCents / 100).toFixed(2) }}</b>
                <button class="primary-button" type="button" :disabled="rechargeBusy"
                  @click="handleRecharge(item.code)">模拟支付</button>
              </article>
            </div>
            <div class="recharge-orders">
              <h3>最近充值</h3>
              <p v-if="rechargeOrders.length === 0" class="history-empty">暂无充值记录</p>
              <div v-for="order in rechargeOrders" :key="order.id" class="recharge-order-row">
                <span>{{ order.packageName }}</span>
                <span class="recharge-order-time">{{ formatDisplayTime(order.paidAt ?? order.createdAt) }}</span>
                <span>{{ order.credits }} 额度</span>
                <span>{{ formatRechargeStatus(order.status) }}</span>
              </div>
            </div>
          </section>
        </section>

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
  createRechargeOrder,
  getStoredToken,
  getExportTask,
  getGenerationTask,
  getRecentExportTasks,
  getRecentGenerationTasks,
  getRecentRechargeOrders,
  getRechargePackages,
  login,
  me,
  register,
  simulateRechargePayment,
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
  RechargeOrderResponse,
  RechargePackageResponse,
  UserProfile,
} from './types/pas'
import { withoutSentencePeriod } from './utils/displayText'

type ViewState = 'auth' | 'ready' | 'generating' | 'generated' | 'exporting' | 'exported' | 'error-recoverable'
type WorkspacePage = 'compose' | 'player' | 'generations' | 'exports' | 'recharge'
type SourceLanguage = 'java' | 'c' | 'cpp'

const sourceExamples: Record<SourceLanguage, string> = {
  java: `public class QuickSort {
  public static void quickSort(int[] array, int low, int high) {
    if (low >= high) {
      return;
    }

    int pivotIndex = partition(array, low, high);
    quickSort(array, low, pivotIndex - 1);
    quickSort(array, pivotIndex + 1, high);
  }

  private static int partition(int[] array, int low, int high) {
    int pivot = array[high];
    int i = low - 1;

    for (int j = low; j < high; j++) {
      if (array[j] <= pivot) {
        i++;
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
      }
    }

    int temp = array[i + 1];
    array[i + 1] = array[high];
    array[high] = temp;
    return i + 1;
  }
}`,
  c: `int partition(int array[], int low, int high) {
  int pivot = array[high];
  int i = low - 1;

  for (int j = low; j < high; j++) {
    if (array[j] <= pivot) {
      i++;
      int temp = array[i];
      array[i] = array[j];
      array[j] = temp;
    }
  }

  int temp = array[i + 1];
  array[i + 1] = array[high];
  array[high] = temp;
  return i + 1;
}

void quickSort(int array[], int low, int high) {
  if (low >= high) {
    return;
  }

  int pivotIndex = partition(array, low, high);
  quickSort(array, low, pivotIndex - 1);
  quickSort(array, pivotIndex + 1, high);
}`,
  cpp: `#include <vector>
using namespace std;

int partition(vector<int>& array, int low, int high) {
  int pivot = array[high];
  int i = low - 1;

  for (int j = low; j < high; j++) {
    if (array[j] <= pivot) {
      i++;
      int temp = array[i];
      array[i] = array[j];
      array[j] = temp;
    }
  }

  int temp = array[i + 1];
  array[i + 1] = array[high];
  array[high] = temp;
  return i + 1;
}

void quickSort(vector<int>& array, int low, int high) {
  if (low >= high) {
    return;
  }

  int pivotIndex = partition(array, low, high);
  quickSort(array, low, pivotIndex - 1);
  quickSort(array, pivotIndex + 1, high);
}`,
}

const defaultSourceCode = sourceExamples.java

const currentUser = ref<UserProfile | null>(null)
const task = ref<GenerationTaskResponse | null>(null)
const exportMeta = ref<CreateExportTaskResponse | null>(null)
const exportTask = ref<ExportTaskResponse | null>(null)
const recentGenerationTasks = ref<GenerationTaskListItemResponse[]>([])
const recentExportTasks = ref<ExportTaskListItemResponse[]>([])
const rechargePackages = ref<RechargePackageResponse[]>([])
const rechargeOrders = ref<RechargeOrderResponse[]>([])
const sourceCode = ref(defaultSourceCode)
const sourceLanguage = ref<SourceLanguage>('java')
const activeIndex = ref(0)
const playbackSpeed = ref(1)
const isPlaying = ref(false)
const authBusy = ref(false)
const generationBusy = ref(false)
const exportBusy = ref(false)
const rechargeBusy = ref(false)
const authMessage = ref('')
const authErrorMessage = ref('')
const submissionErrorMessage = ref('')
const exportErrorMessage = ref('')
const activityErrorMessage = ref('')
const selectedHistoryKind = ref<'new' | 'generation' | 'export'>('new')
const selectedHistoryId = ref<number | null>(null)
const activePage = ref<WorkspacePage>('compose')
let exportPollHandle: ReturnType<typeof setInterval> | null = null
let generationPollHandle: ReturnType<typeof setInterval> | null = null
let playbackHandle: ReturnType<typeof setInterval> | null = null

const currentStep = computed(() => localizeStep(task.value?.visualizationPayload?.steps[activeIndex.value] ?? {
  title: '暂无步骤',
  narration: '',
  arrayState: [],
  activeIndices: [],
  highlightedLines: [],
}))

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
  if (activePage.value === 'compose') {
    return '新建讲解'
  }

  if (activePage.value === 'generations') {
    return '生成记录'
  }

  if (activePage.value === 'exports') {
    return '导出记录'
  }

  if (activePage.value === 'recharge') {
    return '购买额度'
  }

  return '当前讲解'
})

function getErrorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}

function formatRechargeStatus(status: string) {
  switch (status) {
    case 'PENDING':
      return '待支付'
    case 'PAID':
      return '已入账'
    default:
      return status
  }
}

function formatDisplayTime(value: string) {
  return value.replace('T', ' ').replace(/:\d{2}(?:\.\d+)?Z?$/u, '')
}

function normalizeSourceLanguage(value: string | null | undefined): SourceLanguage {
  return value === 'c' || value === 'cpp' ? value : 'java'
}

function isStarterSource(value: string) {
  return Object.values(sourceExamples).includes(value)
}

function handleSourceLanguageChange(nextLanguage: string) {
  const normalizedLanguage = normalizeSourceLanguage(nextLanguage)
  if (normalizedLanguage === sourceLanguage.value) {
    return
  }

  const shouldReplaceSource = sourceCode.value.trim() === '' || isStarterSource(sourceCode.value)
  sourceLanguage.value = normalizedLanguage
  if (shouldReplaceSource) {
    sourceCode.value = sourceExamples[normalizedLanguage]
  }
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
    case 'HEAP_SORT':
      return '堆排序'
    case 'BFS':
      return '广度优先搜索'
    case 'DFS':
      return '深度优先搜索'
    default:
      return algorithm.replaceAll('_', ' ')
  }
}

function localizeStep<T extends { title: string; narration: string }>(step: T): T {
  return {
    ...step,
    title: localizeStepText(step.title),
    narration: localizeStepText(step.narration),
  }
}

function localizeStepText(text: string) {
  const mapping: Record<string, string> = {
    'Choose pivot': '选择基准值',
    'Use the last value as pivot': '使用当前区间最后一个值作为基准值',
    'Compare to pivot': '与基准值比较',
    'Check whether the value belongs on the left side': '判断当前值是否应该放到基准值左侧',
    'Move left of pivot': '移动到基准值左侧',
    'Keep smaller values before the pivot': '把较小的值保留在基准值前面',
    'Place pivot': '放置基准值',
    'The pivot lands in its final position': '基准值移动到本轮排序后的最终位置',
    'Quick sort complete': '快速排序完成',
    'All partitions are sorted': '所有分区都已经完成排序',
    'Check middle': '检查中间位置',
    'Focus the middle candidate': '聚焦当前区间的中间候选值',
    'Split range': '拆分区间',
    'Prepare two sorted halves for merge': '准备两个已排序子区间用于合并',
    'Merge next value': '合并下一个值',
    'Write the smaller front value back into the array': '把两个子区间前端较小的值写回数组',
    'Append remaining left': '追加左侧剩余值',
    'Copy leftover values from the left half': '把左半区剩余的值复制回数组',
    'Append remaining right': '追加右侧剩余值',
    'Copy leftover values from the right half': '把右半区剩余的值复制回数组',
    'Merge sort complete': '归并排序完成',
    'All ranges have been merged back in order': '所有区间都已经按顺序合并完成',
  }

  return withoutSentencePeriod(mapping[text] ?? text)
}

function isAuthExpiredError(error: unknown) {
  if (!(error instanceof Error)) {
    return false
  }

  const status = Reflect.get(error, 'status')
  return error.name === 'AuthExpiredError' || status === 401
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
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
      return
    }
    activityErrorMessage.value = '无法加载最近记录'
  }
}

async function refreshRechargeStore() {
  try {
    const [packages, orders] = await Promise.all([
      getRechargePackages(),
      getRecentRechargeOrders(),
    ])
    rechargePackages.value = packages
    rechargeOrders.value = orders
    activityErrorMessage.value = ''
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
      return
    }
    activityErrorMessage.value = getErrorMessage(error, '充值商店加载失败')
  }
}

function handleAuthExpired(_message = '登录已过期，请重新登录') {
  clearStoredToken()
  currentUser.value = null
  task.value = null
  recentGenerationTasks.value = []
  recentExportTasks.value = []
  clearHistorySelection()
  authMessage.value = ''
  authErrorMessage.value = '登录已过期，请重新登录'
  submissionErrorMessage.value = ''
  resetExportState()
  stopGenerationPolling()
  stopPlayback()
}

function startOver() {
  task.value = null
  submissionErrorMessage.value = ''
  activePage.value = 'compose'
  clearHistorySelection()
  resetExportState()
  stopGenerationPolling()
  stopPlayback()
}

async function continueGeneration() {
  if (!task.value) {
    return
  }

  const taskId = task.value.id
  activePage.value = 'player'
  submissionErrorMessage.value = ''
  try {
    await refreshGenerationTask(taskId)
    if (task.value && task.value.status !== 'COMPLETED' && task.value.status !== 'FAILED') {
      startGenerationPolling(taskId)
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
      return
    }
    submissionErrorMessage.value = getErrorMessage(error, '刷新生成状态失败')
  }
}

function openNewWalkthrough() {
  activePage.value = 'compose'
  clearHistorySelection()
  sourceLanguage.value = 'java'
  sourceCode.value = defaultSourceCode
  task.value = null
  submissionErrorMessage.value = ''
  activityErrorMessage.value = ''
  resetExportState()
  stopGenerationPolling()
  stopPlayback()
}

function openGenerationsPage() {
  activePage.value = 'generations'
}

function openExportsPage() {
  activePage.value = 'exports'
}

async function openRechargePage() {
  activePage.value = 'recharge'
  if (rechargePackages.value.length === 0) {
    await refreshRechargeStore()
  }
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
    sourceLanguage.value = normalizeSourceLanguage(loadedTask.language)
    selectedHistoryKind.value = 'generation'
    selectedHistoryId.value = taskId
    activePage.value = loadedTask.status === 'FAILED' ? 'compose' : 'player'
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
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
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
    sourceLanguage.value = normalizeSourceLanguage(loadedTask.language)
    selectedHistoryKind.value = 'export'
    selectedHistoryId.value = exportTaskId
    activePage.value = 'player'
    activeIndex.value = 0
    activityErrorMessage.value = ''

    if (loadedExportTask.status !== 'COMPLETED' && loadedExportTask.status !== 'FAILED') {
      startExportPolling(exportTaskId)
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
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
    task.value = await createGenerationTask(nextSourceCode, sourceLanguage.value)
    selectedHistoryKind.value = 'generation'
    selectedHistoryId.value = task.value.id
    activePage.value = 'player'
    await refreshRecentActivity()
    if (task.value.status !== 'COMPLETED') {
      await refreshGenerationTask(task.value.id)
      if (task.value.status !== 'COMPLETED' && task.value.status !== 'FAILED') {
        startGenerationPolling(task.value.id)
      }
    }
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
      return
    }
    activePage.value = 'compose'
    submissionErrorMessage.value = getErrorMessage(error, '生成失败')
  } finally {
    generationBusy.value = false
  }
}

async function handleRecharge(packageCode: string) {
  try {
    rechargeBusy.value = true
    activityErrorMessage.value = ''
    const order = await createRechargeOrder(packageCode)
    await simulateRechargePayment(order.id)
    currentUser.value = await me()
    await refreshRechargeStore()
  } catch (error) {
    if (isAuthExpiredError(error)) {
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
      return
    }
    activityErrorMessage.value = getErrorMessage(error, '充值失败')
  } finally {
    rechargeBusy.value = false
  }
}

async function handleRegister(credentials: { username: string; password: string }) {
  authBusy.value = true
  authMessage.value = ''
  authErrorMessage.value = ''
  try {
    await register(credentials.username, credentials.password)
    authMessage.value = '注册成功，请登录'
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
        handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
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
  sourceLanguage.value = normalizeSourceLanguage(task.value.language)
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
      handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
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
        handleAuthExpired(getErrorMessage(error, '登录已过期，请重新登录'))
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
      authErrorMessage.value = '登录已过期，请重新登录'
    } else {
      authErrorMessage.value = '无法恢复登录状态，请重新登录'
    }
    currentUser.value = null
  }
})
</script>
