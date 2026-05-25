import { authState, clearAuth } from './auth'
import type { AiSettings, AnalysisResponse, AuthResponse, BatchItem, BatchJob, BatchJobDetail, Problem, ProblemSet, RecommendationResponse, TagCatalog } from '../types'

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (!(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  if (authState.token) {
    headers.set('Authorization', `Bearer ${authState.token}`)
  }
  const response = await fetch(path, { ...options, headers })
  if (response.status === 401 || response.status === 403) {
    clearAuth()
  }
  if (!response.ok) {
    const text = await response.text()
    throw new Error(errorMessage(text, response.statusText))
  }
  if (response.status === 204) {
    return undefined as T
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
  getTags: () => request<TagCatalog>('/api/tags'),
  getProblem: (id: number) => request<Problem>(`/api/problems/${id}`),
  createProblem: (problem: { title: string; description: string; difficulty: string; tags: string[]; source?: string }) =>
    request<Problem>('/api/problems', { method: 'POST', body: JSON.stringify(problem) }),
  updateProblem: (id: number, problem: { title: string; description: string; difficulty: string; tags: string[]; source?: string }) =>
    request<Problem>(`/api/problems/${id}`, { method: 'PUT', body: JSON.stringify(problem) }),
  deleteProblem: (id: number) => request<void>(`/api/problems/${id}`, { method: 'DELETE' }),
  markPassed: (id: number) => request<Problem>(`/api/problems/${id}/passed`, { method: 'POST' }),
  unmarkPassed: (id: number) => request<Problem>(`/api/problems/${id}/passed`, { method: 'DELETE' }),
  markProblemsPassed: (problemIds: number[]) =>
    request<Problem[]>('/api/problems/bulk/passed', { method: 'POST', body: JSON.stringify({ problemIds }) }),
  deleteProblems: (problemIds: number[]) =>
    request<void>('/api/problems/bulk', { method: 'DELETE', body: JSON.stringify({ problemIds }) }),
  listProblemSets: () => request<ProblemSet[]>('/api/problem-sets'),
  createProblemSet: (name: string, description: string) =>
    request<ProblemSet>('/api/problem-sets', { method: 'POST', body: JSON.stringify({ name, description }) }),
  createProblemSetWithProblems: (name: string, description: string, problemIds: number[]) =>
    request<ProblemSet>('/api/problem-sets/with-problems', { method: 'POST', body: JSON.stringify({ name, description, problemIds }) }),
  addProblemToSet: (setId: number, problemId: number) =>
    request<ProblemSet>(`/api/problem-sets/${setId}/items`, { method: 'POST', body: JSON.stringify({ problemId }) }),
  addProblemsToSet: (setId: number, problemIds: number[]) =>
    request<ProblemSet>(`/api/problem-sets/${setId}/items/bulk`, { method: 'POST', body: JSON.stringify({ problemIds }) }),
  removeProblemFromSet: (setId: number, problemId: number) =>
    request<ProblemSet>(`/api/problem-sets/${setId}/items/${problemId}`, { method: 'DELETE' }),
  recommendations: () => request<RecommendationResponse>('/api/recommendations')
  ,
  getAiSettings: () => request<AiSettings>('/api/settings/ai'),
  updateAiSettings: (settings: AiSettings) =>
    request<AiSettings>('/api/settings/ai', { method: 'PUT', body: JSON.stringify(settings) }),
  uploadBatch: (name: string, files: File[]) => {
    const data = new FormData()
    if (name) data.append('name', name)
    files.forEach(file => data.append('files', file))
    return request<BatchJobDetail>('/api/batch-jobs', { method: 'POST', body: data })
  },
  listBatchJobs: () => request<BatchJob[]>('/api/batch-jobs'),
  getBatchJob: (id: number) => request<BatchJobDetail>(`/api/batch-jobs/${id}`),
  pauseBatchJob: (id: number) => request<BatchJob>(`/api/batch-jobs/${id}/pause`, { method: 'POST' }),
  resumeBatchJob: (id: number) => request<BatchJob>(`/api/batch-jobs/${id}/resume`, { method: 'POST' }),
  updateBatchItem: (jobId: number, itemId: number, payload: { title: string; content: string }) =>
    request<BatchItem>(`/api/batch-jobs/${jobId}/items/${itemId}`, { method: 'PATCH', body: JSON.stringify(payload) }),
  deleteBatchItem: (jobId: number, itemId: number) =>
    request<BatchJobDetail>(`/api/batch-jobs/${jobId}/items/${itemId}`, { method: 'DELETE' }),
  reorderBatchItems: (jobId: number, itemIds: number[]) =>
    request<BatchJobDetail>(`/api/batch-jobs/${jobId}/items/reorder`, { method: 'POST', body: JSON.stringify({ itemIds }) })
}
