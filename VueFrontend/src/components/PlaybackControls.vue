<template>
  <div class="playback-controls">
    <button data-play-toggle type="button" @click="emit('toggle-play')">{{ isPlaying ? '暂停' : '播放' }}</button>
    <button type="button" @click="emit('change', Math.max(0, activeIndex - 1))">上一步</button>
    <span>步骤 {{ activeIndex + 1 }} / {{ stepCount }}</span>
    <input
      data-step-slider
      type="range"
      min="0"
      :max="Math.max(0, stepCount - 1)"
      :value="activeIndex"
      @input="emit('change', Number(($event.target as HTMLInputElement).value))"
    >
    <select
      data-speed-select
      :value="playbackSpeed"
      @change="emit('change-speed', Number(($event.target as HTMLSelectElement).value))"
    >
      <option :value="0.75">0.75x</option>
      <option :value="1">1x</option>
      <option :value="1.5">1.5x</option>
      <option :value="2">2x</option>
    </select>
    <button type="button" @click="emit('change', Math.min(stepCount - 1, activeIndex + 1))">下一步</button>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  stepCount: number
  activeIndex: number
  isPlaying: boolean
  playbackSpeed: number
}>()

const emit = defineEmits<{
  change: [nextIndex: number]
  'toggle-play': []
  'change-speed': [speed: number]
}>()
</script>
