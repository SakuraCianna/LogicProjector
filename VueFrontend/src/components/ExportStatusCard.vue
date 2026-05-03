<template>
  <section class="export-status-card">
    <div class="export-status-card__header">
      <p class="panel-kicker">导出状态</p>
      <h3>{{ statusLabel }}</h3>
      <p>{{ statusCopy }}</p>
    </div>
    <div class="export-progress">
      <span :style="{ width: `${exportTask.progress}%` }"></span>
    </div>
    <dl class="export-meta">
      <div>
        <dt>进度</dt>
        <dd>{{ exportTask.progress }}%</dd>
      </div>
      <div>
        <dt>冻结额度</dt>
        <dd>{{ exportTask.creditsFrozen ?? 0 }}</dd>
      </div>
      <div v-if="exportTask.creditsCharged !== null">
        <dt>实际扣费</dt>
        <dd>{{ exportTask.creditsCharged }}</dd>
      </div>
    </dl>
    <p v-if="displayExportError" class="export-error">{{ displayExportError }}</p>
    <button v-if="exportTask.status === 'FAILED'" class="secondary-button" data-retry-export-button type="button"
      @click="$emit('retry')">重新导出</button>
    <button v-if="exportTask.videoUrl" class="primary-button" type="button" :disabled="downloadBusy"
      data-download-button @click="handleDownload">
      {{ downloadBusy ? '下载中' : '下载视频' }}
    </button>
    <p v-if="displayDownloadError" class="download-error">{{ displayDownloadError }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

import { downloadExportVideo } from '../api/pasApi'
import type { ExportTaskResponse } from '../types/pas'
import { withoutSentencePeriod } from '../utils/displayText'

const props = defineProps<{
  exportTask: ExportTaskResponse
}>()

defineEmits<{
  retry: []
}>()

const downloadBusy = ref(false)
const downloadError = ref('')

const displayExportError = computed(() => withoutSentencePeriod(props.exportTask.errorMessage))
const displayDownloadError = computed(() => withoutSentencePeriod(downloadError.value))

async function handleDownload() {
  if (!props.exportTask.videoUrl) return
  downloadBusy.value = true
  downloadError.value = ''
  try {
    const filename = `export-${props.exportTask.id}.mp4`
    await downloadExportVideo(props.exportTask.id, filename)
  } catch (error) {
    downloadError.value = error instanceof Error ? error.message : '下载失败'
  } finally {
    downloadBusy.value = false
  }
}

const statusCopy = computed(() => {
  if (props.exportTask.status === 'COMPLETED') {
    return '视频已导出完成，可以在下方下载'
  }

  if (props.exportTask.status === 'FAILED') {
    return '导出失败，但已生成的讲解仍可继续查看'
  }

  return '视频正在导出，你可以留在当前页面等待完成'
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
