<template>
  <form class="submission-panel" @submit.prevent="submit">
    <div class="panel-header">
      <p class="panel-kicker">算法讲解</p>
      <h1>生成算法教学演示</h1>
      <p class="panel-copy">粘贴 Java 算法代码，生成适合课堂讲解的可视化流程。</p>
    </div>
    <textarea v-model="sourceCode" class="code-input" spellcheck="false" />
    <p v-if="inlineMessage || errorMessage" class="submission-message">{{ inlineMessage || errorMessage }}</p>
    <button class="primary-button" type="submit" :disabled="busy">{{ busy ? '正在生成...' : '生成讲解' }}</button>
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
