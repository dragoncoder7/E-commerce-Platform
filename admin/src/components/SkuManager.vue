<script setup lang="ts">
import { useUserStore } from '@/stores/modules/user'
const permissionUser = useUserStore()
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
const props = defineProps<{ productId: number }>()
interface Sku { id?: number; skuCode: string; specification: string; price: number; stock: number; status: number }
const rows = ref<Sku[]>([])
const busy = ref(false)
async function load() { rows.value = await http.get<Sku[]>(`/product/${props.productId}/skus`) }
function add() { rows.value.push({ skuCode: '', specification: '', price: 0, stock: 0, status: 1 }) }
async function save(row: Sku) {
  busy.value = true
  try { await http.put(`/product/${props.productId}/skus`, row); ElMessage.success('规格已保存'); await load() }
  catch { /* 请求层显示错误，保留输入 */ }
  finally { busy.value = false }
}
async function adjust(row: Sku) {
  try {
    const amount = await ElMessageBox.prompt('输入变化数量，入库为正数，出库为负数', '调整库存', { inputPattern: /^-?[1-9]\d*$/, inputErrorMessage: '请输入非零整数' })
    const reason = await ElMessageBox.prompt('请输入调整原因', '库存调整原因', { inputValidator: value => !!value?.trim() && value.length <= 200 || '请填写 1–200 字的原因' })
    busy.value = true
    await http.post(`/product/${props.productId}/skus/${row.id}/stock`, { delta: Number(amount.value), reason: reason.value })
    ElMessage.success('库存已调整'); await load()
  } catch { /* 取消或错误不修改页面数据 */ }
  finally { busy.value = false }
}
interface StockLog { id: number; delta: number; balance: number; actor: string; reason: string; createTime: string }
const history = ref<StockLog[]>([])
const historySku = ref<number>()
const historyPage = ref(1)
const historyTotal = ref(0)
async function loadHistory(skuId: number, reset = true) {
  historySku.value = skuId
  if (reset) historyPage.value = 1
  busy.value = true
  try {
    const result = await http.get<{ records: StockLog[]; total: number }>(`/product/${props.productId}/skus/${skuId}/history`, { pageNum: historyPage.value, pageSize: 10 })
    history.value = result.records
    historyTotal.value = result.total
  } catch { /* 请求层显示错误 */ }
  finally { busy.value = false }
}
onMounted(load)
</script>
<template>
  <el-alert title="售价及库存由规格汇总；新规格初始库存为零，保存后可调整库存。" type="info" :closable="false" class="mb-4" />
  <el-table :data="rows" v-loading="busy">
    <el-table-column label="SKU 编码" min-width="170"><template #default="{ row }"><el-input :disabled="!permissionUser.hasPermission('product:write')" v-model="row.skuCode" maxlength="64" aria-label="SKU 编码" /></template></el-table-column>
    <el-table-column label="规格名称" min-width="170"><template #default="{ row }"><el-input :disabled="!permissionUser.hasPermission('product:write')" v-model="row.specification" maxlength="200" aria-label="规格名称" /></template></el-table-column>
    <el-table-column label="售价" width="155"><template #default="{ row }"><el-input-number :disabled="!permissionUser.hasPermission('product:write')" v-model="row.price" :min="0" :max="99999999.99" :precision="2" :controls="false" style="width:125px" /></template></el-table-column>
    <el-table-column label="库存" prop="stock" width="70" />
    <el-table-column label="启用" width="70"><template #default="{ row }"><el-switch :disabled="!permissionUser.hasPermission('product:write')" v-model="row.status" :active-value="1" :inactive-value="0" /></template></el-table-column>
    <el-table-column label="操作" width="215"><template #default="{ row }"><el-button v-if="permissionUser.hasPermission('product:write')" link type="primary" :disabled="busy" @click="save(row)">保存</el-button><el-button v-if="permissionUser.hasPermission('product:write')" link :disabled="!row.id || busy" @click="adjust(row)">调整库存</el-button><el-button link :disabled="!row.id || busy" @click="loadHistory(row.id)">流水</el-button></template></el-table-column>
  </el-table>
  <el-button v-if="permissionUser.hasPermission('product:write')" class="mt-4" @click="add">添加规格</el-button>
  <section v-if="historySku" class="mt-6"><h3 class="mb-3 font-medium">库存流水 · SKU {{ historySku }}</h3>
    <el-table :data="history"><el-table-column prop="delta" label="变化" /><el-table-column prop="balance" label="余额" /><el-table-column prop="actor" label="操作人" /><el-table-column prop="reason" label="原因" min-width="180" /><el-table-column prop="createTime" label="时间" min-width="180" /></el-table>
    <el-pagination v-model:current-page="historyPage" :total="historyTotal" :page-size="10" layout="prev,pager,next" @current-change="loadHistory(historySku!, false)" />
  </section>
</template>
