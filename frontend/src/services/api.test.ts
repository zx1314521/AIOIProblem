import { api } from './api'
import { authState, setAuth } from './auth'

beforeEach(() => {
  localStorage.clear()
  setAuth({ token: 'token', user: { id: 1, username: 'teacher' } })
})

test('keeps auth state when a request is forbidden but not expired', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Forbidden', { status: 403, statusText: 'Forbidden' })))

  await expect(api.listOjImportHistory()).rejects.toThrow('Forbidden')

  expect(authState.token).toBe('token')
  expect(authState.user?.username).toBe('teacher')
})

test('clears auth state when a request is unauthorized', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Unauthorized', { status: 401, statusText: 'Unauthorized' })))

  await expect(api.listOjImportHistory()).rejects.toThrow('Unauthorized')

  expect(authState.token).toBeNull()
  expect(authState.user).toBeNull()
})

test('deletes a problem set with bearer authorization', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 204 })))

  await api.deleteProblemSet(7)

  const [path, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit]
  expect(path).toBe('/api/problem-sets/7')
  expect(init.method).toBe('DELETE')
  expect((init.headers as Headers).get('Authorization')).toBe('Bearer token')
})

test('sends bearer token when starting AI data generation', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({
    id: 1,
    problemId: 7,
    status: 'GENERATING',
    caseCount: 0
  }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' }
  })))

  await api.generateProblemData(7)

  const [, init] = vi.mocked(fetch).mock.calls[0] as [string, RequestInit]
  expect((init.headers as Headers).get('Authorization')).toBe('Bearer token')
})

test('does not clear auth when AI data generation is unauthorized', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Unauthorized', { status: 401, statusText: 'Unauthorized' })))

  await expect(api.generateProblemData(7)).rejects.toThrow('AI 数据生成启动失败')

  expect(authState.token).toBe('token')
  expect(authState.user?.username).toBe('teacher')
})
