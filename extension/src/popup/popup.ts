import './style.css'
import type { ImportCandidate } from '../platforms/types'
import { mergeImportResultsWithHistory } from './importStatus'
import type { ImportResult, OjImportHistoryJob, ImportStatusRow } from './importStatus'
import { shouldClearStoredAuth } from './authState'
import { DEFAULT_API_BASE_URL, DEFAULT_APP_BASE_URL, buildHashAppUrl, normalizeLocalHttpUrl } from './urls'

interface StoredSettings {
  baseUrl?: string
  appUrl?: string
  token?: string
  userId?: number
  username?: string
}

interface AuthResponse {
  token: string
  user: { id: number, username: string }
}

const baseUrlInput = must<HTMLInputElement>('baseUrl')
const appUrlInput = must<HTMLInputElement>('appUrl')
const usernameInput = must<HTMLInputElement>('username')
const passwordInput = must<HTMLInputElement>('password')
const account = must<HTMLElement>('account')
const statusBox = must<HTMLElement>('status')
const results = must<HTMLUListElement>('results')
const quickLinks = must<HTMLDivElement>('quickLinks')
const loginButton = must<HTMLButtonElement>('login')
const scanButton = must<HTMLButtonElement>('scan')
const openOjHistoryButton = must<HTMLButtonElement>('openOjHistory')
const openProblemsButton = must<HTMLButtonElement>('openProblems')

void restore()

loginButton.addEventListener('click', () => {
  void login()
})

scanButton.addEventListener('click', () => {
  void scanAndImport()
})

openOjHistoryButton.addEventListener('click', () => {
  void openAppPath('/oj-imports')
})

openProblemsButton.addEventListener('click', () => {
  void openAppPath('/problems')
})

async function restore() {
  const stored = await chrome.storage.local.get(['baseUrl', 'appUrl', 'token', 'username']) as StoredSettings
  baseUrlInput.value = stored.baseUrl ?? DEFAULT_API_BASE_URL
  appUrlInput.value = stored.appUrl ?? DEFAULT_APP_BASE_URL
  account.textContent = stored.username ? `已绑定 ${stored.username}` : '未绑定'
}

async function login() {
  setBusy('正在绑定账号...')
  try {
    const baseUrl = normalizeBaseUrl(baseUrlInput.value)
    const appUrl = normalizeAppUrl(appUrlInput.value)
    const response = await fetch(`${baseUrl}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: usernameInput.value.trim(), password: passwordInput.value })
    })
    if (!response.ok) throw new Error(await responseText(response, '登录失败'))
    const auth = await response.json() as AuthResponse
    await chrome.storage.local.set({ baseUrl, appUrl, token: auth.token, userId: auth.user.id, username: auth.user.username })
    passwordInput.value = ''
    account.textContent = `已绑定 ${auth.user.username}`
    setStatus('账号已绑定。')
  } catch (error) {
    setError(error)
  }
}

async function scanAndImport() {
  setBusy('正在扫描当前页...')
  results.replaceChildren()
  quickLinks.hidden = true
  try {
    const stored = await chrome.storage.local.get(['baseUrl', 'appUrl', 'token']) as StoredSettings
    if (!stored.token) throw new Error('请先绑定账号')
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true })
    if (!tab.id) throw new Error('无法读取当前标签页')
    const scan = await sendScanMessage(tab.id)
    if (!scan.ok || scan.items.length === 0) throw new Error(scan.message ?? '当前页没有可导入题目')
    setBusy(`扫描到 ${scan.items.length} 题，正在上传...`)
    const baseUrl = normalizeBaseUrl(stored.baseUrl ?? baseUrlInput.value)
    const appUrl = normalizeAppUrl(appUrlInput.value || stored.appUrl || DEFAULT_APP_BASE_URL)
    await chrome.storage.local.set({ baseUrl, appUrl })
    const response = await fetch(`${baseUrl}/api/oj-imports`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${stored.token}`
      },
      body: JSON.stringify({ items: scan.items })
    })
    if (shouldClearStoredAuth(response.status)) {
      await clearStoredAuth()
      throw new Error('登录已过期，请重新绑定账号')
    }
    if (!response.ok) throw new Error(await responseText(response, '上传失败'))
    const body = await response.json() as { items: ImportResult[] }
    renderResults(body.items)
    quickLinks.hidden = false
    setStatus(`已上传 ${body.items.length} 条结果，正在同步队列状态...`)
    try {
      await pollImportStatus(baseUrl, stored.token, body.items)
    } catch (error) {
      setStatus(importStatusSyncFailureMessage(error, body.items.length))
    }
  } catch (error) {
    setError(error)
  }
}

function importStatusSyncFailureMessage(error: unknown, itemCount: number) {
  const detail = error instanceof Error ? error.message : '导入状态加载失败'
  return `已上传 ${itemCount} 条结果，但状态暂时无法同步：${detail}。请打开导入记录查看最新状态。`
}

async function pollImportStatus(baseUrl: string, token: string, importResults: ImportResult[]) {
  for (let attempt = 0; attempt < 6; attempt++) {
    const history = await fetchImportHistory(baseUrl, token)
    const rows = mergeImportResultsWithHistory(importResults, history)
    renderStatusRows(rows)
    if (rows.every(row => ['已入库', '失败', '已存在', '已存在并标记通过', '已跳过'].includes(row.status))) {
      setStatus(`状态已更新：${rows.length} 条导入结果。`)
      return
    }
    if (attempt < 5) {
      setStatus(`状态同步中：${rows.length} 条导入结果，队列仍在处理...`)
      await delay(2500)
    }
  }
  setStatus('导入已提交，后续状态可在网页端导入记录查看。')
}

async function fetchImportHistory(baseUrl: string, token: string) {
  const response = await fetch(`${baseUrl}/api/oj-imports/history`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  if (shouldClearStoredAuth(response.status)) {
    await clearStoredAuth()
    throw new Error('登录已过期，请重新绑定账号')
  }
  if (!response.ok) throw new Error(await responseText(response, '导入状态加载失败'))
  return await response.json() as OjImportHistoryJob[]
}

async function sendScanMessage(tabId: number) {
  try {
    return await chrome.tabs.sendMessage(tabId, { type: 'AIOI_SCAN_PAGE' }) as {
      ok: boolean
      items: ImportCandidate[]
      message?: string
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : ''
    if (message.includes('Receiving end does not exist')) {
      throw new Error('当前页面不是已支持的 OJ 平台')
    }
    throw error
  }
}

function renderResults(items: ImportResult[]) {
  results.replaceChildren(...items.map(item => {
    const li = document.createElement('li')
    li.innerHTML = `<strong>${escapeHtml(item.sourceId)}</strong><span>${escapeHtml(item.status)}</span><small>${escapeHtml(item.title || item.message || '')}</small>`
    return li
  }))
}

function renderStatusRows(items: ImportStatusRow[]) {
  results.replaceChildren(...items.map(item => {
    const li = document.createElement('li')
    li.innerHTML = `<strong>${escapeHtml(item.sourceId)}</strong><span>${escapeHtml(item.status)}</span><small>${escapeHtml(item.title)} · ${escapeHtml(item.detail)}</small>`
    return li
  }))
}

async function openAppPath(path: string) {
  const appUrl = normalizeAppUrl(appUrlInput.value)
  const stored = await chrome.storage.local.get(['token', 'userId', 'username']) as StoredSettings
  await chrome.storage.local.set({ appUrl })
  await chrome.tabs.create({
    url: buildHashAppUrl(appUrl, path, {
      token: stored.token,
      userId: stored.userId,
      username: stored.username
    })
  })
}

function normalizeBaseUrl(value: string) {
  return normalizeLocalHttpUrl(value, DEFAULT_API_BASE_URL, '后端地址')
}

function normalizeAppUrl(value: string) {
  return normalizeLocalHttpUrl(value, DEFAULT_APP_BASE_URL, '前端地址')
}

async function clearStoredAuth() {
  await chrome.storage.local.remove(['token', 'userId', 'username'])
  account.textContent = '未绑定'
}

function delay(ms: number) {
  return new Promise(resolve => window.setTimeout(resolve, ms))
}

async function responseText(response: Response, fallback: string) {
  const text = await response.text()
  try {
    const parsed = JSON.parse(text)
    return parsed.detail ?? fallback
  } catch {
    return text || fallback
  }
}

function setBusy(message: string) {
  scanButton.disabled = true
  loginButton.disabled = true
  statusBox.textContent = message
  statusBox.classList.remove('error')
}

function setStatus(message: string) {
  scanButton.disabled = false
  loginButton.disabled = false
  statusBox.textContent = message
  statusBox.classList.remove('error')
}

function setError(error: unknown) {
  scanButton.disabled = false
  loginButton.disabled = false
  statusBox.textContent = error instanceof Error ? error.message : '操作失败'
  statusBox.classList.add('error')
}

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  })[char] ?? char)
}

function must<T extends HTMLElement>(id: string) {
  const element = document.getElementById(id)
  if (!element) throw new Error(`Missing element #${id}`)
  return element as T
}
