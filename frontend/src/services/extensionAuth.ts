import { setAuth } from './auth'
import type { AuthResponse, User } from '../types'

const tokenParam = 'aioiExtensionToken'
const userParam = 'aioiExtensionUser'

export interface ExtensionAuthHandoff {
  auth: AuthResponse
  cleanedHash: string
}

export function parseExtensionAuthHash(hash: string): ExtensionAuthHandoff | null {
  if (!hash.startsWith('#')) return null
  const queryStart = hash.indexOf('?')
  if (queryStart === -1) return null

  const routePath = hash.slice(0, queryStart)
  const query = hash.slice(queryStart + 1)
  const params = new URLSearchParams(query)
  const token = params.get(tokenParam)
  const rawUser = params.get(userParam)
  if (!token || !rawUser) return null

  const user = parseUser(rawUser)
  if (!user) return null

  params.delete(tokenParam)
  params.delete(userParam)
  const remainingQuery = params.toString()
  return {
    auth: { token, user },
    cleanedHash: remainingQuery ? `${routePath}?${remainingQuery}` : routePath
  }
}

export function consumeExtensionAuthFromLocation() {
  const handoff = parseExtensionAuthHash(window.location.hash)
  if (!handoff) return false
  setAuth(handoff.auth)
  window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}${handoff.cleanedHash}`)
  return true
}

function parseUser(rawUser: string): User | null {
  try {
    const parsed = JSON.parse(rawUser) as Partial<User>
    if (typeof parsed.username !== 'string' || !parsed.username.trim()) {
      return null
    }
    return {
      id: typeof parsed.id === 'number' ? parsed.id : 0,
      username: parsed.username
    }
  } catch {
    return null
  }
}
