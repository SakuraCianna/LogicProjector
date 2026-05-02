<template>
  <section class="generation-status-card">
    <p class="panel-kicker">生成状态</p>
    <h2>{{ statusLabel }}</h2>
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
      return '讲解任务已进入队列，正在等待后台处理。'
    case 'ANALYZING':
      return '系统正在识别算法并准备讲解流程。'
    case 'FAILED':
      return '生成失败，暂时无法创建讲解流程。'
    default:
      return '系统正在处理你的请求。'
  }
})

const statusLabel = computed(() => {
  switch (props.task.status) {
    case 'PENDING':
      return '排队中'
    case 'ANALYZING':
      return '分析中'
    case 'COMPLETED':
      return '已完成'
    case 'FAILED':
      return '失败'
    default:
      return props.task.status
  }
})
</script>
