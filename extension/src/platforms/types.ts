export type OjPlatform = 'CODEFORCES' | 'ATCODER' | 'LUOGU' | 'NOWCODER'

export interface ImportCandidate {
  platform: OjPlatform
  sourceId: string
  title: string
  statement?: string
  url: string
  passed: boolean
}

export interface ParseResult {
  kind: 'problem' | 'submissions' | 'unsupported'
  items: ImportCandidate[]
  message?: string
}
