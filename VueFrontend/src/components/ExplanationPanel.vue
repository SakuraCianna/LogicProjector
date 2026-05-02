<template>
  <section class="explanation-panel">
    <p class="panel-kicker">当前步骤</p>
    <h3>{{ step.title }}</h3>
    <p>{{ step.narration }}</p>
  </section>
</template>

<script setup lang="ts">
import type { VisualizationStep } from '../types/pas'

defineProps<{
  step: VisualizationStep
}>()
</script>
