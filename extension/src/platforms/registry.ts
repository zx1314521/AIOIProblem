import { parseAtCoder } from './atcoder'
import { parseCodeforces } from './codeforces'
import { parseLuogu } from './luogu'
import { parseNowcoder } from './nowcoder'
import { parseVJudge } from './vjudge'
import type { ParseResult } from './types'

export function parseCurrentPage(document: Document, currentUrl: string): ParseResult {
  const host = new URL(currentUrl).hostname
  if (host.endsWith('codeforces.com')) return parseCodeforces(document, currentUrl)
  if (host.endsWith('atcoder.jp')) return parseAtCoder(document, currentUrl)
  if (host.endsWith('luogu.com.cn')) return parseLuogu(document, currentUrl)
  if (host.endsWith('nowcoder.com')) return parseNowcoder(document, currentUrl)
  if (host.endsWith('vjudge.net')) return parseVJudge(document, currentUrl)
  return { kind: 'unsupported', items: [], message: '当前页面不是已支持的 OJ 平台' }
}
