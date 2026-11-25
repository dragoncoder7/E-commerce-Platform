<script setup lang="ts">
import { onMounted, reactive, ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { categories, brands, listProducts, type Option, type Product } from '@/api/catalog'
const route = useRoute()
const home = computed(() => route.path === '/')
const rows = ref<Product[]>([])
const categoryOptions = ref<Option[]>([])
const brandOptions = ref<Option[]>([])
const loading = ref(false)
const error = ref(false)
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 12, keyword: '', categoryId: undefined as number | undefined, brandId: undefined as number | undefined, minPrice: undefined as number | undefined, maxPrice: undefined as number | undefined, sort: 'default' })
let version = 0
async function load() {
  const current = ++version
  loading.value = true
  error.value = false
  try {
    const result = await listProducts({ ...query, recommended: home.value })
    if (current === version) { rows.value = result.records; total.value = result.total }
  } catch { if (current === version) error.value = true }
  finally { if (current === version) loading.value = false }
}
function search() { query.pageNum = 1; void load() }
watch(() => route.fullPath, () => { query.keyword = String(route.query.keyword || ''); query.pageNum = 1; void load() })
onMounted(async () => {
  query.keyword = String(route.query.keyword || '')
  void load()
  try { [categoryOptions.value, brandOptions.value] = await Promise.all([categories(), brands()]) } catch { error.value = true }
})
</script>
<template>
  <section v-if="home" class="hero">
    <div><span class="eyebrow">EVERYDAY, WELL CHOSEN</span><h1>把日常，<br>过成喜欢的模样。</h1><p>从实用到美好，发现适合你的生活好物。</p><router-link to="/products"><el-button type="primary" size="large">探索全部好物 →</el-button></router-link></div>
    <div class="hero-art" aria-hidden="true" />
  </section>
  <div class="section-heading"><div><h2>{{ home ? '为你精选' : '发现好物' }}</h2><span class="muted">{{ home ? '用心挑选的品质之选' : `共 ${total} 件商品，找到你的心仪之选` }}</span></div><router-link v-if="home" class="muted" to="/products">查看全部 →</router-link></div>
  <el-form v-if="!home" class="filters" @submit.prevent="search">
    <el-select v-model="query.categoryId" placeholder="全部分类" clearable aria-label="商品分类"><el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" /></el-select>
    <el-select v-model="query.brandId" placeholder="全部品牌" clearable aria-label="商品品牌"><el-option v-for="item in brandOptions" :key="item.id" :label="item.name" :value="item.id" /></el-select>
    <el-input-number v-model="query.minPrice" placeholder="最低价格" :min="0" :controls="false" aria-label="最低价格" />
    <el-input-number v-model="query.maxPrice" placeholder="最高价格" :min="0" :controls="false" aria-label="最高价格" />
    <el-select v-model="query.sort" aria-label="排序"><el-option label="综合排序" value="default" /><el-option label="价格从低到高" value="priceAsc" /><el-option label="价格从高到低" value="priceDesc" /><el-option label="销量优先" value="sales" /><el-option label="最新上架" value="newest" /></el-select>
    <el-button type="primary" native-type="submit">筛选</el-button>
  </el-form>
  <el-alert v-if="error" title="商品加载失败，请重试" type="error" :closable="false"><el-button link @click="load">重新加载</el-button></el-alert>
  <div v-loading="loading" class="product-grid" style="min-height:180px">
    <router-link v-for="item in rows" :key="item.id" :to="`/products/${item.id}`" class="product-card">
      <div class="product-cover"><el-image v-if="item.image" :src="item.image" fit="cover" style="width:100%;height:100%"><template #error><span>暂无图片</span></template></el-image><span v-else>SHIGUANG · 拾光好物</span></div>
      <div class="product-info"><h3>{{ item.name }}</h3><p>{{ item.subtitle || '简单实用，用心之选' }}</p><span class="price">¥ {{ Number(item.price).toFixed(2) }}</span><span class="muted" style="float:right;margin-top:9px">{{ item.stock > 0 ? '现货' : '暂时售罄' }}</span></div>
    </router-link>
  </div>
  <el-empty v-if="!loading && !error && !rows.length" description="暂无符合条件的商品" />
  <el-pagination v-if="total > query.pageSize" v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="total" layout="prev, pager, next" class="pagination" @current-change="load" />
</template>
