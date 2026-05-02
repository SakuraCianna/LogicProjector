<template>
  <section class="auth-panel">
    <p class="panel-kicker">账号中心</p>
    <h1>{{ mode === 'login' ? '登录' : '注册' }}</h1>
    <p class="panel-copy">使用账号和密码进入算法讲解生成与视频导出工作台。</p>

    <form class="auth-form" @submit.prevent="submit">
      <input v-model="username" type="text" placeholder="用户名" autocomplete="username">
      <input v-model="password" type="password" placeholder="密码" autocomplete="current-password">
      <button class="primary-button" type="submit" :disabled="busy">{{ submitLabel }}</button>
    </form>

    <p v-if="successMessage" class="auth-message auth-success">{{ successMessage }}</p>
    <p v-if="errorMessage" class="auth-message auth-error">{{ errorMessage }}</p>

    <button class="auth-toggle" type="button" @click="toggleMode">
      {{ mode === 'login' ? '还没有账号？去注册' : '已有账号？去登录' }}
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
    return '处理中...'
  }

  return mode.value === 'login' ? '登录' : '注册'
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
