<template>
  <section class="export-status-card">
    <p class="panel-kicker">Export status</p>
    <h3>{{ exportTask.status }}</h3>
    <p>Progress: {{ exportTask.progress }}%</p>
    <p>Frozen credits: {{ exportTask.creditsFrozen ?? 0 }}</p>
    <p v-if="exportTask.creditsCharged !== null">Charged credits: {{ exportTask.creditsCharged }}</p>
    <p v-if="exportTask.errorMessage" class="export-error">{{ exportTask.errorMessage }}</p>
    <a v-if="exportTask.videoUrl" :href="exportTask.videoUrl" data-download-link>Download video</a>
  </section>
</template>

<script setup lang="ts">
import type { ExportTaskResponse } from '../types/pas'

defineProps<{
  exportTask: ExportTaskResponse
}>()
</script>
