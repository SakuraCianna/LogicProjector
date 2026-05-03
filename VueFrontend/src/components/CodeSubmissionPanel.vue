<template>
  <form class="submission-panel" @submit.prevent="submit">
    <div class="panel-header">
      <div>
        <p class="panel-kicker">新建讲解</p>
        <h1>把一段算法代码变成可播放课程</h1>
      </div>
      <p class="panel-copy">系统会识别算法、拆解步骤、生成可视化播放页，并支持视频导出</p>
    </div>
    <label class="language-select-shell">
      <span>源码语言</span>
      <select :value="language" @change="emit('update:language', ($event.target as HTMLSelectElement).value)">
        <option value="java">Java</option>
        <option value="c">C</option>
        <option value="cpp">C++</option>
      </select>
    </label>
    <label class="code-input-shell">
      <span>Java / C / C++ 源码</span>
      <textarea v-model="sourceCode" class="code-input" spellcheck="false" />
    </label>
    <div class="submission-panel__footer">
      <p v-if="displayMessage" class="submission-message">{{ displayMessage }}</p>
      <p v-else class="submission-hint">推荐提交一个完整函数、方法或类，讲解会更稳定</p>
      <button class="primary-button" type="submit" :disabled="busy">{{ busy ? '正在生成' : '生成讲解' }}</button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

import { withoutSentencePeriod } from '../utils/displayText'

const props = withDefaults(defineProps<{
  modelValue?: string
  language?: string
  busy?: boolean
  errorMessage?: string
}>(), {
  modelValue: `public class QuickSort {
  public static void quickSort(int[] array, int low, int high) {
    if (low >= high) {
      return;
    }

    int pivotIndex = partition(array, low, high);
    quickSort(array, low, pivotIndex - 1);
    quickSort(array, pivotIndex + 1, high);
  }

  private static int partition(int[] array, int low, int high) {
    int pivot = array[high];
    int i = low - 1;

    for (int j = low; j < high; j++) {
      if (array[j] <= pivot) {
        i++;
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
      }
    }

    int temp = array[i + 1];
    array[i + 1] = array[high];
    array[high] = temp;
    return i + 1;
  }
}`,
  language: 'java',
  busy: false,
  errorMessage: '',
})

const inlineMessage = ref('')

const emit = defineEmits<{
  submit: [sourceCode: string]
  'update:modelValue': [value: string]
  'update:language': [value: string]
}>()

const sourceCode = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value),
})

const displayMessage = computed(() => withoutSentencePeriod(inlineMessage.value || props.errorMessage))

function submit() {
  if (props.busy) {
    return
  }

  if (!sourceCode.value.trim()) {
    inlineMessage.value = '请先粘贴 Java / C / C++ 代码，再生成讲解'
    return
  }

  inlineMessage.value = ''
  emit('submit', sourceCode.value)
}
</script>
