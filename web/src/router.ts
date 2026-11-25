import { createRouter, createWebHistory } from 'vue-router'
import { useMemberStore } from '@/stores/member'

export function safeReturnPath(value: unknown): string {
  return typeof value === 'string' && /^\/(?!\/)/.test(value) && !/[\\\x00-\x1f]/.test(value)
    && !/^\/(login|register)(?:[/?#]|$)/.test(value) ? value : '/account'
}
const router = createRouter({ history: createWebHistory(), scrollBehavior: () => ({ top: 0 }), routes: [
  { path: '/', component: () => import('./views/Catalog.vue') },
  { path: '/products', component: () => import('./views/Catalog.vue') },
  { path: '/products/:id', component: () => import('./views/Product.vue') },
  { path: '/login', component: () => import('./views/Auth.vue') },
  { path: '/register', component: () => import('./views/Auth.vue') },
  { path: '/account', component: () => import('./views/Account.vue'), meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', component: () => import('./views/NotFound.vue') },
] })
router.beforeEach(async to => {
  const store = useMemberStore()
  await store.restore()
  if (to.meta.requiresAuth && !store.member) return { path: '/login', query: { redirect: to.fullPath } }
  if ((to.path === '/login' || to.path === '/register') && store.member) return safeReturnPath(to.query.redirect)
})
window.addEventListener('store:unauthorized', () => {
  const route = router.currentRoute.value
  if (route.meta.requiresAuth) void router.replace({ path: '/login', query: { redirect: route.fullPath } })
})
export default router
