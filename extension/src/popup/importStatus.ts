export interface ImportResult {
  sourceId: string
  title: string
  status: string
  message?: string
}

export interface OjImportHistoryItem {
  sourceId: string
  title: string
  status: string
  problemId?: number
  errorMessage?: string
}

export interface OjImportHistoryJob {
  id: number
  items: OjImportHistoryItem[]
}

export interface ImportStatusRow {
  sourceId: string
  title: string
  status: string
  detail: string
  problemId?: number
}

export function mergeImportResultsWithHistory(results: ImportResult[], history: OjImportHistoryJob[]): ImportStatusRow[] {
  const latestBySource = new Map<string, OjImportHistoryItem>()
  for (const job of history) {
    for (const item of job.items) {
      if (!latestBySource.has(item.sourceId)) {
        latestBySource.set(item.sourceId, item)
      }
    }
  }
  return results.map(result => {
    const item = latestBySource.get(result.sourceId)
    if (!item) {
      return {
        sourceId: result.sourceId,
        title: result.title || result.message || '',
        status: importResultStatus(result.status),
        detail: result.message || '等待后端队列更新',
        problemId: undefined
      }
    }
    return {
      sourceId: result.sourceId,
      title: item.title || result.title || result.message || '',
      status: historyStatus(item.status),
      detail: item.problemId ? `题库 #${item.problemId}` : item.errorMessage || '等待 AI 队列处理',
      problemId: item.problemId
    }
  })
}

function historyStatus(status: string) {
  return {
    PENDING: '排队中',
    RUNNING: '分析中',
    SUCCEEDED: '已入库',
    FAILED: '失败'
  }[status] ?? status
}

function importResultStatus(status: string) {
  return {
    QUEUED: '已排队',
    EXISTS_MARKED_PASSED: '已存在并标记通过',
    EXISTS_UNCHANGED: '已存在',
    SKIPPED_EMPTY: '已跳过',
    SKIPPED_INACCESSIBLE: '已跳过',
    SKIPPED_INVALID_SOURCE_ID: '已跳过'
  }[status] ?? status
}
