<template>
  <div class="playback-controls">
    <button type="button" @click="emit('change', Math.max(0, activeIndex - 1))">Previous</button>
    <span>Step {{ activeIndex + 1 }} / {{ stepCount }}</span>
    <button type="button" @click="emit('change', Math.min(stepCount - 1, activeIndex + 1))">Next</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  stepCount: number
  activeIndex: number
}>()

const emit = defineEmits<{ change: [nextIndex: number] }>()
</script>
