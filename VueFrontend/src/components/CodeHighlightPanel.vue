<template>
  <section class="code-highlight-panel">
    <header>
      <p class="panel-kicker">源码定位</p>
      <h3>代码高亮</h3>
    </header>
    <pre><code><span
      v-for="(line, index) in lines"
      :key="index"
      class="code-line"
      :class="{ highlighted: highlightedLines.includes(index + 1) }"
    ><span class="line-number">{{ index + 1 }}</span>{{ line }}
</span></code></pre>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  sourceCode: string
  highlightedLines: number[]
}>()

const lines = computed(() => props.sourceCode.split('\n'))
</script>
