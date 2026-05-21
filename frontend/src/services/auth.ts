import { reactive } from 'vue'
import type { AuthResponse, User } from '../types'

const tokenKey = 'aioi-token'
const userKey = 'aioi-user'

export const authState = reactive({
  token: localStorage.getItem(tokenKey),
  user: readUser()
})

export function setAuth(auth: AuthResponse) {
  authState.token = auth.token
  authState.user = auth.user
  localStorage.setItem(tokenKey, auth.token)
  localStorage.setItem(userKey, JSON.stringify(auth.user))
}

export function clearAuth() {
  authState.token = null
  authState.user = null
  localStorage.removeItem(tokenKey)
  localStorage.removeItem(userKey)
}

function readUser(): User | null {
  const raw = localStorage.getItem(userKey)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

