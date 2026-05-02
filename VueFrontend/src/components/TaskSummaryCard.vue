<template>
  <section class="task-summary-card">
    <p class="panel-kicker">生成结果</p>
    <h2>{{ algorithmName }}</h2>
    <p>{{ task.summary }}</p>
    <div class="summary-grid">
      <span>置信度：{{ task.confidenceScore.toFixed(2) }}</span>
      <span>消耗额度：{{ task.creditsCharged }}</span>
    </div>
    <button v-if="task.status === 'COMPLETED'" data-export-button type="button" :disabled="exportBusy" @click="$emit('export')">
      {{ exportBusy ? '正在导出...' : '导出视频' }}
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

import type { GenerationTaskResponse } from '../types/pas'

const props = withDefaults(defineProps<{
  task: GenerationTaskResponse
  exportBusy?: boolean
}>(), {
  exportBusy: false,
})

const algorithmName = computed(() => {
  switch (props.task.detectedAlgorithm) {
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
    case null:
    case undefined:
      return '未识别算法'
    default:
      return props.task.detectedAlgorithm.replaceAll('_', ' ')
  }
})

defineEmits<{
  export: []
}>()
</script>
