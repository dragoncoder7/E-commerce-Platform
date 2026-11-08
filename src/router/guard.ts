/**
 * 路由守卫：登录校验 + 动态路由注册
 * 该文件引入即生效，由 main.ts 以副作用方式导入。
 */
import NProgress from 'nprogress'
import { ElMessage } from 'element-plus'
import router from './index'
import { useUserStore } from '@/stores/modules/user'
import { usePermissionStore } from '@/stores/modules/permission'
import { APP_TITLE } from '@/config'

NProgress.configure({ showSpinner: false, trickleSpeed: 120, minimum: 0.15 })

/** 免登录白名单 */
const WHITE_LIST = ['/login']

router.beforeEach(async (to) => {
  NProgress.start()

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  // 1. 未登录
  if (!userStore.token) {
    if (WHITE_LIST.includes(to.path)) return true
    return {
      path: '/login',
      query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : undefined,
      replace: true,
    }
  }

  // 2. 已登录访问登录页 -> 回首页
  if (to.path === '/login') return { path: '/', replace: true }

  // 3. 动态路由已就绪
  if (permissionStore.isLoaded) return true

  // 4. 首次进入：拉取用户信息 + 菜单，注册动态路由后重新匹配
  try {
    await userStore.fetchUserInfo()
    const menus = await userStore.fetchMenus()
    permissionStore.generateRoutes(menus)
    return { path: to.path, query: to.query, hash: to.hash, replace: true }
  } catch (error) {
    userStore.reset()
    ElMessage.error(error instanceof Error ? error.message : '获取用户信息失败，请重新登录')
    return { path: '/login', query: { redirect: to.fullPath }, replace: true }
  }
})

router.afterEach((to) => {
  NProgress.done()
  const title = to.meta.title
  document.title = title ? `${title} · ${APP_TITLE}` : APP_TITLE
})

router.onError(() => {
  NProgress.done()
})
