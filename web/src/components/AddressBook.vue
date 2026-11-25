<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { ElDialog } from 'element-plus/es/components/dialog/index.mjs'
import { ElCheckbox } from 'element-plus/es/components/checkbox/index.mjs'
import { addressApi, type Address, type AddressInput } from '@/api/address'

const rows = ref<Address[]>([])
const loading = ref(false)
const saving = ref(false)
const acting = ref<number>()
const failed = ref(false)
const visible = ref(false)
const editingId = ref<number>()
const form = ref<FormInstance>()
const empty: AddressInput = { recipient: '', phone: '', province: '', city: '', district: '', detail: '', defaultAddress: false }
const model = reactive<AddressInput>({ ...empty })
const rules: FormRules<AddressInput> = {
  recipient: [{ required: true, whitespace: true, message: '请输入收货人', trigger: 'blur' }, { max: 50, message: '最多50字' }],
  phone: [{ required: true, pattern: /^[0-9+() -]{6,20}$/, message: '请输入有效收货电话', trigger: 'blur' }],
  province: [{ required: true, whitespace: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, whitespace: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, whitespace: true, message: '请输入区县', trigger: 'blur' }],
  detail: [{ required: true, whitespace: true, message: '请输入详细地址', trigger: 'blur' }],
}
async function load() {
  loading.value = true
  failed.value = false
  try { rows.value = await addressApi.list() }
  catch { failed.value = true }
  finally { loading.value = false }
}
function edit(row?: Address) {
  editingId.value = row?.id
  Object.assign(model, row ? { recipient: row.recipient, phone: row.phone, province: row.province, city: row.city, district: row.district, detail: row.detail, defaultAddress: row.defaultAddress } : { ...empty, defaultAddress: rows.value.length === 0 })
  visible.value = true
}
async function save() {
  if (saving.value || !await form.value?.validate().catch(() => false)) return
  saving.value = true
  try { await addressApi.save({ ...model }, editingId.value); visible.value = false; ElMessage.success('收货地址已保存'); await load() }
  catch { /* 请求层展示错误，保留编辑内容。 */ }
  finally { saving.value = false }
}
async function makeDefault(row: Address) {
  if (acting.value !== undefined) return
  acting.value = row.id
  try { await addressApi.setDefault(row.id); await load(); ElMessage.success('默认地址已更新') }
  catch { /* 请求层展示错误。 */ }
  finally { acting.value = undefined }
}
async function remove(row: Address) {
  if (acting.value !== undefined) return
  try { await ElMessageBox.confirm(`删除 ${row.recipient} 的收货地址？`, '删除地址', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }) }
  catch { return }
  acting.value = row.id
  try { await addressApi.delete(row.id); await load(); ElMessage.success('地址已删除') }
  catch { /* 请求层展示错误。 */ }
  finally { acting.value = undefined }
}
onMounted(load)
</script>
<template>
  <section class="address-book" v-loading="loading">
    <div class="section-heading"><div><h2>收货地址</h2><p class="muted">最多保存20个地址；首个地址自动设为默认。</p></div><el-button type="primary" :disabled="loading || rows.length >= 20" @click="edit()">新增地址</el-button></div>
    <el-alert v-if="failed" title="地址加载失败，请重试" type="error" :closable="false"><el-button text @click="load">重新加载</el-button></el-alert>
    <el-empty v-else-if="!loading && !rows.length" description="还没有收货地址，添加一个吧" />
    <div v-else class="address-grid"><article v-for="row in rows" :key="row.id" class="address-card" :class="{ 'is-default': row.defaultAddress }">
      <div class="address-title"><strong>{{ row.recipient }}</strong><span v-if="row.defaultAddress" class="default-badge">默认地址</span></div>
      <p>{{ row.phone }}</p><p class="address-detail">{{ row.province }} {{ row.city }} {{ row.district }}<br>{{ row.detail }}</p>
      <div class="address-actions"><el-button link type="primary" :disabled="acting !== undefined" @click="edit(row)">编辑</el-button><el-button v-if="!row.defaultAddress" link :disabled="acting !== undefined" @click="makeDefault(row)">设为默认</el-button><el-button link type="danger" :disabled="acting !== undefined" @click="remove(row)">删除</el-button></div>
    </article></div>
    <ElDialog v-model="visible" :title="editingId ? '编辑收货地址' : '新增收货地址'" width="560px" style="max-width:calc(100vw - 32px)" :close-on-click-modal="false" :show-close="!saving" :close-on-press-escape="!saving" destroy-on-close @opened="form?.clearValidate()">
      <el-form ref="form" :model="model" :rules="rules" label-position="top" @submit.prevent="save">
        <el-form-item label="收货人" prop="recipient"><el-input v-model="model.recipient" :maxlength="50" autocomplete="name" /></el-form-item>
        <el-form-item label="收货电话" prop="phone"><el-input v-model="model.phone" :maxlength="20" autocomplete="tel" /></el-form-item>
        <div class="address-regions"><el-form-item label="省份" prop="province"><el-input v-model="model.province" :maxlength="50" /></el-form-item><el-form-item label="城市" prop="city"><el-input v-model="model.city" :maxlength="50" /></el-form-item><el-form-item label="区县" prop="district"><el-input v-model="model.district" :maxlength="50" /></el-form-item></div>
        <el-form-item label="详细地址" prop="detail"><el-input v-model="model.detail" type="textarea" :maxlength="200" :rows="3" show-word-limit autocomplete="street-address" /></el-form-item>
        <ElCheckbox v-model="model.defaultAddress" :disabled="rows.length === 0 || rows.find(row => row.id === editingId)?.defaultAddress">设为默认收货地址</ElCheckbox>
        <p class="muted">更换默认地址时，请在目标地址上选择“设为默认”。</p>
        <div class="address-dialog-actions"><el-button :disabled="saving" @click="visible = false">取消</el-button><el-button type="primary" native-type="submit" :loading="saving">保存地址</el-button></div>
      </el-form>
    </ElDialog>
  </section>
</template>
<style scoped>
.address-book{padding-top:12px}.address-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:20px}.address-card{border:1px solid #e3eaf0;border-radius:12px;padding:24px;background:white}.address-card.is-default{border-color:#8aaddb}.address-title{display:flex;justify-content:space-between;gap:12px;align-items:center}.default-badge{font-size:12px;background:#edf4ff;color:#2563eb;padding:3px 8px;border-radius:4px;white-space:nowrap}.address-detail{line-height:1.8;color:#647687;overflow-wrap:anywhere}.address-actions{display:flex;gap:16px;margin-top:20px}.address-regions{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.address-dialog-actions{display:flex;justify-content:flex-end;gap:12px;margin-top:24px}@media(max-width:640px){.address-grid{grid-template-columns:1fr}.address-card{padding:18px}.section-heading{align-items:center}.section-heading h2{font-size:22px}.address-regions{grid-template-columns:1fr;gap:0}}
</style>
