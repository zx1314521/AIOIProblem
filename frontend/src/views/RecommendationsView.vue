<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RefreshCw, SquareCheckBig } from 'lucide-vue-next'
import { api } from '../services/api'
import type { RecommendationResponse } from '../types'

const data = ref<RecommendationResponse | null>(null)
const error = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    data.value = await api.recommendations()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '推荐加载失败'
  } finally {
    loading.value = false
  }
}

async function markPassed(problemId: number) {
  await api.markPassed(problemId)
  await load()
}

onMounted(load)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>AI分析推荐题</h1>
      <p>根据已通过记录、薄弱标签和目标难度，给出查漏补缺练习顺序。</p>
    </div>
    <button class="ghost" type="button" @click="load"><RefreshCw :size="18" />刷新</button>
  </header>

  <section class="panel">
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="data">
      <div class="tag-row weak-row">
        <span class="status">薄弱标签</span>
        <span v-for="tag in data.weakTags" :key="tag" class="tag">{{ tag }}</span>
      </div>
      <div class="problem-list">
        <article v-for="item in data.items" :key="item.problem.id" class="problem-row">
          <header>
            <h3>{{ item.practiceOrder }}. {{ item.problem.title }}</h3>
            <span class="difficulty">{{ item.problem.difficulty }}</span>
          </header>
          <p>{{ item.reason }}</p>
          <div class="tag-row">
            <span v-for="tag in item.problem.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <button class="ghost" type="button" @click="markPassed(item.problem.id)">
            <SquareCheckBig :size="18" />标注已通过
          </button>
        </article>
      </div>
      <p v-if="data.items.length === 0" class="status">暂无可推荐题目，请先在题库中新建题目。</p>
    </template>
    <p v-else-if="loading" class="status">正在生成推荐。</p>
  </section>
</template>

<style scoped>
.weak-row {
  margin-bottom: 16px;
}
</style>

