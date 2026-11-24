<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { deleteCategory, listCategories, saveCategory, type CategoryForm, type ProductCategory } from '@/api/category'
import { categoryEnabled, categoryTree, type CategoryTree } from '@/utils/category'

const emit = defineEmits<{ changed: [id?: number] }>()
const rows = ref<ProductCategory[]>([])
const loading = ref(false)
const error = ref('')
const keyword = ref('')
const status = ref<number>()
const dialog = ref(false)
const saving = ref(false)
const busy = ref(false)
const formRef = ref<FormInstance>()
const emptyForm = (): CategoryForm => ({ parentId: 0, categoryName: '', categoryCode: '', sortOrder: 0, description: '', status: 1 })
const form = reactive<CategoryForm>(emptyForm())
const rules: FormRules<CategoryForm> = {
  categoryName: [{ required: true, whitespace: true, message: '请输入类别名称', trigger: 'blur' }],
  categoryCode: [{ required: true, whitespace: true, message: '请输入唯一类别编号', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级类别', trigger: 'change' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
}
const tree = computed(() => categoryTree(rows.value))
const filteredTree = computed(() => {
  const match = keyword.value.trim().toLowerCase()
  const filter = (nodes: CategoryTree<ProductCategory>[]): CategoryTree<ProductCategory>[] => nodes.flatMap((node) => {
    const children = filter(node.children ?? [])
    const matches = (!match || `${node.categoryName} ${node.categoryCode}`.toLowerCase().includes(match))
      && (status.value === undefined || node.status === status.value)
    return matches || children.length ? [{ ...node, children: children.length ? children : undefined }] : []
  })
  return filter(tree.value)
})
const parentOptions = computed(() => {
  const blocked = new Set<number>(form.id ? [form.id] : [])
  let changed = true
  while (changed) {
    changed = false
    rows.value.forEach((row) => {
      if (blocked.has(row.parentId) && !blocked.has(row.id)) { blocked.add(row.id); changed = true }
    })
  }
  return [{ id: 0, categoryName: '顶级类别', children: categoryTree(rows.value.filter((row) => !blocked.has(row.id))) }]
})

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try { rows.value = await listCategories() }
  catch (e) { error.value = e instanceof Error ? e.message : '类别加载失败，请重试' }
  finally { loading.value = false }
}

async function edit(row?: ProductCategory, parentId = 0): Promise<void> {
  Object.assign(form, emptyForm(), { id: undefined, parentId }, row ? {
    id: row.id, parentId: row.parentId, categoryName: row.categoryName, categoryCode: row.categoryCode,
    sortOrder: row.sortOrder, description: row.description ?? '', status: row.status,
  } : {})
  dialog.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function save(): Promise<void> {
  if (saving.value || !await formRef.value?.validate().catch(() => false)) return
  saving.value = true
  try {
    const saved = await saveCategory({ ...form, categoryName: form.categoryName.trim(), categoryCode: form.categoryCode.trim() })
    dialog.value = false
    ElMessage.success(form.id ? '类别已更新' : '类别已创建')
    emit('changed', saved.id)
    await load()
  } catch { /* 请求层提示，保留输入 */ }
  finally { saving.value = false }
}

async function toggle(row: ProductCategory): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认${row.status === 1 ? '停用' : '启用'}「${row.categoryName}」？`, '修改类别状态', {
      type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消',
    })
  } catch { return }
  busy.value = true
  try {
    await saveCategory({ ...row, status: row.status === 1 ? 0 : 1 })
    ElMessage.success('类别状态已更新')
    emit('changed')
    await load()
  } catch { /* 请求层已提示 */ }
  finally { busy.value = false }
}

async function remove(row: ProductCategory): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除「${row.categoryName}」？有子类别或关联商品的类别不能删除。`, '删除类别', {
      type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消',
    })
  } catch { return }
  busy.value = true
  try {
    await deleteCategory(row.id)
    ElMessage.success('类别已删除')
    emit('changed')
    await load()
  } catch { /* 请求层已提示 */ }
  finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <section class="app-card overflow-hidden">
    <div class="flex flex-wrap items-center gap-3 border-b border-slate-100 p-4">
      <el-input v-model="keyword" clearable placeholder="搜索类别名称或编号" aria-label="搜索类别" class="!w-60" />
      <el-select v-model="status" clearable placeholder="全部状态" aria-label="类别状态" class="!w-32" @clear="status = undefined">
        <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
      </el-select>
      <el-button :icon="Refresh" :loading="loading" :disabled="busy" @click="load">刷新</el-button>
      <el-button type="primary" :icon="Plus" :disabled="loading || busy || !!error" @click="edit()">新增类别</el-button>
      <span class="text-xs text-slate-400">共 {{ rows.length }} 个类别</span>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon><el-button link @click="load">重试</el-button></el-alert>
    <el-table v-loading="loading" :data="filteredTree" row-key="id" default-expand-all>
      <el-table-column prop="categoryName" label="类别名称" min-width="200" />
      <el-table-column prop="categoryCode" label="类别编号" min-width="150" />
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="270" fixed="right"><template #default="{ row }">
        <el-button link type="primary" :disabled="busy || loading || !!error || !categoryEnabled(row, rows)" @click="edit(undefined, row.id)">新增子类</el-button>
        <el-button link type="primary" :disabled="busy || loading || !!error" @click="edit(row)">编辑</el-button>
        <el-button link type="warning" :disabled="busy || loading || !!error" @click="toggle(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
        <el-button link type="danger" :disabled="busy || loading || !!error" @click="remove(row)">删除</el-button>
      </template></el-table-column>
      <template #empty><el-empty :image-size="70" :description="error ? '类别加载失败，请重试' : rows.length ? '没有符合条件的类别' : '暂无类别，点击新增类别开始配置'" /></template>
    </el-table>
    <el-dialog v-model="dialog" :title="form.id ? '编辑类别' : '新增类别'" width="min(520px, 94vw)" append-to-body destroy-on-close :close-on-click-modal="false" :show-close="!saving" :close-on-press-escape="!saving">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="saving">
        <el-form-item label="上级类别" prop="parentId"><el-tree-select v-model="form.parentId" :data="parentOptions" :props="{ label: 'categoryName', value: 'id' }" node-key="id" check-strictly filterable default-expand-all class="w-full" /></el-form-item>
        <el-form-item label="类别名称" prop="categoryName"><el-input v-model="form.categoryName" maxlength="100" show-word-limit placeholder="例如：数码配件" /></el-form-item>
        <el-form-item label="类别编号" prop="categoryCode"><el-input v-model="form.categoryCode" maxlength="50" show-word-limit placeholder="唯一编号，例如 DIGITAL" /></el-form-item>
        <el-form-item label="排序（越小越靠前）" prop="sortOrder"><el-input-number v-model="form.sortOrder" :min="0" :max="2147483647" :precision="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="dialog = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存类别</el-button></template>
    </el-dialog>
  </section>
</template>
