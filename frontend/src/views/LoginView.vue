<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { LogIn, UserPlus } from 'lucide-vue-next'
import { api } from '../services/api'
import { setAuth } from '../services/auth'

const router = useRouter()
const username = ref('teacher')
const password = ref('password123')
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const auth = mode.value === 'login'
      ? await api.login(username.value, password.value)
      : await api.register(username.value, password.value)
    setAuth(auth)
    router.replace('/analysis')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '操作失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="login-panel panel">
    <div class="login-title">
      <span class="brand-mark">AI</span>
      <div>
        <h1>AIOIProblem</h1>
        <p>内部教学辅助选题平台</p>
      </div>
    </div>

    <form class="grid" @submit.prevent="submit">
      <label class="field">
        <span>用户名</span>
        <input v-model="username" class="input" autocomplete="username" minlength="3" required />
      </label>
      <label class="field">
        <span>密码</span>
        <input v-model="password" class="input" type="password" autocomplete="current-password" minlength="6" required />
      </label>
      <p v-if="error" class="error">{{ error }}</p>
      <div class="actions">
        <button class="primary" type="submit" :disabled="loading">
          <LogIn v-if="mode === 'login'" :size="18" />
          <UserPlus v-else :size="18" />
          {{ mode === 'login' ? '登录' : '注册' }}
        </button>
        <button class="ghost" type="button" @click="mode = mode === 'login' ? 'register' : 'login'">
          {{ mode === 'login' ? '切换到注册' : '已有账号，去登录' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.login-panel {
  width: min(440px, 100%);
  padding: 26px;
}

.login-title {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 22px;
}

h1 {
  margin: 0;
  font-size: 32px;
  line-height: 1.1;
}

p {
  margin: 6px 0 0;
  color: #59655f;
}
</style>
