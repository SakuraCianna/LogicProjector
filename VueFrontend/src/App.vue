<template>
  <main class="app-shell">
    <CodeSubmissionPanel v-if="!task" @submit="handleSubmit" />

    <section v-else-if="task.status === 'COMPLETED'" class="player-layout">
      <TaskSummaryCard :task="task" @export="handleExport" />
      <VisualizationStage :step="currentStep" />
      <ExplanationPanel :step="currentStep" />
      <CodeHighlightPanel :source-code="sourceCode" :highlighted-lines="currentStep.highlightedLines" />
      <ExportStatusCard v-if="exportTask" :export-task="exportTask" />
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

    <GenerationStatusCard v-else :task="task" />

    <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'

import { createExportTask, createGenerationTask, getExportTask, getGenerationTask } from './api/pasApi'
import CodeHighlightPanel from './components/CodeHighlightPanel.vue'
import CodeSubmissionPanel from './components/CodeSubmissionPanel.vue'
import ExplanationPanel from './components/ExplanationPanel.vue'
import ExportStatusCard from './components/ExportStatusCard.vue'
import GenerationStatusCard from './components/GenerationStatusCard.vue'
import PlaybackControls from './components/PlaybackControls.vue'
import TaskSummaryCard from './components/TaskSummaryCard.vue'
import VisualizationStage from './components/VisualizationStage.vue'
import type { CreateExportTaskResponse, ExportTaskResponse, GenerationTaskResponse } from './types/pas'

const task = ref<GenerationTaskResponse | null>(null)
const exportMeta = ref<CreateExportTaskResponse | null>(null)
const exportTask = ref<ExportTaskResponse | null>(null)
const sourceCode = ref('')
const activeIndex = ref(0)
const playbackSpeed = ref(1)
const errorMessage = ref('')
const isPlaying = ref(false)
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

async function handleSubmit(nextSourceCode: string) {
  sourceCode.value = nextSourceCode
  errorMessage.value = ''
  activeIndex.value = 0
  playbackSpeed.value = 1
  exportMeta.value = null
  exportTask.value = null
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
    errorMessage.value = error instanceof Error ? error.message : 'Generation failed'
  }
}

function startGenerationPolling(taskId: number) {
  if (generationPollHandle) {
    clearInterval(generationPollHandle)
  }

  generationPollHandle = setInterval(async () => {
    await refreshGenerationTask(taskId)
    if (task.value && (task.value.status === 'COMPLETED' || task.value.status === 'FAILED')) {
      clearInterval(generationPollHandle!)
      generationPollHandle = null
    }
  }, 3000)
}

async function refreshGenerationTask(taskId: number) {
  task.value = await getGenerationTask(taskId)
  if (task.value.status === 'FAILED' && task.value.errorMessage) {
    errorMessage.value = task.value.errorMessage
  }
}

async function handleExport() {
  if (!task.value) {
    return
  }

  try {
    exportMeta.value = await createExportTask(task.value.id)
    await refreshExportTask(exportMeta.value.id)

    if (exportTask.value && exportTask.value.status !== 'COMPLETED' && exportTask.value.status !== 'FAILED') {
      startExportPolling(exportMeta.value.id)
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Export creation failed'
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
  if (exportPollHandle) {
    clearInterval(exportPollHandle)
  }

  exportPollHandle = setInterval(async () => {
    await refreshExportTask(exportTaskId)
    if (exportTask.value && (exportTask.value.status === 'COMPLETED' || exportTask.value.status === 'FAILED')) {
      clearInterval(exportPollHandle!)
      exportPollHandle = null
    }
  }, 3000)
}

async function refreshExportTask(exportTaskId: number) {
  exportTask.value = await getExportTask(exportTaskId)
}

onBeforeUnmount(() => {
  if (exportPollHandle) {
    clearInterval(exportPollHandle)
  }
  if (generationPollHandle) {
    clearInterval(generationPollHandle)
  }
  stopPlayback()
})
</script>
