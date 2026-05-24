<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import markdownItKatex from 'markdown-it-katex'
import 'katex/dist/katex.min.css'
import { ArrowDownAZ, ArrowUpAZ, CheckCircle2, Eye, Pencil, Plus, Save, Search, Trash2, X } from 'lucide-vue-next'
import { api } from '../services/api'
import type { Problem } from '../types'
import { normalizeProblemMath } from '../utils/problemMath'

const markdown = new MarkdownIt({ breaks: true, linkify: true }).use(markdownItKatex)

const keyword = ref('')
const difficulty = ref('')
const tag = ref('')
const problems = ref<Problem[]>([])
const error = ref('')
const loading = ref(false)
const formOpen = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const editingProblemId = ref<number | null>(null)
const detailOpen = ref(false)
const selectedProblem = ref<Problem | null>(null)
const sortKey = ref<'createdAt' | 'title' | 'difficulty'>('createdAt')
const sortDirection = ref<'asc' | 'desc'>('desc')

const problemForm = ref({
  title: '',
  description: '',
  difficulty: '简单',
  tags: '',
  source: ''
})

const difficulties = ['入门', '简单', 'CSPJ中等', 'CSPS提高', 'NOIP困难', '地狱NOI']
const difficultyRank = new Map(difficulties.map((item, index) => [item, index]))

const sortedProblems = computed(() => {
  const direction = sortDirection.value === 'asc' ? 1 : -1
  return [...problems.value].sort((a, b) => compareProblem(a, b) * direction)
})
const formTitle = computed(() => formMode.value === 'create' ? '新建题目' : '编辑题目')
const detailHtml = computed(() => markdown.render(normalizeProblemMath(selectedProblem.value?.description || '')))

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

function openCreate() {
  formMode.value = 'create'
  editingProblemId.value = null
  problemForm.value = { title: '', description: '', difficulty: '简单', tags: '', source: '' }
  formOpen.value = true
}

function openEdit(problem: Problem) {
  formMode.value = 'edit'
  editingProblemId.value = problem.id
  detailOpen.value = false
  problemForm.value = {
    title: problem.title,
    description: problem.description,
    difficulty: problem.difficulty,
    tags: problem.tags.join(' '),
    source: problem.source || ''
  }
  formOpen.value = true
}

async function saveProblem() {
  if (!problemForm.value.title.trim() || !problemForm.value.description.trim()) {
    error.value = '请填写标题和题面描述'
    return
  }
  saving.value = true
  error.value = ''
  const payload = {
    title: problemForm.value.title.trim(),
    description: problemForm.value.description.trim(),
    difficulty: problemForm.value.difficulty,
    tags: parseTags(problemForm.value.tags),
    source: problemForm.value.source.trim() || undefined
  }
  try {
    if (formMode.value === 'edit' && editingProblemId.value) {
      const updated = await api.updateProblem(editingProblemId.value, payload)
      problems.value = problems.value.map(item => item.id === updated.id ? updated : item)
      if (selectedProblem.value?.id === updated.id) {
        selectedProblem.value = updated
      }
    } else {
      const created = await api.createProblem(payload)
      problems.value = [created, ...problems.value]
    }
    formOpen.value = false
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function viewProblem(problem: Problem) {
  error.value = ''
  try {
    selectedProblem.value = await api.getProblem(problem.id)
    detailOpen.value = true
  } catch (err) {
    error.value = err instanceof Error ? err.message : '读取题目失败'
  }
}

async function togglePassed(problem: Problem) {
  const updated = problem.passed ? await api.unmarkPassed(problem.id) : await api.markPassed(problem.id)
  problems.value = problems.value.map(item => item.id === updated.id ? updated : item)
  if (selectedProblem.value?.id === updated.id) {
    selectedProblem.value = updated
  }
}

async function deleteProblem(problem: Problem) {
  const confirmed = window.confirm(`确定删除「${problem.title}」吗？删除后会从题库和题单中移除。`)
  if (!confirmed) return
  error.value = ''
  try {
    await api.deleteProblem(problem.id)
    problems.value = problems.value.filter(item => item.id !== problem.id)
    if (selectedProblem.value?.id === problem.id) {
      detailOpen.value = false
      selectedProblem.value = null
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败'
  }
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

function parseTags(value: string) {
  return value.split(/[,，\s]+/).map(item => item.trim()).filter(Boolean)
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    .format(new Date(value))
}

onMounted(load)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>题目管理</h1>
      <p>检索、排序和维护题库，并标注已通过。</p>
    </div>
    <button class="primary" type="button" @click="openCreate">
      <Plus :size="18" />新建题目
    </button>
  </header>

  <section class="panel management-panel">
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
      <span class="count-chip">{{ sortedProblems.length }} 题</span>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="problem-list">
      <article
        v-for="problem in sortedProblems"
        :key="problem.id"
        class="problem-row problem-row-compact"
        tabindex="0"
        @click="viewProblem(problem)"
        @keyup.enter="viewProblem(problem)"
      >
        <div class="problem-main">
          <header>
            <h3>{{ problem.title }}</h3>
            <span class="difficulty">{{ problem.difficulty }}</span>
          </header>
          <div class="tag-row">
            <span v-for="item in problem.tags" :key="item" class="tag">{{ item }}</span>
            <span v-if="problem.passed" class="tag passed-tag">已通过</span>
            <span class="muted">{{ formatDate(problem.createdAt) }}</span>
          </div>
        </div>
        <div class="row-actions" @click.stop>
          <button class="ghost" type="button" title="查看题面" @click="viewProblem(problem)">
            <Eye :size="17" />查看
          </button>
          <button class="ghost" type="button" title="编辑题目" @click="openEdit(problem)">
            <Pencil :size="17" />编辑
          </button>
          <button class="ghost pass-toggle" :class="{ passed: problem.passed }" type="button" :title="problem.passed ? '取消通过' : '标注已通过'" @click="togglePassed(problem)">
            <CheckCircle2 :size="17" />{{ problem.passed ? '已通过' : '通过' }}
          </button>
          <button class="ghost danger" type="button" title="删除题目" @click="deleteProblem(problem)">
            <Trash2 :size="17" />删除
          </button>
        </div>
      </article>
      <p v-if="loading" class="status">正在加载题目...</p>
      <p v-if="!loading && sortedProblems.length === 0" class="status">暂无题目。</p>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="detailOpen && selectedProblem" class="modal-backdrop" @click.self="detailOpen = false">
      <section class="modal-panel detail-modal" role="dialog" aria-modal="true" aria-labelledby="problem-detail-title">
        <header class="modal-header">
          <div>
            <h2 id="problem-detail-title">{{ selectedProblem.title }}</h2>
            <p class="modal-subtitle">{{ selectedProblem.difficulty }} · {{ formatDate(selectedProblem.createdAt) }}</p>
          </div>
          <button class="icon-only" type="button" title="关闭" @click="detailOpen = false">
            <X :size="18" />
          </button>
        </header>
        <div class="tag-row detail-tags">
          <span v-for="item in selectedProblem.tags" :key="item" class="tag">{{ item }}</span>
          <span v-if="selectedProblem.passed" class="tag passed-tag">已通过</span>
        </div>
        <div class="markdown-preview problem-preview" v-html="detailHtml" />
        <div class="actions modal-actions">
          <button class="ghost" type="button" @click="openEdit(selectedProblem)">
            <Pencil :size="18" />编辑
          </button>
          <button class="ghost pass-toggle" :class="{ passed: selectedProblem.passed }" type="button" @click="togglePassed(selectedProblem)">
            <CheckCircle2 :size="18" />{{ selectedProblem.passed ? '已通过' : '标注已通过' }}
          </button>
          <button class="ghost danger" type="button" @click="deleteProblem(selectedProblem)">
            <Trash2 :size="18" />删除
          </button>
        </div>
      </section>
    </div>
  </Teleport>

  <Teleport to="body">
    <div v-if="formOpen" class="modal-backdrop" @click.self="formOpen = false">
      <section class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="problem-form-title">
        <header class="modal-header">
          <h2 id="problem-form-title">{{ formTitle }}</h2>
          <button class="icon-only" type="button" title="关闭" @click="formOpen = false">
            <X :size="18" />
          </button>
        </header>
        <div class="grid">
          <input v-model="problemForm.title" class="input" placeholder="标题" />
          <textarea v-model="problemForm.description" class="textarea modal-textarea" placeholder="题面描述" />
          <div class="modal-fields">
            <select v-model="problemForm.difficulty" class="select">
              <option v-for="item in difficulties" :key="item">{{ item }}</option>
            </select>
            <input v-model="problemForm.tags" class="input" placeholder="标签，用空格或逗号分隔" />
          </div>
          <input v-model="problemForm.source" class="input" placeholder="来源，可选" />
          <div class="actions">
            <button class="primary" type="button" :disabled="saving" @click="saveProblem">
              <component :is="formMode === 'create' ? Plus : Save" :size="18" />{{ formMode === 'create' ? '添加题目' : '保存修改' }}
            </button>
            <button class="ghost" type="button" @click="formOpen = false">取消</button>
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
  margin: 10px 0 18px;
  flex-wrap: wrap;
  padding: 10px 12px;
  border: 1px solid #e3e9e2;
  border-radius: 8px;
  background: #f8faf7;
}

.inline-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-field span,
.muted,
.modal-subtitle {
  color: #617069;
  font-size: 14px;
}

.inline-field .select {
  min-width: 150px;
}

h2 {
  margin: 0;
}

.management-panel {
  padding: 18px;
}

.count-chip {
  margin-left: auto;
  color: #45524b;
  background: #ffffff;
  border: 1px solid #d9e2da;
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 700;
}

.problem-row-compact {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.problem-row-compact:hover,
.problem-row-compact:focus-visible {
  border-color: #8fb7a7;
  background: #fcfdfb;
  box-shadow: 0 10px 24px rgba(23, 33, 29, 0.07);
  outline: none;
}

.problem-main {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.problem-main header {
  justify-content: flex-start;
}

.row-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.row-actions .ghost {
  min-height: 34px;
  padding: 8px 10px;
  font-size: 13px;
}

.passed-tag {
  border-color: #a9c7b9;
  background: #edf7f1;
  color: #1f6f54;
}

.pass-toggle.passed {
  background: #e9f4ee;
  color: #17684f;
  border-color: #b7d1c3;
}

.ghost:disabled {
  opacity: 0.56;
  cursor: not-allowed;
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
  width: min(820px, 100%);
  max-height: min(88vh, 760px);
  overflow: auto;
  background: #ffffff;
  border: 1px solid #dfe4dc;
  border-radius: 8px;
  box-shadow: 0 26px 80px rgba(20, 35, 29, 0.22);
  padding: 20px;
}

.detail-modal {
  width: min(980px, 100%);
}

.modal-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.modal-subtitle {
  margin: 6px 0 0;
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

.detail-tags {
  margin-bottom: 14px;
}

.problem-preview {
  border-top: 1px solid #e6ebe4;
  border-bottom: 1px solid #e6ebe4;
  padding: 16px 0;
  line-height: 1.75;
}

.modal-actions {
  margin-top: 16px;
}

.modal-textarea {
  min-height: 300px;
}

.modal-fields {
  display: grid;
  grid-template-columns: 190px 1fr;
  gap: 10px;
}

@media (max-width: 900px) {
  .problem-row-compact {
    grid-template-columns: 1fr;
  }

  .row-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 760px) {
  .modal-fields {
    grid-template-columns: 1fr;
  }
}
</style>
