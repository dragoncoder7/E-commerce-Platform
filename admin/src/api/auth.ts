import { http } from '@/utils/request'
import type { AxiosRequestConfig } from 'axios'
import type { LoginParams, LoginResult, UserInfo } from '@/types/api'
import type { MenuItem } from '@/types/menu'

/** 账号密码登录 */
export function loginApi(data: LoginParams) {
  return http.post<LoginResult>('/auth/login', data)
}

/** 退出登录 */
export function logoutApi(config?: AxiosRequestConfig) {
  return http.post<void>('/auth/logout', undefined, config)
}

/** 获取当前登录用户信息 */
export function getUserInfoApi() {
  return http.get<UserInfo>('/auth/userinfo')
}

/** 获取当前用户可见的菜单树（用于生成动态路由） */
export function getMenuTreeApi() {
  return http.get<MenuItem[]>('/auth/menus')
}
