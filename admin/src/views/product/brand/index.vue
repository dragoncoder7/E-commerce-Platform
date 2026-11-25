<script setup lang="ts">
import { useUserStore } from '@/stores/modules/user'
const permissionUser = useUserStore()
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
interface Brand { id?: number; brandName: string; brandCode: string; logo: string; description: string; sortOrder: number; status: number }
const rows = ref<Brand[]>([])
const total = ref(0)
const pageNum = ref(1)
const keyword = ref('')
const visible = ref(false)
const busy = ref(false)
const form = reactive<Brand>({ brandName: '', brandCode: '', logo: '', description: '', sortOrder: 0, status: 1 })
async function load() { busy.value = true; try { const p = await http.get<{records: Brand[]; total: number}>('/product/brand/list', { pageNum: pageNum.value, pageSize: 10, brandName: keyword.value }); rows.value = p.records; total.value = p.total } catch {} finally { busy.value = false } }
function edit(row?: Brand) { Object.assign(form, { id: undefined, brandName: '', brandCode: '', logo: '', description: '', sortOrder: 0, status: 1 }, row); visible.value = true }
async function save() { if (!form.brandName.trim() || !form.brandCode.trim()) { ElMessage.warning('请填写品牌名称和编码'); return } busy.value = true; try { if (form.id) await http.put('/product/brand', form); else await http.post('/product/brand', form); visible.value = false; await load() } catch {} finally { busy.value = false } }
async function remove(row: Brand) { try { await ElMessageBox.confirm(`确认删除品牌 ${row.brandName}？`, '删除品牌'); await http.delete(`/product/brand/${row.id}`); await load() } catch {} }
onMounted(load)
</script>
<template><div class="app-card p-6"><div class="mb-5 flex justify-between"><h1 class="page-title">品牌管理</h1><el-button v-if="permissionUser.hasPermission('product:write')" type="primary" @click="edit()">新增品牌</el-button></div><div class="mb-4 flex gap-3"><el-input v-model="keyword" placeholder="搜索品牌" clearable style="max-width:300px" @keyup.enter="pageNum=1;load()" /><el-button @click="pageNum=1;load()">查询</el-button></div><el-table :data="rows" v-loading="busy"><el-table-column prop="brandName" label="品牌名称" /><el-table-column prop="brandCode" label="编码" /><el-table-column prop="description" label="介绍" /><el-table-column label="状态"><template #default="{row}">{{ row.status ? '启用' : '停用' }}</template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button v-if="permissionUser.hasPermission('product:write')" link type="primary" @click="edit(row)">编辑</el-button><el-button v-if="permissionUser.hasPermission('product:write')" link type="danger" @click="remove(row)">删除</el-button></template></el-table-column></el-table><el-pagination v-model:current-page="pageNum" :page-size="10" :total="total" layout="prev,pager,next" @current-change="load" /><el-dialog v-model="visible" title="品牌信息" width="min(520px,94vw)"><el-form label-position="top"><el-form-item label="品牌名称" required><el-input v-model="form.brandName" maxlength="100" /></el-form-item><el-form-item label="品牌编码" required><el-input v-model="form.brandCode" maxlength="50" /></el-form-item><el-form-item label="介绍"><el-input v-model="form.description" type="textarea" maxlength="500" /></el-form-item><el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item><el-form-item label="启用"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="busy" @click="save">保存</el-button></template></el-dialog></div></template>
