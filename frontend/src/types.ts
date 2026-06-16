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
  dataStatus?: 'NONE' | 'GENERATING' | 'READY' | 'FAILED'
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
  dataGenerationProvider: 'codex' | 'deepseek' | 'mock'
  deepSeekApiKey: string
  deepSeekBaseUrl: string
  deepSeekModel: string
  deepSeekTimeoutSeconds: number
  codexCommand: string
  codexModel: string
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
  taskType?: 'PROBLEM_ANALYSIS' | 'DATA_GENERATION'
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

export interface ProblemDataStatus {
  id?: number
  problemId: number
  status: 'NONE' | 'GENERATING' | 'READY' | 'FAILED'
  caseCount: number
  errorMessage?: string
  notes?: string
  updatedAt?: string
}

export interface ProblemDataCase {
  id: number
  index: number
  input: string
  output: string
}

export interface ProblemDataSet {
  id?: number
  problemId: number
  status: 'NONE' | 'GENERATING' | 'READY' | 'FAILED'
  stdCpp: string
  configYaml: string
  errorMessage?: string
  notes?: string
  updatedAt?: string
  cases: ProblemDataCase[]
}

export interface CodeRunRequest {
  code: string
  input?: string
  caseIndexes?: number[]
}

export interface CaseRunResponse {
  index: number
  status: 'AC' | 'WA' | 'RE' | 'TLE'
  stdout: string
  stderr: string
  expectedOutput: string
  durationMs: number
}

export interface CodeRunResponse {
  status: 'OK' | 'AC' | 'WA' | 'RE' | 'TLE' | 'CE'
  stdout: string
  stderr: string
  exitCode?: number
  durationMs: number
  cases: CaseRunResponse[]
}
