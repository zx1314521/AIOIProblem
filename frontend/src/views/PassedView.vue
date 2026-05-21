<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Search } from 'lucide-vue-next'
import { api } from '../services/api'
import type { Problem } from '../types'

const problems = ref<Problem[]>([])
const keyword = ref('')
const error = ref('')

const passed = computed(() => problems.value.filter(problem =>
  problem.passed && (!keyword.value || problem.title.includes(keyword.value) || problem.tags.some(tag => tag.includes(keyword.value)))
))

async function load() {
  try {
    problems.value = await api.searchProblems(new URLSearchParams())
  } catch (err) {
    error.value = err instanceof Error ? err.message : '通过记录加载失败'
  }
}

onMounted(load)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>通过记录</h1>
      <p>查看当前账号已通过的题目，作为推荐系统判断训练状态的依据。</p>
    </div>
  </header>

  <section class="panel">
    <div class="toolbar">
      <input v-model="keyword" class="input" placeholder="按标题或标签过滤" />
      <button class="ghost" type="button" @click="load"><Search :size="18" />刷新</button>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="problem-list">
      <article v-for="problem in passed" :key="problem.id" class="problem-row">
        <header>
          <h3>{{ problem.title }}</h3>
          <span class="difficulty">{{ problem.difficulty }}</span>
        </header>
        <div class="tag-row">
          <span v-for="tag in problem.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
      </article>
      <p v-if="passed.length === 0" class="status">还没有通过记录。</p>
    </div>
  </section>
</template>

