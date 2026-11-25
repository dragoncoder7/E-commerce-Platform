<script setup lang="ts">
import { useUserStore } from '@/stores/modules/user'
import { useRouter } from 'vue-router'
const user = useUserStore()
const router = useRouter()
async function logout() { await user.logout(); await router.replace('/login') }
</script>
<template><el-result icon="warning" title="暂无访问权限" sub-title="请联系管理员分配所需菜单与操作权限"><template #extra><el-button @click="logout">退出登录</el-button></template></el-result></template>
