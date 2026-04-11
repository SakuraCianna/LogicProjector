<template>
  <main class="app-shell">
    <CodeSubmissionPanel v-if="!task" @submit="handleSubmit" />

    <section v-else class="player-layout">
      <TaskSummaryCard :task="task" />
      <VisualizationStage :step="currentStep" />
      <ExplanationPanel :step="currentStep" />
      <CodeHighlightPanel :source-code="sourceCode" :highlighted-lines="currentStep.highlightedLines" />
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
import { computed, ref } from 'vue'

import { createGenerationTask } from './api/pasApi'
import CodeHighlightPanel from './components/CodeHighlightPanel.vue'
import CodeSubmissionPanel from './components/CodeSubmissionPanel.vue'
import ExplanationPanel from './components/ExplanationPanel.vue'
import PlaybackControls from './components/PlaybackControls.vue'
import TaskSummaryCard from './components/TaskSummaryCard.vue'
import VisualizationStage from './components/VisualizationStage.vue'
import type { GenerationTaskResponse } from './types/pas'

const task = ref<GenerationTaskResponse | null>(null)
const sourceCode = ref('')
const activeIndex = ref(0)
const errorMessage = ref('')

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

  try {
    task.value = await createGenerationTask(nextSourceCode)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Generation failed'
  }
}
</script>
