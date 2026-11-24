const TOKEN_KEY = 'erp:token'
const ACCOUNT_KEY = 'erp:remember-account'

/** Store only the credential; the request interceptor adds the Bearer scheme. */
export function normalizeToken(value: unknown): string {
  if (typeof value !== 'string') return ''
  const token = value.trim().replace(/^(?:Bearer\s+)+/i, '').trim()
  return !token || /^(?:undefined|null|Bearer)$/i.test(token) || /\s/.test(token) ? '' : token
}

export function getToken(): string {
  const stored = localStorage.getItem(TOKEN_KEY)
  const token = normalizeToken(stored)
  if (stored && !token) localStorage.removeItem(TOKEN_KEY)
  return token
}

export function setToken(value: unknown): void {
  const token = normalizeToken(value)
  if (!token) {
    removeToken()
    throw new Error('登录响应缺少有效 token，请重新登录')
  }
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/** 记住的登录账号（不保存密码） */
export function getRememberedAccount(): string {
  return localStorage.getItem(ACCOUNT_KEY) ?? ''
}

export function setRememberedAccount(username: string): void {
  if (!username) {
    localStorage.removeItem(ACCOUNT_KEY)
    return
  }
  localStorage.setItem(ACCOUNT_KEY, username)
}
