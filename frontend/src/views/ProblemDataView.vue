<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Download, Plus, RefreshCw, Save, Trash2 } from 'lucide-vue-next'
import { api } from '../services/api'
import type { Problem, ProblemDataCase, ProblemDataSet } from '../types'

const route = useRoute()
const problemId = computed(() => Number(route.params.id))
const problem = ref<Problem | null>(null)
const dataSet = ref<ProblemDataSet | null>(null)
const selectedCaseId = ref<number | null>(null)
const form = ref({ index: 1, input: '', output: '' })
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const message = ref('')

const sortedCases = computed(() => dataSet.value?.cases.slice().sort((a, b) => a.index - b.index) ?? [])
const selectedCase = computed(() => sortedCases.value.find(item => item.id === selectedCaseId.value) ?? null)
const statusLabel = computed(() => {
  const status = dataSet.value?.status ?? 'NONE'
  return {
    NONE: '无数据',
    GENERATING: '生成中',
    READY: '已有数据',
    FAILED: '生成失败'
  }[status]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [problemResponse, dataResponse] = await Promise.all([
      api.getProblem(problemId.value),
      api.getProblemData(problemId.value)
    ])
    problem.value = problemResponse
    dataSet.value = dataResponse
    if (sortedCases.value.length && !selectedCaseId.value) {
      selectCase(sortedCases.value[0])
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载数据失败'
  } finally {
    loading.value = false
  }
}

function selectCase(item: ProblemDataCase) {
  selectedCaseId.value = item.id
  form.value = { index: item.index, input: item.input, output: item.output }
}

function newCase() {
  const used = new Set(sortedCases.value.map(item => item.index))
  let index = 1
  while (used.has(index)) index += 1
  selectedCaseId.value = null
  form.value = { index, input: '', output: '' }
}

async function saveCase() {
  saving.value = true
  error.value = ''
  message.value = ''
  try {
    dataSet.value = selectedCaseId.value
      ? await api.updateProblemDataCase(problemId.value, selectedCaseId.value, form.value)
      : await api.addProblemDataCase(problemId.value, form.value)
    const updated = sortedCases.value.find(item => item.index === form.value.index)
    selectedCaseId.value = updated?.id ?? selectedCaseId.value
    message.value = '测试点已保存'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function deleteCase() {
  if (!selectedCaseId.value) return
  saving.value = true
  error.value = ''
  message.value = ''
  try {
    dataSet.value = await api.deleteProblemDataCase(problemId.value, selectedCaseId.value)
    selectedCaseId.value = null
    newCase()
    message.value = '测试点已删除'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '删除失败'
  } finally {
    saving.value = false
  }
}

async function generateData() {
  error.value = ''
  message.value = ''
  try {
    const status = await api.generateProblemData(problemId.value)
    dataSet.value = {
      id: status.id,
      problemId: status.problemId,
      status: status.status,
      stdCpp: dataSet.value?.stdCpp ?? '',
      configYaml: dataSet.value?.configYaml ?? '',
      errorMessage: status.errorMessage,
      notes: status.notes,
      updatedAt: status.updatedAt,
      cases: dataSet.value?.cases ?? []
    }
    message.value = 'AI 数据任务已启动'
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'AI 数据生成启动失败'
  }
}

async function downloadData() {
  try {
    const blob = await api.downloadProblemData(problemId.value)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `problem-${problemId.value}-testdata.zip`
    link.click()
    URL.revokeObjectURL(url)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '下载失败'
  }
}

onMounted(load)
</script>

<template>
  <section class="data-page">
    <header class="data-header">
      <div>
        <RouterLink class="back-link" :to="`/problems/${problemId}`">返回题目</RouterLink>
        <h1>{{ problem?.title || '题目数据' }}</h1>
        <p>状态：<strong>{{ statusLabel }}</strong>，共 {{ sortedCases.length }} 组测试点</p>
      </div>
      <div class="actions">
        <button class="secondary" type="button" :disabled="dataSet?.status === 'GENERATING'" @click="generateData">
          <RefreshCw :size="17" />AI数据
        </button>
        <button class="ghost" type="button" :disabled="!sortedCases.length" @click="downloadData">
          <Download :size="17" />下载
        </button>
      </div>
    </header>

    <p v-if="loading" class="status">加载中...</p>
    <p v-if="message" class="status">{{ message }}</p>
    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="dataSet?.errorMessage" class="error">{{ dataSet.errorMessage }}</p>

    <div class="data-grid">
      <aside class="case-list">
        <button class="primary new-case" type="button" @click="newCase">
          <Plus :size="17" />新增测试点
        </button>
        <button
          v-for="item in sortedCases"
          :key="item.id"
          class="case-item"
          :class="{ active: item.id === selectedCaseId }"
          type="button"
          @click="selectCase(item)"
        >
          <strong>#{{ item.index }}</strong>
          <span>{{ item.input.split('\n')[0] || '(空输入)' }}</span>
        </button>
      </aside>

      <section class="case-editor">
        <div class="case-fields">
          <label class="field case-index">
            <span>编号</span>
            <input v-model.number="form.index" class="input" type="number" min="1" max="999" />
          </label>
          <div class="case-actions">
            <button class="primary" type="button" :disabled="saving" @click="saveCase">
              <Save :size="17" />保存
            </button>
            <button class="ghost danger" type="button" :disabled="saving || !selectedCase" @click="deleteCase">
              <Trash2 :size="17" />删除
            </button>
          </div>
        </div>
        <div class="io-grid">
          <label class="field">
            <span>输入 .in</span>
            <textarea v-model="form.input" class="textarea io-area" />
          </label>
          <label class="field">
            <span>输出 .out</span>
            <textarea v-model="form.output" class="textarea io-area" />
          </label>
        </div>

        <section v-if="dataSet?.stdCpp || dataSet?.configYaml" class="generated-files">
          <label class="field">
            <span>std.cpp</span>
            <textarea class="textarea file-area" readonly :value="dataSet?.stdCpp" />
          </label>
          <label class="field">
            <span>config.yaml</span>
            <textarea class="textarea file-area" readonly :value="dataSet?.configYaml" />
          </label>
        </section>
      </section>
    </div>
  </section>
</template>

<style scoped>
.data-page {
  display: grid;
  gap: 14px;
}

.data-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding-bottom: 12px;
  border-bottom: 1px solid #d9dee5;
}

.back-link {
  color: #2475b9;
  font-size: 13px;
  text-decoration: none;
}

.data-header h1 {
  margin: 4px 0 6px;
  font-size: 24px;
}

.data-header p {
  margin: 0;
  color: #66746d;
}

.data-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 14px;
  min-height: calc(100vh - 170px);
}

.case-list {
  display: grid;
  align-content: start;
  gap: 7px;
  max-height: calc(100vh - 180px);
  overflow: auto;
  padding: 10px;
  border: 1px solid #d9dee5;
  border-radius: 6px;
  background: #ffffff;
}

.new-case {
  width: 100%;
  min-height: 34px;
  border-radius: 5px;
}

.case-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  min-height: 34px;
  border: 1px solid #d8e0e8;
  border-radius: 5px;
  background: #f8fafc;
  text-align: left;
  padding: 7px 9px;
}

.case-item.active {
  border-color: #17684f;
  background: #edf8f1;
}

.case-item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #66746d;
}

.case-editor {
  display: grid;
  gap: 12px;
  min-width: 0;
}

.case-fields {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: end;
}

.case-index {
  max-width: 140px;
}

.case-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.io-grid,
.generated-files {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.io-area {
  min-height: 360px;
  font-family: Consolas, "Cascadia Code", monospace;
}

.file-area {
  min-height: 180px;
  font-family: Consolas, "Cascadia Code", monospace;
  background: #f8fafc;
}

@media (max-width: 960px) {
  .data-grid,
  .io-grid,
  .generated-files {
    grid-template-columns: 1fr;
  }

  .case-list {
    max-height: 260px;
  }
}
</style>
