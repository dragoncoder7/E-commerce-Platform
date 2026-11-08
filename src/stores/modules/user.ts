import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getUserInfoApi, getMenuTreeApi, loginApi, logoutApi } from '@/api/auth'
import type { LoginParams, UserInfo } from '@/types/api'
import type { MenuItem } from '@/types/menu'
import { getToken, removeToken, setRememberedAccount, setToken } from '@/utils/auth'
import { usePermissionStore } from './permission'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const userInfo = ref<UserInfo | null>(null)
  const menus = ref<MenuItem[]>([])

  const nickname = computed(() => userInfo.value?.nickname || '未登录')
  const avatarText = computed(() => nickname.value.slice(0, 1).toUpperCase())
  const roles = computed<string[]>(() => userInfo.value?.roles ?? [])
  const permissions = computed<string[]>(() => userInfo.value?.permissions ?? [])

  function hasPermission(code?: string): boolean {
    if (!code) return true
    if (permissions.value.includes('*')) return true
    return permissions.value.includes(code)
  }

  async function login(payload: LoginParams): Promise<void> {
    const result = await loginApi(payload)
    token.value = result.token
    setToken(result.token)
    setRememberedAccount(payload.remember ? payload.username : '')
  }

  async function fetchUserInfo(): Promise<UserInfo> {
    const info = await getUserInfoApi()
    userInfo.value = info
    return info
  }

  async function fetchMenus(): Promise<MenuItem[]> {
    const list = await getMenuTreeApi()
    menus.value = list
    return list
  }

  function reset(): void {
    token.value = ''
    userInfo.value = null
    menus.value = []
    removeToken()
    usePermissionStore().reset()
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi({ showError: false })
    } catch {
      // 退出接口异常不阻塞本地清理
    } finally {
      reset()
    }
  }

  return {
    token,
    userInfo,
    menus,
    nickname,
    avatarText,
    roles,
    permissions,
    hasPermission,
    login,
    fetchUserInfo,
    fetchMenus,
    reset,
    logout,
  }
})
