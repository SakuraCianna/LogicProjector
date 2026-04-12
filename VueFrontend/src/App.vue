<template>
  <main class="app-shell">
    <CodeSubmissionPanel v-if="!task" @submit="handleSubmit" />

    <section v-else class="player-layout">
      <TaskSummaryCard :task="task" @export="handleExport" />
      <VisualizationStage :step="currentStep" />
      <ExplanationPanel :step="currentStep" />
      <CodeHighlightPanel :source-code="sourceCode" :highlighted-lines="currentStep.highlightedLines" />
      <ExportStatusCard v-if="exportTask" :export-task="exportTask" />
      <PlaybackControls
        :step-count="task.visualizationPayload?.steps.length ?? 0"
        :active-index="activeIndex"
        @change="activeIndex = $event"
      />
    </section>

    <p v-if="errorMessage" class="error-banner">{{ errorMessage }}</p>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'

import { createExportTask, createGenerationTask, getExportTask } from './api/pasApi'
import CodeHighlightPanel from './components/CodeHighlightPanel.vue'
import CodeSubmissionPanel from './components/CodeSubmissionPanel.vue'
import ExplanationPanel from './components/ExplanationPanel.vue'
import ExportStatusCard from './components/ExportStatusCard.vue'
import PlaybackControls from './components/PlaybackControls.vue'
import TaskSummaryCard from './components/TaskSummaryCard.vue'
import VisualizationStage from './components/VisualizationStage.vue'
import type { CreateExportTaskResponse, ExportTaskResponse, GenerationTaskResponse } from './types/pas'

const task = ref<GenerationTaskResponse | null>(null)
const exportMeta = ref<CreateExportTaskResponse | null>(null)
const exportTask = ref<ExportTaskResponse | null>(null)
const sourceCode = ref('')
const activeIndex = ref(0)
const errorMessage = ref('')
let exportPollHandle: ReturnType<typeof setInterval> | null = null

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
  exportMeta.value = null
  exportTask.value = null

  try {
    task.value = await createGenerationTask(nextSourceCode)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Generation failed'
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
})
</script>
