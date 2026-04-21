<template>
  <section class="auth-panel">
    <p class="panel-kicker">Pas account</p>
    <h1>{{ mode === 'login' ? 'Login' : 'Register' }}</h1>
    <p class="panel-copy">Use a username and password to access generation and export features.</p>

    <form class="auth-form" @submit.prevent="submit">
      <input v-model="username" type="text" placeholder="Username" autocomplete="username">
      <input v-model="password" type="password" placeholder="Password" autocomplete="current-password">
      <button class="primary-button" type="submit" :disabled="busy">{{ submitLabel }}</button>
    </form>

    <p v-if="successMessage" class="auth-message auth-success">{{ successMessage }}</p>
    <p v-if="errorMessage" class="auth-message auth-error">{{ errorMessage }}</p>

    <button class="auth-toggle" type="button" @click="toggleMode">
      {{ mode === 'login' ? 'Need an account? Register' : 'Already registered? Login' }}
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{
  busy?: boolean
  errorMessage?: string
  successMessage?: string
}>(), {
  busy: false,
  errorMessage: '',
  successMessage: '',
})

const emit = defineEmits<{
  login: [credentials: { username: string; password: string }]
  register: [credentials: { username: string; password: string }]
}>()

const mode = ref<'login' | 'register'>('login')
const username = ref('')
const password = ref('')

const submitLabel = computed(() => {
  if (props.busy) {
    return 'Working...'
  }

  return mode.value === 'login' ? 'Login' : 'Register'
})

function submit() {
  if (props.busy) {
    return
  }

  if (mode.value === 'login') {
    emit('login', { username: username.value, password: password.value })
    return
  }

  emit('register', { username: username.value, password: password.value })
}

function toggleMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
}
</script>
