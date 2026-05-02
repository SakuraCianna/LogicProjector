<template>
  <section class="export-status-card">
    <p class="panel-kicker">导出状态</p>
    <h3>{{ statusLabel }}</h3>
    <p>{{ statusCopy }}</p>
    <p>进度：{{ exportTask.progress }}%</p>
    <p>冻结额度：{{ exportTask.creditsFrozen ?? 0 }}</p>
    <p v-if="exportTask.creditsCharged !== null">实际扣费：{{ exportTask.creditsCharged }}</p>
    <p v-if="exportTask.errorMessage" class="export-error">{{ exportTask.errorMessage }}</p>
    <button v-if="exportTask.status === 'FAILED'" class="secondary-button" data-retry-export-button type="button" @click="$emit('retry')">重新导出</button>
    <a v-if="exportTask.videoUrl" :href="exportTask.videoUrl" data-download-link>下载视频</a>
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
    return '视频已导出完成，可以在下方下载。'
  }

  if (props.exportTask.status === 'FAILED') {
    return '导出失败，但已生成的讲解仍可继续查看。'
  }

  return '视频正在导出，你可以留在当前页面等待完成。'
})

const statusLabel = computed(() => {
  switch (props.exportTask.status) {
    case 'PENDING':
      return '排队中'
    case 'PROCESSING':
      return '处理中'
    case 'COMPLETED':
      return '已完成'
    case 'FAILED':
      return '失败'
    default:
      return props.exportTask.status
  }
})
</script>
