<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { login } from '@/api/modules/login'

const route = useRoute()
const router = useRouter()

const form = reactive({
  username: '',
  password: '',
})

const loading = ref(false)
const errorMessage = ref('')

const canSubmit = computed(() => Boolean(form.username.trim() && form.password.trim()))

async function handleLogin() {
  if (!canSubmit.value || loading.value) {
    return
  }

  try {
    loading.value = true
    errorMessage.value = ''

    await login({
      username: form.username.trim(),
      password: form.password,
    })

    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/app'

    await router.replace(redirect)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section
    class="w-full max-w-md rounded-lg border border-white/40 bg-white/30 p-8 shadow-2xl shadow-slate-900/15 backdrop-blur-xl"
  >
    <header class="mb-8">
      <p class="text-sm font-medium text-sky-700">SlideForge</p>
      <h1 class="mt-2 text-2xl font-semibold text-slate-950">登录工作台</h1>
    </header>

    <form class="grid gap-5" @submit.prevent="handleLogin">
      <label class="grid gap-2">
        <span class="text-sm font-medium text-slate-700">用户名</span>
        <input
          v-model="form.username"
          autocomplete="username"
          class="h-11 rounded-md border border-white/60 bg-white/70 px-3 text-slate-950 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-200"
          placeholder="请输入用户名"
          type="text"
        />
      </label>

      <label class="grid gap-2">
        <span class="text-sm font-medium text-slate-700">用户密码</span>
        <input
          v-model="form.password"
          autocomplete="current-password"
          class="h-11 rounded-md border border-white/60 bg-white/70 px-3 text-slate-950 outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-200"
          placeholder="请输入用户密码"
          type="password"
          @keydown.enter.prevent="handleLogin"
        />
      </label>

      <p v-if="errorMessage" class="text-sm text-red-600">{{ errorMessage }}</p>

      <button
        class="mt-2 h-11 rounded-md bg-slate-950 px-4 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
        :disabled="!canSubmit || loading"
        type="submit"
      >
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>
  </section>
</template>
