<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FolderPlus, Link, Trash2 } from 'lucide-vue-next'
import { api } from '../services/api'
import type { ProblemSet } from '../types'

const sets = ref<ProblemSet[]>([])
const name = ref('')
const description = ref('')
const problemIdBySet = ref<Record<number, string>>({})
const error = ref('')

async function load() {
  try {
    sets.value = await api.listProblemSets()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '题单加载失败'
  }
}

async function createSet() {
  if (!name.value.trim()) return
  try {
    const created = await api.createProblemSet(name.value, description.value)
    sets.value = [created, ...sets.value]
    name.value = ''
    description.value = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : '题单创建失败'
  }
}

async function addProblem(set: ProblemSet) {
  const problemId = Number(problemIdBySet.value[set.id])
  if (!problemId) return
  try {
    const updated = await api.addProblemToSet(set.id, problemId)
    sets.value = sets.value.map(item => item.id === updated.id ? updated : item)
    problemIdBySet.value[set.id] = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : '添加失败，请确认题目 ID 存在'
  }
}

async function removeProblem(setId: number, problemId: number) {
  const updated = await api.removeProblemFromSet(setId, problemId)
  sets.value = sets.value.map(item => item.id === updated.id ? updated : item)
}

onMounted(load)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>题单管理</h1>
      <p>为课程、专题或阶段测评建立题单，并把题库题目加入练习列表。</p>
    </div>
  </header>

  <section class="panel grid">
    <h2>新建题单</h2>
    <div class="toolbar">
      <input v-model="name" class="input" placeholder="题单名称" />
      <input v-model="description" class="input" placeholder="说明" />
      <button class="secondary" type="button" @click="createSet"><FolderPlus :size="18" />新建</button>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
  </section>

  <section class="problem-list set-list">
    <article v-for="set in sets" :key="set.id" class="panel">
      <div class="page-header set-header">
        <div>
          <h2>{{ set.name }}</h2>
          <p>{{ set.description || '暂无说明' }}</p>
        </div>
        <span class="tag">{{ set.problems.length }} 题</span>
      </div>
      <div class="actions">
        <input v-model="problemIdBySet[set.id]" class="input id-input" placeholder="题目 ID" />
        <button class="ghost" type="button" @click="addProblem(set)"><Link :size="18" />加入题目</button>
      </div>
      <div class="problem-list inner-list">
        <article v-for="problem in set.problems" :key="problem.id" class="problem-row">
          <header>
            <h3>#{{ problem.id }} {{ problem.title }}</h3>
            <span class="difficulty">{{ problem.difficulty }}</span>
          </header>
          <div class="tag-row">
            <span v-for="tag in problem.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <button class="ghost" type="button" title="移出题单" @click="removeProblem(set.id, problem.id)">
            <Trash2 :size="18" />移出
          </button>
        </article>
        <p v-if="set.problems.length === 0" class="status">还没有加入题目。</p>
      </div>
    </article>
  </section>
</template>

<style scoped>
.set-list {
  margin-top: 18px;
}

.set-header {
  margin-bottom: 12px;
}

h2 {
  margin: 0;
}

.id-input {
  max-width: 180px;
}

.inner-list {
  margin-top: 12px;
}
</style>

