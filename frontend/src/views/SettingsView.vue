<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Cpu, Save, Sparkles } from 'lucide-vue-next'
import { api } from '../services/api'
import type { AiSettings } from '../types'

const providerOptions = [
  { value: 'codex', label: '本地 Codex CLI' },
  { value: 'deepseek', label: 'DeepSeek API' },
  { value: 'mock', label: '本地规则模型' }
] as const

const settings = ref<AiSettings>({
  provider: 'codex',
  problemAnalysisProvider: 'codex',
  recommendationProvider: 'mock',
  deepSeekApiKey: '',
  deepSeekBaseUrl: 'https://api.deepseek.com/chat/completions',
  deepSeekModel: 'deepseek-chat',
  deepSeekTimeoutSeconds: 45,
  codexCommand: 'codex',
  codexTimeoutSeconds: 180
})
const message = ref('')
const error = ref('')
const loading = ref(false)

async function load() {
  try {
    settings.value = await api.getAiSettings()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '设置加载失败'
  }
}

async function save() {
  loading.value = true
  message.value = ''
  error.value = ''
  try {
    settings.value = await api.updateAiSettings(settings.value)
    message.value = '已保存 AI 设置'
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <header class="page-header">
    <div>
      <h1>AI设置</h1>
      <p>先配置可用的 AI Provider，再按功能选择实际使用的 AI。Codex 适合本地分析，DeepSeek 适合稳定 API，规则模型适合兜底和快速推荐。</p>
    </div>
  </header>

  <section class="settings-layout">
    <section class="panel grid settings-form">
      <div class="section-head">
        <Cpu :size="19" />
        <div>
          <h2>功能分层</h2>
          <p>不同功能可以使用不同 AI，避免一个 Provider 故障拖垮全部流程。</p>
        </div>
      </div>

      <div class="routing-grid">
        <label class="field">
          <span>默认 AI</span>
          <select v-model="settings.provider" class="select">
            <option v-for="option in providerOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <label class="field">
          <span>题面分析 / 批量分析</span>
          <select v-model="settings.problemAnalysisProvider" class="select">
            <option v-for="option in providerOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <label class="field">
          <span>AI 推荐</span>
          <select v-model="settings.recommendationProvider" class="select">
            <option v-for="option in providerOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
      </div>

      <div class="grid two">
        <section class="provider-block">
          <h2>本地 Codex</h2>
          <label class="field">
            <span>命令</span>
            <input v-model="settings.codexCommand" class="input" placeholder="codex 或 codex.cmd" />
          </label>
          <label class="field">
            <span>单题超时秒数</span>
            <input v-model.number="settings.codexTimeoutSeconds" class="input" type="number" min="5" max="1200" />
          </label>
          <p class="note">Windows 上后端会优先解析为 <code>codex.cmd</code>，避免 PowerShell 执行策略拦截 <code>codex.ps1</code>。</p>
        </section>

        <section class="provider-block">
          <h2>DeepSeek</h2>
          <label class="field">
            <span>API Key</span>
            <input v-model="settings.deepSeekApiKey" class="input" type="password" placeholder="sk-..." />
          </label>
          <label class="field">
            <span>接口地址</span>
            <input v-model="settings.deepSeekBaseUrl" class="input" />
          </label>
          <label class="field">
            <span>模型</span>
            <input v-model="settings.deepSeekModel" class="input" />
          </label>
          <label class="field">
            <span>单题超时秒数</span>
            <input v-model.number="settings.deepSeekTimeoutSeconds" class="input" type="number" min="5" max="600" />
          </label>
        </section>
      </div>

      <div class="actions">
        <button class="primary" type="button" :disabled="loading" @click="save">
          <Save :size="18" />保存设置
        </button>
        <span v-if="message" class="status">{{ message }}</span>
        <span v-if="error" class="error">{{ error }}</span>
      </div>
    </section>

    <aside class="panel settings-aside">
      <Sparkles :size="21" />
      <h2>建议配置</h2>
      <p>题面分析优先用 Codex 或 DeepSeek；推荐功能当前仍以规则排序为主，可以先设成本地规则模型，速度更稳。</p>
      <p>如果 Codex 仍超时，先把题面分析切到 DeepSeek 或规则模型，历史日志会标出是否发生兜底。</p>
    </aside>
  </section>
</template>

<style scoped>
.settings-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 18px;
  align-items: start;
}

.settings-form {
  max-width: 1120px;
}

.section-head {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  border-bottom: 1px solid var(--line);
  padding-bottom: 14px;
}

.section-head svg,
.settings-aside svg {
  color: var(--primary);
}

.section-head h2,
.provider-block h2,
.settings-aside h2 {
  margin: 0;
}

.section-head p,
.settings-aside p,
.note {
  margin: 6px 0 0;
  color: var(--muted);
  line-height: 1.55;
}

.routing-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.provider-block {
  border: 1px solid #dfe4dc;
  border-radius: 8px;
  padding: 18px;
  display: grid;
  gap: 14px;
  background: #fbfcfa;
}

.note {
  border-left: 3px solid #d8a94d;
  padding-left: 10px;
  font-size: 13px;
}

.note code {
  background: #eef2eb;
  padding: 1px 4px;
  border-radius: 4px;
}

.settings-aside {
  display: grid;
  gap: 10px;
  box-shadow: var(--shadow-sm);
}

@media (max-width: 1080px) {
  .settings-layout,
  .routing-grid {
    grid-template-columns: 1fr;
  }
}
</style>
