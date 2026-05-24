<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import MarkdownIt from 'markdown-it'
import markdownItKatex from 'markdown-it-katex'
import 'katex/dist/katex.min.css'
import { Edit3, FileText, Pause, Play, Save, Trash2, UploadCloud } from 'lucide-vue-next'
import { api } from '../services/api'
import type { AnalysisResponse, BatchItem, BatchJob, BatchJobDetail } from '../types'

const markdown = new MarkdownIt({ breaks: true, linkify: true }).use(markdownItKatex)

const title = ref('')
const text = ref('')
const fileName = ref('')
const result = ref<AnalysisResponse | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const saveMessage = ref('')
const openHints = ref(new Set<number>())

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
const visibleQueueItems = computed(() => {
  return selected.value?.items.filter(item => item.status === 'RUNNING' || item.status === 'PENDING') ?? []
})
const progress = computed(() => {
  const job = selected.value?.job
  if (!job || job.totalCount === 0) return 0
  return Math.round(((job.successCount + job.failedCount) / job.totalCount) * 100)
})
const previewHtml = computed(() => markdown.render(selectedItem.value?.content || ''))

async function analyzeText() {
  loading.value = true
  error.value = ''
  saveMessage.value = ''
  selectedItem.value = null
  try {
    result.value = await api.analyzeText(title.value || '未命名题目', text.value)
    openHints.value = new Set()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '分析失败'
  } finally {
    loading.value = false
  }
}

async function saveAsProblem() {
  if (!result.value) return
  saving.value = true
  saveMessage.value = ''
  try {
    await api.createProblem({
      title: title.value || '未命名题目',
      description: text.value || '由文件上传分析生成',
      difficulty: result.value.difficulty,
      tags: result.value.tags,
      source: 'AI分析'
    })
    saveMessage.value = '已保存到题库'
  } catch (err) {
    saveMessage.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    saving.value = false
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

function toggleHint(index: number) {
  const next = new Set(openHints.value)
  next.has(index) ? next.delete(index) : next.add(index)
  openHints.value = next
}

async function loadJobs(keepSelection = true) {
  try {
    jobs.value = await api.listBatchJobs()
    if (selected.value && keepSelection) {
      selected.value = await api.getBatchJob(selected.value.job.id)
      if (selectedItem.value) {
        selectedItem.value = selected.value.items.find(item => item.id === selectedItem.value?.id) ?? selectedItem.value
      }
    } else if (jobs.value.length > 0) {
      await selectJob(jobs.value[0].id)
    }
  } catch (err) {
    batchError.value = err instanceof Error ? err.message : '任务加载失败'
  }
}

async function selectJob(id: number) {
  selected.value = await api.getBatchJob(id)
  selectedItem.value = visibleQueueItems.value[0] ?? selected.value.items[0] ?? null
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
    selectedItem.value = visibleQueueItems.value[0] ?? selected.value.items[0] ?? null
    batchFiles.value = []
    await loadJobs()
  } catch (err) {
    batchError.value = err instanceof Error ? err.message : '上传失败'
  } finally {
    uploading.value = false
  }
}

async function pauseJob() {
  if (!selectedJob.value) return
  await api.pauseBatchJob(selectedJob.value.id)
  await loadJobs()
}

async function resumeJob() {
  if (!selectedJob.value) return
  await api.resumeBatchJob(selectedJob.value.id)
  await loadJobs()
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
  selectedItem.value = visibleQueueItems.value[0] ?? selected.value.items[0] ?? null
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
  const pending = visibleQueueItems.value.filter(item => item.status === 'PENDING')
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

function isTextProblemFile(file: File) {
  const name = file.name.toLowerCase()
  return name.endsWith('.txt') || name.endsWith('.md')
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
      <form class="panel grid" @submit.prevent="analyzeText">
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

        <label v-if="batchFiles.length > 1" class="field compact-field">
          <span>任务名</span>
          <input v-model="batchName" class="input" />
        </label>
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
        <span v-if="selectedJob" class="tag">{{ statusText(selectedJob.status) }}</span>
      </div>

      <div class="job-strip">
        <button
          v-for="job in jobs"
          :key="job.id"
          class="job-button"
          :class="{ active: selectedJob?.id === job.id }"
          type="button"
          @click="selectJob(job.id)"
        >
          <strong>{{ job.name }}</strong>
          <span>{{ job.successCount + job.failedCount }}/{{ job.totalCount }} · 等待 {{ job.pendingCount }} · 运行 {{ job.runningCount }}</span>
        </button>
        <p v-if="jobs.length === 0" class="status">暂无任务，选择多个题面文件后可加入队列。</p>
      </div>

      <div v-if="selectedJob" class="progress">
        <span :style="{ width: `${progress}%` }"></span>
      </div>

      <div v-if="selectedJob" class="actions">
        <button v-if="selectedJob.status === 'RUNNING'" class="ghost" type="button" @click="pauseJob">
          <Pause :size="16" />暂停
        </button>
        <button v-if="selectedJob.status === 'PAUSED'" class="secondary" type="button" @click="resumeJob">
          <Play :size="16" />继续
        </button>
      </div>

      <div class="queue-list">
        <button
          v-for="item in visibleQueueItems"
          :key="item.id"
          class="queue-item"
          :class="{ active: selectedItem?.id === item.id, locked: item.status !== 'PENDING' }"
          type="button"
          :draggable="item.status === 'PENDING'"
          @click="selectItem(item)"
          @dragstart="onDragStart(item)"
          @dragover.prevent
          @drop="onDrop(item)"
        >
          <strong>{{ item.title }}</strong>
          <span>{{ statusText(item.status) }}</span>
        </button>
        <p v-if="selectedJob && visibleQueueItems.length === 0" class="status">当前任务没有运行或等待中的题。</p>
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

      <template v-else-if="result">
        <div>
          <span class="difficulty">{{ result.difficulty }}</span>
          <span class="status"> 置信度 {{ Math.round(result.confidence * 100) }}%</span>
        </div>
        <div class="tag-row">
          <span v-for="tag in result.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <p>{{ result.reasoningSummary }}</p>
        <article v-for="(hint, index) in result.hints" :key="hint" class="hint">
          <button type="button" @click="toggleHint(index)">提示{{ index + 1 }}</button>
          <p v-if="openHints.has(index)">{{ hint }}</p>
        </article>
        <div>
          <h3>相似题</h3>
          <div v-for="problem in result.similarProblems" :key="problem.id" class="problem-row">
            <header>
              <h3>{{ problem.title }}</h3>
              <span class="difficulty">{{ problem.difficulty }}</span>
            </header>
            <div class="tag-row">
              <span v-for="tag in problem.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <p>{{ problem.reason }}</p>
          </div>
        </div>
        <div class="actions">
          <button class="secondary" type="button" :disabled="saving" @click="saveAsProblem">
            <Save :size="18" />保存到题库
          </button>
          <span class="status">{{ saveMessage }}</span>
        </div>
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
}

.job-strip,
.queue-list {
  display: grid;
  gap: 8px;
}

.job-button,
.queue-item {
  border: 1px solid #dfe4dc;
  border-radius: 6px;
  background: #fbfcfa;
  text-align: left;
  padding: 10px;
  display: grid;
  gap: 4px;
}

.job-button.active,
.queue-item.active {
  border-color: #1f6f54;
  background: #eef8f2;
}

.queue-item.locked {
  cursor: default;
}

.job-button span,
.queue-item span {
  color: #617069;
  font-size: 13px;
}

.progress {
  height: 10px;
  background: #e8ede8;
  border-radius: 999px;
  overflow: hidden;
}

.progress span {
  display: block;
  height: 100%;
  background: #1f6f54;
}

.form-title {
  margin: 0;
  font-size: 22px;
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

.compact-field {
  max-width: 360px;
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
