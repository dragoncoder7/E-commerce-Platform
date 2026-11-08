/** 菜单类型：目录 / 菜单 / 按钮 */
export type MenuType = 'catalog' | 'menu' | 'button'

/** 菜单状态：0 停用，1 正常 */
export type MenuStatus = 0 | 1

export const MENU_TYPE_LABEL: Record<MenuType, string> = {
  catalog: '目录',
  menu: '菜单',
  button: '按钮',
}

export const MENU_TYPE_TAG: Record<MenuType, 'primary' | 'success' | 'info'> = {
  catalog: 'primary',
  menu: 'success',
  button: 'info',
}

/** 菜单实体（后端返回结构） */
export interface MenuItem {
  id: number
  parentId: number
  /** 路由 name，全局唯一 */
  name: string
  /** 菜单标题 */
  title: string
  type: MenuType
  /** 路由路径，须以 / 开头 */
  path: string
  /** 组件路径，相对 src/views，如 system/menu/index */
  component: string
  /** Element Plus 图标名 */
  icon: string
  /** 权限标识，如 system:menu:create */
  permission: string
  /** 排序，值越小越靠前 */
  sort: number
  /** 是否在侧边栏显示 */
  visible: boolean
  /** 是否缓存页面 */
  keepAlive: boolean
  status: MenuStatus
  createTime: string
  children?: MenuItem[]
}

/** 菜单查询条件 */
export interface MenuQuery {
  title?: string
  type?: MenuType | ''
  status?: MenuStatus | ''
}

/** 菜单新增/编辑表单 */
export interface MenuForm {
  id?: number
  parentId: number
  name?: string
  title: string
  type: MenuType
  path: string
  component: string
  icon: string
  permission: string
  sort: number
  visible: boolean
  keepAlive: boolean
  status: MenuStatus
}

/** 侧边栏渲染节点（静态路由 + 动态菜单归一化后的结构） */
export interface SidebarNode {
  path: string
  title: string
  icon?: string
  children?: SidebarNode[]
}
