<script setup lang="ts">
import { ref } from 'vue'
import { FileText, Save, UploadCloud } from 'lucide-vue-next'
import { api } from '../services/api'
import type { AnalysisResponse } from '../types'

const title = ref('')
const text = ref('')
const file = ref<File | null>(null)
const result = ref<AnalysisResponse | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const saveMessage = ref('')
const openHints = ref(new Set<number>())

async function analyzeText() {
  loading.value = true
  error.value = ''
  saveMessage.value = ''
  try {
    result.value = await api.analyzeText(title.value || '未命名题目', text.value)
    openHints.value = new Set()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '分析失败'
  } finally {
    loading.value = false
  }
}

async function analyzeFile() {
  if (!file.value) {
    error.value = '请选择 .txt 或 .md 文件'
    return
  }
  loading.value = true
  error.value = ''
  saveMessage.value = ''
  try {
    result.value = await api.analyzeFile(file.value)
    title.value ||= file.value.name
    openHints.value = new Set()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '文件分析失败'
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

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  file.value = input.files?.[0] ?? null
}

function toggleHint(index: number) {
  const next = new Set(openHints.value)
  next.has(index) ? next.delete(index) : next.add(index)
  openHints.value = next
}
</script>

<template>
  <header class="page-header">
    <div>
      <h1>题面分析</h1>
      <p>粘贴题目或上传文本文件，返回难度、标签、置信度和分层提示。</p>
    </div>
  </header>

  <section class="grid two">
    <form class="panel grid" @submit.prevent="analyzeText">
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
          <FileText :size="18" />分析文本
        </button>
        <label class="secondary file-button">
          <UploadCloud :size="18" />选择文件
          <input type="file" accept=".txt,.md" @change="onFileChange" />
        </label>
        <button class="ghost" type="button" :disabled="loading || !file" @click="analyzeFile">分析文件</button>
        <span v-if="file" class="status">{{ file.name }}</span>
      </div>
      <p v-if="error" class="error">{{ error }}</p>
    </form>

    <section class="panel result-list">
      <template v-if="result">
        <div>
          <span class="difficulty">{{ result.difficulty }}</span>
          <span class="status"> 置信度 {{ Math.round(result.confidence * 100) }}%</span>
        </div>
        <div class="tag-row">
          <span v-for="tag in result.tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
        <p>{{ result.reasoningSummary }}</p>
        <div>
          <article v-for="(hint, index) in result.hints" :key="hint" class="hint">
            <button type="button" @click="toggleHint(index)">提示{{ index + 1 }}</button>
            <p v-if="openHints.has(index)">{{ hint }}</p>
          </article>
        </div>
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
      <p v-else class="status">分析结果会显示在这里。</p>
    </section>
  </section>
</template>

<style scoped>
.file-button {
  position: relative;
  overflow: hidden;
}

.file-button input {
  position: absolute;
  inset: 0;
  opacity: 0;
}
</style>

