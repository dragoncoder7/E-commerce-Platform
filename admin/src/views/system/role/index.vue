<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { rbacApi, type RoleInput, type RoleRow } from '@/api/rbac'
import { listMenusApi } from '@/api/menu'
import type { MenuItem } from '@/types/menu'
import { useUserStore } from '@/stores/modules/user'
const user = useUserStore()
const rows = ref<RoleRow[]>([])
const menus = ref<MenuItem[]>([])
const loading = ref(false)
const saving = ref(false)
const failed = ref(false)
const visible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const form = reactive<RoleInput>({ name: '', code: '', status: 1, sort: 0, remark: '', menuIds: [] })
const rules: FormRules = { name: [{ required: true, whitespace: true, message: '请输入角色名称' }], code: [{ required: true, pattern: /^[a-z][a-z0-9_]{1,49}$/, message: '编码2–50位，小写字母开头' }] }
async function load() { loading.value = true; failed.value = false; try { rows.value = await rbacApi.roles() } catch { failed.value = true } finally { loading.value = false } }
async function edit(row?: RoleRow) {
  try { menus.value = await listMenusApi() } catch { return }
  editingId.value = row?.id
  Object.assign(form, row ? { name: row.name, code: row.code, status: row.status, sort: row.sort, remark: row.remark || '', menuIds: [...row.menuIds] } : { name: '', code: '', status: 1, sort: 0, remark: '', menuIds: [] })
  visible.value = true
}
async function save() {
  if (saving.value || !await formRef.value?.validate().catch(() => false)) return
  saving.value = true
  try { await rbacApi.saveRole({ ...form }, editingId.value); visible.value = false; ElMessage.success('角色授权已保存，接口权限立即生效'); await load() } catch { /* 统一反馈 */ } finally { saving.value = false }
}
async function remove(row: RoleRow) {
  try { await ElMessageBox.confirm(`删除角色 ${row.name}？`, '删除角色', { type: 'warning' }) } catch { return }
  try { await rbacApi.deleteRole(row.id); ElMessage.success('角色已删除'); await load() } catch { /* 统一反馈 */ }
}
onMounted(load)
</script>
<template>
  <div class="app-card p-6"><div class="mb-5 flex justify-between"><h1 class="page-title">角色权限</h1><el-button v-if="user.hasPermission('roles:write')" type="primary" @click="edit()">新增角色</el-button></div><el-alert v-if="failed" title="角色加载失败" type="error" :closable="false"><el-button link @click="load">重试</el-button></el-alert>
    <el-table :data="rows" v-loading="loading"><el-table-column prop="name" label="角色名称" min-width="160" /><el-table-column prop="code" label="编码" min-width="160" /><el-table-column label="状态" width="100"><template #default="{ row }">{{ row.status ? '启用' : '停用' }}</template></el-table-column><el-table-column prop="remark" label="备注" min-width="220" /><el-table-column v-if="user.hasPermission('roles:write')" label="操作" width="160" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="edit(row)">编辑授权</el-button><el-button link type="danger" :disabled="row.code === 'admin'" @click="remove(row)">删除</el-button></template></el-table-column></el-table>
    <el-dialog v-model="visible" :title="editingId ? '编辑角色权限' : '新增角色'" width="min(620px,94vw)" :close-on-click-modal="false" :show-close="!saving" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="save"><el-form-item label="角色名称" prop="name"><el-input v-model="form.name" maxlength="50" /></el-form-item><el-form-item label="角色编码" prop="code"><el-input v-model="form.code" maxlength="50" :disabled="form.code === 'admin' && !!editingId" /></el-form-item><el-form-item label="菜单与按钮权限"><el-tree-select v-model="form.menuIds" :data="menus" node-key="id" :props="{ label: 'title', value: 'id', children: 'children' }" multiple show-checkbox check-strictly :render-after-expand="false" style="width:100%" placeholder="选择可访问菜单和操作权限" /></el-form-item><p class="mb-4 text-xs text-slate-500">目录和子菜单分别授权；菜单控制访问入口，按钮权限控制接口操作。管理员具有全部权限。</p><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item><el-form-item label="启用"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" :disabled="form.code === 'admin'" /></el-form-item><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" maxlength="500" /></el-form-item><el-button type="primary" native-type="submit" :loading="saving">保存角色</el-button></el-form>
    </el-dialog>
  </div>
</template>
