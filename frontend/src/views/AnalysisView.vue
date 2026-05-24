<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import markdownItKatex from 'markdown-it-katex'
import 'katex/dist/katex.min.css'
import { Edit3, FileText, Save, Trash2, UploadCloud } from 'lucide-vue-next'
import { api } from '../services/api'
import type { BatchItem, BatchJob, BatchJobDetail } from '../types'
import { normalizeProblemMath } from '../utils/problemMath'

const markdown = new MarkdownIt({ breaks: true, linkify: true }).use(markdownItKatex)
type BatchStatusFilter = 'ALL' | BatchItem['status']

const title = ref('')
const text = ref('')
const fileName = ref('')
const loading = ref(false)
const error = ref('')

const jobs = ref<BatchJob[]>([])
const selected = ref<BatchJobDetail | null>(null)
const selectedItem = ref<BatchItem | null>(null)
const batchFiles = ref<File[]>([])
const batchName = ref('批量题面分析')
const batchError = ref('')
const uploading = ref(false)
const editing = ref(false)
const editTitle = ref('')
const editContent = ref('')
const draggedItemId = ref<number | null>(null)
const statusFilter = ref<BatchStatusFilter>('ALL')
const expandedErrorItemIds = ref<Set<number>>(new Set())
let timer: number | undefined

const selectedJob = computed(() => selected.value?.job)
const queueItems = computed(() => selected.value?.items ?? [])
const filteredQueueItems = computed(() => queueItems.value.filter(item => matchesStatusFilter(item, statusFilter.value)))
const statusFilterOptions = computed(() => [
  { value: 'ALL' as const, label: '全部', count: queueItems.value.length },
  { value: 'PENDING' as const, label: '等待', count: countItemsByStatus('PENDING') },
  { value: 'RUNNING' as const, label: '运行中', count: countItemsByStatus('RUNNING') },
  { value: 'SUCCEEDED' as const, label: '成功', count: countItemsByStatus('SUCCEEDED') },
  { value: 'FAILED' as const, label: '失败', count: countItemsByStatus('FAILED') }
])
const emptyFilterText = computed(() => {
  const option = statusFilterOptions.value.find(item => item.value === statusFilter.value)
  return option ? `当前没有${option.label === '全部' ? '任务' : `${option.label}题`}` : '当前没有任务'
})
const previewHtml = computed(() => markdown.render(normalizeProblemMath(selectedItem.value?.content || '')))

async function analyzeText() {
  const content = text.value.trim()
  if (!content) return
  loading.value = true
  error.value = ''
  batchError.value = ''
  try {
    const problemTitle = title.value.trim() || (fileName.value ? titleFromFilename(fileName.value) : '未命名题目')
    const file = new File([content], `${filenameSafeTitle(problemTitle)}.md`, { type: 'text/markdown' })
    selected.value = await api.uploadBatch('单题分析', [file])
    statusFilter.value = 'ALL'
    selectedItem.value = preferredItem()
    editing.value = false
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加入任务失败'
  } finally {
    loading.value = false
  }
}

async function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  if (files.length === 0) return
  const validFiles = files.filter(isTextProblemFile)
  if (validFiles.length !== files.length) {
    error.value = '请选择 .txt 或 .md 文件'
    input.value = ''
    return
  }
  if (validFiles.length > 1) {
    batchFiles.value = validFiles
    fileName.value = ''
    error.value = ''
    return
  }
  const selectedFile = validFiles[0]
  try {
    batchFiles.value = []
    fileName.value = selectedFile.name
    title.value = title.value || titleFromFilename(selectedFile.name)
    text.value = await selectedFile.text()
    error.value = ''
  } catch {
    error.value = '文件读取失败'
  }
}

async function loadJobs(keepSelection = true) {
  try {
    jobs.value = await api.listBatchJobs()
    if (selected.value && keepSelection) {
      const stillExists = jobs.value.some(job => job.id === selected.value?.job.id)
      if (stillExists) {
        selected.value = await api.getBatchJob(selected.value.job.id)
        syncSelectedItem()
        return
      }
    }
    const activeJob = jobs.value.find(isActiveJob)
    if (activeJob) {
      await selectJob(activeJob.id)
    } else {
      selected.value = null
      selectedItem.value = null
    }
  } catch (err) {
    batchError.value = err instanceof Error ? err.message : '任务加载失败'
  }
}

async function selectJob(id: number) {
  selected.value = await api.getBatchJob(id)
  selectedItem.value = preferredItem()
  editing.value = false
}

async function uploadBatch() {
  if (batchFiles.value.length === 0) {
    batchError.value = '请选择 .txt 或 .md 文件'
    return
  }
  uploading.value = true
  batchError.value = ''
  try {
    selected.value = await api.uploadBatch(batchName.value, batchFiles.value)
    statusFilter.value = 'ALL'
    selectedItem.value = preferredItem()
    batchFiles.value = []
  } catch (err) {
    batchError.value = err instanceof Error ? err.message : '上传失败'
  } finally {
    uploading.value = false
  }
}

function selectItem(item: BatchItem) {
  selectedItem.value = item
  editing.value = false
}

function selectStatusFilter(nextFilter: BatchStatusFilter) {
  statusFilter.value = nextFilter
  editing.value = false
  if (selectedItem.value && matchesStatusFilter(selectedItem.value, nextFilter)) return
  selectedItem.value = filteredQueueItems.value[0] ?? null
}

function startEdit(item: BatchItem) {
  if (item.status !== 'PENDING') return
  selectedItem.value = item
  editTitle.value = item.title
  editContent.value = item.content
  editing.value = true
}

async function saveItemEdit() {
  if (!selectedJob.value || !selectedItem.value) return
  const updated = await api.updateBatchItem(selectedJob.value.id, selectedItem.value.id, {
    title: editTitle.value,
    content: editContent.value
  })
  selectedItem.value = updated
  editing.value = false
  await loadJobs()
}

async function deleteItem(item: BatchItem) {
  if (!selectedJob.value || item.status !== 'PENDING') return
  selected.value = await api.deleteBatchItem(selectedJob.value.id, item.id)
  selectedItem.value = filteredQueueItems.value[0] ?? preferredItem()
  await loadJobs()
}

function onDragStart(item: BatchItem) {
  if (item.status !== 'PENDING') return
  draggedItemId.value = item.id
}

async function onDrop(target: BatchItem) {
  if (!selectedJob.value || target.status !== 'PENDING' || draggedItemId.value === null || draggedItemId.value === target.id) {
    draggedItemId.value = null
    return
  }
  const pending = queueItems.value.filter(item => item.status === 'PENDING')
  const from = pending.findIndex(item => item.id === draggedItemId.value)
  const to = pending.findIndex(item => item.id === target.id)
  if (from < 0 || to < 0) return
  const reordered = [...pending]
  const [moved] = reordered.splice(from, 1)
  reordered.splice(to, 0, moved)
  selected.value = await api.reorderBatchItems(selectedJob.value.id, reordered.map(item => item.id))
  draggedItemId.value = null
  syncSelectedItem()
}

function countItemsByStatus(status: BatchItem['status']) {
  return queueItems.value.filter(item => item.status === status).length
}

function matchesStatusFilter(item: BatchItem, filter: BatchStatusFilter) {
  return filter === 'ALL' || item.status === filter
}

function preferredItem() {
  return filteredQueueItems.value.find(item => item.status === 'RUNNING' || item.status === 'PENDING')
    ?? filteredQueueItems.value[0]
    ?? null
}

function syncSelectedItem() {
  if (!selected.value) {
    selectedItem.value = null
    return
  }
  const updated = selectedItem.value
    ? selected.value.items.find(item => item.id === selectedItem.value?.id) ?? null
    : null
  selectedItem.value = updated && matchesStatusFilter(updated, statusFilter.value)
    ? updated
    : preferredItem()
}

function errorSummary(message: string) {
  const normalized = message.replace(/\s+/g, ' ').trim()
  return normalized.length > 88 ? `${normalized.slice(0, 88)}...` : normalized
}

function isErrorExpanded(itemId: number) {
  return expandedErrorItemIds.value.has(itemId)
}

function toggleError(itemId: number) {
  const next = new Set(expandedErrorItemIds.value)
  if (next.has(itemId)) {
    next.delete(itemId)
  } else {
    next.add(itemId)
  }
  expandedErrorItemIds.value = next
}

function statusText(status: string) {
  return {
    RUNNING: '运行中',
    PAUSED: '已暂停',
    COMPLETED: '已完成',
    FAILED: '失败',
    PENDING: '等待中',
    SUCCEEDED: '成功'
  }[status] ?? status
}

function titleFromFilename(name: string) {
  return name.replace(/\.(txt|md)$/i, '') || '未命名题目'
}

function filenameSafeTitle(source: string) {
  return source.replace(/[\\/:*?"<>|]/g, '_').trim() || '未命名题目'
}

function isTextProblemFile(file: File) {
  const name = file.name.toLowerCase()
  return name.endsWith('.txt') || name.endsWith('.md')
}

function isActiveJob(job: BatchJob) {
  return job.status === 'RUNNING' || job.status === 'PAUSED' || job.pendingCount > 0 || job.runningCount > 0
}

onMounted(() => {
  loadJobs(false)
  timer = window.setInterval(() => loadJobs(true), 3000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <header class="page-header">
    <div>
      <h1>题面分析</h1>
      <p>粘贴题面或选择 txt/md 文件，单个文件导入分析，多个文件加入任务队列。</p>
    </div>
  </header>

  <section class="analysis-workbench">
    <section class="grid">
      <form class="panel grid analysis-card" @submit.prevent="analyzeText">
        <h2 class="form-title">分析</h2>
        <label class="field">
          <span>题目标题</span>
          <input v-model="title" class="input" placeholder="例如：区间最大值" />
        </label>
        <label class="field">
          <span>题目描述</span>
          <textarea v-model="text" class="textarea" placeholder="在这里粘贴题面、输入输出与数据范围" />
        </label>
        <div class="actions">
          <button class="primary" type="submit" :disabled="loading || !text.trim()">
            <FileText :size="18" />开始分析
          </button>
          <label class="secondary file-button batch-picker">
            <UploadCloud :size="18" />选择 txt/md 文件
            <input type="file" accept=".txt,.md" multiple @change="onFileChange" />
          </label>
          <span v-if="fileName" class="status">{{ fileName }} 已导入</span>
          <span v-if="batchFiles.length > 1" class="status">{{ batchFiles.length }} 个文件待入队</span>
        </div>

        <div v-if="batchFiles.length > 1" class="actions">
          <button class="primary" type="button" :disabled="uploading || batchFiles.length === 0" @click="uploadBatch">
            上传并加入队列
          </button>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="batchError" class="error">{{ batchError }}</p>
      </form>
    </section>

    <aside class="panel task-rail">
      <div class="rail-header">
        <h2>任务列表</h2>
      </div>

      <div v-if="selectedJob" class="status-filters" aria-label="任务状态筛选">
        <button
          v-for="option in statusFilterOptions"
          :key="option.value"
          class="filter-chip"
          :class="{ active: statusFilter === option.value }"
          type="button"
          :aria-pressed="statusFilter === option.value"
          @click="selectStatusFilter(option.value)"
        >
          <span>{{ option.label }}</span>
          <strong>{{ option.count }}</strong>
        </button>
      </div>

      <div class="queue-list" aria-label="任务题目列表">
        <button
          v-for="(item, index) in filteredQueueItems"
          :key="item.id"
          class="queue-item"
          :class="{ active: selectedItem?.id === item.id, locked: item.status !== 'PENDING', running: item.status === 'RUNNING', failed: item.status === 'FAILED', succeeded: item.status === 'SUCCEEDED' }"
          type="button"
          :draggable="item.status === 'PENDING'"
          @click="selectItem(item)"
          @dragstart="onDragStart(item)"
          @dragover.prevent
          @drop="onDrop(item)"
        >
          <strong><span>{{ index + 1 }}</span>{{ item.title }}</strong>
          <small>{{ statusText(item.status) }}</small>
          <span v-if="item.status === 'FAILED' && item.errorMessage" class="error-summary">
            {{ isErrorExpanded(item.id) ? item.errorMessage : errorSummary(item.errorMessage) }}
          </span>
        </button>
        <p v-if="selectedJob && filteredQueueItems.length === 0" class="status">{{ emptyFilterText }}</p>
        <p v-if="!selectedJob" class="status">暂无任务，选择多个题面文件后可加入队列。</p>
      </div>

    </aside>

    <section class="panel result-list preview-panel">
      <template v-if="selectedItem">
        <header class="preview-header">
          <div>
            <span class="status">{{ statusText(selectedItem.status) }}</span>
            <h2>{{ selectedItem.title }}</h2>
          </div>
          <div v-if="selectedItem.status === 'PENDING'" class="actions">
            <button class="ghost" type="button" @click="startEdit(selectedItem)"><Edit3 :size="16" />编辑</button>
            <button class="ghost" type="button" @click="deleteItem(selectedItem)"><Trash2 :size="16" />删除</button>
          </div>
        </header>

        <div v-if="selectedItem.status === 'FAILED' && selectedItem.errorMessage" class="failure-detail">
          <strong>失败原因</strong>
          <p>{{ isErrorExpanded(selectedItem.id) ? selectedItem.errorMessage : errorSummary(selectedItem.errorMessage) }}</p>
          <button
            v-if="selectedItem.errorMessage.length > errorSummary(selectedItem.errorMessage).length"
            class="ghost compact"
            type="button"
            @click="toggleError(selectedItem.id)"
          >
            {{ isErrorExpanded(selectedItem.id) ? '收起失败原因' : '查看完整失败原因' }}
          </button>
        </div>

        <div v-if="editing" class="grid">
          <input v-model="editTitle" class="input" />
          <textarea v-model="editContent" class="textarea editor" />
          <div class="actions">
            <button class="primary" type="button" @click="saveItemEdit"><Save :size="16" />保存</button>
            <button class="ghost" type="button" @click="editing = false">取消</button>
          </div>
        </div>
        <article v-else class="markdown-preview" v-html="previewHtml"></article>
      </template>

      <p v-else class="status">分析结果或任务题面会显示在这里。</p>
    </section>
  </section>
</template>

<style scoped>
.analysis-workbench {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(220px, 260px) minmax(0, 1.1fr);
  gap: 18px;
  align-items: start;
  min-width: 0;
}

.task-rail,
.preview-panel {
  max-height: calc(100vh - 120px);
  overflow: auto;
  min-width: 0;
}

.analysis-card,
.task-rail,
.preview-panel {
  border-color: #d8e1d9;
  min-width: 0;
}

.rail-header,
.preview-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.rail-header h2,
.preview-header h2 {
  margin: 0;
  line-height: 1.25;
}

.job-strip,
.queue-list {
  display: grid;
  gap: 8px;
}

.status-filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(52px, 1fr));
  gap: 6px;
  margin: 14px 0 12px;
}

.filter-chip {
  min-width: 0;
  border: 1px solid #d6e0d9;
  border-radius: 8px;
  background: #f8faf7;
  color: #31453b;
  padding: 7px 6px;
  display: grid;
  gap: 2px;
  justify-items: center;
  line-height: 1.15;
}

.filter-chip span,
.filter-chip strong {
  min-width: 0;
  max-width: 100%;
}

.filter-chip span {
  font-size: 12px;
  white-space: normal;
  overflow-wrap: anywhere;
}

.filter-chip strong {
  font-size: 15px;
}

.filter-chip.active {
  border-color: #17684f;
  background: #eaf5ef;
  color: #0f533f;
}

.queue-item {
  border: 1px solid #dce5dd;
  border-radius: 8px;
  background: #fbfcfa;
  text-align: left;
  padding: 10px;
  display: grid;
  gap: 4px;
  transition: border-color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.queue-item:hover {
  border-color: #b9cabc;
  box-shadow: 0 8px 18px rgba(23, 33, 29, 0.06);
}

.queue-item.active {
  border-color: #17684f;
  background: #eaf5ef;
}

.queue-item.running {
  border-color: #d8a94d;
  background: #fff9e8;
}

.queue-item.failed {
  border-color: #efc0ba;
  background: #fff7f5;
}

.queue-item.succeeded {
  border-color: #c9dfcf;
  background: #f4fbf5;
}

.queue-item.locked {
  cursor: default;
}

.queue-list {
  max-height: min(56vh, 720px);
  overflow: auto;
}

.queue-item {
  padding: 7px 9px;
  min-height: 46px;
}

.queue-item strong {
  display: flex;
  gap: 7px;
  align-items: baseline;
  min-width: 0;
  overflow-wrap: anywhere;
}

.queue-item strong span {
  color: #1f6f54;
  font-size: 12px;
  font-weight: 700;
  min-width: 20px;
}

.queue-item small {
  color: #617069;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.error-summary {
  color: #9b2f29;
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.failure-detail {
  border: 1px solid #f0c3bd;
  border-radius: 8px;
  background: #fff7f5;
  padding: 12px;
  display: grid;
  gap: 8px;
}

.failure-detail strong {
  color: #8f2822;
}

.failure-detail p {
  margin: 0;
  color: #5f302d;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.compact {
  min-height: 32px;
  width: max-content;
  padding: 7px 10px;
}

.form-title {
  margin: 0;
  font-size: 20px;
  letter-spacing: 0;
}

.file-button {
  position: relative;
  overflow: hidden;
}

.file-button input {
  position: absolute;
  inset: 0;
  opacity: 0;
}

.batch-picker {
  max-width: 100%;
}

.editor {
  min-height: 360px;
}

.markdown-preview {
  color: #24312c;
  line-height: 1.75;
}

.markdown-preview :deep(h1),
.markdown-preview :deep(h2),
.markdown-preview :deep(h3) {
  margin: 18px 0 10px;
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
  .analysis-workbench {
    grid-template-columns: 1fr;
  }

  .task-rail,
  .preview-panel {
    max-height: none;
  }
}
</style>
