<template>
  <section class="task-summary-card">
    <div>
      <p class="panel-kicker">生成结果</p>
      <h2>{{ algorithmName }}</h2>
      <p>{{ summaryText }}</p>
    </div>
    <button v-if="task.status === 'COMPLETED'" class="primary-button" data-export-button type="button"
      :disabled="exportBusy" @click="$emit('export')">
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

const summaryText = computed(() => {
  const summary = props.task.summary ?? ''
  const mapping: Record<string, string> = {
    'QuickSort recursively partitions the array around a pivot element to sort it.': '快速排序围绕基准值不断分区，并递归完成左右两侧排序。',
    'Quick sort picks a pivot and partitions the array.': '快速排序选择基准值，将数组划分为较小值和较大值两部分。',
    'Quick sort picks a pivot, partitions the array, and recursively sorts both sides.': '快速排序选择基准值完成分区，再递归排序左右两侧。',
  }

  return mapping[summary] ?? summary
})

defineEmits<{
  export: []
}>()
</script>
