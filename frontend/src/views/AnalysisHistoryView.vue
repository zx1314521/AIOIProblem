<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import markdownItKatex from 'markdown-it-katex'
import 'katex/dist/katex.min.css'
import { RefreshCw } from 'lucide-vue-next'
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
    .sort((a, b) => Date.parse(b.item.createdAt) - Date.parse(a.item.createdAt))
})

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

onMounted(loadHistory)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>历史记录</h1>
      <p>查看每个批量题目的 AI 分析结果、失败原因和原始题面。</p>
    </div>
    <button class="ghost" type="button" :disabled="loading" @click="loadHistory">
      <RefreshCw :size="18" />刷新
    </button>
  </header>

  <section class="history-workspace">
    <aside class="panel history-list">
      <div class="history-title">
        <h2>分析日志</h2>
        <span class="tag">{{ historyEntries.length }}</span>
      </div>
      <button
        v-for="entry in historyEntries"
        :key="entry.item.id"
        class="history-item"
        :class="{ active: selected?.item.id === entry.item.id, failed: entry.item.status === 'FAILED' }"
        type="button"
        @click="selectEntry(entry)"
      >
        <strong>{{ entry.item.title }}</strong>
        <small>
          {{ statusText(entry.item.status) }}
          <template v-if="entry.item.difficulty"> · {{ entry.item.difficulty }}</template>
          · {{ entry.job.name }}
        </small>
      </button>
      <p v-if="!loading && historyEntries.length === 0" class="status">暂无历史记录。</p>
      <p v-if="error" class="error">{{ error }}</p>
    </aside>

    <section class="panel result-list">
      <template v-if="selected">
        <div class="log-card">
          <span class="status">{{ statusText(selected.item.status) }}</span>
          <h2>{{ selected.item.title }}</h2>
          <div v-if="selected.item.difficulty">
            <span class="difficulty">{{ selected.item.difficulty }}</span>
          </div>
          <div v-if="selected.item.tags?.length" class="tag-row">
            <span v-for="tag in selected.item.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <p v-if="selected.item.errorMessage" class="error">{{ selected.item.errorMessage }}</p>
          <p v-else-if="selected.item.problemId" class="status">已保存到题库 #{{ selected.item.problemId }}</p>
        </div>
        <article class="markdown-preview" v-html="previewHtml"></article>
      </template>
      <p v-else class="status">选择一条历史记录查看详情。</p>
    </section>
  </section>
</template>

<style scoped>
.history-workspace {
  display: grid;
  grid-template-columns: 340px minmax(520px, 1fr);
  gap: 18px;
  align-items: start;
}

.history-list {
  max-height: calc(100vh - 120px);
  overflow: auto;
}

.history-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.history-title h2 {
  margin: 0;
}

.history-item {
  border: 1px solid #e3e7e1;
  border-radius: 6px;
  background: #fbfcfa;
  padding: 9px 10px;
  text-align: left;
  display: grid;
  gap: 4px;
}

.history-item.active {
  border-color: #1f6f54;
  background: #eef8f2;
}

.history-item.failed {
  border-color: #e4bbb6;
  background: #fff6f4;
}

.history-item small {
  color: #617069;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-card {
  border: 1px solid #dfe4dc;
  border-radius: 6px;
  background: #fbfcfa;
  padding: 12px;
  display: grid;
  gap: 10px;
}

.log-card h2 {
  margin: 0;
}

.markdown-preview {
  color: #24312c;
  line-height: 1.75;
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

@media (max-width: 1100px) {
  .history-workspace {
    grid-template-columns: 1fr;
  }

  .history-list {
    max-height: none;
  }
}
</style>
