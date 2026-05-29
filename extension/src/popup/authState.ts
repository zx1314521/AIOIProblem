export function shouldClearStoredAuth(status: number) {
  return status === 401
}

export function expiredLoginMessage() {
  return '登录已过期，请重新绑定账号'
}
