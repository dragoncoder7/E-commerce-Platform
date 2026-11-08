import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { RouteRecordRaw } from 'vue-router'
import router from '@/router'
import { LayoutComponent, constantRoutes } from '@/router/routes'
import type { MenuItem, SidebarNode } from '@/types/menu'

/** 所有视图组件（构建期静态收集，按需懒加载） */
const viewModules = import.meta.glob('../../views/**/*.vue')
const NotFoundView = () => import('@/views/error/404.vue')

/** 路由组件类型（非空） */
type RouteComponentValue = NonNullable<RouteRecordRaw['component']>

/** 把后端返回的组件路径（如 system/menu/index）解析成异步组件 */
function resolveView(component?: string): RouteComponentValue {
  const fallback = NotFoundView as unknown as RouteComponentValue
  if (!component) return fallback
  const normalized = component.replace(/^\/+/, '').replace(/\.vue$/, '')
  const loader = viewModules[`../../views/${normalized}.vue`]
  if (!loader) {
    console.warn(`[permission] 未找到视图组件：src/views/${normalized}.vue，已降级为 404 页面`)
    return fallback
  }
  return loader as unknown as RouteComponentValue
}

function joinPath(parent: string, child: string): string {
  if (!child) return parent
  if (child.startsWith('/')) return child
  return `${parent.replace(/\/+$/, '')}/${child}`
}

function firstLeafPath(routes: RouteRecordRaw[], base: string): string {
  if (!routes.length) return base
  const [first] = routes
  const path = joinPath(base, first.path)
  const children = (first.children ?? []) as RouteRecordRaw[]
  return children.length ? firstLeafPath(children, path) : path
}

/** MenuItem -> 路由记录 */
function toRoute(menu: MenuItem): RouteRecordRaw | null {
  if (menu.type === 'button' || menu.status !== 1) return null

  const children = (menu.children ?? [])
    .map((child) => toRoute(child))
    .filter((route): route is RouteRecordRaw => Boolean(route))

  const meta = {
    title: menu.title,
    icon: menu.icon || undefined,
    hidden: !menu.visible,
    keepAlive: menu.keepAlive,
    permission: menu.permission || undefined,
  }

  if (menu.type === 'catalog') {
    return {
      path: menu.path,
      name: menu.name,
      component: LayoutComponent,
      redirect: children.length ? firstLeafPath(children, menu.path) : undefined,
      meta,
      children,
    }
  }

  const route: RouteRecordRaw = {
    path: menu.path,
    name: menu.name,
    component: resolveView(menu.component),
    meta,
    ...(children.length ? { children } : {}),
  }
  return route
}

/** 顶层菜单没有布局容器，需要包一层 Layout */
function buildRoutes(menus: MenuItem[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  menus.forEach((menu) => {
    const route = toRoute(menu)
    if (!route) return

    if (menu.type === 'menu') {
      routes.push({
        path: route.path,
        name: `${menu.name}Layout`,
        component: LayoutComponent,
        meta: { hidden: !menu.visible },
        children: [{ ...route, path: '' }],
      })
      return
    }

    routes.push(route)
  })

  return routes
}

/** 收集路由及其子路由的 name，用于 reset 时精确移除 */
function collectRouteNames(route: RouteRecordRaw, acc: string[]): string[] {
  if (route.name) acc.push(String(route.name))
  ;((route.children ?? []) as RouteRecordRaw[]).forEach((child) => collectRouteNames(child, acc))
  return acc
}

/** 侧边栏节点归一化（只保留可见项） */
function toSidebarNodes(list: MenuItem[]): SidebarNode[] {
  return list
    .filter((item) => item.visible && item.type !== 'button' && item.status === 1)
    .map((item) => {
      const children = item.children?.length ? toSidebarNodes(item.children) : []
      return {
        path: item.path,
        title: item.title,
        icon: item.icon || undefined,
        children: children.length ? children : undefined,
      }
    })
}

export const usePermissionStore = defineStore('permission', () => {
  /** 后端菜单（已过滤按钮/停用项） */
  const menuTree = ref<MenuItem[]>([])
  /** 动态路由记录 */
  const dynamicRoutes = ref<RouteRecordRaw[]>([])
  /** 已注册的动态路由 name，用于重建时精确注销 */
  const registeredNames = ref<string[]>([])
  /** 是否已完成动态路由注册 */
  const isLoaded = ref(false)

  /** 侧边栏 = 静态路由（工作台）+ 动态菜单 */
  const sidebarMenus = computed<SidebarNode[]>(() => {
    const staticChildren = constantRoutes.find((route) => route.path === '/')?.children ?? []
    const staticNodes: SidebarNode[] = staticChildren
      .filter((child) => !child.meta?.hidden)
      .map((child) => ({
        path: `/${String(child.path)}`.replace(/\/+/g, '/'),
        title: String(child.meta?.title ?? ''),
        icon: child.meta?.icon,
      }))
    return [...staticNodes, ...toSidebarNodes(menuTree.value)]
  })

  /** 生成并注册动态路由 */
  function generateRoutes(menus: MenuItem[]): RouteRecordRaw[] {
    menuTree.value = menus
    const routes = buildRoutes(menus)

    routes.forEach((route) => {
      const names = collectRouteNames(route, [])
      const primaryName = names[0]
      if (!primaryName || router.hasRoute(primaryName)) return
      router.addRoute(route)
      registeredNames.value.push(...names)
    })

    dynamicRoutes.value = routes
    isLoaded.value = true
    return routes
  }

  /** 注销全部动态路由 */
  function reset(): void {
    registeredNames.value.forEach((name) => {
      if (router.hasRoute(name)) router.removeRoute(name)
    })
    registeredNames.value = []
    dynamicRoutes.value = []
    menuTree.value = []
    isLoaded.value = false
  }

  /** 菜单变更后重建路由（菜单管理页使用） */
  function rebuild(menus: MenuItem[]): RouteRecordRaw[] {
    reset()
    return generateRoutes(menus)
  }

  return {
    menuTree,
    dynamicRoutes,
    registeredNames,
    isLoaded,
    sidebarMenus,
    generateRoutes,
    rebuild,
    reset,
  }
})
