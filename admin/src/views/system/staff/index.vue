<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { rbacApi, type RoleRow, type StaffInput, type StaffRow } from '@/api/rbac'
import { useUserStore } from '@/stores/modules/user'
const user = useUserStore()
const rows = ref<StaffRow[]>([])
const roles = ref<RoleRow[]>([])
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const failed = ref(false)
const visible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<StaffInput>({ username: '', nickname: '', password: '', status: 1, roleIds: [] })
const rules: FormRules = { username: [{ required: true, pattern: /^[A-Za-z][A-Za-z0-9_]{3,49}$/, message: '账号4–50位，字母开头，只含字母数字下划线' }], nickname: [{ required: true, whitespace: true, message: '请输入姓名或昵称' }], roleIds: [{ required: true, type: 'array', min: 1, message: '至少选择一个角色' }], password: [{ validator: (_rule, value: string, callback) => { if (editingId.value && !value) return callback(); callback(value && value.length >= 8 && /[A-Za-z]/.test(value) && /[0-9]/.test(value) ? undefined : new Error('密码至少8位，包含字母和数字')) } }] }
async function load() { loading.value = true; failed.value = false; try { const data = await rbacApi.staff(query); rows.value = data.records; total.value = data.total } catch { failed.value = true } finally { loading.value = false } }
async function edit(row?: StaffRow) {
  try { roles.value = await rbacApi.roles() } catch { return }
  editingId.value = row?.id
  Object.assign(form, row ? { username: row.username, nickname: row.nickname, status: row.status, roleIds: [...row.roleIds], password: '' } : { username: '', nickname: '', password: '', status: 1, roleIds: [] })
  visible.value = true
}
async function save() {
  if (saving.value || !await formRef.value?.validate().catch(() => false)) return
  saving.value = true
  try { await rbacApi.saveStaff({ ...form }, editingId.value); visible.value = false; ElMessage.success('人员已保存，已有会话需重新登录'); await load() } catch { /* 统一反馈 */ } finally { saving.value = false }
}
async function remove(row: StaffRow) {
  try { await ElMessageBox.confirm(`删除后台人员 ${row.username}？`, '删除人员', { type: 'warning' }) } catch { return }
  try { await rbacApi.deleteStaff(row.id); ElMessage.success('人员已删除'); await load() } catch { /* 统一反馈 */ }
}
onMounted(load)
</script>
<template>
  <div class="app-card p-6"><div class="mb-5 flex justify-between"><h1 class="page-title">后台人员</h1><el-button v-if="user.hasPermission('staff:write')" type="primary" @click="edit()">新增人员</el-button></div>
    <el-form inline @submit.prevent="query.pageNum = 1; load()"><el-form-item label="账号或昵称"><el-input v-model="query.keyword" clearable maxlength="100" /></el-form-item><el-form-item><el-button native-type="submit" :loading="loading">查询</el-button></el-form-item></el-form>
    <el-alert v-if="failed" title="人员加载失败，请重试" type="error" :closable="false"><el-button link @click="load">重试</el-button></el-alert>
    <el-table :data="rows" v-loading="loading"><el-table-column prop="username" label="账号" min-width="180" /><el-table-column prop="nickname" label="姓名/昵称" min-width="150" /><el-table-column label="状态" width="100"><template #default="{ row }">{{ row.status ? '启用' : '禁用' }}</template></el-table-column><el-table-column prop="createTime" label="创建时间" min-width="180" /><el-table-column v-if="user.hasPermission('staff:write')" label="操作" width="160" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="danger" :disabled="row.id === user.userInfo?.id" @click="remove(row)">删除</el-button></template></el-table-column></el-table>
    <el-pagination class="mt-5" v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total" layout="total,prev,pager,next" @current-change="load" />
    <el-dialog v-model="visible" :title="editingId ? '编辑后台人员' : '新增后台人员'" width="min(560px,94vw)" :close-on-click-modal="false" :show-close="!saving" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="save"><el-form-item label="账号" prop="username"><el-input v-model="form.username" maxlength="50" autocomplete="off" /></el-form-item><el-form-item label="姓名/昵称" prop="nickname"><el-input v-model="form.nickname" maxlength="50" /></el-form-item><el-form-item :label="editingId ? '新密码（留空保持原密码）' : '密码'" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" maxlength="64" /></el-form-item><el-form-item label="角色" prop="roleIds"><el-select v-model="form.roleIds" multiple placeholder="选择启用角色" style="width:100%"><el-option v-for="role in roles" :key="role.id" :value="role.id" :label="role.name" :disabled="!role.status" /></el-select></el-form-item><el-form-item label="启用"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item><el-button type="primary" native-type="submit" :loading="saving">保存人员</el-button></el-form>
    </el-dialog>
  </div>
</template>
