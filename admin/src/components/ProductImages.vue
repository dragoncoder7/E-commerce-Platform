<script setup lang="ts">
import { useUserStore } from '@/stores/modules/user'
const permissionUser = useUserStore()
import { computed, onMounted, ref } from 'vue'
const canEdit = computed(() => permissionUser.hasPermission('product:write'))
const canUpload = computed(() => canEdit.value && permissionUser.hasPermission('upload:write'))
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
const props = defineProps<{ productId: number }>()
const urls = ref<string[]>([])
const pending = ref('')
const busy = ref(false)
const error = ref('')
async function load() {
  busy.value = true
  error.value = ''
  try { urls.value = await http.get<string[]>(`/product/${props.productId}/images`) }
  catch { error.value = '图片加载失败，请重试' }
  finally { busy.value = false }
}
function add() {
  const value = pending.value.trim()
  if (!/^https?:\/\//i.test(value) || urls.value.length >= 9 || urls.value.includes(value)) {
    ElMessage.warning('请输入不同的 HTTP/HTTPS 图片地址，最多 9 张')
    return
  }
  urls.value.push(value)
  pending.value = ''
}
async function upload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024 || urls.value.length >= 9) { ElMessage.warning('图片最多 5MB，共不超过 9 张'); input.value = ''; return }
  const data = new FormData()
  data.append('file', file)
  busy.value = true
  try { urls.value.push(await http.post<string>('/upload/image', data)) }
  catch { /* 请求层显示失败原因 */ }
  finally { busy.value = false; input.value = '' }
}
async function save() {
  busy.value = true
  try { await http.post(`/product/${props.productId}/images`, urls.value); ElMessage.success('商品轮播图片已保存') }
  catch { /* 保留编辑内容以便重试 */ }
  finally { busy.value = false }
}
onMounted(load)
</script>
<template>
  <div v-loading="busy">
    <el-alert v-if="error" :title="error" type="error" :closable="false"><el-button link @click="load">重试</el-button></el-alert>
    <p class="mb-4 text-sm text-slate-500">图片按下方顺序展示，支持 JPEG / PNG，每张不超过 5MB，最多 9 张。</p>
    <div class="mb-5 grid grid-cols-2 gap-4 sm:grid-cols-3">
      <div v-for="(url, index) in urls" :key="url" class="rounded-lg border p-3">
        <el-image :src="url" fit="contain" class="h-28 w-full" />
        <div v-if="canEdit" class="mt-2 flex justify-between"><el-button link :disabled="index === 0 || busy" @click="[urls[index - 1], urls[index]] = [urls[index]!, urls[index - 1]! ]">前移</el-button><el-button link type="danger" :disabled="busy" @click="urls.splice(index, 1)">移除</el-button></div>
      </div>
    </div>
    <label v-if="canUpload" class="mb-4 block text-sm">上传图片 <input type="file" accept="image/jpeg,image/png" :disabled="busy || urls.length >= 9" @change="upload" /></label>
    <div v-if="canEdit" class="flex gap-2"><el-input v-model="pending" placeholder="或输入外部图片地址 https://..." maxlength="500" /><el-button v-if="canEdit" :disabled="busy" @click="add">添加地址</el-button></div>
    <el-button v-if="canEdit" class="mt-5" type="primary" :loading="busy" @click="save">保存轮播图片</el-button>
  </div>
</template>
