<template>
  <form class="submission-panel" @submit.prevent="emit('submit', sourceCode)">
    <div class="panel-header">
      <p class="panel-kicker">Pas</p>
      <h1>Generate an algorithm walkthrough</h1>
      <p class="panel-copy">Paste Java algorithm code and turn it into a guided visualization for class.</p>
    </div>
    <textarea v-model="sourceCode" class="code-input" spellcheck="false" />
    <button class="primary-button" type="submit">Generate walkthrough</button>
  </form>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{ submit: [sourceCode: string] }>()

const sourceCode = ref(`public class QuickSort {
  void sort(int[] arr, int low, int high) {
    int pivot = arr[high];
    partition(arr, low, high);
  }
}`)
</script>
