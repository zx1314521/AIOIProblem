import { absoluteUrl, blockText, textOf } from './dom'
import type { ImportCandidate, ParseResult } from './types'

const PROBLEM_RE = /\/problem\/([A-Za-z0-9_]+)\/?/

export function parseLuogu(document: Document, currentUrl: string): ParseResult {
  const match = new URL(currentUrl).pathname.match(PROBLEM_RE)
  if (match) {
    const problemId = match[1].toUpperCase()
    const statement = document.querySelector('article') ?? document.querySelector('[data-v-md-line]')
    const title = textOf(document.querySelector('h1')) || problemId
    return {
      kind: 'problem',
      items: [{
        platform: 'LUOGU',
        sourceId: `LG${problemId}`,
        title,
        statement: blockText(statement),
        url: currentUrl,
        passed: false
      }]
    }
  }
  return parseLuoguSubmissions(document, currentUrl)
}

function parseLuoguSubmissions(document: Document, currentUrl: string): ParseResult {
  const bySource = new Map<string, ImportCandidate>()
  for (const row of Array.from(document.querySelectorAll('tr, .record, .submission'))) {
    const link = Array.from(row.querySelectorAll<HTMLAnchorElement>('a[href*="/problem/"]'))
      .find(anchor => PROBLEM_RE.test(anchor.pathname))
    if (!link) continue
    const problemId = link.pathname.match(PROBLEM_RE)?.[1]?.toUpperCase()
    if (!problemId) continue
    const sourceId = `LG${problemId}`
    const verdict = textOf(row)
    const existing = bySource.get(sourceId)
    bySource.set(sourceId, {
      platform: 'LUOGU',
      sourceId,
      title: textOf(link) || problemId,
      url: absoluteUrl(link.getAttribute('href') ?? '', currentUrl),
      passed: (existing?.passed ?? false) || /accepted|通过|ac\b/i.test(verdict)
    })
  }
  return bySource.size
    ? { kind: 'submissions', items: Array.from(bySource.values()) }
    : { kind: 'unsupported', items: [], message: '未识别到洛谷题目或提交记录' }
}
