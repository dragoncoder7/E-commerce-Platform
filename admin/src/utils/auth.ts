const TOKEN_KEY = 'erp:token'
const ACCOUNT_KEY = 'erp:remember-account'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}

export function setToken(token: string): void {
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
