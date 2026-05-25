<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import markdownItKatex from 'markdown-it-katex'
import 'katex/dist/katex.min.css'
import { Clock3, Cpu, FileText, RefreshCw, Sparkles } from 'lucide-vue-next'
import { api } from '../services/api'
import type { BatchItem, BatchJob, BatchJobDetail } from '../types'
import { normalizeProblemMath } from '../utils/problemMath'

const markdown = new MarkdownIt({ breaks: true, linkify: true }).use(markdownItKatex)

interface HistoryEntry {
  job: BatchJob
  item: BatchItem
}

const loading = ref(false)
const error = ref('')
const details = ref<BatchJobDetail[]>([])
const selected = ref<HistoryEntry | null>(null)

const historyEntries = computed(() => {
  return details.value
    .flatMap(detail => detail.items
      .filter(item => item.status === 'SUCCEEDED' || item.status === 'FAILED')
      .map(item => ({ job: detail.job, item })))
    .sort((a, b) => Date.parse(logTime(b.item)) - Date.parse(logTime(a.item)))
})

const successCount = computed(() => historyEntries.value.filter(entry => entry.item.status === 'SUCCEEDED').length)
const failedCount = computed(() => historyEntries.value.filter(entry => entry.item.status === 'FAILED').length)
const previewHtml = computed(() => markdown.render(normalizeProblemMath(selected.value?.item.content || '')))

async function loadHistory() {
  loading.value = true
  error.value = ''
  try {
    const jobs = await api.listBatchJobs()
    details.value = await Promise.all(jobs.map(job => api.getBatchJob(job.id)))
    if (!selected.value || !historyEntries.value.some(entry => entry.item.id === selected.value?.item.id)) {
      selected.value = historyEntries.value[0] ?? null
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '历史记录加载失败'
  } finally {
    loading.value = false
  }
}

function selectEntry(entry: HistoryEntry) {
  selected.value = entry
}

function statusText(status: string) {
  return {
    FAILED: '失败',
    SUCCEEDED: '成功'
  }[status] ?? status
}

function statusClass(status: string) {
  return status === 'FAILED' ? 'danger' : 'success'
}

function logTime(item: BatchItem) {
  return item.finishedAt || item.startedAt || item.createdAt
}

function formatDate(value?: string) {
  if (!value) {
    return '未记录'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(value))
}

function formatDuration(value?: number) {
  if (value === undefined || value === null) {
    return '未记录'
  }
  if (value < 1000) {
    return `${value} ms`
  }
  return `${(value / 1000).toFixed(value < 10000 ? 1 : 0)} s`
}

function formatConfidence(value?: number) {
  if (value === undefined || value === null) {
    return '未记录'
  }
  return `${Math.round(value * 100)}%`
}

onMounted(loadHistory)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>历史记录</h1>
      <p>按日志视角查看每道题的 AI 分析过程、模型、耗时和结果。</p>
    </div>
    <button class="ghost" type="button" :disabled="loading" @click="loadHistory">
      <RefreshCw :size="18" />刷新
    </button>
  </header>

  <section class="history-workspace">
    <aside class="panel history-list">
      <div class="history-title">
        <div>
          <h2>分析日志</h2>
          <p>{{ historyEntries.length }} 条记录 · {{ successCount }} 成功 · {{ failedCount }} 失败</p>
        </div>
        <span class="count-badge">{{ historyEntries.length }}</span>
      </div>

      <div class="timeline">
        <button
          v-for="entry in historyEntries"
          :key="entry.item.id"
          class="history-item"
          :class="{ active: selected?.item.id === entry.item.id, failed: entry.item.status === 'FAILED' }"
          type="button"
          @click="selectEntry(entry)"
        >
          <span class="timeline-dot" />
          <span class="item-topline">
            <span class="status-pill" :class="statusClass(entry.item.status)">{{ statusText(entry.item.status) }}</span>
            <time>{{ formatDate(logTime(entry.item)) }}</time>
          </span>
          <strong>{{ entry.item.title }}</strong>
          <span class="item-meta">
            {{ entry.item.aiProvider || 'AI 未记录' }}
            <template v-if="entry.item.aiModel"> · {{ entry.item.aiModel }}</template>
            · {{ formatDuration(entry.item.aiDurationMs) }}
          </span>
          <span class="item-meta">
            {{ entry.item.difficulty || '难度未记录' }} · {{ entry.job.name }}
          </span>
        </button>
      </div>

      <p v-if="!loading && historyEntries.length === 0" class="status">暂无历史记录。</p>
      <p v-if="error" class="error">{{ error }}</p>
    </aside>

    <section class="panel history-detail">
      <template v-if="selected">
        <div class="detail-head">
          <div>
            <span class="status-pill" :class="statusClass(selected.item.status)">{{ statusText(selected.item.status) }}</span>
            <h2>{{ selected.item.title }}</h2>
            <p>{{ selected.job.name }} · 保存记录 #{{ selected.item.id }}</p>
          </div>
          <span v-if="selected.item.problemId" class="problem-link">题库 #{{ selected.item.problemId }}</span>
        </div>

        <div class="metric-grid">
          <div class="metric-card">
            <Cpu :size="18" />
            <span>AI 来源</span>
            <strong>{{ selected.item.aiProvider || '未记录' }}</strong>
          </div>
          <div class="metric-card">
            <Sparkles :size="18" />
            <span>模型/命令</span>
            <strong>{{ selected.item.aiModel || '未记录' }}</strong>
          </div>
          <div class="metric-card">
            <Clock3 :size="18" />
            <span>耗时</span>
            <strong>{{ formatDuration(selected.item.aiDurationMs) }}</strong>
          </div>
          <div class="metric-card">
            <FileText :size="18" />
            <span>置信度</span>
            <strong>{{ formatConfidence(selected.item.aiConfidence) }}</strong>
          </div>
        </div>

        <div class="log-section result-summary">
          <div>
            <span class="section-label">分析结果</span>
            <h3>{{ selected.item.difficulty || '难度未记录' }}</h3>
          </div>
          <div v-if="selected.item.tags?.length" class="tag-row">
            <span v-for="tag in selected.item.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <p v-if="selected.item.errorMessage" class="error detail-error">{{ selected.item.errorMessage }}</p>
        </div>

        <div class="log-section">
          <span class="section-label">AI 分析过程</span>
          <p class="reasoning">
            {{ selected.item.aiReasoningSummary || (selected.item.status === 'FAILED' ? '分析失败，未生成过程摘要。' : '旧记录未保存过程摘要。') }}
          </p>
        </div>

        <div v-if="selected.item.aiHints?.length" class="log-section">
          <span class="section-label">分层提示</span>
          <ol class="hint-list">
            <li v-for="(hint, index) in selected.item.aiHints" :key="hint">
              <span>提示 {{ index + 1 }}</span>
              <p>{{ hint }}</p>
            </li>
          </ol>
        </div>

        <div class="meta-strip">
          <span>入队：{{ formatDate(selected.item.createdAt) }}</span>
          <span>开始：{{ formatDate(selected.item.startedAt) }}</span>
          <span>结束：{{ formatDate(selected.item.finishedAt) }}</span>
        </div>

        <div class="log-section problem-source">
          <span class="section-label">原始题面</span>
          <article class="markdown-preview" v-html="previewHtml"></article>
        </div>
      </template>
      <p v-else class="status">选择一条历史记录查看详情。</p>
    </section>
  </section>
</template>

<style scoped>
.history-workspace {
  display: grid;
  grid-template-columns: 420px minmax(520px, 1fr);
  gap: 18px;
  align-items: start;
}

.history-list {
  max-height: calc(100vh - 120px);
  overflow: auto;
  padding: 18px;
}

.history-title {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e4ebe4;
}

.history-title h2,
.detail-head h2,
.result-summary h3 {
  margin: 0;
}

.history-title p,
.detail-head p,
.item-meta,
.item-topline time,
.section-label,
.metric-card span,
.meta-strip {
  color: #617069;
  font-size: 13px;
}

.count-badge {
  min-width: 38px;
  height: 38px;
  border-radius: 999px;
  border: 1px solid #c8dccf;
  background: #f3faf5;
  color: #17684f;
  display: grid;
  place-items: center;
  font-weight: 800;
}

.timeline {
  position: relative;
  display: grid;
  gap: 10px;
  margin-top: 14px;
  padding-left: 12px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 18px;
  top: 8px;
  bottom: 8px;
  width: 1px;
  background: #dce6dd;
}

.history-item {
  position: relative;
  border: 1px solid #e1e8e1;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px 12px 12px 24px;
  text-align: left;
  display: grid;
  gap: 6px;
  box-shadow: 0 6px 18px rgba(24, 34, 29, 0.04);
}

.history-item.active {
  border-color: #1f6f54;
  background: #f1faf5;
  box-shadow: 0 10px 26px rgba(31, 111, 84, 0.11);
}

.history-item.failed {
  border-color: #ecc2bd;
  background: #fff8f6;
}

.timeline-dot {
  position: absolute;
  left: -1px;
  top: 18px;
  width: 11px;
  height: 11px;
  border-radius: 999px;
  border: 2px solid #ffffff;
  background: #8fb7a7;
  box-shadow: 0 0 0 1px #a8c8ba;
}

.history-item.failed .timeline-dot {
  background: #c96c5d;
  box-shadow: 0 0 0 1px #d79b93;
}

.item-topline,
.detail-head,
.meta-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  min-height: 24px;
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 12px;
  font-weight: 800;
}

.status-pill.success {
  background: #e8f5ee;
  color: #17684f;
  border: 1px solid #b9d8c8;
}

.status-pill.danger {
  background: #fff0ed;
  color: #a43e31;
  border: 1px solid #e5b7af;
}

.history-detail {
  display: grid;
  gap: 14px;
  padding: 20px;
}

.detail-head {
  align-items: flex-start;
  border-bottom: 1px solid #e4ebe4;
  padding-bottom: 16px;
}

.detail-head h2 {
  margin-top: 8px;
}

.problem-link {
  border: 1px solid #d8e4dc;
  border-radius: 999px;
  padding: 6px 10px;
  color: #17684f;
  background: #f3faf5;
  font-weight: 800;
  white-space: nowrap;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.metric-card {
  border: 1px solid #e2e8e1;
  border-radius: 8px;
  background: #fbfcfa;
  padding: 12px;
  display: grid;
  gap: 6px;
}

.metric-card svg {
  color: #1f6f54;
}

.metric-card strong {
  color: #1f2d27;
  font-size: 14px;
  overflow-wrap: anywhere;
}

.log-section {
  border: 1px solid #e2e8e1;
  border-radius: 8px;
  background: #ffffff;
  padding: 14px;
  display: grid;
  gap: 10px;
}

.section-label {
  font-weight: 800;
}

.reasoning {
  margin: 0;
  color: #24312c;
  line-height: 1.7;
}

.detail-error {
  margin: 0;
}

.hint-list {
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
  list-style: none;
}

.hint-list li {
  border-left: 3px solid #8fb7a7;
  background: #f7faf7;
  padding: 8px 10px;
}

.hint-list span {
  font-size: 12px;
  color: #17684f;
  font-weight: 800;
}

.hint-list p {
  margin: 4px 0 0;
}

.meta-strip {
  flex-wrap: wrap;
  border: 1px dashed #d9e4dd;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fbfcfa;
}

.problem-source {
  background: #fbfcfa;
}

.markdown-preview {
  color: #24312c;
  line-height: 1.75;
  max-height: 520px;
  overflow: auto;
}

.markdown-preview :deep(pre) {
  overflow: auto;
  background: #f0f3ef;
  padding: 12px;
  border-radius: 6px;
}

.markdown-preview :deep(code) {
  background: #eef2eb;
  padding: 2px 5px;
  border-radius: 4px;
}

@media (max-width: 1180px) {
  .history-workspace {
    grid-template-columns: 1fr;
  }

  .history-list {
    max-height: none;
  }
}

@media (max-width: 760px) {
  .metric-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 520px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
