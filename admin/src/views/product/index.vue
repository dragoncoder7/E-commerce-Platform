<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Goods, Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createProduct, deleteProduct, getProduct, getProductOptions, listProducts, updateProduct,
  type Product, type ProductForm, type ProductOptions, type ProductQuery,
} from '@/api/product'

import CategoryManager from '@/components/CategoryManager.vue'
import { categoryEnabled, categoryTree } from '@/utils/category'

const route = useRoute()
const router = useRouter()
const rows = ref<Product[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const options = ref<ProductOptions>({ categories: [], brands: [] })
const optionsReady = ref(false)
const query = reactive<ProductQuery>({ pageNum: 1, pageSize: 10, productName: '', productCode: '' })
const categoryNames = computed(() => new Map(options.value.categories.map((c) => [c.id, c.categoryName])))
const brandNames = computed(() => new Map(options.value.brands.map((b) => [b.id, b.brandName])))
let requestVersion = 0

const money = (value: number | null) => value == null ? '—' : `¥${Number(value).toFixed(2)}`
const time = (value?: string) => value?.replace('T', ' ') || '—'
const categoryName = (id: number | null) => id == null ? '—' : categoryNames.value.get(id) || `分类 #${id}`
const brandName = (id: number | null) => id == null ? '未设置' : brandNames.value.get(id) || `品牌 #${id}`

async function loadList(): Promise<void> {
  const version = ++requestVersion
  loading.value = true
  error.value = ''
  try {
    const page = await listProducts({ ...query })
    if (version !== requestVersion) return
    rows.value = page.records
    total.value = page.total
    if (!page.records.length && page.total > 0 && query.pageNum > 1) {
      query.pageNum = Math.max(1, Math.ceil(page.total / query.pageSize))
      await loadList()
    }
  } catch (e) {
    if (version === requestVersion) {
      rows.value = []
      total.value = 0
      error.value = e instanceof Error ? e.message : '商品加载失败，请重试'
    }
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

async function loadOptions(): Promise<void> {
  try {
    options.value = await getProductOptions()
    optionsReady.value = true
  } catch {
    optionsReady.value = false
  }
}

function search(): void { query.pageNum = 1; void loadList() }
function reset(): void {
  Object.assign(query, { productName: '', productCode: '', categoryId: undefined, brandId: undefined, status: undefined })
  search()
}

function emptyForm(): ProductForm {
  return { productCode: '', productName: '', categoryId: null, brandId: null, mainImage: '', subTitle: '',
    description: '', originalPrice: null, salePrice: null, stock: 0, unit: '件', status: 0,
    isHot: 0, isNew: 0, isRecommend: 0, sortOrder: 0 }
}
const form = ref<ProductForm>(emptyForm())
const formRef = ref<FormInstance>()
const dialogVisible = ref(false)
const saving = ref(false)
const detail = ref<Product | null>(null)
const detailVisible = ref(false)
const busyId = ref<number | null>(null)
const categoryDialog = ref(false)
const enabledCategories = computed(() => options.value.categories.filter((c) => categoryEnabled(c, options.value.categories)))
const categoryOptions = computed(() => categoryTree(options.value.categories.map((c) => ({
  ...c, disabled: !categoryEnabled(c, options.value.categories),
}))))
async function categoriesChanged(id?: number): Promise<void> {
  await loadOptions()
  if (id && enabledCategories.value.some((c) => c.id === id)) form.value.categoryId = id
  if (form.value.categoryId && !enabledCategories.value.some((c) => c.id === form.value.categoryId)) {
    form.value.categoryId = null
  }
}
const rules: FormRules<ProductForm> = {
  productCode: [{ required: true, whitespace: true, message: '请输入商品编号', trigger: 'blur' }],
  productName: [{ required: true, whitespace: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'change' }],
  unit: [{ required: true, whitespace: true, message: '请输入计量单位', trigger: 'blur' }],
  mainImage: [{ validator: (_rule, value: string, callback) => {
    if (value && !/^https?:\/\//i.test(value)) callback(new Error('请输入 http 或 https 图片地址'))
    else callback()
  }, trigger: 'blur' }],
}

async function openCreate(): Promise<void> {
  await loadOptions()
  Object.assign(form.value, emptyForm(), { id: undefined })
  dialogVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row: Product): Promise<void> {
  busyId.value = row.id
  try {
    const product = await getProduct(row.id)
    await loadOptions()
    Object.assign(form.value, emptyForm(), { id: product.id })
    for (const key of Object.keys(emptyForm()) as (keyof ProductForm)[]) {
      // 仅回填可编辑字段，保留售价为零等有效值。
      Object.assign(form.value, { [key]: product[key] ?? emptyForm()[key] })
    }
    dialogVisible.value = true
    await nextTick()
    formRef.value?.clearValidate()
  } catch { /* 请求层已提示 */ }
  finally { busyId.value = null }
}

async function openDetail(row: Product): Promise<void> {
  busyId.value = row.id
  try { detail.value = await getProduct(row.id); detailVisible.value = true }
  catch { /* 请求层已提示 */ }
  finally { busyId.value = null }
}

async function save(): Promise<void> {
  if (saving.value || !await formRef.value?.validate().catch(() => false)) return
  if (!enabledCategories.value.some((c) => c.id === form.value.categoryId)) {
    ElMessage.warning('请选择启用的商品类别')
    return
  }
  saving.value = true
  try {
    const payload = { ...form.value, productCode: form.value.productCode.trim(), productName: form.value.productName.trim(),
      brandId: form.value.brandId || null, originalPrice: form.value.originalPrice ?? null }
    if (form.value.id) await updateProduct(payload)
    else await createProduct(payload)
    ElMessage.success(form.value.id ? '商品已更新' : '商品已创建')
    dialogVisible.value = false
    await loadList()
  } catch { /* 保留表单供用户修正 */ }
  finally { saving.value = false }
}

async function remove(row: Product): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除「${row.productName}」？删除后该商品将从列表中移除。`, '删除商品', {
      type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消',
    })
  } catch { return }
  busyId.value = row.id
  try { await deleteProduct(row.id); ElMessage.success('商品已删除'); await loadList() }
  catch { /* 请求层已提示 */ }
  finally { busyId.value = null }
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadList()])
  if (route.query.action === 'create') {
    await openCreate()
    await router.replace({ path: route.path, query: {} })
  }
})
</script>

<template>
  <div class="space-y-4">
    <section class="flex flex-wrap items-center justify-between gap-4 rounded-xl bg-slate-900 px-6 py-5 text-white">
      <div>
        <p class="text-[11px] tracking-widest text-blue-300">商品中心</p>
        <h2 class="mt-1 text-xl font-semibold">商品管理</h2>
        <p class="mt-2 text-xs text-slate-400">管理商品信息、价格与库存，让每一次上架都有条不紊。</p>
      </div>
      <div class="flex gap-2"><el-button @click="router.push('/product/category')">管理类别</el-button><el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button></div>
    </section>

    <section class="app-card p-4">
      <el-form :inline="true" @submit.prevent="search" class="product-filters">
        <el-form-item label="商品名称"><el-input v-model="query.productName" clearable placeholder="搜索商品名称" @keyup.enter="search" /></el-form-item>
        <el-form-item label="商品编号"><el-input v-model="query.productCode" clearable placeholder="搜索商品编号" @keyup.enter="search" /></el-form-item>
        <el-form-item label="分类"><el-select v-model="query.categoryId" clearable filterable placeholder="全部分类"><el-option v-for="c in options.categories" :key="c.id" :label="c.categoryName" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="品牌"><el-select v-model="query.brandId" clearable filterable placeholder="全部品牌"><el-option v-for="b in options.brands" :key="b.id" :label="b.brandName" :value="b.id" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部状态"><el-option label="已上架" :value="1" /><el-option label="已下架" :value="0" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" :loading="loading" @click="search">查询</el-button><el-button :icon="Refresh" @click="reset">重置</el-button></el-form-item>
      </el-form>
      <el-alert v-if="!optionsReady" type="warning" :closable="false" title="分类和品牌加载失败"><el-button link @click="loadOptions">重新加载</el-button></el-alert>
    </section>

    <section class="app-card overflow-hidden">
      <div class="app-card__header"><h3 class="text-sm font-semibold">商品列表 <span class="ml-2 text-xs font-normal text-slate-400">共 {{ total }} 件</span></h3><el-button :icon="Refresh" text :loading="loading" @click="loadList">刷新</el-button></div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mb-3"><el-button link @click="loadList">重试</el-button></el-alert>
      <el-table v-loading="loading" :data="rows" row-key="id" stripe style="width: 100%">
        <el-table-column label="商品" min-width="270">
          <template #default="{ row }"><div class="flex items-center gap-3 py-2">
            <el-image v-if="row.mainImage" :src="row.mainImage" fit="cover" class="h-12 w-12 shrink-0 rounded-lg"><template #error><div class="flex h-full items-center justify-center bg-slate-100 text-slate-400"><el-icon><Goods /></el-icon></div></template></el-image>
            <div v-else class="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-400"><el-icon :size="22"><Goods /></el-icon></div>
            <div class="min-w-0"><el-button link type="primary" class="!justify-start !whitespace-normal text-left" @click="openDetail(row)">{{ row.productName }}</el-button><p class="mt-1 font-mono text-[11px] text-slate-400">{{ row.productCode }}</p><p v-if="row.subTitle" class="mt-1 line-clamp-1 text-xs text-slate-500">{{ row.subTitle }}</p></div>
          </div></template>
        </el-table-column>
        <el-table-column label="分类 / 品牌" min-width="140"><template #default="{ row }"><p>{{ categoryName(row.categoryId) }}</p><p class="mt-1 text-xs text-slate-400">{{ brandName(row.brandId) }}</p></template></el-table-column>
        <el-table-column label="售价" width="125"><template #default="{ row }"><span class="font-semibold text-slate-800">{{ money(row.salePrice) }}</span><p v-if="row.originalPrice != null" class="text-xs text-slate-400 line-through">{{ money(row.originalPrice) }}</p></template></el-table-column>
        <el-table-column label="库存" width="105"><template #default="{ row }"><span :class="row.stock <= 10 ? 'text-amber-600 font-medium' : ''">{{ row.stock }} {{ row.unit }}</span></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light">{{ row.status === 1 ? '已上架' : '已下架' }}</el-tag></template></el-table-column>
        <el-table-column label="标签" width="150"><template #default="{ row }"><div class="flex flex-wrap gap-1"><el-tag v-if="row.isNew" size="small">新品</el-tag><el-tag v-if="row.isHot" type="danger" size="small">热销</el-tag><el-tag v-if="row.isRecommend" type="warning" size="small">推荐</el-tag><span v-if="!row.isNew && !row.isHot && !row.isRecommend" class="text-slate-300">—</span></div></template></el-table-column>
        <el-table-column label="更新时间" width="175"><template #default="{ row }"><span class="text-xs">{{ time(row.updateTime) }}</span></template></el-table-column>
        <el-table-column label="操作" fixed="right" width="170"><template #default="{ row }"><el-button link type="primary" :disabled="busyId === row.id" @click="openDetail(row)">详情</el-button><el-button link type="primary" :disabled="busyId === row.id" @click="openEdit(row)">编辑</el-button><el-button link type="danger" :disabled="busyId === row.id" @click="remove(row)">删除</el-button></template></el-table-column>
        <template #empty><el-empty :description="error ? '暂时无法加载商品' : '暂无符合条件的商品'" :image-size="80"><el-button v-if="!error" type="primary" plain @click="openCreate">新增商品</el-button></el-empty></template>
      </el-table>
      <div class="overflow-x-auto border-t border-slate-100 p-4"><el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper" @size-change="search" @current-change="loadList" /></div>
    </section>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑商品' : '新增商品'" width="min(760px, 94vw)" :close-on-click-modal="false" :close-on-press-escape="!saving" :show-close="!saving" destroy-on-close>
      <el-alert v-if="optionsReady && !enabledCategories.length" title="暂无可用商品类别，请先打开「管理类别」新增或启用类别。" type="info" :closable="false" class="mb-4" />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="saving">
        <div class="grid grid-cols-1 gap-x-5 sm:grid-cols-2">
          <el-form-item label="商品编号" prop="productCode"><el-input v-model="form.productCode" maxlength="50" show-word-limit placeholder="唯一编号，如 SKU-2026-001" /></el-form-item>
          <el-form-item label="商品名称" prop="productName"><el-input v-model="form.productName" maxlength="200" placeholder="请输入商品名称" /></el-form-item>
          <el-form-item label="商品分类" prop="categoryId"><el-tree-select v-model="form.categoryId" :data="categoryOptions" :props="{ label: 'categoryName', value: 'id' }" node-key="id" check-strictly filterable default-expand-all placeholder="选择启用的类别" class="w-full" /><el-button link type="primary" @click="categoryDialog = true">管理类别</el-button><el-button link :disabled="saving" @click="loadOptions">刷新类别</el-button></el-form-item>
          <el-form-item label="品牌"><el-select v-model="form.brandId" filterable clearable placeholder="可选" class="w-full"><el-option v-for="b in options.brands" :key="b.id" :label="b.brandName" :value="b.id" /></el-select></el-form-item>
          <el-form-item label="售价（元）" prop="salePrice"><el-input-number v-model="form.salePrice" :min="0" :max="99999999.99" :precision="2" controls-position="right" class="!w-full" /></el-form-item>
          <el-form-item label="原价（元，可选）"><el-input-number v-model="form.originalPrice" :min="0" :max="99999999.99" :precision="2" controls-position="right" class="!w-full" /></el-form-item>
          <el-form-item label="库存" prop="stock"><el-input-number v-model="form.stock" :min="0" :max="2147483647" :precision="0" controls-position="right" class="!w-full" /></el-form-item>
          <el-form-item label="计量单位" prop="unit"><el-input v-model="form.unit" maxlength="20" placeholder="件 / 台 / 盒" /></el-form-item>
          <el-form-item label="商品状态"><el-radio-group v-model="form.status"><el-radio-button :value="0">下架</el-radio-button><el-radio-button :value="1">上架</el-radio-button></el-radio-group></el-form-item>
          <el-form-item label="排序（越小越靠前）"><el-input-number v-model="form.sortOrder" :min="0" :max="2147483647" :precision="0" controls-position="right" class="!w-full" /></el-form-item>
        </div>
        <el-form-item label="商品标签"><div class="flex flex-wrap gap-6"><el-switch v-model="form.isNew" :active-value="1" :inactive-value="0" active-text="新品" /><el-switch v-model="form.isHot" :active-value="1" :inactive-value="0" active-text="热销" /><el-switch v-model="form.isRecommend" :active-value="1" :inactive-value="0" active-text="推荐" /></div></el-form-item>
        <el-form-item label="主图地址" prop="mainImage"><el-input v-model="form.mainImage" maxlength="500" placeholder="https://example.com/product.jpg" clearable /></el-form-item>
        <el-form-item label="副标题"><el-input v-model="form.subTitle" maxlength="300" show-word-limit /></el-form-item>
        <el-form-item label="商品描述"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="10000" show-word-limit placeholder="介绍规格、用途等商品信息" /></el-form-item>
      </el-form>
      <template #footer><el-button :disabled="saving" @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" :disabled="!optionsReady || !enabledCategories.length" @click="save">保存商品</el-button></template>
    </el-dialog>

    <el-dialog v-model="categoryDialog" title="管理商品类别" width="min(1080px, 96vw)" append-to-body destroy-on-close :close-on-click-modal="false" @closed="categoriesChanged()">
      <CategoryManager @changed="categoriesChanged" />
      <template #footer><el-button @click="categoryDialog = false">返回商品表单</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="商品详情" width="min(720px, 94vw)">
      <template v-if="detail">
        <div class="mb-5 flex items-center gap-4"><el-image v-if="detail.mainImage" :src="detail.mainImage" fit="cover" class="h-24 w-24 rounded-lg" /><div><h3 class="text-lg font-semibold">{{ detail.productName }}</h3><p class="mt-1 text-sm text-slate-500">{{ detail.subTitle }}</p><p class="mt-2 text-xl font-semibold text-blue-600">{{ money(detail.salePrice) }}</p></div></div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="编号">{{ detail.productCode }}</el-descriptions-item><el-descriptions-item label="状态">{{ detail.status === 1 ? '已上架' : '已下架' }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ categoryName(detail.categoryId) }}</el-descriptions-item><el-descriptions-item label="品牌">{{ brandName(detail.brandId) }}</el-descriptions-item>
          <el-descriptions-item label="库存">{{ detail.stock }} {{ detail.unit }}</el-descriptions-item><el-descriptions-item label="原价">{{ money(detail.originalPrice) }}</el-descriptions-item>
          <el-descriptions-item label="销量">{{ detail.salesCount }}</el-descriptions-item><el-descriptions-item label="浏览量">{{ detail.viewCount }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ time(detail.createTime) }}</el-descriptions-item><el-descriptions-item label="更新时间">{{ time(detail.updateTime) }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="mb-2 mt-5 font-medium">商品描述</h4><p class="whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">{{ detail.description || '暂无描述' }}</p>
      </template>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.product-filters :deep(.el-form-item) { margin-bottom: 12px; }
.product-filters :deep(.el-input), .product-filters :deep(.el-select) { width: 175px; }
</style>
