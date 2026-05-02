<template>
  <section class="auth-panel">
    <div class="auth-panel__header">
      <p class="panel-kicker">账号中心</p>
      <h1>{{ mode === 'login' ? '欢迎回来' : '创建账号' }}</h1>
      <p class="panel-copy">进入你的算法讲解空间，继续生成课堂可视化和导出视频。</p>
    </div>

    <form class="auth-form" @submit.prevent="submit">
      <label>
        <span>用户名</span>
        <input v-model="username" type="text" placeholder="用户名" autocomplete="username">
      </label>
      <label>
        <span>密码</span>
        <input v-model="password" type="password" placeholder="密码" autocomplete="current-password">
      </label>
      <button class="primary-button" type="submit" :disabled="busy">{{ submitLabel }}</button>
    </form>

    <div class="auth-panel__feedback">
      <p v-if="successMessage" class="auth-message auth-success">{{ successMessage }}</p>
      <p v-if="errorMessage" class="auth-message auth-error">{{ errorMessage }}</p>
    </div>

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
