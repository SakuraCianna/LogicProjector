<template>
  <form class="submission-panel" @submit.prevent="submit">
    <div class="panel-header">
      <div>
        <p class="panel-kicker">新建讲解</p>
        <h1>把一段 Java 算法变成可播放课程。</h1>
      </div>
      <p class="panel-copy">系统会识别算法、拆解步骤、生成可视化播放页，并支持视频导出。</p>
    </div>
    <label class="code-input-shell">
      <span>Java 源码</span>
      <textarea v-model="sourceCode" class="code-input" spellcheck="false" />
    </label>
    <div class="submission-panel__footer">
      <p v-if="inlineMessage || errorMessage" class="submission-message">{{ inlineMessage || errorMessage }}</p>
      <p v-else class="submission-hint">推荐提交一个完整方法或类，讲解会更稳定。</p>
      <button class="primary-button" type="submit" :disabled="busy">{{ busy ? '正在生成...' : '生成讲解' }}</button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  busy?: boolean
  errorMessage?: string
}>(), {
  modelValue: `public class QuickSort {
  void sort(int[] arr, int low, int high) {
    int pivot = arr[high];
    partition(arr, low, high);
  }
}`,
  busy: false,
  errorMessage: '',
})

const inlineMessage = ref('')

const emit = defineEmits<{
  submit: [sourceCode: string]
  'update:modelValue': [value: string]
}>()

const sourceCode = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
})

function submit() {
  if (props.busy) {
    return
  }

  if (!sourceCode.value.trim()) {
    inlineMessage.value = '请先粘贴 Java 代码，再生成讲解。'
    return
  }

  inlineMessage.value = ''
  emit('submit', sourceCode.value)
}
</script>
