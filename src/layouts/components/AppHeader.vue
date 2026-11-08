<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, Bell, Expand, Fold, FullScreen, SwitchButton, User } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/modules/app'
import { useUserStore } from '@/stores/modules/user'
import { USE_MOCK } from '@/config'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const collapsed = computed(() => appStore.sidebarCollapsed)
const isFullscreen = ref(false)

const breadcrumbs = computed(() => {
  const items = route.matched
    .filter((item) => item.meta?.title && item.path !== '/')
    .map((item) => ({ title: String(item.meta.title), path: item.path }))

  if (items.length === 0 || items[0].path !== '/dashboard') {
    items.unshift({ title: '工作台', path: '/dashboard' })
  }
  return items
})

const notices = [
  { id: 1, title: '订单库将于 11-12 02:00 扩容，预计 30 分钟', time: '2 小时前' },
  { id: 2, title: 'ERP v0.1.0 已上线，菜单管理支持动态路由', time: '昨天' },
  { id: 3, title: '双十一大促期间请关注库存预警', time: '2 天前' },
]

async function toggleFullscreen(): Promise<void> {
  try {
    if (!document.fullscreenElement) {
      await document.documentElement.requestFullscreen()
      isFullscreen.value = true
    } else {
      await document.exitFullscreen()
      isFullscreen.value = false
    }
  } catch {
    ElMessage.warning('当前浏览器不支持全屏或已被拦截')
  }
}

async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm('退出后需要重新登录，确定继续吗？', '退出登录', {
      type: 'warning',
      confirmButtonText: '确定退出',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }
  await userStore.logout()
  ElMessage.success('已安全退出')
  await router.replace('/login')
}

function handleCommand(command: string): void {
  if (command === 'logout') {
    void handleLogout()
    return
  }
  ElMessage.info('该功能将在后续版本开放')
}
</script>

<template>
  <header class="z-10 flex h-14 shrink-0 items-center justify-between border-b border-slate-200/80 bg-white px-4">
    <div class="flex min-w-0 items-center gap-2">
      <button
        type="button"
        class="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-800"
        @click="appStore.toggleSidebar()"
      >
        <el-icon :size="17">
          <Expand v-if="collapsed" />
          <Fold v-else />
        </el-icon>
      </button>

      <span class="mx-1 h-4 w-px bg-slate-200"></span>

      <el-breadcrumb separator="/" class="hidden sm:block">
        <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="item.path">
          <span :class="index === breadcrumbs.length - 1 ? 'font-medium text-slate-800' : 'text-slate-500'">
            {{ item.title }}
          </span>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="flex items-center gap-1">
      <span
        class="mr-2 hidden rounded-full border px-2 py-0.5 text-[11px] md:inline-block"
        :class="
          USE_MOCK
            ? 'border-amber-200 bg-amber-50 text-amber-600'
            : 'border-emerald-200 bg-emerald-50 text-emerald-600'
        "
      >
        {{ USE_MOCK ? 'Mock 数据' : '联调模式' }}
      </span>

      <button
        type="button"
        class="flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-800"
        @click="toggleFullscreen"
      >
        <el-icon :size="16"><FullScreen /></el-icon>
      </button>

      <el-popover placement="bottom-end" :width="300" trigger="click" popper-class="notice-popover">
        <template #reference>
          <button
            type="button"
            class="relative flex h-8 w-8 items-center justify-center rounded-lg text-slate-500 transition hover:bg-slate-100 hover:text-slate-800"
          >
            <el-icon :size="16"><Bell /></el-icon>
            <span class="absolute right-1.5 top-1.5 h-1.5 w-1.5 rounded-full bg-red-500"></span>
          </button>
        </template>
        <div class="max-h-[320px] overflow-y-auto">
          <p class="border-b border-slate-100 px-1 pb-2 text-[13px] font-medium text-slate-800">系统通知</p>
          <ul class="mt-1 divide-y divide-slate-50">
            <li v-for="notice in notices" :key="notice.id" class="cursor-pointer py-2.5 px-1">
              <p class="text-[12.5px] leading-snug text-slate-700">{{ notice.title }}</p>
              <p class="mt-1 text-[11px] text-slate-400">{{ notice.time }}</p>
            </li>
          </ul>
        </div>
      </el-popover>

      <span class="mx-1 h-4 w-px bg-slate-200"></span>

      <el-dropdown trigger="click" @command="handleCommand">
        <div class="flex cursor-pointer items-center gap-2 rounded-lg px-1.5 py-1 transition hover:bg-slate-100">
          <div
            class="flex h-7 w-7 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-blue-700 text-[12px] font-medium text-white"
          >
            {{ userStore.avatarText }}
          </div>
          <div class="hidden leading-tight sm:block">
            <p class="text-[12.5px] font-medium text-slate-800">{{ userStore.nickname }}</p>
            <p class="text-[10.5px] text-slate-400">
              {{ userStore.roles.includes('super_admin') ? '超级管理员' : '运营人员' }}
            </p>
          </div>
          <el-icon class="hidden text-[12px] text-slate-400 sm:block"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile" :icon="User">个人中心</el-dropdown-item>
            <el-dropdown-item command="password" :icon="User">修改密码</el-dropdown-item>
            <el-dropdown-item command="logout" :icon="SwitchButton" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>
