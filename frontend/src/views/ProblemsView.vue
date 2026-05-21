<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { CheckCircle2, Plus, Search } from 'lucide-vue-next'
import { api } from '../services/api'
import type { Problem } from '../types'

const keyword = ref('')
const difficulty = ref('')
const tag = ref('')
const problems = ref<Problem[]>([])
const error = ref('')
const loading = ref(false)

const newProblem = ref({
  title: '',
  description: '',
  difficulty: '简单',
  tags: ''
})

const difficulties = ['入门', '简单', 'CSPJ中等', 'CSPS提高', 'NOIP困难', '地狱NOI']

async function load() {
  loading.value = true
  error.value = ''
  const params = new URLSearchParams()
  if (keyword.value) params.set('keyword', keyword.value)
  if (difficulty.value) params.set('difficulty', difficulty.value)
  if (tag.value) params.set('tag', tag.value)
  try {
    problems.value = await api.searchProblems(params)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '搜索失败'
  } finally {
    loading.value = false
  }
}

async function createProblem() {
  try {
    await api.createProblem({
      title: newProblem.value.title,
      description: newProblem.value.description,
      difficulty: newProblem.value.difficulty,
      tags: newProblem.value.tags.split(/[,，\s]+/).filter(Boolean)
    })
    newProblem.value = { title: '', description: '', difficulty: '简单', tags: '' }
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '新建失败'
  }
}

async function markPassed(problem: Problem) {
  const updated = await api.markPassed(problem.id)
  problems.value = problems.value.map(item => item.id === updated.id ? updated : item)
}

onMounted(load)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>题目搜索</h1>
      <p>按关键词、难度和标签检索题库，并标注已通过。</p>
    </div>
  </header>

  <section class="panel">
    <div class="toolbar">
      <input v-model="keyword" class="input" placeholder="关键词" @keyup.enter="load" />
      <select v-model="difficulty" class="select">
        <option value="">全部难度</option>
        <option v-for="item in difficulties" :key="item">{{ item }}</option>
      </select>
      <input v-model="tag" class="input" placeholder="标签" @keyup.enter="load" />
      <button class="primary" type="button" @click="load"><Search :size="18" />搜索</button>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="problem-list">
      <article v-for="problem in problems" :key="problem.id" class="problem-row">
        <header>
          <h3>{{ problem.title }}</h3>
          <span class="difficulty">{{ problem.difficulty }}</span>
        </header>
        <p>{{ problem.description }}</p>
        <div class="tag-row">
          <span v-for="item in problem.tags" :key="item" class="tag">{{ item }}</span>
        </div>
        <div class="actions">
          <button class="ghost" type="button" :disabled="problem.passed" @click="markPassed(problem)">
            <CheckCircle2 :size="18" />{{ problem.passed ? '已通过' : '标注已通过' }}
          </button>
        </div>
      </article>
      <p v-if="!loading && problems.length === 0" class="status">暂无题目。</p>
    </div>
  </section>

  <section class="panel grid create-panel">
    <h2>新建题目</h2>
    <input v-model="newProblem.title" class="input" placeholder="标题" />
    <textarea v-model="newProblem.description" class="textarea" placeholder="题面描述" />
    <div class="toolbar">
      <select v-model="newProblem.difficulty" class="select">
        <option v-for="item in difficulties" :key="item">{{ item }}</option>
      </select>
      <input v-model="newProblem.tags" class="input" placeholder="标签，用空格或逗号分隔" />
      <button class="secondary" type="button" @click="createProblem"><Plus :size="18" />添加</button>
    </div>
  </section>
</template>

<style scoped>
.create-panel {
  margin-top: 18px;
}

h2 {
  margin: 0;
}
</style>

