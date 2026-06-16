<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
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

  <section class="panel recommendation-panel">
    <p v-if="error" class="error">{{ error }}</p>
    <template v-if="data">
      <div class="weak-row">
        <span class="weak-title">薄弱标签</span>
        <span v-for="tag in data.weakTags" :key="tag" class="tag">{{ tag }}</span>
      </div>
      <div class="problem-list recommendation-list">
        <article v-for="item in data.items" :key="item.problem.id" class="problem-row recommendation-row">
          <header>
            <div class="recommendation-title">
              <span class="order-badge">{{ item.practiceOrder }}</span>
              <h3>
                <RouterLink
                  class="problem-title-link"
                  :to="`/problems/${item.problem.id}`"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  {{ item.problem.title }}
                </RouterLink>
              </h3>
            </div>
            <span class="difficulty">{{ item.problem.difficulty }}</span>
          </header>
          <p>{{ item.reason }}</p>
          <div class="recommendation-footer">
            <div class="tag-row">
              <span v-for="tag in item.problem.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <button class="ghost pass-action" type="button" @click="markPassed(item.problem.id)">
              <SquareCheckBig :size="18" />标注已通过
            </button>
          </div>
        </article>
      </div>
      <p v-if="data.items.length === 0" class="status">暂无可推荐题目，请先在题库中新建题目。</p>
    </template>
    <p v-else-if="loading" class="status">正在生成推荐。</p>
  </section>
</template>

<style scoped>
.recommendation-panel {
  display: grid;
  gap: 16px;
}

.weak-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  border: 1px solid #e4eadf;
  border-radius: 8px;
  background: #f8faf6;
}

.weak-title {
  color: #58665f;
  font-size: 14px;
  font-weight: 800;
  margin-right: 2px;
}

.recommendation-list {
  gap: 12px;
}

.recommendation-row {
  display: grid;
  gap: 12px;
}

.recommendation-title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-badge {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 8px;
  background: #eaf2f9;
  color: #37658f;
  font-weight: 900;
}

.recommendation-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.pass-action {
  white-space: nowrap;
}

.problem-title-link {
  color: inherit;
  text-decoration: none;
}

.problem-title-link:hover,
.problem-title-link:focus-visible {
  color: #2475b9;
  text-decoration: underline;
}
</style>
