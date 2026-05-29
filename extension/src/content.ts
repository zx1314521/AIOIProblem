import { parseCurrentPage } from './platforms/registry'
import type { ImportCandidate } from './platforms/types'

interface ScanResponse {
  ok: boolean
  items: ImportCandidate[]
  message?: string
}

const MAX_HYDRATE_CONCURRENCY = 4
const FETCH_TIMEOUT_MS = 10_000

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message?.type !== 'AIOI_SCAN_PAGE') return false
  scanPage()
    .then(sendResponse)
    .catch(error => sendResponse({ ok: false, items: [], message: errorMessage(error) }))
  return true
})

async function scanPage(): Promise<ScanResponse> {
  const parsed = parseCurrentPage(document, location.href)
  if (parsed.kind === 'unsupported') {
    return { ok: false, items: [], message: parsed.message }
  }
  const hydrated = await hydrateStatements(parsed.items)
  const items = hydrated.filter(item => item.title.trim() && item.statement?.trim())
  return {
    ok: items.length > 0,
    items,
    message: items.length > 0 ? undefined : '没有找到可导入的非空题面'
  }
}

async function hydrateStatements(items: ImportCandidate[]) {
  const hydrated: ImportCandidate[] = []
  for (let index = 0; index < items.length; index += MAX_HYDRATE_CONCURRENCY) {
    const batch = items.slice(index, index + MAX_HYDRATE_CONCURRENCY)
    hydrated.push(...await Promise.all(batch.map(item => hydrateStatement(item))))
  }
  return hydrated
}

async function hydrateStatement(item: ImportCandidate): Promise<ImportCandidate> {
  if (item.statement?.trim()) return item
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS)
  try {
    const response = await fetch(item.url, { credentials: 'include', signal: controller.signal })
    if (!response.ok) return item
    const html = await response.text()
    const page = new DOMParser().parseFromString(html, 'text/html')
    const parsed = parseCurrentPage(page, item.url)
    const problem = parsed.items.find(candidate => candidate.sourceId === item.sourceId)
    return problem ? { ...problem, passed: item.passed || problem.passed } : item
  } catch {
    return item
  } finally {
    window.clearTimeout(timeout)
  }
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '扫描失败'
}
