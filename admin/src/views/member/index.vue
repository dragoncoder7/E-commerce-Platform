<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { memberApi, type MemberRow } from '@/api/member'
import { useUserStore } from '@/stores/modules/user'

const user = useUserStore()
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })
const rows = ref<MemberRow[]>([])
const total = ref(0)
const loading = ref(false)
const pending = ref<number>()
const failed = ref(false)
async function load() {
  loading.value = true
  failed.value = false
  try { const result = await memberApi.list(query); rows.value = result.records; total.value = result.total }
  catch { failed.value = true }
  finally { loading.value = false }
}
async function change(row: MemberRow) {
  if (pending.value !== undefined) return
  const next = row.status === 1 ? 0 : 1
  try { await ElMessageBox.confirm(next ? '恢复此会员账号？会员需要重新登录。' : '禁用此会员并撤销所有商城会话？', '会员状态', { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }) }
  catch { return }
  pending.value = row.id
  try { await memberApi.status(row.id, next); ElMessage.success('会员状态已更新'); await load() }
  catch { /* 请求层提示失败，保持原状态。 */ }
  finally { pending.value = undefined }
}
function search() { query.pageNum = 1; void load() }
onMounted(load)
</script>
<template>
  <div class="app-card p-6"><div class="mb-5"><h1 class="page-title">商城会员</h1><p class="mt-2 text-sm text-slate-500">查询会员与账号状态，会员身份独立于后台人员。</p></div>
    <el-form inline @submit.prevent="search"><el-form-item label="搜索会员"><el-input v-model="query.keyword" placeholder="账号 / 手机 / 邮箱 / 昵称" clearable maxlength="100" /></el-form-item><el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部状态" style="width:140px"><el-option label="启用" :value="1" /><el-option label="禁用" :value="0" /></el-select></el-form-item><el-form-item><el-button type="primary" native-type="submit" :loading="loading">查询</el-button></el-form-item></el-form>
    <el-alert v-if="failed" title="会员加载失败，请重试" type="error" :closable="false"><el-button link @click="load">重新加载</el-button></el-alert>
    <el-table :data="rows" v-loading="loading"><el-table-column prop="id" label="ID" width="90" /><el-table-column prop="username" label="账号" min-width="160" /><el-table-column prop="nickname" label="昵称" min-width="120" /><el-table-column prop="phone" label="手机号" min-width="140" /><el-table-column prop="email" label="邮箱" min-width="210" /><el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '启用' : '禁用' }}</el-tag></template></el-table-column><el-table-column prop="createTime" label="注册时间" min-width="180" /><el-table-column v-if="user.hasPermission('members:write')" label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link :type="row.status ? 'danger' : 'primary'" :disabled="pending !== undefined" @click="change(row)">{{ row.status ? '禁用' : '启用' }}</el-button></template></el-table-column><template #empty><el-empty description="暂无符合条件的会员" /></template></el-table>
    <el-pagination class="mt-5" v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total" layout="total,prev,pager,next" @current-change="load" />
  </div>
</template>
