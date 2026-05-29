export const DEFAULT_API_BASE_URL = 'http://localhost:8080'
export const DEFAULT_APP_BASE_URL = 'http://localhost:5173'

export interface AppAuthHandoff {
  token?: string
  userId?: number
  username?: string
}

export function normalizeLocalHttpUrl(value: string | undefined, fallback: string, label: string) {
  const normalized = value?.trim().replace(/\/+$/, '') || fallback
  const url = new URL(normalized)
  if (!['http:', 'https:'].includes(url.protocol)) {
    throw new Error(`${label}必须以 http:// 或 https:// 开头`)
  }
  if (!['localhost', '127.0.0.1'].includes(url.hostname)) {
    throw new Error(`当前插件版本只支持本机${label} localhost 或 127.0.0.1`)
  }
  return url.origin
}

export function buildHashAppUrl(appUrl: string | undefined, path: string, auth?: AppAuthHandoff) {
  const baseUrl = normalizeLocalHttpUrl(appUrl, DEFAULT_APP_BASE_URL, '前端地址')
  const hashPath = path.startsWith('/') ? path : `/${path}`
  if (!auth?.token || !auth.username) {
    return `${baseUrl}/#${hashPath}`
  }
  const separator = hashPath.includes('?') ? '&' : '?'
  const params = new URLSearchParams({
    aioiExtensionToken: auth.token,
    aioiExtensionUser: JSON.stringify({
      id: auth.userId ?? 0,
      username: auth.username
    })
  })
  return `${baseUrl}/#${hashPath}${separator}${params.toString()}`
}
