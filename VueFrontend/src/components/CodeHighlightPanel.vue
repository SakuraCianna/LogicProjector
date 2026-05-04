<template>
  <section class="code-highlight-panel">
    <header>
      <p class="panel-kicker">源码定位</p>
      <h3>代码高亮</h3>
    </header>
    <pre><code><span
      v-for="(line, index) in lines"
      :key="index"
      :ref="(element) => setLineRef(element, index + 1)"
      class="code-line"
      :class="{ highlighted: highlightedLines.includes(index + 1) }"
    ><span class="line-number">{{ index + 1 }}</span>{{ line }}
</span></code></pre>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'

const props = defineProps<{
  sourceCode: string
  highlightedLines: number[]
}>()

const lines = computed(() => props.sourceCode.split('\n'))
const lineRefs = ref(new Map<number, Element>())

const firstHighlightedLine = computed(() => {
  const firstHighlight = props.highlightedLines
    .filter((line) => line >= 1 && line <= lines.value.length)
    .sort((left, right) => left - right)[0]
  return firstHighlight ?? 1
})

function setLineRef(element: unknown, lineNumber: number) {
  if (element instanceof Element) {
    lineRefs.value.set(lineNumber, element)
  }
}

watch(firstHighlightedLine, async (lineNumber) => {
  await nextTick()
  const lineElement = lineRefs.value.get(lineNumber)
  if (lineElement && typeof lineElement.scrollIntoView === 'function') {
    lineElement.scrollIntoView({ block: 'center', inline: 'nearest' })
  }
}, { immediate: true })
</script>
