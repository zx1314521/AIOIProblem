import { authState, clearAuth } from './auth'
import type { AnalysisResponse, AuthResponse, Problem, ProblemSet, RecommendationResponse } from '../types'

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (!(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (authState.token) {
    headers.set('Authorization', `Bearer ${authState.token}`)
  }
  const response = await fetch(path, { ...options, headers })
  if (response.status === 401) {
    clearAuth()
  }
  if (!response.ok) {
    const text = await response.text()
    throw new Error(errorMessage(text, response.statusText))
  }
  return response.json() as Promise<T>
}

function errorMessage(text: string, fallback: string) {
  try {
    const parsed = JSON.parse(text)
    return parsed.detail ?? fallback
  } catch {
    return text || fallback
  }
}

export const api = {
  register: (username: string, password: string) =>
    request<AuthResponse>('/api/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) }),
  login: (username: string, password: string) =>
    request<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  analyzeText: (title: string, text: string) =>
    request<AnalysisResponse>('/api/analysis/text', { method: 'POST', body: JSON.stringify({ title, text }) }),
  analyzeFile: (file: File) => {
    const data = new FormData()
    data.append('file', file)
    return request<AnalysisResponse>('/api/analysis/file', { method: 'POST', body: data })
  },
  searchProblems: (params: URLSearchParams) => request<Problem[]>(`/api/problems?${params.toString()}`),
  createProblem: (problem: { title: string; description: string; difficulty: string; tags: string[]; source?: string }) =>
    request<Problem>('/api/problems', { method: 'POST', body: JSON.stringify(problem) }),
  markPassed: (id: number) => request<Problem>(`/api/problems/${id}/passed`, { method: 'POST' }),
  listProblemSets: () => request<ProblemSet[]>('/api/problem-sets'),
  createProblemSet: (name: string, description: string) =>
    request<ProblemSet>('/api/problem-sets', { method: 'POST', body: JSON.stringify({ name, description }) }),
  addProblemToSet: (setId: number, problemId: number) =>
    request<ProblemSet>(`/api/problem-sets/${setId}/items`, { method: 'POST', body: JSON.stringify({ problemId }) }),
  removeProblemFromSet: (setId: number, problemId: number) =>
    request<ProblemSet>(`/api/problem-sets/${setId}/items/${problemId}`, { method: 'DELETE' }),
  recommendations: () => request<RecommendationResponse>('/api/recommendations')
}

