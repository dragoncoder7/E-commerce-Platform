<script setup lang="ts">
import { ref } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useRoute, useRouter } from 'vue-router'
import { useMemberStore } from '@/stores/member'
const search = ref('')
const router = useRouter()
const route = useRoute()
const member = useMemberStore()
</script>
<template>
  <el-config-provider :locale="zhCn">
  <div class="announcement">用心挑选，让好物走进日常。</div>
  <header class="header"><div class="container header-inner">
    <router-link class="logo" to="/"><span class="logo-mark">拾</span> 拾光 <small>SHIGUANG</small></router-link>
    <nav><router-link to="/">首页</router-link><router-link to="/products">全部好物</router-link></nav>
    <form class="search" @submit.prevent="router.push({ path: '/products', query: { keyword: search } })"><el-input v-model="search" aria-label="搜索商品" placeholder="寻找心仪好物" clearable /><el-button native-type="submit">搜索</el-button></form>
    <div class="member-links"><router-link v-if="member.member" to="/account">个人中心</router-link><template v-else><router-link :to="{ path: '/login', query: { redirect: route.fullPath } }">登录</router-link><span>/</span><router-link :to="{ path: '/register', query: { redirect: route.fullPath } }">注册</router-link></template></div>
  </div></header>
  <main class="container"><router-view /></main>
  <footer class="container footer"><div><b>拾光 SHIGUANG</b><p>简单选择，认真生活。</p></div><span>品质好物 · 清晰价格 · 用心服务</span></footer>
  </el-config-provider>
</template>
