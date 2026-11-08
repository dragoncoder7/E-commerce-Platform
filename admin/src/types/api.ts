/** 后端统一响应结构 */
export interface ApiResult<T = unknown> {
  code: number
  message: string
  /** 部分后端使用 msg，做兼容 */
  msg?: string
  data: T
  timestamp?: number
}

/** 登录入参 */
export interface LoginParams {
  username: string
  password: string
  /** 记住账号（仅本地记录用户名，不存储密码） */
  remember?: boolean
}

/** 登录返回 */
export interface LoginResult {
  token: string
  /** 有效期（秒） */
  expiresIn: number
  refreshToken?: string
}

/** 当前登录用户信息 */
export interface UserInfo {
  userId: number
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  deptName: string
  roles: string[]
  permissions: string[]
  lastLoginTime: string
}
