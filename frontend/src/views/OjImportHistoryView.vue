<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Dumbbell, ExternalLink, RefreshCw, SquareCheckBig, TriangleAlert } from 'lucide-vue-next'
import { api } from '../services/api'
import type { DuplicateHint, OjImportHistoryItem, OjImportHistoryJob, Problem } from '../types'

const loading = ref(false)
const error = ref('')
const jobs = ref<OjImportHistoryJob[]>([])
const selectedItem = ref<OjImportHistoryItem | null>(null)
const selectedProblem = ref<Problem | null>(null)
const duplicateHints = ref<DuplicateHint[]>([])
const statementDraft = ref('')
const statementStatus = ref('')
const statementLoading = ref(false)
const practiceSetStatus = ref('')
const practiceSetSaving = ref(false)

const totalItems = computed(() => jobs.value.reduce((sum, job) => sum + job.items.length, 0))
const successItems = computed(() => jobs.value.flatMap(job => job.items).filter(item => item.status === 'SUCCEEDED').length)
const failedItems = computed(() => jobs.value.flatMap(job => job.items).filter(item => item.status === 'FAILED').length)

async function loadHistory() {
  loading.value = true
  error.value = ''
  try {
    jobs.value = await api.listOjImportHistory()
  } catch (err) {
    error.value = ojImportHistoryErrorMessage(err)
  } finally {
    loading.value = false
  }
}

function ojImportHistoryErrorMessage(err: unknown) {
  const message = err instanceof Error ? err.message : 'OJ 导入记录加载失败'
  if (message === 'Forbidden') {
    return '当前网页没有权限读取导入记录，请确认网页端已登录同一账号，并且插件里的前端地址指向当前网页地址。'
  }
  return message
}

async function openStatementEditor(item: OjImportHistoryItem) {
  if (!item.problemId) return
  statementLoading.value = true
  statementStatus.value = ''
  duplicateHints.value = []
  try {
    selectedItem.value = item
    const [problem, hints] = await Promise.all([
      api.getProblem(item.problemId),
      api.listSimilarProblems(item.problemId)
    ])
    selectedProblem.value = problem
    duplicateHints.value = hints
    statementDraft.value = selectedProblem.value.description
  } catch (err) {
    statementStatus.value = err instanceof Error ? err.message : '题面加载失败'
  } finally {
    statementLoading.value = false
  }
}

async function saveStatementCorrection() {
  if (!selectedProblem.value) return
  statementLoading.value = true
  statementStatus.value = ''
  try {
    const problem = selectedProblem.value
    selectedProblem.value = await api.updateProblem(problem.id, {
      title: problem.title,
      description: statementDraft.value,
      difficulty: problem.difficultyCode,
      tags: problem.tags,
      source: problem.source
    })
    statementDraft.value = selectedProblem.value.description
    statementStatus.value = '题面修正已保存。'
  } catch (err) {
    statementStatus.value = err instanceof Error ? err.message : '题面修正保存失败'
  } finally {
    statementLoading.value = false
  }
}

function importedProblemIds(job: OjImportHistoryJob) {
  return [...new Set(job.items
    .filter(item => item.status === 'SUCCEEDED' && item.problemId)
    .map(item => item.problemId as number))]
}

async function createPracticeSet(job: OjImportHistoryJob) {
  const problemIds = importedProblemIds(job)
  if (!problemIds.length) return
  practiceSetSaving.value = true
  practiceSetStatus.value = ''
  try {
    await api.createProblemSetWithProblems(`OJ 导入训练 ${formatDate(job.createdAt)}`, '来自 OJ 导入记录', problemIds)
    practiceSetStatus.value = '训练题单已创建'
  } catch (err) {
    practiceSetStatus.value = err instanceof Error ? err.message : '训练题单创建失败'
  } finally {
    practiceSetSaving.value = false
  }
}

function platformName(platform: string) {
  return {
    CODEFORCES: 'Codeforces',
    ATCODER: 'AtCoder',
    LUOGU: '洛谷',
    NOWCODER: '牛客'
  }[platform] ?? platform
}

function statusText(status: string) {
  return {
    PENDING: '排队中',
    RUNNING: '分析中',
    SUCCEEDED: '已入库',
    FAILED: '失败'
  }[status] ?? status
}

function statusClass(status: string) {
  return {
    PENDING: 'pending',
    RUNNING: 'running',
    SUCCEEDED: 'success',
    FAILED: 'danger'
  }[status] ?? 'pending'
}

function formatDate(value?: string) {
  if (!value) return '未记录'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(value))
}

function formatDuration(value?: number) {
  if (value === undefined || value === null) return '未记录'
  if (value < 1000) return `${value} ms`
  return `${(value / 1000).toFixed(value < 10000 ? 1 : 0)} s`
}

function runtimeText(item: OjImportHistoryItem) {
  const parts = [item.aiProvider, item.aiModel, item.aiDurationMs === undefined ? undefined : formatDuration(item.aiDurationMs)]
    .filter(Boolean)
  return parts.length ? parts.join(' · ') : 'AI 处理信息未记录'
}

onMounted(loadHistory)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>OJ 导入记录</h1>
      <p>查看浏览器插件导入后的排队、AI 分析、入库和失败情况。</p>
    </div>
    <button class="ghost" type="button" :disabled="loading" @click="loadHistory">
      <RefreshCw :size="18" />刷新
    </button>
  </header>

  <section class="import-summary">
    <article class="panel summary-card">
      <span>导入项</span>
      <strong>{{ totalItems }}</strong>
    </article>
    <article class="panel summary-card success">
      <span>已入库</span>
      <strong>{{ successItems }}</strong>
    </article>
    <article class="panel summary-card danger">
      <span>失败</span>
      <strong>{{ failedItems }}</strong>
    </article>
  </section>

  <p v-if="error" class="error">{{ error }}</p>
  <p v-if="practiceSetStatus" class="status">{{ practiceSetStatus }}</p>

  <section class="import-list">
    <article v-for="job in jobs" :key="job.id" class="panel import-job">
      <div class="job-head">
        <div>
          <h2>{{ job.name }}</h2>
          <p>{{ job.items.length }} 个导入项 · {{ job.successCount }} 成功 · {{ job.failedCount }} 失败</p>
        </div>
        <div class="job-actions">
          <time>{{ formatDate(job.createdAt) }}</time>
          <button
            class="secondary practice-button"
            type="button"
            :disabled="practiceSetSaving || importedProblemIds(job).length === 0"
            @click="createPracticeSet(job)"
          >
            <Dumbbell :size="16" />生成训练题单
          </button>
        </div>
      </div>

      <div class="item-list">
        <div v-for="item in job.items" :key="item.id" class="import-item">
          <div class="item-main">
            <span class="status-pill" :class="statusClass(item.status)">
              {{ statusText(item.status) }}
            </span>
            <div>
              <h3>{{ item.title }}</h3>
              <p class="item-meta">
                <strong>{{ item.sourceId }}</strong>
                <span>{{ platformName(item.platform) }}</span>
                <span v-if="item.passedRequested" class="passed-request">
                  <SquareCheckBig :size="14" />已请求标记通过
                </span>
              </p>
            </div>
          </div>

          <div class="item-side">
            <span v-if="item.problemId" class="problem-chip">题库 #{{ item.problemId }}</span>
            <a class="source-link" :href="item.sourceUrl" target="_blank" rel="noreferrer">
              <ExternalLink :size="15" />来源
            </a>
            <button
              v-if="item.problemId"
              class="source-link statement-button"
              type="button"
              @click="openStatementEditor(item)"
            >
              预览/修正 {{ item.title }}
            </button>
          </div>

          <p class="runtime">{{ runtimeText(item) }}</p>
          <p v-if="item.errorMessage" class="item-error">
            <TriangleAlert :size="15" />{{ item.errorMessage }}
          </p>
        </div>
      </div>
    </article>

    <p v-if="!loading && jobs.length === 0" class="status">暂无 OJ 导入记录。</p>
  </section>

  <section v-if="selectedItem" class="panel statement-review">
    <div class="review-head">
      <div>
        <span class="section-label">题面整理预览</span>
        <h2>{{ selectedItem.title }}</h2>
      </div>
      <span v-if="statementLoading" class="status">处理中...</span>
    </div>

    <div class="statement-grid">
      <article>
        <h3>原始题面</h3>
        <pre>{{ selectedItem.originalStatement }}</pre>
      </article>
      <article>
        <label for="polished-statement">整理后题面</label>
        <textarea
          id="polished-statement"
          v-model="statementDraft"
          :disabled="statementLoading || !selectedProblem"
        />
      </article>
    </div>

    <section v-if="duplicateHints.length" class="duplicate-hints">
      <h3>潜在重复题</h3>
      <div class="duplicate-list">
        <article v-for="hint in duplicateHints" :key="hint.id" class="duplicate-item">
          <div>
            <strong>{{ hint.title }}</strong>
            <p>{{ hint.reason }}</p>
          </div>
          <div class="duplicate-meta">
            <span>#{{ hint.id }}</span>
            <span>{{ hint.difficulty }}</span>
            <span>{{ hint.score }}</span>
            <a v-if="hint.sourceUrl" :href="hint.sourceUrl" target="_blank" rel="noreferrer">来源</a>
          </div>
        </article>
      </div>
    </section>

    <div class="review-actions">
      <button class="primary" type="button" :disabled="statementLoading || !selectedProblem" @click="saveStatementCorrection">
        保存修正
      </button>
      <span v-if="statementStatus" class="status">{{ statementStatus }}</span>
    </div>
  </section>
</template>

<style scoped>
.import-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.summary-card {
  display: grid;
  gap: 6px;
  padding: 16px;
}

.summary-card span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 800;
}

.summary-card strong {
  font-size: 30px;
}

.summary-card.success strong {
  color: var(--primary);
}

.summary-card.danger strong {
  color: var(--danger);
}

.import-list {
  display: grid;
  gap: 14px;
}

.import-job {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.job-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.job-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.practice-button {
  min-height: 32px;
  padding: 7px 10px;
  border-radius: 6px;
}

.job-head h2 {
  margin: 0 0 4px;
  font-size: 19px;
}

.job-head p,
.job-head time,
.item-meta,
.runtime {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
}

.item-list {
  display: grid;
  gap: 10px;
}

.import-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px 14px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-soft);
}

.item-main {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  min-width: 0;
}

.item-main h3 {
  margin: 0 0 5px;
  font-size: 16px;
}

.item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.passed-request,
.source-link,
.problem-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.problem-chip {
  padding: 5px 8px;
  border-radius: 999px;
  background: rgba(23, 104, 79, 0.1);
  color: var(--primary);
  font-size: 12px;
  font-weight: 800;
}

.source-link {
  color: var(--blue);
  font-weight: 800;
  text-decoration: none;
}

.statement-button {
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
  font: inherit;
}

.item-side {
  display: flex;
  gap: 8px;
  align-items: center;
}

.runtime {
  grid-column: 1 / -1;
}

.item-error {
  grid-column: 1 / -1;
  display: flex;
  gap: 6px;
  align-items: center;
  margin: 0;
  color: var(--danger);
  font-weight: 800;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 24px;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.status-pill.success {
  color: var(--primary);
  background: rgba(23, 104, 79, 0.1);
}

.status-pill.danger {
  color: var(--danger);
  background: var(--danger-soft);
}

.status-pill.pending {
  background: #f4f7fb;
  color: var(--blue);
}

.status-pill.running {
  background: #fff7ed;
  color: #9a5a00;
}

.statement-review {
  display: grid;
  gap: 16px;
  margin-top: 16px;
  padding: 18px;
}

.review-head,
.review-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.section-label {
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.review-head h2 {
  margin: 4px 0 0;
  font-size: 20px;
}

.statement-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
}

.statement-grid article {
  display: grid;
  gap: 8px;
}

.statement-grid h3,
.statement-grid label {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  font-weight: 900;
}

.statement-grid pre,
.statement-grid textarea {
  min-height: 280px;
  margin: 0;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-soft);
  color: var(--ink);
  font: inherit;
  line-height: 1.65;
  white-space: pre-wrap;
}

.statement-grid textarea {
  resize: vertical;
  background: #fff;
}

.duplicate-hints {
  display: grid;
  gap: 10px;
}

.duplicate-hints h3 {
  margin: 0;
  color: var(--muted);
  font-size: 13px;
  font-weight: 900;
}

.duplicate-list {
  display: grid;
  gap: 8px;
}

.duplicate-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: var(--surface-soft);
}

.duplicate-item p {
  margin: 4px 0 0;
  color: var(--muted);
  font-size: 13px;
}

.duplicate-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
  justify-content: flex-end;
}

.duplicate-meta a {
  color: var(--blue);
}

@media (max-width: 760px) {
  .import-summary {
    grid-template-columns: 1fr;
  }

  .job-head,
  .import-item {
    grid-template-columns: 1fr;
  }

  .job-head {
    display: grid;
  }

  .item-side {
    justify-content: flex-start;
  }

  .statement-grid {
    grid-template-columns: 1fr;
  }
}
</style>
