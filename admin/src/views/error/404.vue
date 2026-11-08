<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Compass, HomeFilled } from '@element-plus/icons-vue'

const router = useRouter()

function goHome(): void {
  void router.replace('/')
}

function goBack(): void {
  if (window.history.length > 1) router.back()
  else goHome()
}
</script>

<template>
  <div class="flex min-h-[calc(100vh-140px)] flex-col items-center justify-center px-6 py-8">
    <div class="relative select-none">
      <p class="text-[104px] font-semibold leading-none tracking-tighter text-slate-200">404</p>
      <el-icon class="absolute -right-7 top-3 text-[26px] text-blue-300"><Compass /></el-icon>
    </div>

    <h1 class="mt-3 text-[19px] font-semibold text-slate-800">页面走丢了</h1>
    <p class="mt-2 max-w-[430px] text-center text-[13px] leading-relaxed text-slate-500">
      当前地址没有匹配的菜单或路由。可能是菜单被停用、被删除，或者组件路径填写有误。
      可以回到工作台，或在「菜单管理」中检查对应配置。
    </p>

    <div class="mt-6 flex items-center gap-3">
      <el-button type="primary" :icon="HomeFilled" @click="goHome">返回工作台</el-button>
      <el-button @click="goBack">返回上一页</el-button>
    </div>
  </div>
</template>
