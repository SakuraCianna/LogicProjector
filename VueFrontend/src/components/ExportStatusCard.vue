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
    <a v-if="exportTask.videoUrl" :href="exportTask.videoUrl" data-download-link>Download video</a>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { ExportTaskResponse } from '../types/pas'

const props = defineProps<{
  exportTask: ExportTaskResponse
}>()

defineEmits<{
  retry: []
}>()

const statusCopy = computed(() => {
  if (props.exportTask.status === 'COMPLETED') {
    return 'Video export is ready. Download the result below.'
  }

  if (props.exportTask.status === 'FAILED') {
    return 'Export failed, but your generated walkthrough is still available.'
  }

  return 'Export is running. You can stay on this page while Pas finishes the video.'
})
</script>
