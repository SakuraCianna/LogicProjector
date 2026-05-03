<template>
  <section class="explanation-panel">
    <p class="panel-kicker">当前步骤</p>
    <h3>{{ withoutSentencePeriod(step.title) }}</h3>
    <p>{{ withoutSentencePeriod(step.narration) }}</p>
  </section>
</template>

<script setup lang="ts">
import type { VisualizationStep } from '../types/pas'
import { withoutSentencePeriod } from '../utils/displayText'

defineProps<{
  step: VisualizationStep
}>()
</script>
