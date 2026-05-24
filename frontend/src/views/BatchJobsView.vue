<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Pause, Play, RefreshCw, UploadCloud } from 'lucide-vue-next'
import { api } from '../services/api'
import type { BatchJob, BatchJobDetail } from '../types'

const jobs = ref<BatchJob[]>([])
const selected = ref<BatchJobDetail | null>(null)
const files = ref<File[]>([])
const name = ref('批量题面分析')
const loading = ref(false)
const uploading = ref(false)
const error = ref('')
let timer: number | undefined

const selectedJob = computed(() => selected.value?.job)
const progress = computed(() => {
  const job = selected.value?.job
  if (!job || job.totalCount === 0) return 0
  return Math.round(((job.successCount + job.failedCount) / job.totalCount) * 100)
})

async function loadJobs() {
  loading.value = true
  error.value = ''
  try {
    jobs.value = await api.listBatchJobs()
    if (!selected.value && jobs.value.length > 0) {
      await selectJob(jobs.value[0].id)
    } else if (selected.value) {
      await selectJob(selected.value.job.id, false)
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '任务加载失败'
  } finally {
    loading.value = false
  }
}

async function selectJob(id: number, clearError = true) {
  if (clearError) error.value = ''
  selected.value = await api.getBatchJob(id)
}

function onFilesChange(event: Event) {
  const input = event.target as HTMLInputElement
  files.value = Array.from(input.files ?? []).filter(file => {
    const name = file.name.toLowerCase()
    return name.endsWith('.txt') || name.endsWith('.md')
  })
}

async function upload() {
  if (files.value.length === 0) {
    error.value = '请选择 .txt 或 .md 文件'
    return
  }
  uploading.value = true
  error.value = ''
  try {
    selected.value = await api.uploadBatch(name.value, files.value)
    files.value = []
    await loadJobs()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '上传失败'
  } finally {
    uploading.value = false
  }
}

async function pause() {
  if (!selectedJob.value) return
  await api.pauseBatchJob(selectedJob.value.id)
  await loadJobs()
}

async function resume() {
  if (!selectedJob.value) return
  await api.resumeBatchJob(selectedJob.value.id)
  await loadJobs()
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

onMounted(() => {
  loadJobs()
  timer = window.setInterval(loadJobs, 3000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <header class="page-header">
    <div>
      <h1>批量任务</h1>
      <p>一次上传多个 `title.txt` 或 `title.md` 文件，系统会按队列逐题调用 AI 分析并保存到题库。</p>
    </div>
    <button class="ghost" type="button" @click="loadJobs"><RefreshCw :size="18" />刷新</button>
  </header>

  <section class="grid sidebar-main">
    <aside class="panel grid">
      <h2>任务列表</h2>
      <label class="field">
        <span>任务名</span>
        <input v-model="name" class="input" />
      </label>
      <label class="secondary file-picker">
        <UploadCloud :size="18" />选择 txt/md 文件
        <input type="file" accept=".txt,.md" multiple @change="onFilesChange" />
      </label>
      <div class="actions">
        <button class="primary" type="button" :disabled="uploading || files.length === 0" @click="upload">
          上传并开始
        </button>
        <span class="status">{{ files.length }} 个文件</span>
      </div>
      <p v-if="error" class="error">{{ error }}</p>

      <div class="job-list">
        <button
          v-for="job in jobs"
          :key="job.id"
          class="job-button"
          :class="{ active: selectedJob?.id === job.id }"
          type="button"
          @click="selectJob(job.id)"
        >
          <strong>{{ job.name }}</strong>
          <span>{{ statusText(job.status) }} · {{ job.successCount + job.failedCount }}/{{ job.totalCount }}</span>
        </button>
        <p v-if="!loading && jobs.length === 0" class="status">还没有批量任务。</p>
      </div>
    </aside>

    <section class="panel grid">
      <template v-if="selected">
        <div class="page-header detail-header">
          <div>
            <h2>{{ selected.job.name }}</h2>
            <p>{{ statusText(selected.job.status) }} · 成功 {{ selected.job.successCount }} · 失败 {{ selected.job.failedCount }} · 等待 {{ selected.job.pendingCount }}</p>
          </div>
          <div class="actions">
            <button v-if="selected.job.status === 'RUNNING'" class="ghost" type="button" @click="pause">
              <Pause :size="18" />暂停
            </button>
            <button v-if="selected.job.status === 'PAUSED'" class="secondary" type="button" @click="resume">
              <Play :size="18" />继续
            </button>
          </div>
        </div>

        <div class="progress">
          <span :style="{ width: `${progress}%` }"></span>
        </div>

        <div class="problem-list batch-items">
          <article v-for="item in selected.items" :key="item.id" class="problem-row compact">
            <header>
              <h3>{{ item.title }}</h3>
              <span class="tag">{{ statusText(item.status) }}</span>
            </header>
            <p v-if="item.errorMessage" class="error">{{ item.errorMessage }}</p>
            <p v-else-if="item.problemId" class="status">已保存为题目 #{{ item.problemId }}</p>
          </article>
        </div>
      </template>
      <p v-else class="status">选择或上传一个任务后查看进度。</p>
    </section>
  </section>
</template>

<style scoped>
h2 {
  margin: 0;
}

.file-picker {
  position: relative;
  overflow: hidden;
}

.file-picker input {
  position: absolute;
  inset: 0;
  opacity: 0;
}

.job-list {
  display: grid;
  gap: 8px;
}

.job-button {
  text-align: left;
  border: 1px solid #dfe4dc;
  background: #fbfcfa;
  padding: 11px;
  border-radius: 6px;
  display: grid;
  gap: 4px;
}

.job-button.active {
  border-color: #1f6f54;
  background: #eff8f3;
}

.job-button span {
  color: #5d6962;
  font-size: 13px;
}

.detail-header {
  margin-bottom: 0;
}

.progress {
  height: 12px;
  background: #e8ede8;
  border-radius: 999px;
  overflow: hidden;
}

.progress span {
  display: block;
  height: 100%;
  background: #1f6f54;
}

.batch-items {
  max-height: 62vh;
  overflow: auto;
}

.compact {
  padding: 10px 12px;
}
</style>
