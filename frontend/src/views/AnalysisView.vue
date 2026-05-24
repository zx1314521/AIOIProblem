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
let timer: number | undefined

const selectedJob = computed(() => selected.value?.job)
const queueItems = computed(() => selected.value?.items ?? [])
const activeItems = computed(() => queueItems.value.filter(item => item.status === 'PENDING' || item.status === 'RUNNING'))
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
    selectedItem.value = selected.value.items.find(item => item.status === 'RUNNING' || item.status === 'PENDING') ?? selected.value.items[0] ?? null
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
    if (selected.value && keepSelection && isActiveJob(selected.value.job)) {
      const stillExists = jobs.value.some(job => job.id === selected.value?.job.id)
      if (stillExists) {
        selected.value = await api.getBatchJob(selected.value.job.id)
        if (selectedItem.value) {
          const updated = selected.value.items.find(item => item.id === selectedItem.value?.id)
          selectedItem.value = updated && (updated.status === 'PENDING' || updated.status === 'RUNNING')
            ? updated
            : selected.value.items.find(item => item.status === 'RUNNING' || item.status === 'PENDING') ?? null
        }
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
  selectedItem.value = selected.value.items.find(item => item.status === 'RUNNING' || item.status === 'PENDING') ?? selected.value.items[0] ?? null
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
    selectedItem.value = selected.value.items.find(item => item.status === 'RUNNING' || item.status === 'PENDING') ?? selected.value.items[0] ?? null
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
  selectedItem.value = queueItems.value[0] ?? null
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

      <div class="queue-list">
        <button
          v-for="(item, index) in activeItems"
          :key="item.id"
          class="queue-item"
          :class="{ active: selectedItem?.id === item.id, locked: item.status !== 'PENDING', running: item.status === 'RUNNING' }"
          type="button"
          :draggable="item.status === 'PENDING'"
          @click="selectItem(item)"
          @dragstart="onDragStart(item)"
          @dragover.prevent
          @drop="onDrop(item)"
        >
          <strong><span>{{ index + 1 }}</span>{{ item.title }}</strong>
          <small>{{ statusText(item.status) }}<template v-if="item.errorMessage"> · {{ item.errorMessage }}</template></small>
        </button>
        <p v-if="selectedJob && activeItems.length === 0" class="status">当前没有等待或运行中的题。</p>
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
  grid-template-columns: minmax(360px, 0.95fr) 300px minmax(360px, 1.05fr);
  gap: 18px;
  align-items: start;
}

.task-rail,
.preview-panel {
  max-height: calc(100vh - 120px);
  overflow: auto;
}

.analysis-card,
.task-rail,
.preview-panel {
  border-color: #d8e1d9;
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
  width: max-content;
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
