/**
 * 内置 Mock 服务
 *
 * 通过 axios adapter 劫持请求，不依赖任何 mock 中间件：
 * - 环境变量 VITE_USE_MOCK=true 时生效，改成 false 即直接访问真实后端
 * - 菜单数据持久化在 localStorage，刷新/重启后保留菜单管理页的改动
 * - 未命中的接口会返回 404，方便发现漏配的 API
 */
import type { AxiosAdapter, AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import type { ApiResult, LoginParams, LoginResult, UserInfo } from '@/types/api'
import type { MenuForm, MenuItem, MenuStatus, MenuType } from '@/types/menu'
import type { DashboardSummary } from '@/api/dashboard'
import { MOCK_ACCOUNTS, createDashboardSummary, createSeedMenus, type MockAccount } from './data'

const MENU_STORAGE_KEY = 'erp:mock:menus'
const DELAY_MIN = 120
const DELAY_MAX = 380

/* -------------------------------------------------------------------------- */
/*                                   工具方法                                  */
/* -------------------------------------------------------------------------- */

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms))

function ok<T>(data: T, message = 'ok'): ApiResult<T> {
  return { code: 200, message, data, timestamp: Date.now() }
}

function fail(message: string, code = 4001): ApiResult<null> {
  return { code, message, data: null, timestamp: Date.now() }
}

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

function now(): string {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(
    d.getMinutes(),
  )}:${pad(d.getSeconds())}`
}

/* -------------------------------------------------------------------------- */
/*                                  菜单数据源                                 */
/* -------------------------------------------------------------------------- */

function loadMenus(): MenuItem[] {
  const raw = localStorage.getItem(MENU_STORAGE_KEY)
  if (!raw) return createSeedMenus()
  try {
    const parsed = JSON.parse(raw) as MenuItem[]
    return Array.isArray(parsed) && parsed.length ? parsed : createSeedMenus()
  } catch {
    return createSeedMenus()
  }
}

let menuTable: MenuItem[] = loadMenus()

function persistMenus(): void {
  localStorage.setItem(MENU_STORAGE_KEY, JSON.stringify(menuTable))
}

function buildTree(flat: MenuItem[]): MenuItem[] {
  const nodes: MenuItem[] = flat.map((item) => ({ ...item, children: [] }))
  const dict = new Map<number, MenuItem>()
  nodes.forEach((node) => dict.set(node.id, node))

  const roots: MenuItem[] = []
  nodes.forEach((node) => {
    const parent = node.parentId ? dict.get(node.parentId) : undefined
    if (parent) parent.children!.push(node)
    else roots.push(node)
  })

  const sortNodes = (list: MenuItem[]) => {
    list.sort((a, b) => a.sort - b.sort || a.id - b.id)
    list.forEach((node) => {
      if (node.children && node.children.length) sortNodes(node.children)
      else delete node.children
    })
  }
  sortNodes(roots)
  return roots
}

function filterTree(list: MenuItem[], predicate: (item: MenuItem) => boolean): MenuItem[] {
  const result: MenuItem[] = []
  list.forEach((node) => {
    const children = node.children ? filterTree(node.children, predicate) : []
    if (predicate(node) || children.length) {
      result.push({ ...node, children: children.length ? children : undefined })
    }
  })
  return result
}

let autoIncrementId = menuTable.reduce((max, item) => Math.max(max, item.id), 0)

function nextId(): number {
  autoIncrementId += 1
  return autoIncrementId
}

/* -------------------------------------------------------------------------- */
/*                                   Mock 路由                                 */
/* -------------------------------------------------------------------------- */

interface MockContext {
  method: string
  pathname: string
  query: Record<string, string>
  params: Record<string, string>
  body: Record<string, any>
  token: string
  account: MockAccount | null
}

type MockHandler = (ctx: MockContext) => ApiResult<unknown>

interface MockRoute {
  method: string
  path: string
  auth?: boolean
  handler: MockHandler
}

function resolveAccount(token: string): MockAccount | null {
  // token 形如 mock.<username>.<timestamp>
  const [, username] = token.split('.')
  if (!username) return null
  return MOCK_ACCOUNTS.find((item) => item.username === username) ?? null
}

const routes: MockRoute[] = [
  {
    method: 'POST',
    path: '/auth/login',
    handler: ({ body }) => {
      const { username, password } = body as LoginParams
      const account = MOCK_ACCOUNTS.find((item) => item.username === username)
      if (!account || account.password !== password) {
        return fail('用户名或密码错误，请重新输入')
      }
      const payload: LoginResult = {
        token: `mock.${account.username}.${Date.now()}`,
        expiresIn: 7200,
        refreshToken: `refresh.${account.username}`,
      }
      return ok(payload, '登录成功')
    },
  },
  {
    method: 'POST',
    path: '/auth/logout',
    auth: true,
    handler: () => ok(null, '已退出登录'),
  },
  {
    method: 'GET',
    path: '/auth/userinfo',
    auth: true,
    handler: ({ account }) => {
      const info: UserInfo = {
        userId: account!.username === 'admin' ? 1001 : 1002,
        username: account!.username,
        nickname: account!.nickname,
        avatar: '',
        email: account!.email,
        phone: account!.phone,
        deptName: account!.deptName,
        roles: account!.roles,
        permissions: account!.permissions,
        lastLoginTime: account!.lastLoginTime,
      }
      return ok(info)
    },
  },
  {
    method: 'GET',
    path: '/auth/menus',
    auth: true,
    handler: () => ok(buildTree(menuTable)),
  },
  {
    method: 'GET',
    path: '/dashboard/summary',
    auth: true,
    handler: () => ok<DashboardSummary>(createDashboardSummary()),
  },
  {
    method: 'GET',
    path: '/menu/list',
    auth: true,
    handler: ({ query }) => {
      const title = (query.title ?? '').trim()
      const type = (query.type ?? '') as MenuType | ''
      const status = query.status === '' || query.status === undefined ? '' : (Number(query.status) as MenuStatus)
      const tree = buildTree(menuTable)
      const filtered = filterTree(tree, (item) => {
        if (title && !item.title.includes(title)) return false
        if (type && item.type !== type) return false
        if (status !== '' && item.status !== status) return false
        return true
      })
      return ok(filtered)
    },
  },
  {
    method: 'POST',
    path: '/menu',
    auth: true,
    handler: ({ body }) => {
      const form = body as MenuForm
      if (!form.title) return fail('菜单名称不能为空')
      const duplicated = menuTable.some(
        (item) => item.type !== 'button' && form.path && item.path === form.path,
      )
      if (duplicated) return fail(`路由路径 ${form.path} 已存在，请更换`)

      const item: MenuItem = {
        id: nextId(),
        parentId: form.parentId ?? 0,
        name: form.name || `Menu${autoIncrementId}`,
        title: form.title,
        type: form.type,
        path: form.type === 'button' ? '' : form.path,
        component: form.type === 'menu' ? form.component : '',
        icon: form.icon ?? '',
        permission: form.permission ?? '',
        sort: form.sort ?? 1,
        visible: form.visible ?? true,
        keepAlive: form.keepAlive ?? false,
        status: form.status ?? 1,
        createTime: now(),
      }
      menuTable = [...menuTable, item]
      persistMenus()
      return ok(item, '新增成功')
    },
  },
  {
    method: 'PUT',
    path: '/menu/:id',
    auth: true,
    handler: ({ params, body }) => {
      const id = Number(params.id)
      const target = menuTable.find((item) => item.id === id)
      if (!target) return fail('菜单不存在或已被删除')
      const form = body as MenuForm
      if (form.parentId === id) return fail('上级菜单不能选择自己')
      if (form.path) {
        const duplicated = menuTable.some(
          (item) => item.id !== id && item.type !== 'button' && item.path === form.path,
        )
        if (duplicated) return fail(`路由路径 ${form.path} 已存在，请更换`)
      }
      const updated: MenuItem = {
        ...target,
        parentId: form.parentId ?? target.parentId,
        name: form.name || target.name,
        title: form.title,
        type: form.type,
        path: form.type === 'button' ? '' : form.path,
        component: form.type === 'menu' ? form.component : '',
        icon: form.icon ?? '',
        permission: form.permission ?? '',
        sort: form.sort ?? target.sort,
        visible: form.visible ?? target.visible,
        keepAlive: form.keepAlive ?? target.keepAlive,
        status: form.status ?? target.status,
      }
      menuTable = menuTable.map((item) => (item.id === id ? updated : item))
      persistMenus()
      return ok(updated, '保存成功')
    },
  },
  {
    method: 'DELETE',
    path: '/menu/:id',
    auth: true,
    handler: ({ params }) => {
      const id = Number(params.id)
      const hasChildren = menuTable.some((item) => item.parentId === id)
      if (hasChildren) return fail('该菜单下存在子节点，请先删除子节点')
      menuTable = menuTable.filter((item) => item.id !== id)
      persistMenus()
      return ok(null, '删除成功')
    },
  },
]

/* -------------------------------------------------------------------------- */
/*                                   URL 解析                                  */
/* -------------------------------------------------------------------------- */

function normalizePath(pathname: string): string {
  const withLeading = pathname.startsWith('/') ? pathname : `/${pathname}`
  return withLeading.length > 1 ? withLeading.replace(/\/+$/, '') : withLeading
}

function matchPath(pattern: string, pathname: string): Record<string, string> | null {
  const patternParts = pattern.split('/').filter(Boolean)
  const pathParts = pathname.split('/').filter(Boolean)
  if (patternParts.length !== pathParts.length) return null
  const params: Record<string, string> = {}
  for (let i = 0; i < patternParts.length; i += 1) {
    const segment = patternParts[i]
    if (segment.startsWith(':')) {
      params[segment.slice(1)] = decodeURIComponent(pathParts[i])
    } else if (segment !== pathParts[i]) {
      return null
    }
  }
  return params
}

function parseBody(data: unknown): Record<string, any> {
  if (!data) return {}
  if (typeof data === 'string') {
    try {
      return JSON.parse(data) as Record<string, any>
    } catch {
      return {}
    }
  }
  return data as Record<string, any>
}

function buildResponse(config: AxiosRequestConfig, body: ApiResult<unknown>, status = 200): AxiosResponse {
  return {
    data: body,
    status,
    statusText: status === 200 ? 'OK' : 'ERROR',
    headers: { 'content-type': 'application/json' },
    config: config as AxiosResponse['config'],
    request: {},
  } as AxiosResponse
}

/* -------------------------------------------------------------------------- */
/*                                   适配器入口                                */
/* -------------------------------------------------------------------------- */

export function setupMock(instance: AxiosInstance): void {
  const adapter: AxiosAdapter = async (config) => {
    await sleep(DELAY_MIN + Math.random() * (DELAY_MAX - DELAY_MIN))

    const baseURL = config.baseURL ?? ''
    let rawUrl = config.url ?? ''
    if (baseURL && rawUrl.startsWith(baseURL)) rawUrl = rawUrl.slice(baseURL.length)

    const [pathname, search = ''] = rawUrl.split('?')
    const query: Record<string, string> = {}
    new URLSearchParams(search).forEach((value, key) => {
      query[key] = value
    })
    const extraParams = (config.params ?? {}) as Record<string, unknown>
    Object.entries(extraParams).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') query[key] = String(value)
    })

    const method = (config.method ?? 'get').toUpperCase()
    const normalized = normalizePath(pathname)
    const token = String(
      (config.headers as Record<string, unknown> | undefined)?.Authorization ??
        (config.headers as Record<string, unknown> | undefined)?.authorization ??
        '',
    ).replace(/^Bearer\s+/i, '')

    const route = routes.find(
      (item) => item.method === method && matchPath(item.path, normalized) !== null,
    )

    if (!route) {
      return buildResponse(
        config,
        { code: 404, message: `Mock 接口未定义：${method} ${normalized}`, data: null, timestamp: Date.now() },
        404,
      )
    }

    const params = matchPath(route.path, normalized) ?? {}
    const account = resolveAccount(token)

    if (route.auth && !account) {
      return buildResponse(config, { code: 401, message: '登录已过期，请重新登录', data: null, timestamp: Date.now() })
    }

    const result = route.handler({
      method,
      pathname: normalized,
      query,
      params,
      body: parseBody(config.data),
      token,
      account,
    })

    return buildResponse(config, result)
  }

  instance.defaults.adapter = adapter
  console.info('[mock] 已启用内置 Mock 服务，接口前缀：/api')
}

/** 重置 mock 菜单数据（调试用：在控制台执行 window.__resetErpMock?.() ） */
export function resetMockData(): void {
  localStorage.removeItem(MENU_STORAGE_KEY)
  menuTable = createSeedMenus()
  autoIncrementId = menuTable.reduce((max, item) => Math.max(max, item.id), 0)
}

if (typeof window !== 'undefined') {
  ;(window as unknown as Record<string, unknown>).__resetErpMock = resetMockData
}
