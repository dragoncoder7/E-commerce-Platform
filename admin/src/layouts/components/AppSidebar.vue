<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/modules/app'
import { usePermissionStore } from '@/stores/modules/permission'
import { APP_TITLE, APP_VERSION, USE_MOCK } from '@/config'
import SidebarMenu from './SidebarMenu.vue'

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

const collapsed = computed(() => appStore.sidebarCollapsed)
const menus = computed(() => permissionStore.sidebarMenus)
const activeMenu = computed(() => route.path)
</script>

<template>
  <aside
    class="relative z-20 flex shrink-0 flex-col overflow-hidden bg-[#0b1220] transition-[width] duration-200 ease-out"
    :class="collapsed ? 'w-[68px]' : 'w-[226px]'"
  >
    <!-- 品牌 -->
    <div class="flex h-14 shrink-0 items-center gap-2.5 px-4">
      <div
        class="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-blue-500 to-blue-700 shadow-lg shadow-blue-950/60"
      >
        <svg width="18" height="18" viewBox="0 0 32 32" aria-hidden="true">
          <path d="M9 21.5 16 9l7 12.5H9Z" fill="#fff" fill-opacity="0.95" />
        </svg>
      </div>
      <div v-show="!collapsed" class="min-w-0">
        <p class="truncate text-[14px] font-semibold tracking-wide text-white">{{ APP_TITLE }}</p>
        <p class="mt-0.5 text-[10px] tracking-[0.18em] text-slate-500">ERP CONSOLE</p>
      </div>
    </div>

    <!-- 菜单 -->
    <el-scrollbar class="min-h-0 flex-1">
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
        unique-opened
        class="sidebar-menu"
      >
        <SidebarMenu :items="menus" />
      </el-menu>
    </el-scrollbar>

    <!-- 底部信息 -->
    <div class="shrink-0 border-t border-white/5 px-4 py-3">
      <div v-show="!collapsed" class="flex items-center justify-between text-[11px]">
        <span class="text-slate-600">v{{ APP_VERSION }}</span>
        <span class="rounded-full bg-white/5 px-2 py-0.5 text-[10px] tracking-wide text-slate-500">
          {{ USE_MOCK ? 'MOCK' : 'API' }}
        </span>
      </div>
      <div v-show="collapsed" class="text-center text-[10px] text-slate-600">v{{ APP_VERSION }}</div>
    </div>
  </aside>
</template>
