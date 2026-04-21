<template>
  <section class="task-summary-card">
    <p class="panel-kicker">Generated result</p>
    <h2>{{ task.detectedAlgorithm }}</h2>
    <p>{{ task.summary }}</p>
    <div class="summary-grid">
      <span>Confidence: {{ task.confidenceScore.toFixed(2) }}</span>
      <span>Credits: {{ task.creditsCharged }}</span>
    </div>
    <button v-if="task.status === 'COMPLETED'" data-export-button type="button" :disabled="exportBusy" @click="$emit('export')">
      {{ exportBusy ? 'Exporting...' : 'Export video' }}
    </button>
  </section>
</template>

<script setup lang="ts">
import type { GenerationTaskResponse } from '../types/pas'

withDefaults(defineProps<{
  task: GenerationTaskResponse
  exportBusy?: boolean
}>(), {
  exportBusy: false,
})

defineEmits<{
  export: []
}>()
</script>
