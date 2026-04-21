<template>
  <form class="submission-panel" @submit.prevent="submit">
    <div class="panel-header">
      <p class="panel-kicker">Pas</p>
      <h1>Generate an algorithm walkthrough</h1>
      <p class="panel-copy">Paste Java algorithm code and turn it into a guided visualization for class.</p>
    </div>
    <textarea v-model="sourceCode" class="code-input" spellcheck="false" />
    <p v-if="inlineMessage || errorMessage" class="submission-message">{{ inlineMessage || errorMessage }}</p>
    <button class="primary-button" type="submit" :disabled="busy">{{ busy ? 'Generating...' : 'Generate walkthrough' }}</button>
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
    inlineMessage.value = 'Paste Java code before generating a walkthrough.'
    return
  }

  inlineMessage.value = ''
  emit('submit', sourceCode.value)
}
</script>
