import { absoluteUrl, blockText, textOf } from './dom'
import type { ImportCandidate, ParseResult } from './types'

const PROBLEM_RE = /\/(?:(?:contest|gym)\/(\d+)\/problem|problemset\/problem\/(\d+))\/([A-Za-z0-9]+)\/?/

export function parseCodeforces(document: Document, currentUrl: string): ParseResult {
  const problemMatch = parseProblemPath(new URL(currentUrl).pathname)
  if (problemMatch) {
    const { contestId, index } = problemMatch
    const statement = document.querySelector('.problem-statement')
    const title = textOf(statement?.querySelector('.header .title')) || `Codeforces ${contestId}${index}`
    return {
      kind: 'problem',
      items: [{
        platform: 'CODEFORCES',
        sourceId: `CF${contestId}${index.toUpperCase()}`,
        title,
        statement: blockText(statement),
        url: currentUrl,
        passed: false
      }]
    }
  }
  return parseCodeforcesSubmissions(document, currentUrl)
}

function parseCodeforcesSubmissions(document: Document, currentUrl: string): ParseResult {
  const rows = Array.from(document.querySelectorAll('tr'))
  const bySource = new Map<string, ImportCandidate>()
  for (const row of rows) {
    const link = Array.from(row.querySelectorAll<HTMLAnchorElement>('a[href*="/problem/"]'))
      .find(anchor => parseProblemPath(anchor.pathname) !== null)
    if (!link) continue
    const match = parseProblemPath(link.pathname)
    if (!match) continue
    const { contestId, index } = match
    const sourceId = `CF${contestId}${index.toUpperCase()}`
    const verdict = textOf(row).toLowerCase()
    const existing = bySource.get(sourceId)
    bySource.set(sourceId, {
      platform: 'CODEFORCES',
      sourceId,
      title: textOf(link) || sourceId,
      url: absoluteUrl(link.getAttribute('href') ?? '', currentUrl),
      passed: (existing?.passed ?? false) || verdict.includes('accepted')
    })
  }
  return bySource.size
    ? { kind: 'submissions', items: Array.from(bySource.values()) }
    : { kind: 'unsupported', items: [], message: '未识别到 Codeforces 题目或提交记录' }
}

function parseProblemPath(pathname: string) {
  const match = pathname.match(PROBLEM_RE)
  if (!match) return null
  return {
    contestId: match[1] ?? match[2],
    index: match[3]
  }
}
