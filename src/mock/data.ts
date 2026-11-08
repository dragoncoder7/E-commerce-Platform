import type { MenuItem } from '@/types/menu'
import type { DashboardSummary } from '@/api/dashboard'

export interface MockAccount {
  username: string
  password: string
  nickname: string
  email: string
  phone: string
  deptName: string
  roles: string[]
  permissions: string[]
  lastLoginTime: string
}

export const MOCK_ACCOUNTS: MockAccount[] = [
  {
    username: 'admin',
    password: '123456',
    nickname: '林知远',
    email: 'admin@cloudmall.com',
    phone: '138****6621',
    deptName: '电商中台 / 平台研发部',
    roles: ['super_admin'],
    permissions: ['system:menu:list', 'system:menu:create', 'system:menu:update', 'system:menu:delete'],
    lastLoginTime: '2026-11-07 18:42:11',
  },
  {
    username: 'editor',
    password: '123456',
    nickname: '周雨桐',
    email: 'editor@cloudmall.com',
    phone: '139****3308',
    deptName: '电商中台 / 运营中心',
    roles: ['operator'],
    permissions: ['system:menu:list'],
    lastLoginTime: '2026-11-06 09:20:03',
  },
]

/** 初始菜单数据（可通过菜单管理页增删改，改动会持久化在浏览器 localStorage） */
export function createSeedMenus(): MenuItem[] {
  return [
    {
      id: 1,
      parentId: 0,
      name: 'System',
      title: '系统管理',
      type: 'catalog',
      path: '/system',
      component: '',
      icon: 'Setting',
      permission: '',
      sort: 10,
      visible: true,
      keepAlive: false,
      status: 1,
      createTime: '2026-10-12 09:30:00',
    },
    {
      id: 2,
      parentId: 1,
      name: 'SystemMenu',
      title: '菜单管理',
      type: 'menu',
      path: '/system/menu',
      component: 'system/menu/index',
      icon: 'Menu',
      permission: 'system:menu:list',
      sort: 1,
      visible: true,
      keepAlive: true,
      status: 1,
      createTime: '2026-10-12 09:36:12',
    },
    {
      id: 3,
      parentId: 2,
      name: 'SystemMenuCreate',
      title: '新增菜单',
      type: 'button',
      path: '',
      component: '',
      icon: '',
      permission: 'system:menu:create',
      sort: 1,
      visible: true,
      keepAlive: false,
      status: 1,
      createTime: '2026-10-12 09:40:55',
    },
    {
      id: 4,
      parentId: 2,
      name: 'SystemMenuUpdate',
      title: '编辑菜单',
      type: 'button',
      path: '',
      component: '',
      icon: '',
      permission: 'system:menu:update',
      sort: 2,
      visible: true,
      keepAlive: false,
      status: 1,
      createTime: '2026-10-12 09:41:20',
    },
    {
      id: 5,
      parentId: 2,
      name: 'SystemMenuDelete',
      title: '删除菜单',
      type: 'button',
      path: '',
      component: '',
      icon: '',
      permission: 'system:menu:delete',
      sort: 3,
      visible: true,
      keepAlive: false,
      status: 1,
      createTime: '2026-10-12 09:41:48',
    },
  ]
}

export function createDashboardSummary(): DashboardSummary {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return {
    serverTime: `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(
      now.getHours(),
    )}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`,
    stats: [
      { key: 'gmv', label: '今日成交额', value: 1286430, unit: '元', trend: 12.5, hint: '较昨日' },
      { key: 'order', label: '今日订单量', value: 4862, unit: '单', trend: 8.2, hint: '较昨日' },
      { key: 'member', label: '新增会员', value: 328, unit: '人', trend: -2.1, hint: '较昨日' },
      { key: 'ticket', label: '待处理工单', value: 24, unit: '件', trend: 0, hint: '待跟进' },
    ],
    todos: [
      { id: 1, title: '「双十一」预售商品价格复核', level: 'high', deadline: '今天 18:00' },
      { id: 2, title: '华东仓库存预警需补货确认', level: 'high', deadline: '今天 20:00' },
      { id: 3, title: '新商户入驻资质审核（3 家）', level: 'normal', deadline: '明天 12:00' },
      { id: 4, title: '月度结算单核对并推送财务', level: 'normal', deadline: '11-10' },
      { id: 5, title: '会员等级规则文档更新', level: 'low', deadline: '11-15' },
    ],
    notices: [
      { id: 1, title: '系统将于 11 月 12 日 02:00 进行订单库扩容，预计 30 分钟', type: '运维通知', time: '11-08' },
      { id: 2, title: 'ERP v0.1.0 已上线：动态路由菜单管理能力开放', type: '版本发布', time: '11-06' },
      { id: 3, title: '关于规范商品类目命名的最新说明', type: '运营规范', time: '11-04' },
    ],
  }
}
