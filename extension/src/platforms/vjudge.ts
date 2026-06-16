import { absoluteUrl, blockText, textOf } from './dom'
import type { ImportCandidate, ParseResult } from './types'

const platform = 'VJUDGE' as const

export function parseVJudge(document: Document, currentUrl: string): ParseResult {
  const url = new URL(currentUrl)
  if (url.pathname.startsWith('/contest/')) {
    return parseContestPage(document, url)
  }
  if (url.pathname.startsWith('/problem/')) {
    return parseDirectProblem(document, currentUrl)
  }
  return { kind: 'unsupported', items: [], message: '当前 VJudge 页面不是可导入的题目或比赛页面' }
}

function parseDirectProblem(document: Document, currentUrl: string): ParseResult {
  const slug = decodeURIComponent(new URL(currentUrl).pathname.replace(/^\/problem\//, ''))
  if (!slug) return { kind: 'unsupported', items: [] }

  const title = textOf(document.querySelector('#prob-title')) || slug
  const statement = readStatement(document)
  return {
    kind: 'problem',
    items: [{
      platform,
      sourceId: `VJ${slug}`,
      title,
      statement,
      url: currentUrl,
      passed: false
    }]
  }
}

function parseContestPage(document: Document, url: URL): ParseResult {
  const contestId = contestIdFromPath(url.pathname)
  if (!contestId) return { kind: 'unsupported', items: [] }

  const problemLetter = problemLetterFromHash(url.hash)
  if (problemLetter) {
    const title = textOf(document.querySelector('#prob-title-contest')) || problemLetter
    return {
      kind: 'problem',
      items: [{
        platform,
        sourceId: contestSourceId(contestId, problemLetter),
        title,
        statement: readStatement(document),
        url: contestProblemUrl(url, contestId, problemLetter),
        passed: false
      }]
    }
  }

  const rows = Array.from(document.querySelectorAll('#contest-problems tr'))
  const items = rows.flatMap(row => parseContestProblemRow(row, url, contestId))
  return items.length > 0
    ? { kind: 'submissions', items }
    : { kind: 'unsupported', items: [] }
}

function parseContestProblemRow(row: Element, url: URL, contestId: string): ImportCandidate[] {
  const link = row.querySelector<HTMLAnchorElement>('a[href^="#problem/"]')
  if (!link) return []

  const letter = problemLetterFromHash(link.getAttribute('href') ?? '')
  if (!letter) return []

  const title = `${letter} - ${textOf(link)}`
  return [{
    platform,
    sourceId: contestSourceId(contestId, letter),
    title,
    url: contestProblemUrl(url, contestId, letter),
    passed: /已解决|Accepted|Solved/i.test(textOf(row))
  }]
}

function readStatement(document: Document) {
  const frame = document.querySelector<HTMLIFrameElement>('iframe#frame-description, iframe[src^="/problem/description/"]')
  const frameText = blockText(frame?.contentDocument?.body)
  if (frameText) return frameText
  return blockText(document.querySelector('#frame-description-container, #frame-description'))
}

function contestIdFromPath(pathname: string) {
  return pathname.match(/^\/contest\/(\d+)/)?.[1] ?? ''
}

function problemLetterFromHash(hash: string) {
  return hash.match(/^#problem\/([A-Za-z0-9_ -]+)$/)?.[1]?.trim() ?? ''
}

function contestSourceId(contestId: string, letter: string) {
  return `VJ${contestId}${letter}`
}

function contestProblemUrl(url: URL, contestId: string, letter: string) {
  return absoluteUrl(`/contest/${contestId}#problem/${letter}`, url.toString())
}
