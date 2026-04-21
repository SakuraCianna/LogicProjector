<template>
  <section class="generation-status-card">
    <p class="panel-kicker">Generation status</p>
    <h2>{{ task.status }}</h2>
    <p>{{ statusCopy }}</p>
    <slot name="actions" />
    <p v-if="task.errorMessage" class="generation-error">{{ task.errorMessage }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { GenerationTaskResponse } from '../types/pas'

const props = defineProps<{
  task: GenerationTaskResponse
}>()

const statusCopy = computed(() => {
  switch (props.task.status) {
    case 'PENDING':
      return 'Your walkthrough has been queued and is waiting for a worker.'
    case 'ANALYZING':
      return 'Pas is recognizing the algorithm and preparing the walkthrough.'
    case 'FAILED':
      return 'Generation failed before a walkthrough could be created.'
    default:
      return 'Pas is processing your request.'
  }
})
</script>
