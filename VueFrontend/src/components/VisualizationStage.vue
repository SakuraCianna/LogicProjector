<template>
  <section class="visualization-stage">
    <header class="stage-header">
      <p class="stage-kicker">Data structure state</p>
      <h2>{{ step.title }}</h2>
    </header>

    <div class="array-bars">
      <div
        v-for="(value, index) in step.arrayState"
        :key="`${index}-${value}`"
        class="array-bar"
        :class="{ active: step.activeIndices.includes(index) }"
        :style="{ height: `${Math.max(56, value * 18)}px` }"
        :data-array-value="value"
      >
        <span>{{ value }}</span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { VisualizationStep } from '../types/pas'

defineProps<{
  step: VisualizationStep
}>()
</script>
