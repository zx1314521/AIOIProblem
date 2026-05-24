<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowDownAZ, ArrowUpAZ, CheckCircle2, Plus, Search, X } from 'lucide-vue-next'
import { api } from '../services/api'
import type { Problem } from '../types'

const keyword = ref('')
const difficulty = ref('')
const tag = ref('')
const problems = ref<Problem[]>([])
const error = ref('')
const loading = ref(false)
const createOpen = ref(false)
const creating = ref(false)
const sortKey = ref<'createdAt' | 'title' | 'difficulty'>('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')

const newProblem = ref({
  title: '',
  description: '',
  difficulty: '简单',
  tags: ''
})

const difficulties = ['入门', '简单', 'CSPJ中等', 'CSPS提高', 'NOIP困难', '地狱NOI']
const difficultyRank = new Map(difficulties.map((item, index) => [item, index]))

const sortedProblems = computed(() => {
  const direction = sortDirection.value === 'asc' ? 1 : -1
  return [...problems.value].sort((a, b) => compareProblem(a, b) * direction)
})

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
  if (!newProblem.value.title.trim() || !newProblem.value.description.trim()) {
    error.value = '请填写标题和题面描述'
    return
  }
  creating.value = true
  error.value = ''
  try {
    await api.createProblem({
      title: newProblem.value.title.trim(),
      description: newProblem.value.description.trim(),
      difficulty: newProblem.value.difficulty,
      tags: newProblem.value.tags.split(/[,，\s]+/).filter(Boolean)
    })
    newProblem.value = { title: '', description: '', difficulty: '简单', tags: '' }
    createOpen.value = false
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '新建失败'
  } finally {
    creating.value = false
  }
}

async function markPassed(problem: Problem) {
  const updated = await api.markPassed(problem.id)
  problems.value = problems.value.map(item => item.id === updated.id ? updated : item)
}

function toggleSortDirection() {
  sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
}

function compareProblem(a: Problem, b: Problem) {
  if (sortKey.value === 'title') {
    return a.title.localeCompare(b.title, 'zh-Hans-CN')
  }
  if (sortKey.value === 'difficulty') {
    return (difficultyRank.get(a.difficulty) ?? 0) - (difficultyRank.get(b.difficulty) ?? 0)
  }
  return Date.parse(a.createdAt) - Date.parse(b.createdAt)
}

onMounted(load)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>题目管理</h1>
      <p>检索、排序和维护题库，并标注已通过。</p>
    </div>
    <button class="primary" type="button" @click="createOpen = true">
      <Plus :size="18" />新建题目
    </button>
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
    <div class="manage-bar">
      <label class="inline-field">
        <span>排序</span>
        <select v-model="sortKey" class="select">
          <option value="createdAt">创建时间</option>
          <option value="title">题目标题</option>
          <option value="difficulty">难度</option>
        </select>
      </label>
      <button class="secondary" type="button" @click="toggleSortDirection">
        <component :is="sortDirection === 'asc' ? ArrowUpAZ : ArrowDownAZ" :size="18" />
        {{ sortDirection === 'asc' ? '正序' : '倒序' }}
      </button>
      <span class="status">{{ sortedProblems.length }} 题</span>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="problem-list">
      <article v-for="problem in sortedProblems" :key="problem.id" class="problem-row">
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
      <p v-if="!loading && sortedProblems.length === 0" class="status">暂无题目。</p>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="createOpen" class="modal-backdrop" @click.self="createOpen = false">
      <section class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="create-problem-title">
        <header class="modal-header">
          <h2 id="create-problem-title">新建题目</h2>
          <button class="icon-only" type="button" title="关闭" @click="createOpen = false">
            <X :size="18" />
          </button>
        </header>
        <div class="grid">
          <input v-model="newProblem.title" class="input" placeholder="标题" />
          <textarea v-model="newProblem.description" class="textarea modal-textarea" placeholder="题面描述" />
          <div class="modal-fields">
            <select v-model="newProblem.difficulty" class="select">
              <option v-for="item in difficulties" :key="item">{{ item }}</option>
            </select>
            <input v-model="newProblem.tags" class="input" placeholder="标签，用空格或逗号分隔" />
          </div>
          <div class="actions">
            <button class="primary" type="button" :disabled="creating" @click="createProblem">
              <Plus :size="18" />添加题目
            </button>
            <button class="ghost" type="button" @click="createOpen = false">取消</button>
          </div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.manage-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 12px 0 16px;
  flex-wrap: wrap;
}

.inline-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-field span {
  color: #617069;
  font-size: 14px;
}

.inline-field .select {
  min-width: 150px;
}

h2 {
  margin: 0;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 30;
  background: rgba(23, 35, 30, 0.34);
  display: grid;
  place-items: center;
  padding: 24px;
}

.modal-panel {
  width: min(760px, 100%);
  max-height: min(86vh, 720px);
  overflow: auto;
  background: #ffffff;
  border: 1px solid #dfe4dc;
  border-radius: 8px;
  box-shadow: 0 24px 70px rgba(30, 45, 38, 0.22);
  padding: 20px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.icon-only {
  width: 36px;
  height: 36px;
  border: 1px solid #dfe4dc;
  border-radius: 6px;
  background: #f7f9f6;
  display: grid;
  place-items: center;
}

.modal-textarea {
  min-height: 260px;
}

.modal-fields {
  display: grid;
  grid-template-columns: 190px 1fr;
  gap: 10px;
}

@media (max-width: 760px) {
  .modal-fields {
    grid-template-columns: 1fr;
  }
}
</style>
