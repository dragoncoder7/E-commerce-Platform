<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/modules/user'
import { APP_SUBTITLE, APP_TITLE, APP_VERSION, COPYRIGHT, DEMO_ACCOUNT, USE_MOCK } from '@/config'
import { getRememberedAccount } from '@/utils/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  remember: false,
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' },
  ],
}

const features = [
  { title: '动态路由权限', desc: '菜单即路由，配置完立即生效' },
  { title: '一致的中台体验', desc: 'Element Plus + Tailwind 统一视觉' },
  { title: '容器化交付', desc: 'Docker 一键构建与部署' },
]

/** 仅允许站内路径，避免开放重定向 */
function resolveRedirect(): string {
  const { redirect } = route.query
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    return redirect
  }
  return '/'
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({
      username: form.username.trim(),
      password: form.password,
      remember: form.remember,
    })
    ElMessage.success('登录成功，正在进入系统')
    await router.replace(resolveRedirect())
  } catch {
    // 失败提示已在请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function fillDemoAccount(): void {
  form.username = DEMO_ACCOUNT.username
  form.password = DEMO_ACCOUNT.password
}

onMounted(() => {
  const remembered = getRememberedAccount()
  if (remembered) {
    form.username = remembered
    form.remember = true
  }
})
</script>

<template>
  <div class="min-h-screen bg-slate-950 lg:grid lg:grid-cols-[1.05fr_1fr]">
    <!-- 品牌宣传区 -->
    <section class="relative hidden flex-col justify-between overflow-hidden px-16 py-14 lg:flex">
      <div class="brand-glow pointer-events-none absolute inset-0"></div>
      <div class="brand-grid pointer-events-none absolute inset-0 opacity-[0.18]"></div>

      <header class="relative z-10 flex items-center gap-3">
        <div
          class="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-blue-500 to-blue-700 shadow-lg shadow-blue-900/50"
        >
          <svg width="22" height="22" viewBox="0 0 32 32" aria-hidden="true">
            <path d="M9 21.5 16 9l7 12.5H9Z" fill="#fff" fill-opacity="0.95" />
          </svg>
        </div>
        <div>
          <p class="text-[17px] font-semibold tracking-wide text-white">{{ APP_TITLE }}</p>
          <p class="text-[11px] tracking-[0.22em] text-slate-400">{{ APP_SUBTITLE }}</p>
        </div>
      </header>

      <div class="relative z-10 max-w-[520px]">
        <h1 class="text-[38px] font-semibold leading-[1.25] tracking-tight text-white">
          让电商经营<br />
          回到一条<span class="text-blue-400">清晰的流水线</span>
        </h1>
        <p class="mt-5 max-w-[440px] text-[14px] leading-relaxed text-slate-400">
          订单、商品、库存、结算，全链路收敛到一个后台。按角色下发菜单，按菜单生成路由，权限与页面始终一致。
        </p>

        <ul class="mt-10 space-y-5">
          <li v-for="(item, index) in features" :key="item.title" class="flex gap-4">
            <span
              class="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg border border-white/10 bg-white/5 text-[11px] font-medium text-blue-300"
            >
              {{ String(index + 1).padStart(2, '0') }}
            </span>
            <div>
              <p class="text-[13px] font-medium text-slate-100">{{ item.title }}</p>
              <p class="mt-1 text-[12px] text-slate-500">{{ item.desc }}</p>
            </div>
          </li>
        </ul>
      </div>

      <footer class="relative z-10 flex items-center justify-between text-[11px] text-slate-600">
        <span>{{ COPYRIGHT }}</span>
        <span class="rounded-full border border-white/10 px-2.5 py-1 tracking-wider">v{{ APP_VERSION }}</span>
      </footer>
    </section>

    <!-- 登录表单区 -->
    <section class="flex min-h-screen items-center justify-center bg-slate-50 px-5 py-12 lg:min-h-0">
      <div class="w-full max-w-[404px]">
        <div class="mb-8 flex items-center gap-2.5 lg:hidden">
          <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-gradient-to-br from-blue-500 to-blue-700">
            <svg width="18" height="18" viewBox="0 0 32 32" aria-hidden="true">
              <path d="M9 21.5 16 9l7 12.5H9Z" fill="#fff" fill-opacity="0.95" />
            </svg>
          </div>
          <span class="text-[15px] font-semibold text-slate-800">{{ APP_TITLE }}</span>
        </div>

        <div class="rounded-2xl border border-slate-200/80 bg-white p-8 shadow-[0_18px_40px_-28px_rgba(15,23,42,0.35)]">
          <h2 class="text-[22px] font-semibold tracking-tight text-slate-900">欢迎回来</h2>
          <p class="mt-1.5 text-[13px] text-slate-500">登录 {{ APP_TITLE }} 管理工作台</p>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            class="mt-7"
            @submit.prevent
          >
            <el-form-item label="账号" prop="username">
              <el-input
                v-model="form.username"
                size="large"
                :prefix-icon="User"
                placeholder="请输入账号"
                autocomplete="username"
                clearable
              />
            </el-form-item>

            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                size="large"
                :prefix-icon="Lock"
                placeholder="请输入密码"
                autocomplete="current-password"
                show-password
                @keyup.enter="handleSubmit"
              />
            </el-form-item>

            <div class="mb-5 flex items-center justify-between">
              <el-checkbox v-model="form.remember" size="default">记住账号</el-checkbox>
              <span class="cursor-pointer text-[12px] text-blue-600 hover:text-blue-700">忘记密码？</span>
            </div>

            <el-button
              type="primary"
              size="large"
              class="h-11 w-full rounded-lg text-[15px] font-medium"
              :loading="loading"
              @click="handleSubmit"
            >
              <span v-if="!loading" class="flex items-center gap-1.5">
                登 录
                <el-icon class="text-[13px]"><ArrowRight /></el-icon>
              </span>
              <span v-else>登录中…</span>
            </el-button>
          </el-form>

          <div v-if="USE_MOCK" class="mt-6 rounded-xl border border-dashed border-slate-200 bg-slate-50/80 p-3.5">
            <div class="flex items-center justify-between">
              <p class="text-[12px] text-slate-500">
                演示账号
                <span class="ml-1 font-mono text-slate-700">{{ DEMO_ACCOUNT.username }}</span>
                <span class="mx-1 text-slate-300">/</span>
                <span class="font-mono text-slate-700">{{ DEMO_ACCOUNT.password }}</span>
              </p>
              <button
                type="button"
                class="text-[12px] font-medium text-blue-600 transition hover:text-blue-700"
                @click="fillDemoAccount"
              >
                一键填充
              </button>
            </div>
          </div>
        </div>

        <p class="mt-6 text-center text-[11px] text-slate-400">
          登录即代表同意《服务条款》与《隐私政策》
        </p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.brand-glow {
  background:
    radial-gradient(circle at 18% 12%, rgba(37, 99, 235, 0.5), transparent 55%),
    radial-gradient(circle at 82% 78%, rgba(14, 165, 233, 0.32), transparent 52%),
    linear-gradient(160deg, #020617 0%, #0b1220 45%, #0f172a 100%);
}

.brand-grid {
  background-image:
    linear-gradient(to right, rgba(148, 163, 184, 0.28) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(148, 163, 184, 0.28) 1px, transparent 1px);
  background-size: 56px 56px;
  mask-image: radial-gradient(circle at 30% 30%, #000 0%, transparent 72%);
}
</style>
