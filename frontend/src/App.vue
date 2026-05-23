<script setup lang="ts">
import { computed, watchEffect } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { BrainCircuit, ClipboardList, ListChecks, LogOut, Search, Settings, Sparkles, SquareCheckBig, UploadCloud } from 'lucide-vue-next'
import { authState, clearAuth } from './services/auth'
import { requireAuth } from './routes'

const route = useRoute()
const router = useRouter()
const isLogin = computed(() => route.path === '/login')

watchEffect(() => {
  const target = requireAuth(route.path)
  if (target !== route.path) {
    router.replace(target)
  }
})

function logout() {
  clearAuth()
  router.replace('/login')
}
</script>

<template>
  <main v-if="isLogin" class="login-shell">
    <RouterView />
  </main>
  <main v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">AI</span>
        <div>
          <strong>AIOIProblem</strong>
          <small>教学选题工作台</small>
        </div>
      </div>

      <nav class="nav-list" aria-label="主导航">
        <RouterLink to="/analysis"><UploadCloud :size="18" />题面分析</RouterLink>
        <RouterLink to="/problems"><Search :size="18" />题目搜索</RouterLink>
        <RouterLink to="/sets"><ClipboardList :size="18" />题单管理</RouterLink>
        <RouterLink to="/batch"><ListChecks :size="18" />批量任务</RouterLink>
        <RouterLink to="/recommendations"><Sparkles :size="18" />AI推荐</RouterLink>
        <RouterLink to="/passed"><SquareCheckBig :size="18" />通过记录</RouterLink>
        <RouterLink to="/settings"><Settings :size="18" />AI设置</RouterLink>
      </nav>

      <div class="sidebar-footer">
        <div class="user-chip">
          <BrainCircuit :size="18" />
          <span>{{ authState.user?.username }}</span>
        </div>
        <button class="icon-text" type="button" @click="logout" title="退出登录">
          <LogOut :size="17" />退出
        </button>
      </div>
    </aside>

    <section class="workspace">
      <RouterView />
    </section>
  </main>
</template>
