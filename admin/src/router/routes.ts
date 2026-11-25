import type { RouteRecordRaw } from 'vue-router'

/** 布局容器（异步加载，避免首屏打包进登录页） */
export const LayoutComponent = () => import('@/layouts/index.vue')

/**
 * 静态路由：登录页、工作台、404
 * 其余业务路由由后端菜单在登录后动态注册
 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true },
  },
  {
    path: '/',
    component: LayoutComponent,
    redirect: '/dashboard',
    children: [
      { path: 'staff', name: 'StaffManagement', component: () => import('@/views/system/staff/index.vue'), meta: { title: '后台人员', icon: 'UserFilled', permission: 'staff:read' } },
      { path: 'roles', name: 'RoleManagement', component: () => import('@/views/system/role/index.vue'), meta: { title: '角色权限', icon: 'Key', permission: 'roles:read' } },
      { path: 'system/menu', name: 'SystemMenuManagement', component: () => import('@/views/system/menu/index.vue'), meta: { title: '菜单权限', icon: 'Menu', permission: 'menu:read' } },
      { path: 'members', name: 'MemberManagement', component: () => import('@/views/member/index.vue'), meta: { title: '商城会员', icon: 'User', permission: 'members:read' } },
      { path: 'forbidden', name: 'Forbidden', component: () => import('@/views/error/403.vue'), meta: { title: '暂无权限', hidden: true } },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled', affix: true, permission: 'dashboard:read' },
      },
      {
        path: 'product/category',
        name: 'ProductCategoryManagement',
        component: () => import('@/views/product/category/index.vue'),
        meta: { title: '商品类别', icon: 'Menu', permission: 'product:read' },
      },
      { path: 'product/brand', name: 'BrandManagement', component: () => import('@/views/product/brand/index.vue'), meta: { title: '品牌管理', icon: 'CollectionTag', permission: 'product:read' } },
      {
        path: 'product',
        name: 'ProductManagement',
        component: () => import('@/views/product/index.vue'),
        meta: { title: '商品管理', icon: 'Goods', permission: 'product:read' },
      },
      {
        // 兜底路由放在布局内：菜单组件路径填写错误时仍保留导航，方便切换
        path: ':pathMatch(.*)*',
        name: 'NotFound',
        component: () => import('@/views/error/404.vue'),
        meta: { title: '页面不存在', hidden: true },
      },
    ],
  },
]
