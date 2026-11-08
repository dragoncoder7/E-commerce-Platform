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
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled', affix: true },
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
