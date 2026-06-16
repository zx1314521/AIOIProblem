import type { Problem } from '../types'

let cachedProblems: Problem[] | null = null

export function getCachedProblems() {
  return cachedProblems ? [...cachedProblems] : null
}

export function setCachedProblems(problems: Problem[]) {
  cachedProblems = [...problems]
}

export function upsertCachedProblem(problem: Problem) {
  const current = cachedProblems ?? []
  const exists = current.some(item => item.id === problem.id)
  cachedProblems = exists
    ? current.map(item => item.id === problem.id ? problem : item)
    : [problem, ...current]
}

export function updateCachedProblems(problems: Problem[]) {
  if (!cachedProblems) return
  const updated = new Map(problems.map(problem => [problem.id, problem]))
  cachedProblems = cachedProblems.map(problem => updated.get(problem.id) ?? problem)
}

export function removeCachedProblems(problemIds: number[]) {
  if (!cachedProblems) return
  const removed = new Set(problemIds)
  cachedProblems = cachedProblems.filter(problem => !removed.has(problem.id))
}

export function resetProblemListCacheForTests() {
  cachedProblems = null
}
