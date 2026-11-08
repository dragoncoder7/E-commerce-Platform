<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  DataLine,
  Goods,
  Plus,
  Setting,
  ShoppingCart,
  Tickets,
  TrendCharts,
  Wallet,
} from '@element-plus/icons-vue'
import { getDashboardSummaryApi, type DashboardSummary } from '@/api/dashboard'
import { useUserStore } from '@/stores/modules/user'
import { APP_VERSION, USE_MOCK } from '@/config'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(true)
const summary = ref<DashboardSummary | null>(null)

const stats = computed(() => summary.value?.stats ?? [])
const todos = computed(() => summary.value?.todos ?? [])
const notices = computed(() => summary.value?.notices ?? [])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const today = computed(() => {
  const date = new Date()
  const week = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月 ${date.getDate()} 日 · ${week}`
})

const quickActions = [
  { title: '新增商品', desc: '发布商品到店铺', icon: Plus, key: 'goods' },
  { title: '订单管理', desc: '处理待发货订单', icon: ShoppingCart, key: 'order' },
  { title: '菜单管理', desc: '配置后台菜单与路由', icon: Setting, key: 'menu' },
  { title: '经营分析', desc: '查看核心数据看板', icon: DataLine, key: 'analysis' },
]

const techStack = [
  'Vue 3.5',
  'TypeScript 5.9',
  'Element Plus 2.14',
  'Tailwind CSS 4',
  'Pinia 3',
  'Vite 7',
  'Docker',
]

const levelStyle: Record<string, { dot: string; label: string; text: string }> = {
  high: { dot: 'bg-red-500', label: '紧急', text: 'text-red-600' },
  normal: { dot: 'bg-amber-500', label: '普通', text: 'text-amber-600' },
  low: { dot: 'bg-slate-400', label: '较低', text: 'text-slate-500' },
}

function formatNumber(value: number): string {
  return value.toLocaleString('zh-CN')
}

async function loadSummary(): Promise<void> {
  loading.value = true
  try {
    summary.value = await getDashboardSummaryApi()
  } finally {
    loading.value = false
  }
}

function handleQuickAction(key: string): void {
  if (key === 'menu') {
    void router.push('/system/menu')
    return
  }
  ElMessage.info('该模块将在后续迭代中接入，当前仅保留入口')
}

onMounted(loadSummary)
</script>

<template>
  <div class="space-y-4">
    <!-- 欢迎横幅 -->
    <section class="relative overflow-hidden rounded-xl bg-gradient-to-r from-[#0b1220] via-[#111c33] to-[#1d3a6e] px-6 py-6">
      <div class="pointer-events-none absolute -right-10 -top-16 h-48 w-48 rounded-full bg-blue-500/20 blur-3xl"></div>
      <div class="relative flex flex-wrap items-end justify-between gap-4">
        <div>
          <p class="text-[12px] tracking-[0.14em] text-blue-300/80">{{ today }}</p>
          <h2 class="mt-2 text-[24px] font-semibold tracking-tight text-white">
            {{ greeting }}，{{ userStore.nickname }}
          </h2>
          <p class="mt-2 max-w-[520px] text-[13px] leading-relaxed text-slate-400">
            欢迎回到 {{ userStore.userInfo?.deptName || '电商中台' }}。左侧菜单由后端配置动态生成，
            在「系统管理 / 菜单管理」中新增菜单后，导航与路由会立即同步。
          </p>
        </div>
        <div class="flex items-center gap-6 text-white">
          <div class="text-right">
            <p class="text-[11px] text-slate-400">上次登录</p>
            <p class="mt-1 text-[13px] font-medium">{{ userStore.userInfo?.lastLoginTime || '--' }}</p>
          </div>
          <div class="h-9 w-px bg-white/10"></div>
          <div class="text-right">
            <p class="text-[11px] text-slate-400">服务时间</p>
            <p class="mt-1 text-[13px] font-medium">{{ summary?.serverTime || '--' }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 核心指标 -->
    <section
      v-loading="loading"
      class="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4"
    >
      <div
        v-for="stat in stats"
        :key="stat.key"
        class="app-card group px-5 py-4 transition hover:border-blue-200 hover:shadow-[0_10px_30px_-22px_rgba(37,99,235,0.6)]"
      >
        <div class="flex items-center justify-between">
          <p class="text-[12.5px] text-slate-500">{{ stat.label }}</p>
          <span
            class="flex items-center gap-1 rounded-full px-1.5 py-0.5 text-[11px] font-medium"
            :class="
              stat.trend > 0
                ? 'bg-red-50 text-red-600'
                : stat.trend < 0
                  ? 'bg-emerald-50 text-emerald-600'
                  : 'bg-slate-100 text-slate-500'
            "
          >
            <el-icon v-if="stat.trend !== 0" :size="11">
              <TrendCharts />
            </el-icon>
            {{ stat.trend === 0 ? '持平' : `${stat.trend > 0 ? '+' : ''}${stat.trend}%` }}
          </span>
        </div>
        <div class="mt-3 flex items-baseline gap-1">
          <span class="text-[26px] font-semibold tracking-tight text-slate-900">
            {{ formatNumber(stat.value) }}
          </span>
          <span class="text-[12px] text-slate-400">{{ stat.unit }}</span>
        </div>
        <p class="mt-1.5 text-[11px] text-slate-400">{{ stat.hint }}</p>
      </div>
    </section>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-3">
      <!-- 左侧：快捷入口 + 待办 -->
      <div class="space-y-4 xl:col-span-2">
        <section class="app-card">
          <div class="app-card__header">
            <h3 class="text-[14px] font-semibold text-slate-800">快捷入口</h3>
            <span class="text-[11.5px] text-slate-400">常用功能直达</span>
          </div>
          <div class="grid grid-cols-2 gap-3 p-4 sm:grid-cols-4">
            <button
              v-for="action in quickActions"
              :key="action.key"
              type="button"
              class="group flex flex-col items-start gap-2 rounded-lg border border-slate-100 bg-slate-50/70 p-3.5 text-left transition hover:-translate-y-0.5 hover:border-blue-200 hover:bg-white hover:shadow-sm"
              @click="handleQuickAction(action.key)"
            >
              <span
                class="flex h-8 w-8 items-center justify-center rounded-lg bg-white text-blue-600 shadow-sm ring-1 ring-slate-100 transition group-hover:bg-blue-600 group-hover:text-white"
              >
                <el-icon :size="15"><component :is="action.icon" /></el-icon>
              </span>
              <span class="text-[13px] font-medium text-slate-800">{{ action.title }}</span>
              <span class="text-[11px] leading-tight text-slate-400">{{ action.desc }}</span>
            </button>
          </div>
        </section>

        <section class="app-card">
          <div class="app-card__header">
            <h3 class="text-[14px] font-semibold text-slate-800">待办事项</h3>
            <span class="rounded-full bg-blue-50 px-2 py-0.5 text-[11px] font-medium text-blue-600">
              {{ todos.length }} 项待处理
            </span>
          </div>
          <ul class="divide-y divide-slate-50">
            <li
              v-for="todo in todos"
              :key="todo.id"
              class="flex items-center gap-3 px-5 py-3 transition hover:bg-slate-50/70"
            >
              <span class="h-1.5 w-1.5 shrink-0 rounded-full" :class="levelStyle[todo.level].dot"></span>
              <p class="min-w-0 flex-1 truncate text-[13px] text-slate-700">{{ todo.title }}</p>
              <span class="shrink-0 text-[11px]" :class="levelStyle[todo.level].text">
                {{ levelStyle[todo.level].label }}
              </span>
              <span class="w-16 shrink-0 text-right text-[11px] text-slate-400">{{ todo.deadline }}</span>
            </li>
          </ul>
        </section>
      </div>

      <!-- 右侧：公告 + 系统信息 -->
      <div class="space-y-4">
        <section class="app-card">
          <div class="app-card__header">
            <h3 class="text-[14px] font-semibold text-slate-800">系统公告</h3>
            <el-icon class="text-slate-300"><Tickets /></el-icon>
          </div>
          <ul class="divide-y divide-slate-50">
            <li v-for="notice in notices" :key="notice.id" class="cursor-pointer px-5 py-3 hover:bg-slate-50/70">
              <div class="flex items-center gap-2">
                <span class="rounded bg-slate-100 px-1.5 py-0.5 text-[10.5px] text-slate-500">
                  {{ notice.type }}
                </span>
                <span class="text-[11px] text-slate-400">{{ notice.time }}</span>
              </div>
              <p class="mt-1.5 line-clamp-2 text-[12.5px] leading-relaxed text-slate-700">
                {{ notice.title }}
              </p>
            </li>
          </ul>
        </section>

        <section class="app-card">
          <div class="app-card__header">
            <h3 class="text-[14px] font-semibold text-slate-800">运行环境</h3>
            <el-icon class="text-slate-300"><Wallet /></el-icon>
          </div>
          <div class="space-y-2.5 px-5 py-4 text-[12.5px]">
            <div class="flex items-center justify-between">
              <span class="text-slate-500">应用版本</span>
              <span class="font-medium text-slate-800">v{{ APP_VERSION }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-slate-500">数据来源</span>
              <span
                class="rounded-full px-2 py-0.5 text-[11px] font-medium"
                :class="USE_MOCK ? 'bg-amber-50 text-amber-600' : 'bg-emerald-50 text-emerald-600'"
              >
                {{ USE_MOCK ? '内置 Mock' : '后端网关' }}
              </span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-slate-500">接口前缀</span>
              <span class="font-mono text-[11.5px] text-slate-700">/api</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-slate-500">当前角色</span>
              <span class="font-medium text-slate-800">
                {{ userStore.roles.includes('super_admin') ? '超级管理员' : '运营人员' }}
              </span>
            </div>
            <div class="border-t border-slate-50 pt-2.5">
              <p class="mb-2 text-slate-500">技术栈</p>
              <div class="flex flex-wrap gap-1.5">
                <span
                  v-for="tech in techStack"
                  :key="tech"
                  class="rounded-md bg-slate-50 px-1.5 py-0.5 text-[10.5px] text-slate-500 ring-1 ring-slate-100"
                >
                  {{ tech }}
                </span>
              </div>
            </div>
          </div>
        </section>

        <button
          type="button"
          class="flex w-full items-center justify-between rounded-xl border border-dashed border-blue-200 bg-blue-50/50 px-4 py-3 text-left transition hover:bg-blue-50"
          @click="router.push('/system/menu')"
        >
          <div class="flex items-center gap-2.5">
            <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-white text-blue-600 shadow-sm">
              <el-icon :size="15"><Goods /></el-icon>
            </span>
            <div>
              <p class="text-[12.5px] font-medium text-slate-800">配置菜单与路由</p>
              <p class="text-[11px] text-slate-400">新增菜单即可生成动态路由</p>
            </div>
          </div>
          <el-icon class="text-blue-500"><ArrowRight /></el-icon>
        </button>
      </div>
    </div>
  </div>
</template>
