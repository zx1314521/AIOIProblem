<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Save } from 'lucide-vue-next'
import { api } from '../services/api'
import type { AiSettings } from '../types'

const settings = ref<AiSettings>({
  provider: 'codex',
  deepSeekApiKey: '',
  deepSeekBaseUrl: 'https://api.deepseek.com/chat/completions',
  deepSeekModel: 'deepseek-chat',
  deepSeekTimeoutSeconds: 45,
  codexCommand: 'codex',
  codexTimeoutSeconds: 60
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
      <p>选择批量分析和单题分析使用的 AI Provider，默认使用本地 Codex CLI。</p>
    </div>
  </header>

  <section class="panel grid settings-form">
    <label class="field">
      <span>AI Provider</span>
      <select v-model="settings.provider" class="select">
        <option value="codex">本地 Codex CLI</option>
        <option value="deepseek">DeepSeek API</option>
        <option value="mock">本地规则模型</option>
      </select>
    </label>

    <div class="grid two">
      <section class="provider-block">
        <h2>本地 Codex</h2>
        <label class="field">
          <span>命令</span>
          <input v-model="settings.codexCommand" class="input" placeholder="codex" />
        </label>
        <label class="field">
          <span>单题超时秒数</span>
          <input v-model.number="settings.codexTimeoutSeconds" class="input" type="number" min="5" max="1200" />
        </label>
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
</template>

<style scoped>
.settings-form {
  max-width: 1100px;
}

.provider-block {
  border: 1px solid #dfe4dc;
  border-radius: 8px;
  padding: 18px;
  display: grid;
  gap: 14px;
  background: #fbfcfa;
}

h2 {
  margin: 0;
}
</style>
