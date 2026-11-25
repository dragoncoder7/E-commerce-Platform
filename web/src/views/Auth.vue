<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { memberApi, type Registration } from '@/api/member'
import { useMemberStore } from '@/stores/member'
import { safeReturnPath } from '@/router'

const route = useRoute()
const router = useRouter()
const member = useMemberStore()
const register = computed(() => route.path === '/register')
const form = ref<FormInstance>()
const busy = ref(false)
const sending = ref(false)
const error = ref('')
const seconds = ref(0)
const capabilities = ref({ emailRegistration: false, mailMode: 'disabled' })
const model = reactive({ type: 'account' as Registration['type'], account: '', password: '', confirm: '', code: '' })
const types: { key: Registration['type']; label: string }[] = [{ key: 'account', label: '账号注册' }, { key: 'phone', label: '手机注册' }, { key: 'email', label: '邮箱注册' }]
const label = computed(() => !register.value ? '账号 / 手机号 / 邮箱' : model.type === 'phone' ? '手机号' : model.type === 'email' ? '邮箱' : '账号')
const rules = computed<FormRules>(() => ({
  account: [{ required: true, message: `请输入${label.value}`, trigger: 'blur' }, { validator: (_rule, value: string, callback) => {
    if (!register.value) return callback()
    const pattern = model.type === 'phone' ? /^1[3-9]\d{9}$/ : model.type === 'email' ? /^[^\s@]+@[^\s@]+\.[^\s@]+$/ : /^[a-zA-Z][a-zA-Z0-9_]{3,31}$/
    callback(pattern.test(value.trim()) ? undefined : new Error(model.type === 'account' ? '4–32 位字母、数字或下划线，以字母开头' : `请输入有效${label.value}`))
  }, trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { validator: (_rule, value: string, callback) => {
    callback(!register.value || (value.length >= 8 && value.length <= 64 && /[a-zA-Z]/.test(value) && /\d/.test(value) && new TextEncoder().encode(value).length <= 72) ? undefined : new Error('8–64 位，须包含字母和数字，最多 72 字节'))
  }, trigger: 'blur' }],
  confirm: [{ validator: (_rule, value: string, callback) => callback(value === model.password ? undefined : new Error('两次输入的密码不一致')), trigger: 'blur' }],
  code: [{ required: true, pattern: /^\d{6}$/, message: '请输入 6 位验证码', trigger: 'blur' }],
}))
watch([register, () => model.type], () => { error.value = ''; model.password = ''; model.confirm = ''; model.code = ''; form.value?.clearValidate() })
const timer = window.setInterval(() => { if (seconds.value > 0) seconds.value-- }, 1000)
onUnmounted(() => window.clearInterval(timer))
onMounted(async () => { try { capabilities.value = await memberApi.capabilities() } catch { error.value = '注册配置加载失败，请刷新页面重试' } })
async function sendCode() {
  if (sending.value || seconds.value) return
  if (!await form.value?.validateField('account').catch(() => false)) return
  sending.value = true
  try { await memberApi.code(model.account.trim()); seconds.value = 60; ElMessage.success(capabilities.value.mailMode === 'capture' ? '验证码已写入本地测试邮件目录' : '验证码已发送，请查收邮箱') }
  catch (cause) { error.value = (cause as Error).message }
  finally { sending.value = false }
}
async function submit() {
  if (busy.value || !await form.value?.validate().catch(() => false)) return
  busy.value = true
  error.value = ''
  try {
    if (register.value) {
      await memberApi.register({ type: model.type, account: model.account.trim(), password: model.password, code: model.code })
      ElMessage.success('注册成功，请登录')
      await router.replace({ path: '/login', query: { redirect: route.query.redirect } })
    } else {
      await member.login(model.account.trim(), model.password)
      await router.replace(safeReturnPath(route.query.redirect))
      ElMessage.success('欢迎回来')
    }
  } catch (cause) { error.value = (cause as Error).message }
  finally { busy.value = false }
}
</script>
<template>
  <section class="auth-shell">
    <aside class="auth-story"><span class="eyebrow">A LITTLE EVERYDAY JOY</span><h1>遇见好物，<br>也遇见好生活。</h1><p>从一件用心挑选的日常好物开始，<br>收藏属于你的拾光。</p><router-link to="/products">先逛一逛 →</router-link></aside>
    <div class="auth-card">
      <span class="eyebrow">SHIGUANG MEMBER</span><h2>{{ register ? '创建商城账号' : '欢迎回来' }}</h2><p class="muted">{{ register ? '开启你的拾光日常' : '登录后管理你的个人资料' }}</p>
      <div v-if="register" class="auth-tabs" role="group" aria-label="注册方式"><button v-for="item in types" :key="item.key" type="button" :aria-pressed="model.type === item.key" :class="{ active: model.type === item.key }" @click="model.type = item.key; model.account = ''">{{ item.label }}</button></div>
      <el-alert v-if="register && model.type === 'email'" :closable="false" :type="capabilities.emailRegistration ? 'info' : 'warning'" :title="!capabilities.emailRegistration ? '邮箱注册暂未启用，请选择账号或手机号注册' : capabilities.mailMode === 'capture' ? '本地测试模式：验证码保存在服务端测试邮件目录，不发送真实邮件' : '请获取邮箱验证码后注册'" class="form-note" />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="form-note" />
      <el-form ref="form" :model="model" :rules="rules" label-position="top" size="large" @submit.prevent="submit">
        <el-form-item :label="label" prop="account"><el-input v-model="model.account" :placeholder="`请输入${label}`" autocomplete="username" :maxlength="254" /></el-form-item>
        <el-form-item label="密码" prop="password"><el-input v-model="model.password" type="password" show-password :autocomplete="register ? 'new-password' : 'current-password'" placeholder="请输入密码" :maxlength="64" /></el-form-item>
        <template v-if="register">
          <el-form-item label="确认密码" prop="confirm"><el-input v-model="model.confirm" type="password" show-password autocomplete="new-password" placeholder="请再次输入密码" :maxlength="64" /></el-form-item>
          <el-form-item v-if="model.type === 'email'" label="邮箱验证码" prop="code"><div class="code-input"><el-input v-model="model.code" placeholder="6 位验证码" inputmode="numeric" autocomplete="one-time-code" :maxlength="6" /><el-button :loading="sending" :disabled="seconds > 0 || !capabilities.emailRegistration" @click="sendCode">{{ seconds ? `${seconds} 秒后重发` : '获取验证码' }}</el-button></div></el-form-item>
          <p v-if="model.type === 'phone'" class="muted">手机号用于密码登录，本次注册不验证手机号归属。</p>
        </template>
        <el-button native-type="submit" type="primary" :loading="busy" :disabled="register && model.type === 'email' && !capabilities.emailRegistration" class="auth-submit">{{ register ? '注册账号' : '登录商城' }}</el-button>
      </el-form>
      <p class="auth-switch">{{ register ? '已有账号？' : '还没有账号？' }} <router-link :to="{ path: register ? '/login' : '/register', query: { redirect: route.query.redirect } }">{{ register ? '去登录' : '立即注册' }}</router-link></p>
    </div>
  </section>
</template>
