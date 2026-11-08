<script setup lang="ts">
import type { SidebarNode } from '@/types/menu'
import { resolveIcon } from '@/utils/icons'

defineOptions({ name: 'SidebarMenu' })

defineProps<{
  items: SidebarNode[]
}>()
</script>

<template>
  <template v-for="item in items" :key="item.path">
    <el-sub-menu v-if="item.children && item.children.length" :index="item.path">
      <template #title>
        <el-icon v-if="resolveIcon(item.icon)">
          <component :is="resolveIcon(item.icon)" />
        </el-icon>
        <span class="truncate">{{ item.title }}</span>
      </template>
      <SidebarMenu :items="item.children" />
    </el-sub-menu>

    <el-menu-item v-else :index="item.path">
      <el-icon v-if="resolveIcon(item.icon)">
        <component :is="resolveIcon(item.icon)" />
      </el-icon>
      <template #title>
        <span class="truncate">{{ item.title }}</span>
      </template>
    </el-menu-item>
  </template>
</template>
