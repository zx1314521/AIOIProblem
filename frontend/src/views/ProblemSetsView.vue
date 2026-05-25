<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import markdownItKatex from 'markdown-it-katex'
import 'katex/dist/katex.min.css'
import { ArrowDown, ArrowUp, CheckCircle2, FolderPlus, Plus, Search, Trash2, X } from 'lucide-vue-next'
import { api } from '../services/api'
import type { Problem, ProblemSet } from '../types'
import { normalizeProblemMath } from '../utils/problemMath'

const difficulties = ['入门', '简单', 'CSPJ中等', 'CSPS提高', 'NOIP困难', '地狱NOI']
const markdown = new MarkdownIt({ breaks: true, linkify: true }).use(markdownItKatex)

const sets = ref<ProblemSet[]>([])
const selectedSetId = ref<number | null>(null)
const name = ref('')
const description = ref('')
const error = ref('')
const searchOpen = ref(false)
const searchKeyword = ref('')
const searchDifficulty = ref('')
const searchResults = ref<Problem[]>([])
const selectedProblemIds = ref<number[]>([])
const searching = ref(false)
const adding = ref(false)
const draggedProblemId = ref<number | null>(null)
const selectedProblem = ref<Problem | null>(null)

const selectedSet = computed(() => sets.value.find(set => set.id === selectedSetId.value) ?? sets.value[0])
const selectedSetProblemIds = computed(() => new Set(selectedSet.value?.problems.map(problem => problem.id) ?? []))
const selectedProblemHtml = computed(() => markdown.render(normalizeProblemMath(selectedProblem.value?.description || '')))

const selectedSetStats = computed(() => {
  const problems = selectedSet.value?.problems ?? []
  const difficultyCounts = new Map<string, number>()
  const tagCounts = new Map<string, number>()
  for (const problem of problems) {
    difficultyCounts.set(problem.difficulty, (difficultyCounts.get(problem.difficulty) ?? 0) + 1)
    for (const tag of problem.tags) {
      tagCounts.set(tag, (tagCounts.get(tag) ?? 0) + 1)
    }
  }
  return {
    total: problems.length,
    passed: problems.filter(problem => problem.passed).length,
    difficulties: Array.from(difficultyCounts.entries()).slice(0, 4),
    tags: Array.from(tagCounts.entries()).sort((a, b) => b[1] - a[1]).slice(0, 6)
  }
})

async function load() {
  try {
    sets.value = await api.listProblemSets()
    if (!selectedSetId.value && sets.value.length > 0) {
      selectedSetId.value = sets.value[0].id
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '题单加载失败'
  }
}

async function createSet() {
  if (!name.value.trim()) return
  try {
    const created = await api.createProblemSet(name.value, description.value)
    sets.value = [created, ...sets.value]
    selectedSetId.value = created.id
    name.value = ''
    description.value = ''
    error.value = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : '题单创建失败'
  }
}

function selectSet(set: ProblemSet) {
  selectedSetId.value = set.id
  selectedProblem.value = null
  error.value = ''
}

function updateSet(updated: ProblemSet) {
  sets.value = sets.value.map(set => set.id === updated.id ? updated : set)
  selectedSetId.value = updated.id
}

function openSearch() {
  if (!selectedSet.value) return
  searchOpen.value = true
  selectedProblemIds.value = []
  if (searchResults.value.length === 0) {
    void searchProblems()
  }
}

function closeSearch() {
  searchOpen.value = false
  selectedProblemIds.value = []
}

function openProblem(problem: Problem) {
  selectedProblem.value = problem
}

function closeProblem() {
  selectedProblem.value = null
}

async function searchProblems() {
  searching.value = true
  try {
    const params = new URLSearchParams()
    if (searchKeyword.value.trim()) params.set('keyword', searchKeyword.value.trim())
    if (searchDifficulty.value) params.set('difficulty', searchDifficulty.value)
    searchResults.value = await api.searchProblems(params)
    error.value = ''
  } catch (err) {
    error.value = err instanceof Error ? err.message : '题目搜索失败'
  } finally {
    searching.value = false
  }
}

function toggleCandidate(problemId: number) {
  selectedProblemIds.value = selectedProblemIds.value.includes(problemId)
    ? selectedProblemIds.value.filter(id => id !== problemId)
    : [...selectedProblemIds.value, problemId]
}

async function addSelectedProblems() {
  if (!selectedSet.value || selectedProblemIds.value.length === 0) return
  adding.value = true
  try {
    const updated = await api.addProblemsToSet(selectedSet.value.id, selectedProblemIds.value)
    updateSet(updated)
    closeSearch()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加入题单失败'
  } finally {
    adding.value = false
  }
}

async function removeProblem(problemId: number) {
  if (!selectedSet.value) return
  const updated = await api.removeProblemFromSet(selectedSet.value.id, problemId)
  updateSet(updated)
}

async function reorderProblemIds(problemIds: number[]) {
  if (!selectedSet.value) return
  const updated = await api.reorderProblemSetItems(selectedSet.value.id, problemIds)
  updateSet(updated)
}

async function moveProblem(problemId: number, direction: -1 | 1) {
  const ids = selectedSet.value?.problems.map(problem => problem.id) ?? []
  const index = ids.indexOf(problemId)
  const nextIndex = index + direction
  if (index < 0 || nextIndex < 0 || nextIndex >= ids.length) return
  const reordered = [...ids]
  const [moved] = reordered.splice(index, 1)
  reordered.splice(nextIndex, 0, moved)
  await reorderProblemIds(reordered)
}

function startDrag(problemId: number) {
  draggedProblemId.value = problemId
}

async function dropOn(targetProblemId: number) {
  if (!draggedProblemId.value || draggedProblemId.value === targetProblemId) {
    draggedProblemId.value = null
    return
  }
  const ids = selectedSet.value?.problems.map(problem => problem.id) ?? []
  const from = ids.indexOf(draggedProblemId.value)
  const to = ids.indexOf(targetProblemId)
  if (from < 0 || to < 0) return
  const reordered = [...ids]
  const [moved] = reordered.splice(from, 1)
  reordered.splice(to, 0, moved)
  draggedProblemId.value = null
  await reorderProblemIds(reordered)
}

onMounted(load)
</script>

<template>
  <header class="page-header compact-header">
    <div>
      <h1>题单管理</h1>
      <p>按课程、专题或阶段测评组织题目，维护练习顺序和完成状态。</p>
    </div>
  </header>

  <p v-if="error" class="error">{{ error }}</p>

  <section class="set-workspace">
    <aside class="set-sidebar panel">
      <div class="create-strip">
        <input v-model="name" class="input" placeholder="新题单名称" />
        <input v-model="description" class="input" placeholder="说明，可留空" />
        <button class="secondary" type="button" @click="createSet"><FolderPlus :size="17" />新建</button>
      </div>

      <div class="set-tabs">
        <button
          v-for="set in sets"
          :key="set.id"
          class="set-tab"
          :class="{ active: selectedSet?.id === set.id }"
          type="button"
          @click="selectSet(set)"
        >
          <span class="set-tab-title">{{ set.name }}</span>
          <span class="set-tab-meta">{{ set.problems.length }} 题</span>
        </button>
      </div>

      <p v-if="sets.length === 0" class="empty-state">还没有题单，先在上方新建一个。</p>
    </aside>

    <main class="set-detail panel">
      <template v-if="selectedSet">
        <div class="set-detail-header">
          <div>
            <h2>{{ selectedSet.name }}</h2>
            <p>{{ selectedSet.description || '暂无说明' }}</p>
          </div>
          <button class="primary" type="button" @click="openSearch"><Plus :size="17" />添加题目</button>
        </div>

        <div class="set-metrics">
          <span><strong>{{ selectedSetStats.total }}</strong> 道题</span>
          <span><strong>{{ selectedSetStats.passed }}</strong> 已通过</span>
          <span v-for="[difficulty, count] in selectedSetStats.difficulties" :key="difficulty">{{ difficulty }} {{ count }}</span>
        </div>
        <div v-if="selectedSetStats.tags.length > 0" class="tag-row compact-tags">
          <span v-for="[tag, count] in selectedSetStats.tags" :key="tag" class="tag">{{ tag }} × {{ count }}</span>
        </div>

        <div class="set-table-wrap">
          <table class="set-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>状态</th>
                <th>题号</th>
                <th>题目名称</th>
                <th>标签</th>
                <th>难度</th>
                <th>排序</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(problem, index) in selectedSet.problems"
                :key="problem.id"
                class="set-problem-row"
                draggable="true"
                @dragstart="startDrag(problem.id)"
                @dragover.prevent
                @drop="dropOn(problem.id)"
              >
                <td>{{ index + 1 }}</td>
                <td>
                  <span class="pass-state" :class="{ passed: problem.passed }">
                    <CheckCircle2 :size="14" />{{ problem.passed ? '已过' : '未过' }}
                  </span>
                </td>
                <td>P{{ problem.id }}</td>
                <td>
                  <button class="set-problem-title title-link" type="button" @click="openProblem(problem)">
                    {{ problem.title }}
                  </button>
                </td>
                <td>
                  <div class="table-tags">
                    <span v-for="tag in problem.tags.slice(0, 3)" :key="tag" class="mini-tag">{{ tag }}</span>
                    <span v-if="problem.tags.length > 3" class="mini-tag muted">+{{ problem.tags.length - 3 }}</span>
                  </div>
                </td>
                <td><span class="difficulty">{{ problem.difficulty }}</span></td>
                <td>
                  <div class="row-actions compact">
                    <button class="icon-btn" type="button" :disabled="index === 0" @click="moveProblem(problem.id, -1)" aria-label="上移">
                      <ArrowUp :size="15" />
                    </button>
                    <button class="icon-btn" type="button" :disabled="index === selectedSet.problems.length - 1" @click="moveProblem(problem.id, 1)" aria-label="下移">
                      <ArrowDown :size="15" />
                    </button>
                  </div>
                </td>
                <td>
                  <button class="text-danger" type="button" @click="removeProblem(problem.id)"><Trash2 :size="15" />移出</button>
                </td>
              </tr>
            </tbody>
          </table>
          <p v-if="selectedSet.problems.length === 0" class="empty-state">这个题单还没有题目，点击右上角添加题目。</p>
        </div>
      </template>

      <p v-else class="empty-state">还没有可管理的题单。</p>
    </main>
  </section>

  <div v-if="searchOpen" class="modal-backdrop" @click.self="closeSearch">
    <section class="search-modal panel" role="dialog" aria-modal="true" aria-label="添加题目到题单">
      <header class="modal-header">
        <div>
          <h2>添加题目</h2>
          <p>从题库搜索题目，勾选后批量加入当前题单。</p>
        </div>
        <button class="icon-btn" type="button" aria-label="关闭" @click="closeSearch"><X :size="18" /></button>
      </header>

      <div class="search-toolbar">
        <input v-model="searchKeyword" class="input" placeholder="搜索题目关键词" @keyup.enter="searchProblems" />
        <select v-model="searchDifficulty" class="select">
          <option value="">全部难度</option>
          <option v-for="difficulty in difficulties" :key="difficulty" :value="difficulty">{{ difficulty }}</option>
        </select>
        <button class="primary" type="button" :disabled="searching" @click="searchProblems"><Search :size="17" />搜索</button>
      </div>

      <div class="candidate-list">
        <label
          v-for="problem in searchResults"
          :key="problem.id"
          class="candidate-row"
          :class="{ disabled: selectedSetProblemIds.has(problem.id) }"
        >
          <input
            type="checkbox"
            :aria-label="`选择题目 ${problem.title}`"
            :checked="selectedProblemIds.includes(problem.id)"
            :disabled="selectedSetProblemIds.has(problem.id)"
            @change="toggleCandidate(problem.id)"
          />
          <span class="candidate-title">P{{ problem.id }} {{ problem.title }}</span>
          <span class="difficulty">{{ problem.difficulty }}</span>
          <span class="candidate-tags">{{ problem.tags.slice(0, 3).join(' / ') || '没有标签' }}</span>
        </label>
        <p v-if="searchResults.length === 0" class="empty-state">暂无搜索结果。</p>
      </div>

      <footer class="modal-footer">
        <span class="status">已选择 {{ selectedProblemIds.length }} 题</span>
        <button class="primary" type="button" :disabled="adding || selectedProblemIds.length === 0" @click="addSelectedProblems">
          加入当前题单
        </button>
      </footer>
    </section>
  </div>

  <div v-if="selectedProblem" class="modal-backdrop" @click.self="closeProblem">
    <section class="problem-detail-modal panel" role="dialog" aria-modal="true" :aria-label="selectedProblem.title">
      <header class="modal-header problem-modal-header">
        <div>
          <h2>P{{ selectedProblem.id }} {{ selectedProblem.title }}</h2>
          <div class="problem-meta-line">
            <span class="difficulty">{{ selectedProblem.difficulty }}</span>
            <span v-if="selectedProblem.passed" class="pass-state passed"><CheckCircle2 :size="14" />已通过</span>
            <span v-else class="pass-state"><CheckCircle2 :size="14" />未通过</span>
          </div>
        </div>
        <button class="icon-btn" type="button" aria-label="关闭题面" @click="closeProblem"><X :size="18" /></button>
      </header>

      <div class="problem-detail-body">
        <div class="tag-row problem-detail-tags">
          <span v-for="tag in selectedProblem.tags" :key="tag" class="tag">{{ tag }}</span>
          <span v-if="selectedProblem.tags.length === 0" class="tag">没有标签</span>
        </div>
        <article class="markdown-preview problem-statement" v-html="selectedProblemHtml"></article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.compact-header {
  margin-bottom: 14px;
}

.set-workspace {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.set-sidebar,
.set-detail {
  box-shadow: var(--shadow-sm);
}

.set-sidebar {
  padding: 14px;
  position: sticky;
  top: 18px;
}

.create-strip {
  display: grid;
  gap: 8px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--line);
}

.set-tabs {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.set-tab {
  border: 1px solid transparent;
  background: transparent;
  border-radius: 8px;
  padding: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  text-align: left;
  color: var(--ink);
}

.set-tab:hover {
  background: var(--surface-soft);
}

.set-tab.active {
  border-color: rgba(23, 104, 79, 0.42);
  background: var(--primary-soft);
}

.set-tab-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 800;
}

.set-tab-meta {
  color: var(--muted);
  font-size: 13px;
}

.set-detail {
  padding: 0;
  overflow: hidden;
}

.set-detail-header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  padding: 18px 20px 12px;
  border-bottom: 1px solid var(--line);
}

.set-detail-header h2,
.modal-header h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
}

.set-detail-header p,
.modal-header p {
  margin: 6px 0 0;
  color: var(--muted);
  line-height: 1.5;
}

.set-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 20px 6px;
}

.set-metrics span {
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 4px 9px;
  color: #46534c;
  background: var(--surface-soft);
  font-size: 13px;
}

.set-metrics strong {
  color: var(--primary-strong);
}

.compact-tags {
  padding: 0 20px 12px;
}

.set-table-wrap {
  border-top: 1px solid var(--line);
  overflow-x: auto;
}

.set-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 820px;
}

.set-table th,
.set-table td {
  border-bottom: 1px solid var(--line);
  padding: 10px 12px;
  text-align: left;
  vertical-align: middle;
  font-size: 14px;
}

.set-table th {
  background: #f7f9f5;
  color: #59655f;
  font-weight: 800;
}

.set-table tbody tr {
  background: #fff;
}

.set-table tbody tr:hover {
  background: #fbfcfa;
}

.set-problem-title {
  min-width: 180px;
  color: #1d5f92;
  font-weight: 800;
}

.title-link {
  border: 0;
  padding: 0;
  background: transparent;
  text-align: left;
  line-height: 1.45;
}

.title-link:hover {
  color: #0b75bd;
  text-decoration: underline;
}

.pass-state,
.text-danger,
.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.pass-state {
  border-radius: 999px;
  padding: 3px 7px;
  color: var(--muted);
  background: var(--surface-muted);
  font-size: 13px;
}

.pass-state.passed {
  color: var(--primary-strong);
  background: var(--primary-soft);
}

.table-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  max-width: 240px;
}

.mini-tag {
  border-radius: 4px;
  background: #ecf1ed;
  color: #34483f;
  padding: 2px 6px;
  font-size: 12px;
}

.mini-tag.muted {
  color: var(--muted);
}

.row-actions.compact {
  flex-wrap: nowrap;
  gap: 5px;
}

.icon-btn {
  width: 30px;
  height: 30px;
  border: 1px solid var(--line);
  border-radius: 6px;
  background: #fff;
  color: #31443b;
}

.icon-btn:hover {
  border-color: var(--line-strong);
  background: var(--surface-soft);
}

.icon-btn:disabled {
  color: var(--faint);
  background: #f4f6f3;
  cursor: not-allowed;
}

.text-danger {
  border: 1px solid #efc6c1;
  border-radius: 6px;
  background: var(--danger-soft);
  color: var(--danger);
  padding: 6px 9px;
  font-weight: 800;
}

.empty-state {
  color: var(--muted);
  padding: 14px;
  margin: 0;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: rgba(23, 31, 27, 0.38);
  display: grid;
  place-items: center;
  padding: 22px;
}

.search-modal {
  width: min(920px, 100%);
  max-height: min(720px, calc(100vh - 44px));
  padding: 0;
  overflow: hidden;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
}

.problem-detail-modal {
  width: min(1040px, 100%);
  max-height: min(820px, calc(100vh - 44px));
  padding: 0;
  overflow: hidden;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
}

.modal-header,
.modal-footer {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
}

.modal-footer {
  border-top: 1px solid var(--line);
  border-bottom: 0;
}

.problem-modal-header {
  align-items: flex-start;
}

.problem-meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.problem-detail-body {
  overflow: auto;
  padding: 18px 22px 26px;
}

.problem-detail-tags {
  margin-bottom: 14px;
}

.problem-statement {
  max-width: 900px;
}

.problem-statement :deep(h1),
.problem-statement :deep(h2),
.problem-statement :deep(h3) {
  margin-top: 18px;
  margin-bottom: 10px;
}

.search-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 160px auto;
  gap: 8px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
}

.candidate-list {
  overflow: auto;
}

.candidate-row {
  display: grid;
  grid-template-columns: 28px minmax(220px, 1fr) 120px minmax(140px, 0.8fr);
  gap: 10px;
  align-items: center;
  padding: 10px 18px;
  border-bottom: 1px solid var(--line);
}

.candidate-row:hover {
  background: #fbfcfa;
}

.candidate-row.disabled {
  opacity: 0.58;
}

.candidate-title {
  color: #1d5f92;
  font-weight: 800;
}

.candidate-tags {
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 980px) {
  .set-workspace {
    grid-template-columns: 1fr;
  }

  .set-sidebar {
    position: static;
  }

  .search-toolbar,
  .candidate-row {
    grid-template-columns: 1fr;
  }
}
</style>
