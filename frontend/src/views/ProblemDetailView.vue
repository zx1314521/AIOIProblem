<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { Database, Play, RefreshCw, TerminalSquare } from 'lucide-vue-next'
import CodeEditor from '../components/CodeEditor.vue'
import { api } from '../services/api'
import type { CodeRunResponse, Problem, ProblemDataStatus } from '../types'
import { createProblemMarkdown, renderProblemMarkdown } from '../utils/problemMath'

const route = useRoute()
const markdown = createProblemMarkdown()
const problemId = computed(() => Number(route.params.id))
const problem = ref<Problem | null>(null)
const dataStatus = ref<ProblemDataStatus | null>(null)
const code = ref(`#include <bits/stdc++.h>
using namespace std;

int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    return 0;
}
`)
const mode = ref<'debug' | 'run'>('debug')
const debugInput = ref('')
const running = ref(false)
const loading = ref(false)
const error = ref('')
const result = ref<CodeRunResponse | null>(null)

const statementHtml = computed(() => renderProblemMarkdown(markdown, problem.value?.description || ''))
const currentDataStatus = computed(() => dataStatus.value?.status ?? problem.value?.dataStatus ?? 'NONE')
const hasNoData = computed(() => currentDataStatus.value === 'NONE')
const statusLabel = computed(() => {
  return {
    NONE: '无数据',
    GENERATING: '生成中',
    READY: '已有数据',
    FAILED: '生成失败'
  }[currentDataStatus.value]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    problem.value = await api.getProblem(problemId.value)
    dataStatus.value = null
  } catch (err) {
    error.value = err instanceof Error ? err.message : '加载题目失败'
  } finally {
    loading.value = false
  }
}

async function generateData() {
  error.value = ''
  try {
    dataStatus.value = await api.generateProblemData(problemId.value)
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'AI 数据生成启动失败'
  }
}

async function runCode() {
  running.value = true
  error.value = ''
  result.value = null
  try {
    result.value = mode.value === 'debug'
      ? await api.runProblemDebug(problemId.value, { code: code.value, input: debugInput.value })
      : await api.runProblemCases(problemId.value, { code: code.value })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '运行失败'
  } finally {
    running.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="problem-workspace">
    <header class="problem-topbar">
      <div>
        <RouterLink class="back-link" to="/problems">题目管理</RouterLink>
        <h1>{{ problem?.title || '题目' }}</h1>
        <div v-if="problem" class="meta-row">
          <span class="difficulty">{{ problem.difficulty }}</span>
          <span v-for="tag in problem.tags" :key="tag" class="tag">{{ tag }}</span>
          <span class="data-state" :class="currentDataStatus.toLowerCase()">{{ statusLabel }}</span>
        </div>
      </div>
      <div class="top-actions">
        <button class="secondary" type="button" :disabled="dataStatus?.status === 'GENERATING'" @click="generateData">
          <RefreshCw :size="17" />AI数据
        </button>
        <RouterLink class="ghost link-button" :to="`/problems/${problemId}/data`">
          <Database :size="17" />数据管理
        </RouterLink>
      </div>
    </header>

    <p v-if="error" class="error">{{ error }}</p>
    <p v-if="loading" class="status">加载中...</p>

    <div v-if="problem" class="solve-grid">
      <article class="statement-pane">
        <div v-if="hasNoData" class="no-data-notice">
          <strong>本题没数据</strong>
          <span>可以点击右上角 AI数据 生成测试点，或进入数据管理手动添加。</span>
        </div>
        <div class="markdown-preview statement-body" v-html="statementHtml" />
      </article>

      <section class="code-pane">
        <CodeEditor v-model="code" />
        <div class="run-console">
          <div class="run-head">
            <div class="segmented">
              <button type="button" :class="{ active: mode === 'debug' }" @click="mode = 'debug'">
                <TerminalSquare :size="16" />调试
              </button>
              <button type="button" :class="{ active: mode === 'run' }" @click="mode = 'run'">
                <Play :size="16" />运行
              </button>
            </div>
            <button class="primary compact-run" type="button" :disabled="running || (mode === 'run' && currentDataStatus !== 'READY')" @click="runCode">
              <Play :size="17" />{{ running ? '运行中' : '开始' }}
            </button>
          </div>

          <textarea v-if="mode === 'debug'" v-model="debugInput" class="console-input" placeholder="stdin 输入" />
          <p v-else-if="hasNoData" class="run-note warning-note">本题没数据，暂时不能运行全部测试点。</p>
          <p v-else class="run-note">运行模式会使用当前题目的全部测试数据。</p>

          <div v-if="result" class="result-panel" :class="result.status.toLowerCase()">
            <strong>{{ result.status }}</strong>
            <span>{{ result.durationMs }} ms</span>
            <pre v-if="result.stdout">{{ result.stdout }}</pre>
            <pre v-if="result.stderr" class="stderr">{{ result.stderr }}</pre>
            <div v-if="result.cases.length" class="case-results">
              <div v-for="item in result.cases" :key="item.index" class="case-result" :class="item.status.toLowerCase()">
                <span>#{{ item.index }}</span>
                <strong>{{ item.status }}</strong>
                <small>{{ item.durationMs }} ms</small>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.problem-workspace {
  height: calc(100vh - 60px);
  min-height: 620px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}

.problem-topbar {
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

.problem-topbar h1 {
  margin: 4px 0 8px;
  font-size: 24px;
}

.meta-row,
.top-actions,
.run-head,
.segmented,
.case-result {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.link-button {
  text-decoration: none;
}

.data-state {
  border: 1px solid #cfd7df;
  border-radius: 4px;
  padding: 4px 8px;
  color: #334155;
  background: #f6f7f9;
  font-size: 12px;
  font-weight: 800;
}

.data-state.ready {
  border-color: #b8d8c4;
  color: #257044;
  background: #edf8f1;
}

.data-state.generating {
  border-color: #e2cc91;
  color: #8a5c12;
  background: #fff8e4;
}

.data-state.failed {
  border-color: #efbbb5;
  color: #a43e31;
  background: #fff0ed;
}

.solve-grid {
  display: grid;
  grid-template-columns: minmax(360px, 0.92fr) minmax(420px, 1.08fr);
  gap: 14px;
  flex: 1 1 auto;
  min-height: 0;
}

.statement-pane,
.code-pane {
  min-width: 0;
}

.statement-pane {
  overflow: auto;
  overscroll-behavior: contain;
  min-height: 0;
  padding: 18px;
  border: 1px solid #d9dee5;
  border-radius: 6px;
  background: #ffffff;
}

.statement-body {
  max-width: 820px;
}

.no-data-notice {
  display: grid;
  gap: 4px;
  margin-bottom: 14px;
  border: 1px solid #e3c878;
  border-radius: 6px;
  background: #fff8df;
  color: #6f4e08;
  padding: 10px 12px;
}

.no-data-notice strong {
  font-size: 14px;
}

.no-data-notice span {
  font-size: 13px;
}

.code-pane {
  display: grid;
  grid-template-rows: minmax(260px, 1fr) minmax(174px, auto);
  gap: 10px;
  min-height: 0;
  overflow: hidden;
}

.run-console {
  border: 1px solid #d9dee5;
  border-radius: 6px;
  background: #ffffff;
  padding: 10px;
  display: grid;
  gap: 10px;
  min-height: 0;
  max-height: 260px;
  overflow: auto;
}

.run-head {
  justify-content: space-between;
}

.segmented {
  border: 1px solid #d8e0e8;
  border-radius: 6px;
  padding: 3px;
  background: #f6f7f9;
}

.segmented button {
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #4b5563;
  padding: 7px 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.segmented button.active {
  background: #ffffff;
  color: #17684f;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.1);
}

.compact-run {
  min-height: 34px;
  border-radius: 5px;
  padding: 8px 12px;
}

.console-input {
  width: 100%;
  min-height: 74px;
  max-height: 118px;
  border: 1px solid #d8e0e8;
  border-radius: 5px;
  padding: 9px;
  resize: vertical;
  font-family: Consolas, "Cascadia Code", monospace;
}

.run-note {
  margin: 0;
  color: #66746d;
  font-size: 13px;
}

.warning-note {
  color: #8a5c12;
}

.result-panel {
  display: grid;
  gap: 8px;
  border-left: 4px solid #64748b;
  background: #f8fafc;
  padding: 10px;
  overflow: auto;
}

.result-panel.ac,
.result-panel.ok {
  border-color: #2f9e44;
}

.result-panel.wa,
.result-panel.ce,
.result-panel.re,
.result-panel.tle {
  border-color: #c43c33;
}

.result-panel pre {
  margin: 0;
  max-height: 110px;
  overflow: auto;
  padding: 8px;
  border-radius: 4px;
  background: #111827;
  color: #e5edf5;
}

.result-panel .stderr {
  color: #ffd1cc;
}

.case-results {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  gap: 6px;
}

.case-result {
  justify-content: space-between;
  border: 1px solid #d8e0e8;
  border-radius: 4px;
  padding: 6px 8px;
  background: #fff;
}

.case-result.ac strong {
  color: #2f9e44;
}

.case-result.wa strong,
.case-result.re strong,
.case-result.tle strong {
  color: #c43c33;
}

@media (max-width: 1100px) {
  .problem-workspace {
    height: auto;
    min-height: calc(100vh - 60px);
    overflow: visible;
  }

  .solve-grid {
    grid-template-columns: 1fr;
    flex: initial;
  }

  .statement-pane {
    overflow: visible;
  }

  .code-pane {
    grid-template-rows: minmax(320px, 54vh) auto;
    overflow: visible;
  }

  .run-console {
    max-height: none;
  }
}
</style>
