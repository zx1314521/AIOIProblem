<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import markdownItKatex from 'markdown-it-katex'
import 'katex/dist/katex.min.css'
import { ArrowDownAZ, ArrowUpAZ, CheckCircle2, FolderPlus, ListChecks, Pencil, Plus, Save, Search, Trash2, X } from 'lucide-vue-next'
import { api } from '../services/api'
import type { Problem, ProblemSet, TagCategory } from '../types'
import { normalizeProblemMath } from '../utils/problemMath'

const markdown = new MarkdownIt({ breaks: true, linkify: true }).use(markdownItKatex)

const keyword = ref('')
const difficulty = ref('')
const selectedFilterTags = ref<string[]>([])
const tagSearch = ref('')
const formTagSearch = ref('')
const tagCategories = ref<TagCategory[]>([])
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
const selectionMode = ref(false)
const selectedProblemIds = ref<number[]>([])
const bulkSaving = ref(false)
const bulkSetOpen = ref(false)
const problemSets = ref<ProblemSet[]>([])
const selectedSetId = ref<number | ''>('')
const newSetName = ref('')

const problemForm = ref({
  title: '',
  description: '',
  difficulty: '简单',
  tags: [] as string[],
  source: ''
})

const difficulties = ['入门', '简单', 'CSPJ中等', 'CSPS提高', 'NOIP困难', '地狱NOI']
const difficultyRank = new Map(difficulties.map((item, index) => [item, index]))

const sortedProblems = computed(() => {
  return [...problems.value].sort(compareProblem)
})
const formTitle = computed(() => formMode.value === 'create' ? '新建题目' : '编辑题目')
const detailHtml = computed(() => markdown.render(normalizeProblemMath(selectedProblem.value?.description || '')))
const filteredTagCategories = computed(() => filterCategories(tagSearch.value))
const filteredFormTagCategories = computed(() => filterCategories(formTagSearch.value))
const selectedFilterTagSet = computed(() => new Set(selectedFilterTags.value))
const selectedProblemIdSet = computed(() => new Set(selectedProblemIds.value))
const selectedProblems = computed(() => {
  const ids = selectedProblemIdSet.value
  return problems.value.filter(problem => ids.has(problem.id))
})
const allVisibleSelected = computed(() => {
  return sortedProblems.value.length > 0 && sortedProblems.value.every(problem => selectedProblemIdSet.value.has(problem.id))
})
const selectedPreview = computed(() => selectedProblems.value.slice(0, 3).map(problem => `《${problem.title}》`).join('、'))
const relatedFilterTagSet = computed(() => {
  const related = new Set<string>()
  for (const tag of selectedFilterTags.value) {
    for (const category of tagCategories.value) {
      if (category.tags.includes(tag)) {
        category.tags.forEach(item => {
          if (!selectedFilterTagSet.value.has(item)) {
            related.add(item)
          }
        })
      }
    }
  }
  return related
})

async function loadCatalog() {
  try {
    tagCategories.value = (await api.getTags()).categories
  } catch (err) {
    error.value = err instanceof Error ? err.message : '标签库加载失败'
  }
}

async function load() {
  loading.value = true
  error.value = ''
  const params = new URLSearchParams()
  if (keyword.value) params.set('keyword', keyword.value)
  if (difficulty.value) params.set('difficulty', difficulty.value)
  selectedFilterTags.value.forEach(tag => params.append('tags', tag))
  try {
    problems.value = await api.searchProblems(params)
    selectedProblemIds.value = selectedProblemIds.value.filter(id => problems.value.some(problem => problem.id === id))
  } catch (err) {
    error.value = err instanceof Error ? err.message : '搜索失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  formMode.value = 'create'
  editingProblemId.value = null
  problemForm.value = { title: '', description: '', difficulty: '简单', tags: [], source: '' }
  formTagSearch.value = ''
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
    tags: [...problem.tags],
    source: problem.source || ''
  }
  formTagSearch.value = ''
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
    tags: problemForm.value.tags,
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

function toggleProblemSelection(id: number) {
  selectedProblemIds.value = selectedProblemIdSet.value.has(id)
    ? selectedProblemIds.value.filter(item => item !== id)
    : [...selectedProblemIds.value, id]
}

function startSelectionMode() {
  selectionMode.value = true
}

function toggleAllVisible() {
  selectionMode.value = true
  if (allVisibleSelected.value) {
    const visibleIds = new Set(sortedProblems.value.map(problem => problem.id))
    selectedProblemIds.value = selectedProblemIds.value.filter(id => !visibleIds.has(id))
    return
  }
  const merged = new Set(selectedProblemIds.value)
  sortedProblems.value.forEach(problem => merged.add(problem.id))
  selectedProblemIds.value = [...merged]
}

function clearSelection() {
  selectedProblemIds.value = []
  selectionMode.value = false
}

function syncUpdatedProblems(updatedProblems: Problem[]) {
  const updatedMap = new Map(updatedProblems.map(problem => [problem.id, problem]))
  problems.value = problems.value.map(problem => updatedMap.get(problem.id) ?? problem)
  if (selectedProblem.value) {
    selectedProblem.value = updatedMap.get(selectedProblem.value.id) ?? selectedProblem.value
  }
}

async function bulkMarkPassed() {
  if (!selectedProblemIds.value.length) return
  bulkSaving.value = true
  error.value = ''
  try {
    const updated = await api.markProblemsPassed(selectedProblemIds.value)
    syncUpdatedProblems(updated)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '批量标注失败'
  } finally {
    bulkSaving.value = false
  }
}

async function bulkDelete() {
  if (!selectedProblemIds.value.length) return
  const summary = selectedPreview.value ? `：${selectedPreview.value}${selectedProblems.value.length > 3 ? ' 等' : ''}` : ''
  const confirmed = window.confirm(`确定删除已选 ${selectedProblemIds.value.length} 题${summary}？删除后会从题库和题单中移除。`)
  if (!confirmed) return
  const ids = [...selectedProblemIds.value]
  bulkSaving.value = true
  error.value = ''
  try {
    await api.deleteProblems(ids)
    const idSet = new Set(ids)
    problems.value = problems.value.filter(problem => !idSet.has(problem.id))
    if (selectedProblem.value && idSet.has(selectedProblem.value.id)) {
      detailOpen.value = false
      selectedProblem.value = null
    }
    clearSelection()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '批量删除失败'
  } finally {
    bulkSaving.value = false
  }
}

async function openBulkSetDialog() {
  if (!selectedProblemIds.value.length) return
  bulkSaving.value = true
  error.value = ''
  try {
    problemSets.value = await api.listProblemSets()
    selectedSetId.value = problemSets.value[0]?.id ?? ''
    newSetName.value = ''
    bulkSetOpen.value = true
  } catch (err) {
    error.value = err instanceof Error ? err.message : '题单加载失败'
  } finally {
    bulkSaving.value = false
  }
}

async function addSelectedToExistingSet() {
  if (!selectedProblemIds.value.length || selectedSetId.value === '') return
  bulkSaving.value = true
  error.value = ''
  try {
    await api.addProblemsToSet(Number(selectedSetId.value), selectedProblemIds.value)
    bulkSetOpen.value = false
  } catch (err) {
    error.value = err instanceof Error ? err.message : '添加到题单失败'
  } finally {
    bulkSaving.value = false
  }
}

async function createSetAndAddSelected() {
  const name = newSetName.value.trim()
  if (!name) {
    error.value = '请填写题单名称'
    return
  }
  bulkSaving.value = true
  error.value = ''
  try {
    await api.createProblemSetWithProblems(name, '题目管理批量创建', selectedProblemIds.value)
    bulkSetOpen.value = false
  } catch (err) {
    error.value = err instanceof Error ? err.message : '新建题单失败'
  } finally {
    bulkSaving.value = false
  }
}

async function deleteProblem(problem: Problem) {
  const confirmed = window.confirm(`确定删除《${problem.title}》吗？删除后会从题库和题单中移除。`)
  if (!confirmed) return
  error.value = ''
  try {
    await api.deleteProblem(problem.id)
    problems.value = problems.value.filter(item => item.id !== problem.id)
    selectedProblemIds.value = selectedProblemIds.value.filter(id => id !== problem.id)
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
  if (selectedFilterTags.value.length) {
    const relevance = searchRelevance(b).score - searchRelevance(a).score
    if (relevance !== 0) {
      return relevance
    }
  }
  const direction = sortDirection.value === 'asc' ? 1 : -1
  if (sortKey.value === 'title') {
    return a.title.localeCompare(b.title, 'zh-Hans-CN') * direction
  }
  if (sortKey.value === 'difficulty') {
    return ((difficultyRank.get(a.difficulty) ?? 0) - (difficultyRank.get(b.difficulty) ?? 0)) * direction
  }
  return (Date.parse(a.createdAt) - Date.parse(b.createdAt)) * direction
}

function filterCategories(query: string) {
  const cleaned = query.trim().toLowerCase()
  if (!cleaned) {
    return tagCategories.value
  }
  return tagCategories.value
    .map(category => ({
      name: category.name,
      tags: category.tags.filter(tag => tag.toLowerCase().includes(cleaned) || category.name.toLowerCase().includes(cleaned))
    }))
    .filter(category => category.tags.length > 0)
}

function chooseFilterTag(value: string) {
  selectedFilterTags.value = selectedFilterTags.value.includes(value)
    ? selectedFilterTags.value.filter(tag => tag !== value)
    : [...selectedFilterTags.value, value]
}

function clearFilters() {
  keyword.value = ''
  difficulty.value = ''
  selectedFilterTags.value = []
  tagSearch.value = ''
  load()
}

function toggleFormTag(value: string) {
  problemForm.value.tags = problemForm.value.tags.includes(value)
    ? problemForm.value.tags.filter(tag => tag !== value)
    : [...problemForm.value.tags, value]
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
    .format(new Date(value))
}

function searchRelevance(problem: Problem) {
  if (!selectedFilterTags.value.length) {
    return { score: 0, exact: 0, related: 0 }
  }
  const exact = selectedFilterTags.value.filter(tag => problem.tags.includes(tag)).length
  const related = problem.tags.filter(tag => relatedFilterTagSet.value.has(tag)).length
  return { score: exact * 100 + related * 20, exact, related }
}

function relevanceLabel(problem: Problem) {
  const relevance = searchRelevance(problem)
  if (!selectedFilterTags.value.length) {
    return ''
  }
  if (relevance.exact === selectedFilterTags.value.length) {
    return `完全匹配 ${relevance.exact}/${selectedFilterTags.value.length}`
  }
  if (relevance.exact > 0) {
    return `命中 ${relevance.exact}/${selectedFilterTags.value.length}`
  }
  if (relevance.related > 0) {
    return '同类相关'
  }
  return '低相关'
}

onMounted(async () => {
  await loadCatalog()
  await load()
})
</script>

<template>
  <header class="page-header compact-page-header">
    <div>
      <h1>题目管理</h1>
      <p>题库检索、维护、批量操作</p>
    </div>
    <button class="primary compact-create" type="button" @click="openCreate">
      <Plus :size="18" />新建题目
    </button>
  </header>

  <section class="management-panel compact-management">
    <div class="filter-strip">
      <div class="filter-line">
        <span class="filter-label">难度</span>
        <button
          class="tab-pill"
          type="button"
          :class="{ active: difficulty === '' }"
          @click="difficulty = ''; load()"
        >
          全部
        </button>
        <button
          v-for="item in difficulties"
          :key="item"
          class="tab-pill"
          type="button"
          :class="{ active: difficulty === item }"
          @click="difficulty = item; load()"
        >
          {{ item }}
        </button>
      </div>

      <div class="filter-line search-line">
        <span class="filter-label">筛选</span>
        <input v-model="keyword" class="input compact-input keyword-input" placeholder="关键词" @keyup.enter="load" />
        <input v-model="tagSearch" class="input compact-input tag-input" placeholder="搜索标准标签" />
        <button class="primary compact-search" type="button" @click="load"><Search :size="16" />搜索</button>
        <button class="plain-link" type="button" @click="clearFilters">清除筛选</button>
      </div>

      <div class="filter-line selected-line">
        <span class="filter-label">已选</span>
        <span v-if="!selectedFilterTags.length" class="selected-empty">暂无，可在下方选择算法标签</span>
        <button
          v-for="item in selectedFilterTags"
          :key="item"
          class="tag-choice active"
          type="button"
          :aria-label="`移除筛选标签 ${item}`"
          @click="chooseFilterTag(item)"
        >
          {{ item }} <X :size="13" />
        </button>
      </div>
    </div>

    <div class="tag-picker compact-tag-picker">
      <div v-for="category in filteredTagCategories" :key="category.name" class="tag-group">
        <span class="tag-group-title">{{ category.name }}</span>
        <button
          v-for="item in category.tags"
          :key="item"
          class="tag-choice"
          :class="{ active: selectedFilterTags.includes(item) }"
          type="button"
          :aria-label="`选择标签 ${item}`"
          @click="chooseFilterTag(item)"
        >
          {{ item }}
        </button>
      </div>
    </div>

    <div class="result-toolbar">
      <div class="result-count">共计 <strong>{{ sortedProblems.length }}</strong> 条结果</div>
      <label class="inline-field compact-sort">
        <span>排序</span>
        <select v-model="sortKey" class="select compact-select">
          <option value="createdAt">创建时间</option>
          <option value="title">题目标题</option>
          <option value="difficulty">难度</option>
        </select>
      </label>
      <button class="ghost compact-tool" type="button" @click="toggleSortDirection">
        <component :is="sortDirection === 'asc' ? ArrowUpAZ : ArrowDownAZ" :size="15" />
        {{ sortDirection === 'asc' ? '正序' : '倒序' }}
      </button>
      <button
        class="secondary compact-tool select-mode-button"
        type="button"
        :disabled="!sortedProblems.length"
        @click="selectionMode ? toggleAllVisible() : startSelectionMode()"
      >
        <ListChecks :size="15" />{{ selectionMode ? (allVisibleSelected ? '取消全选' : '全选当前') : '选择' }}
      </button>
      <button v-if="selectionMode" class="ghost compact-tool" type="button" @click="clearSelection">
        <X :size="15" />退出选择
      </button>
    </div>
    <div v-if="selectedProblemIds.length" class="bulk-bar" aria-live="polite">
      <strong>已选 {{ selectedProblemIds.length }} 题</strong>
      <span v-if="selectedPreview" class="bulk-preview">{{ selectedPreview }}{{ selectedProblems.length > 3 ? ' 等' : '' }}</span>
      <div class="bulk-actions">
        <button class="secondary" type="button" :disabled="bulkSaving" @click="bulkMarkPassed">
          <CheckCircle2 :size="18" />批量通过
        </button>
        <button class="secondary" type="button" :disabled="bulkSaving" @click="openBulkSetDialog">
          <FolderPlus :size="18" />加入题单
        </button>
        <button class="ghost danger" type="button" :disabled="bulkSaving" @click="bulkDelete">
          <Trash2 :size="18" />批量删除
        </button>
        <button class="ghost" type="button" :disabled="bulkSaving" @click="clearSelection">
          <X :size="18" />清空
        </button>
      </div>
    </div>
    <p v-if="error" class="error">{{ error }}</p>
    <div class="problem-list compact-problem-list">
      <div v-if="sortedProblems.length" class="problem-table-head" :class="{ selecting: selectionMode }">
        <span v-if="selectionMode"></span>
        <span>状态</span>
        <span>题号</span>
        <span>题目名称</span>
        <span>标签</span>
        <span>难度</span>
        <span>操作</span>
      </div>
      <article
        v-for="problem in sortedProblems"
        :key="problem.id"
        class="problem-row problem-row-compact"
        :class="{ 'selecting-row': selectionMode }"
        tabindex="0"
        @click="viewProblem(problem)"
        @keyup.enter="viewProblem(problem)"
      >
        <label v-if="selectionMode" class="select-cell" @click.stop>
          <input
            type="checkbox"
            :checked="selectedProblemIdSet.has(problem.id)"
            :aria-label="`选择题目 ${problem.title}`"
            @change="toggleProblemSelection(problem.id)"
          />
        </label>
        <div class="status-cell" :class="{ passed: problem.passed }">{{ problem.passed ? '✓' : '－' }}</div>
        <div class="id-cell">P{{ problem.id }}</div>
        <div class="problem-main title-cell">
          <header class="problem-title-line">
            <h3>{{ problem.title }}</h3>
          </header>
          <span v-if="selectedFilterTags.length" class="match-chip">{{ relevanceLabel(problem) }}</span>
          <span class="muted">{{ formatDate(problem.createdAt) }}</span>
        </div>
        <div class="tag-row tags-cell">
          <span v-for="item in problem.tags" :key="item" class="tag">{{ item }}</span>
        </div>
        <div class="difficulty-cell">
          <span class="difficulty">{{ problem.difficulty }}</span>
        </div>
        <div class="row-actions" @click.stop>
          <button class="ghost icon-action edit-action" type="button" title="编辑题目" @click="openEdit(problem)">
            <Pencil :size="15" />编辑
          </button>
          <button class="ghost icon-action pass-action pass-toggle" :class="{ passed: problem.passed }" type="button" :title="problem.passed ? '取消通过' : '标注已通过'" @click="togglePassed(problem)">
            <CheckCircle2 :size="15" />{{ problem.passed ? '已通过' : '通过' }}
          </button>
          <button class="ghost icon-action delete-action danger" type="button" title="删除题目" @click="deleteProblem(problem)">
            <Trash2 :size="15" />删除
          </button>
        </div>
      </article>
      <p v-if="loading" class="status">正在加载题目...</p>
      <p v-if="!loading && sortedProblems.length === 0" class="status">暂无题目。</p>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="bulkSetOpen" class="modal-backdrop" @click.self="bulkSetOpen = false">
      <section class="modal-panel set-modal" role="dialog" aria-modal="true" aria-labelledby="bulk-set-title">
        <header class="modal-header">
          <div>
            <h2 id="bulk-set-title">加入题单</h2>
            <p class="modal-subtitle">已选 {{ selectedProblemIds.length }} 题，可以加入已有题单，也可以新建题单后加入。</p>
          </div>
          <button class="icon-only" type="button" title="关闭" @click="bulkSetOpen = false">
            <X :size="18" />
          </button>
        </header>
        <div class="set-actions">
          <section class="set-action-block">
            <h3>已有题单</h3>
            <select v-model="selectedSetId" class="select" :disabled="!problemSets.length">
              <option v-if="!problemSets.length" value="">暂无题单</option>
              <option v-for="set in problemSets" :key="set.id" :value="set.id">
                {{ set.name }}（{{ set.problems.length }} 题）
              </option>
            </select>
            <button class="primary" type="button" :disabled="bulkSaving || selectedSetId === ''" @click="addSelectedToExistingSet">
              <FolderPlus :size="18" />添加到已有题单
            </button>
          </section>
          <section class="set-action-block">
            <h3>创建新题单</h3>
            <input v-model="newSetName" class="input" placeholder="输入新题单名称" @keyup.enter="createSetAndAddSelected" />
            <button class="secondary" type="button" :disabled="bulkSaving || !newSetName.trim()" @click="createSetAndAddSelected">
              <Plus :size="18" />新建题单并加入
            </button>
          </section>
        </div>
      </section>
    </div>
  </Teleport>

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
            <input v-model="formTagSearch" class="input" placeholder="搜索并选择标准标签" />
          </div>
          <div v-if="problemForm.tags.length" class="tag-row">
            <button v-for="item in problemForm.tags" :key="item" class="tag-choice active" type="button" @click="toggleFormTag(item)">
              {{ item }} <X :size="14" />
            </button>
          </div>
          <div class="tag-picker form-tag-picker">
            <div v-for="category in filteredFormTagCategories" :key="category.name" class="tag-group">
              <span class="tag-group-title">{{ category.name }}</span>
              <button
                v-for="item in category.tags"
                :key="item"
                class="tag-choice"
                :class="{ active: problemForm.tags.includes(item) }"
                type="button"
                :aria-label="`选择标签 ${item}`"
                @click="toggleFormTag(item)"
              >
                {{ item }}
              </button>
            </div>
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
.modal-subtitle,
.tag-group-title {
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

.tag-picker {
  display: grid;
  gap: 10px;
  max-height: 230px;
  overflow: auto;
  padding: 12px;
  margin: 10px 0;
  border: 1px solid #e3e9e2;
  border-radius: 8px;
  background: #fbfcfa;
}

.form-tag-picker {
  max-height: 260px;
}

.tag-group {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
}

.selected-filter-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5ece5;
}

.tag-group-title {
  min-width: 104px;
  padding-top: 7px;
  font-weight: 700;
}

.tag-choice {
  min-height: 30px;
  border: 1px solid #d9e2da;
  border-radius: 999px;
  background: #ffffff;
  color: #33443b;
  padding: 6px 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
}

.tag-choice.active {
  border-color: #1f6f54;
  background: #e9f4ee;
  color: #17684f;
  font-weight: 700;
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

.bulk-bar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  margin: -6px 0 16px;
  padding: 12px;
  border: 1px solid #b9d5c8;
  border-radius: 8px;
  background: #eff8f3;
}

.bulk-bar strong {
  color: #143d30;
}

.bulk-preview {
  min-width: 0;
  color: #567065;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bulk-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.problem-row-compact {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  cursor: pointer;
  transition: border-color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.problem-row-compact.selecting-row {
  grid-template-columns: 32px minmax(0, 1fr) auto;
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

.select-cell {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  cursor: pointer;
}

.select-cell input {
  width: 18px;
  height: 18px;
  accent-color: #1f6f54;
  cursor: pointer;
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

.match-chip {
  border: 1px solid #d7c392;
  background: #fff7df;
  color: #7a5512;
  border-radius: 999px;
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 700;
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

.set-modal {
  width: min(720px, 100%);
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

.set-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.set-action-block {
  display: grid;
  gap: 12px;
  align-content: start;
  padding: 14px;
  border: 1px solid #e3e9e2;
  border-radius: 8px;
  background: #fbfcfa;
}

.set-action-block h3 {
  margin: 0;
  font-size: 16px;
}

@media (max-width: 900px) {
  .problem-row-compact {
    grid-template-columns: 1fr;
  }

  .problem-row-compact.selecting-row {
    grid-template-columns: 32px 1fr;
  }

  .row-actions {
    justify-content: flex-start;
    grid-column: 2;
  }
}

@media (max-width: 760px) {
  .modal-fields,
  .set-actions,
  .bulk-bar {
    grid-template-columns: 1fr;
  }

  .bulk-actions {
    justify-content: flex-start;
  }
}

/* Compact OI library draft: flatter, denser, closer to table scanning. */
.compact-page-header {
  margin-bottom: 10px;
  align-items: center;
}

.compact-page-header h1 {
  font-size: 22px;
  font-weight: 800;
}

.compact-page-header p {
  margin-top: 3px;
  font-size: 13px;
  color: #7b8791;
}

.compact-create,
.compact-search,
.compact-tool,
.icon-action {
  min-height: 30px;
  border-radius: 4px;
  padding: 7px 10px;
  font-size: 13px;
  box-shadow: none;
}

.compact-management {
  padding: 0;
  border: 1px solid #d9dee5;
  border-radius: 4px;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.filter-strip {
  display: grid;
  gap: 9px;
  padding: 16px 20px 14px;
  border-bottom: 1px solid #e4e7eb;
  background: #fbfcfd;
}

.filter-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-height: 30px;
}

.filter-label {
  width: 62px;
  flex: 0 0 62px;
  color: #222831;
  font-size: 14px;
  font-weight: 500;
}

.tab-pill {
  min-height: 28px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: #1f2937;
  padding: 5px 9px;
  font-size: 14px;
}

.tab-pill:hover,
.tab-pill.active {
  background: #e4f2fb;
  color: #2386d1;
}

.tab-pill.active {
  font-weight: 700;
}

.search-line {
  align-items: center;
}

.compact-input,
.compact-select {
  height: 32px;
  border-radius: 3px;
  padding: 6px 10px;
  background: #ffffff;
  font-size: 14px;
}

.keyword-input {
  width: min(360px, 100%);
}

.tag-input {
  width: min(220px, 100%);
}

.plain-link {
  border: 0;
  background: transparent;
  color: #2c7dcc;
  padding: 5px 4px;
  font-size: 13px;
}

.plain-link:hover {
  text-decoration: underline;
}

.selected-line {
  color: #7b8791;
  font-size: 13px;
}

.selected-empty {
  color: #a0a8b1;
}

.compact-tag-picker {
  max-height: 128px;
  margin: 0;
  padding: 10px 20px 12px;
  border: 0;
  border-bottom: 1px solid #e4e7eb;
  border-radius: 0;
  background: #ffffff;
  gap: 8px;
}

.tag-group {
  gap: 6px;
}

.tag-group-title {
  min-width: 86px;
  padding-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.tag-choice {
  min-height: 24px;
  border-radius: 12px;
  padding: 3px 8px;
  background: #ffffff;
  color: #4b5563;
  font-size: 12px;
}

.tag-choice.active {
  background: #eaf5ff;
  border-color: #58a7df;
  color: #1677bd;
}

.result-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7eb;
  background: #f6f7f9;
}

.result-count {
  margin-right: auto;
  color: #64748b;
  font-size: 14px;
}

.result-count strong {
  color: #111827;
  font-weight: 800;
}

.compact-sort {
  gap: 6px;
}

.compact-sort span {
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.compact-sort .select {
  width: 128px;
  min-width: 128px;
}

.bulk-bar {
  margin: 0;
  padding: 10px 20px;
  border-width: 0 0 1px;
  border-radius: 0;
  background: #fff7df;
}

.bulk-actions .secondary,
.bulk-actions .ghost {
  min-height: 28px;
  border-radius: 4px;
  padding: 6px 9px;
  font-size: 12px;
  box-shadow: none;
}

.compact-problem-list {
  gap: 0;
}

.problem-table-head,
.problem-row-compact {
  display: grid;
  grid-template-columns: 46px 78px minmax(260px, 1fr) minmax(220px, 0.8fr) 100px 188px;
  align-items: center;
}

.problem-table-head.selecting,
.problem-row-compact.selecting-row {
  grid-template-columns: 36px 46px 78px minmax(260px, 1fr) minmax(220px, 0.8fr) 100px 188px;
}

.problem-table-head {
  min-height: 38px;
  padding: 0 20px;
  border-bottom: 1px solid #dfe3e8;
  color: #334155;
  background: #ffffff;
  font-size: 13px;
  font-weight: 700;
}

.problem-table-head span:last-child {
  text-align: right;
  padding-right: 4px;
}

.problem-row-compact {
  gap: 0;
  min-height: 42px;
  padding: 0 20px;
  border: 0;
  border-bottom: 1px solid #e5e7eb;
  border-radius: 0;
  background: #ffffff;
  box-shadow: none;
}

.problem-row-compact:hover,
.problem-row-compact:focus-visible {
  border-color: #e5e7eb;
  background: #f8fbff;
  box-shadow: none;
}

.select-cell {
  width: 22px;
  height: 22px;
  justify-self: start;
}

.select-cell input {
  width: 15px;
  height: 15px;
}

.status-cell {
  color: #6b7280;
  font-size: 18px;
  line-height: 1;
  font-weight: 800;
}

.status-cell.passed {
  color: #37b24d;
}

.id-cell {
  color: #334155;
  font-family: Consolas, "Microsoft YaHei UI", monospace;
  font-size: 14px;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.problem-title-line h3 {
  color: #2680d9;
  font-size: 15px;
  font-weight: 500;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.problem-main {
  gap: 0;
}

.muted {
  font-size: 12px;
  color: #9aa4b2;
  white-space: nowrap;
}

.tags-cell {
  min-width: 0;
  gap: 4px;
  overflow: hidden;
}

.tag {
  border-color: #274db2;
  border-radius: 3px;
  background: #274db2;
  color: #ffffff;
  padding: 2px 6px;
  font-size: 12px;
  font-weight: 700;
}

.difficulty-cell {
  display: flex;
  justify-content: flex-start;
}

.difficulty {
  border-radius: 3px;
  padding: 4px 8px;
  background: #ffaf24;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
}

.match-chip {
  border: 0;
  border-radius: 3px;
  background: #eaf5ff;
  color: #1677bd;
  padding: 3px 6px;
  font-size: 12px;
}

.row-actions {
  justify-content: flex-end;
  gap: 5px;
  justify-self: end;
  width: 100%;
}

.icon-action {
  min-width: 54px;
  background: #ffffff;
  border: 1px solid #d8e0e8;
  color: #4b5563;
}

.icon-action:hover {
  background: #edf2f7;
  border-color: #dbe2ea;
}

.edit-action {
  border-color: #b8d8f3;
  background: #f0f7ff;
  color: #2475b9;
}

.edit-action:hover {
  border-color: #85bde8;
  background: #e3f1ff;
}

.pass-action {
  border-color: #c7e4d1;
  background: #effaf2;
  color: #2f9e44;
}

.pass-action:hover {
  border-color: #92cf9f;
  background: #e4f6e8;
}

.delete-action {
  border-color: #f1c1bc;
  background: #fff2f0;
  color: #c43c33;
}

.delete-action:hover {
  border-color: #eba29a;
  background: #ffe8e5;
}

.pass-toggle.passed {
  background: #2f9e44;
  color: #2f9e44;
  border-color: #2f9e44;
  color: #ffffff;
}

.modal-panel {
  border-radius: 4px;
}

@media (max-width: 1120px) {
  .problem-table-head,
  .problem-row-compact {
    grid-template-columns: 38px 68px minmax(220px, 1fr) minmax(160px, 0.65fr) 88px;
  }

  .problem-table-head.selecting,
  .problem-row-compact.selecting-row {
    grid-template-columns: 30px 38px 68px minmax(220px, 1fr) minmax(160px, 0.65fr) 88px;
  }

  .problem-table-head span:last-child,
  .row-actions {
    display: none;
  }
}

@media (max-width: 760px) {
  .filter-line,
  .result-toolbar {
    align-items: stretch;
  }

  .filter-label {
    width: 100%;
    flex-basis: 100%;
  }

  .keyword-input,
  .tag-input {
    width: 100%;
  }

  .problem-table-head {
    display: none;
  }

  .problem-row-compact,
  .problem-row-compact.selecting-row {
    grid-template-columns: 1fr;
    gap: 6px;
    padding: 12px 14px;
  }

  .select-cell,
  .status-cell,
  .id-cell,
  .difficulty-cell {
    justify-self: start;
  }

  .title-cell {
    flex-wrap: wrap;
  }

  .row-actions {
    display: flex;
    justify-content: flex-start;
    grid-column: auto;
  }
}
</style>
