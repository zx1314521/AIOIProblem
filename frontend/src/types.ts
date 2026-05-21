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

