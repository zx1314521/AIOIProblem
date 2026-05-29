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
