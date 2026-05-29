import { absoluteUrl, blockText, textOf } from './dom'
import type { ImportCandidate, ParseResult } from './types'

const PROBLEM_RE = /\/acm\/problem\/(\d+)\/?/

export function parseNowcoder(document: Document, currentUrl: string): ParseResult {
  const match = new URL(currentUrl).pathname.match(PROBLEM_RE)
  if (match) {
    const id = match[1]
    const statement = document.querySelector('.subject-content') ?? document.querySelector('.problem-content') ?? document.querySelector('article')
    const title = textOf(document.querySelector('h1')) || `NC${id}`
    return {
      kind: 'problem',
      items: [{
        platform: 'NOWCODER',
        sourceId: `NC${id}`,
        title,
        statement: blockText(statement),
        url: currentUrl,
        passed: false
      }]
    }
  }
  return parseNowcoderSubmissions(document, currentUrl)
}

function parseNowcoderSubmissions(document: Document, currentUrl: string): ParseResult {
  const bySource = new Map<string, ImportCandidate>()
  for (const row of Array.from(document.querySelectorAll('tr, .status-item, .submission'))) {
    const link = Array.from(row.querySelectorAll<HTMLAnchorElement>('a[href*="/acm/problem/"]'))
      .find(anchor => PROBLEM_RE.test(anchor.pathname))
    if (!link) continue
    const id = link.pathname.match(PROBLEM_RE)?.[1]
    if (!id) continue
    const sourceId = `NC${id}`
    const verdict = textOf(row)
    const existing = bySource.get(sourceId)
    bySource.set(sourceId, {
      platform: 'NOWCODER',
      sourceId,
      title: textOf(link) || sourceId,
      url: absoluteUrl(link.getAttribute('href') ?? '', currentUrl),
      passed: (existing?.passed ?? false) || /accepted|答案正确|通过|ac\b/i.test(verdict)
    })
  }
  return bySource.size
    ? { kind: 'submissions', items: Array.from(bySource.values()) }
    : { kind: 'unsupported', items: [], message: '未识别到牛客题目或提交记录' }
}
