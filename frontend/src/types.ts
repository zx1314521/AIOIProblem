export type DifficultyCode = 'ENTRY' | 'EASY' | 'CSPJ_MEDIUM' | 'CSPS_ADVANCED' | 'NOIP_HARD' | 'NOI_HELL'

export interface User {
  id: number
  username: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface Problem {
  id: number
  title: string
  description: string
  difficulty: string
  difficultyCode: DifficultyCode
  tags: string[]
  source?: string
  externalPlatform?: string
  externalSourceId?: string
  sourceUrl?: string
  createdAt: string
  passed: boolean
}

export interface AnalysisResponse {
  difficulty: string
  difficultyCode: DifficultyCode
  confidence: number
  tags: string[]
  hints: string[]
  reasoningSummary: string
  similarProblems: SimilarProblem[]
}

export interface SimilarProblem {
  id: number
  title: string
  difficulty: string
  tags: string[]
  reason: string
}

export interface DuplicateHint {
  id: number
  title: string
  difficulty: string
  difficultyCode: DifficultyCode
  tags: string[]
  externalPlatform?: string
  externalSourceId?: string
  sourceUrl?: string
  score: number
  reason: string
}

export interface TagCategory {
  name: string
  tags: string[]
}

export interface TagCatalog {
  categories: TagCategory[]
}

export interface ProblemSet {
  id: number
  name: string
  description?: string
  createdAt: string
  problems: Problem[]
}

export interface RecommendationResponse {
  weakTags: string[]
  items: RecommendationItem[]
}

export interface RecommendationItem {
  problem: Problem
  reason: string
  practiceOrder: number
}

export interface AiSettings {
  provider: 'codex' | 'deepseek' | 'mock'
  problemAnalysisProvider: 'codex' | 'deepseek' | 'mock'
  recommendationProvider: 'codex' | 'deepseek' | 'mock'
  deepSeekApiKey: string
  deepSeekBaseUrl: string
  deepSeekModel: string
  deepSeekTimeoutSeconds: number
  codexCommand: string
  codexTimeoutSeconds: number
}

export interface BatchJob {
  id: number
  name: string
  status: 'RUNNING' | 'PAUSED' | 'COMPLETED' | 'FAILED'
  totalCount: number
  successCount: number
  failedCount: number
  pendingCount: number
  runningCount: number
  createdAt: string
}

export interface BatchItem {
  id: number
  title: string
  content: string
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  sortOrder: number
  problemId?: number
  difficulty?: string
  difficultyCode?: DifficultyCode
  tags: string[]
  errorMessage?: string
  createdAt: string
  startedAt?: string
  finishedAt?: string
  aiProvider?: string
  aiModel?: string
  aiConfidence?: number
  aiReasoningSummary?: string
  aiHints?: string[]
  aiDurationMs?: number
}

export interface BatchJobDetail {
  job: BatchJob
  items: BatchItem[]
}

export interface OjImportHistoryItem {
  id: number
  platform: 'CODEFORCES' | 'ATCODER' | 'LUOGU' | 'NOWCODER' | string
  sourceId: string
  title: string
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  problemId?: number
  sourceUrl: string
  originalStatement: string
  passedRequested: boolean
  errorMessage?: string
  createdAt: string
  startedAt?: string
  finishedAt?: string
  aiProvider?: string
  aiModel?: string
  aiDurationMs?: number
}

export interface OjImportHistoryJob extends BatchJob {
  items: OjImportHistoryItem[]
}
