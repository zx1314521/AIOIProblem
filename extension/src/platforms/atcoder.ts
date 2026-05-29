import { absoluteUrl, blockText, textOf } from './dom'
import type { ImportCandidate, ParseResult } from './types'

const TASK_RE = /\/contests\/([^/]+)\/tasks\/([^/?#]+)\/?/

export function parseAtCoder(document: Document, currentUrl: string): ParseResult {
  const match = new URL(currentUrl).pathname.match(TASK_RE)
  if (match) {
    const [, contestId, taskId] = match
    const statement = document.querySelector('#task-statement')
    const title = textOf(document.querySelector('span.h2')) || taskId
    return {
      kind: 'problem',
      items: [{
        platform: 'ATCODER',
        sourceId: `AT${contestId.toUpperCase()}_${taskId.toUpperCase()}`,
        title,
        statement: blockText(statement),
        url: currentUrl,
        passed: false
      }]
    }
  }
  return parseAtCoderSubmissions(document, currentUrl)
}

function parseAtCoderSubmissions(document: Document, currentUrl: string): ParseResult {
  const bySource = new Map<string, ImportCandidate>()
  for (const row of Array.from(document.querySelectorAll('tr'))) {
    const link = Array.from(row.querySelectorAll<HTMLAnchorElement>('a[href*="/tasks/"]'))
      .find(anchor => TASK_RE.test(anchor.pathname))
    if (!link) continue
    const match = link.pathname.match(TASK_RE)
    if (!match) continue
    const [, contestId, taskId] = match
    const sourceId = `AT${contestId.toUpperCase()}_${taskId.toUpperCase()}`
    const verdict = textOf(row).toLowerCase()
    const existing = bySource.get(sourceId)
    bySource.set(sourceId, {
      platform: 'ATCODER',
      sourceId,
      title: textOf(link) || sourceId,
      url: absoluteUrl(link.getAttribute('href') ?? '', currentUrl),
      passed: (existing?.passed ?? false) || verdict.includes('accepted') || /\bac\b/i.test(textOf(row))
    })
  }
  return bySource.size
    ? { kind: 'submissions', items: Array.from(bySource.values()) }
    : { kind: 'unsupported', items: [], message: '未识别到 AtCoder 题目或提交记录' }
}
