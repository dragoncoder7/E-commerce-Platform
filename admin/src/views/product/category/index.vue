<script setup lang="ts">
import CategoryManager from '@/components/CategoryManager.vue'
</script>

<template>
  <div class="space-y-4">
    <section class="rounded-xl bg-slate-900 px-6 py-5 text-white">
      <p class="text-[11px] tracking-widest text-blue-300">商品中心</p>
      <h2 class="mt-1 text-xl font-semibold">商品类别</h2>
      <p class="mt-2 text-xs text-slate-400">维护类别层级、排序与启用状态，新增商品时即可选择。</p>
    </section>
    <CategoryManager />
  </div>
</template>
