<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { useMemberStore } from '@/stores/member'
import { memberApi } from '@/api/member'
import AddressBook from '@/components/AddressBook.vue'

const store = useMemberStore()
const router = useRouter()
const nickname = ref(store.member?.nickname ?? '')
const saving = ref(false)
const changing = ref(false)
const loggingOut = ref(false)
const error = ref('')
const password = reactive({ current: '', next: '', confirm: '' })
async function save() {
  if (!nickname.value.trim() || saving.value) return
  saving.value = true
  try { store.member = await memberApi.update(nickname.value.trim()); ElMessage.success('资料已保存') }
  catch (cause) { error.value = (cause as Error).message }
  finally { saving.value = false }
}
async function changePassword() {
  if (changing.value) return
  if (!password.current || password.next.length < 8 || !/[A-Za-z]/.test(password.next) || !/\d/.test(password.next) || password.next !== password.confirm) {
    error.value = '请填写原密码；新密码至少 8 位且含字母和数字，两次输入须一致'
    return
  }
  changing.value = true
  try { await memberApi.password(password.current, password.next); store.clear(); await router.replace('/login'); ElMessage.success('密码已更新，请重新登录') }
  catch (cause) { error.value = (cause as Error).message }
  finally { changing.value = false }
}
async function logout() {
  loggingOut.value = true
  try { await store.logout(); await router.replace('/'); ElMessage.success('已退出登录') }
  catch (cause) { error.value = (cause as Error).message }
  finally { loggingOut.value = false }
}
</script>
<template>
  <section class="account-page" v-if="store.member">
    <div class="section-heading"><div><span class="eyebrow">MY SHIGUANG</span><h1>个人中心</h1><p class="muted">你好，{{ store.member.nickname }}</p></div><el-button :loading="loggingOut" @click="logout">退出登录</el-button></div>
    <el-alert v-if="error" :title="error" type="error" show-icon @close="error = ''" />
    <div class="account-grid">
      <section class="panel"><h2>个人资料</h2><p class="muted">{{ store.member.username || store.member.email || store.member.phone }}</p><p v-if="store.member.phone" class="muted">手机号归属未验证</p>
        <el-form label-position="top" @submit.prevent="save"><el-form-item label="昵称"><el-input v-model="nickname" placeholder="请输入昵称" :maxlength="50" /></el-form-item><el-button type="primary" native-type="submit" :loading="saving" :disabled="!nickname.trim()">保存资料</el-button></el-form>
      </section>
      <section class="panel"><h2>修改密码</h2><p class="muted">修改后所有商城会话均需重新登录。</p><el-form label-position="top" @submit.prevent="changePassword">
        <el-form-item label="原密码"><el-input v-model="password.current" type="password" show-password autocomplete="current-password" /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="password.next" type="password" show-password autocomplete="new-password" :maxlength="64" /></el-form-item>
        <el-form-item label="确认新密码"><el-input v-model="password.confirm" type="password" show-password autocomplete="new-password" :maxlength="64" /></el-form-item>
        <el-button native-type="submit" :loading="changing">更新密码</el-button>
      </el-form></section>
    </div>
    <AddressBook />
  </section>
</template>
