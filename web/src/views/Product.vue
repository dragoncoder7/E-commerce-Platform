<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getProduct, type Product } from '@/api/catalog'
const route = useRoute()
const product = ref<Product>()
const selectedId = ref<number>()
const loading = ref(false)
const error = ref(false)
const selected = computed(() => product.value?.skus.find(s => s.id === selectedId.value))
async function load() {
  loading.value = true
  error.value = false
  product.value = undefined
  try { product.value = await getProduct(String(route.params.id)); selectedId.value = product.value.skus[0]?.id }
  catch { error.value = true }
  finally { loading.value = false }
}
watch(() => route.params.id, load, { immediate: true })
</script>
<template>
  <div v-loading="loading" style="min-height:360px;padding-top:28px">
    <el-breadcrumb separator="/"><el-breadcrumb-item to="/">首页</el-breadcrumb-item><el-breadcrumb-item to="/products">全部好物</el-breadcrumb-item><el-breadcrumb-item>商品详情</el-breadcrumb-item></el-breadcrumb>
    <el-empty v-if="error" description="商品不存在、已下架或暂时无法加载"><el-button @click="load">重试</el-button></el-empty>
    <template v-if="product">
      <section class="detail">
        <div class="detail-image"><el-carousel v-if="product.images.length" height="100%" style="height:100%" :autoplay="false"><el-carousel-item v-for="img in product.images" :key="img"><el-image :src="img" fit="contain" style="width:100%;height:100%" /></el-carousel-item></el-carousel><div v-else class="product-cover">SHIGUANG · 拾光好物</div></div>
        <div><span class="eyebrow">SHIGUANG SELECTION</span><h1>{{ product.name }}</h1><p class="muted">{{ product.subtitle }}</p><div class="panel"><span class="price">¥ {{ Number(selected?.price ?? product.price).toFixed(2) }}</span><p class="muted">价格透明 · 用心挑选</p></div><h4>选择规格</h4><el-radio-group v-model="selectedId"><el-radio-button v-for="sku in product.skus" :key="sku.id" :value="sku.id">{{ sku.specification }}</el-radio-button></el-radio-group><p class="muted" style="margin-top:22px">{{ selected?.stock ? `现有库存 ${selected.stock} 件` : '该规格暂时售罄' }}</p></div>
      </section>
      <h2 style="font-size:22px;margin-bottom:20px">商品介绍</h2><div class="description">{{ product.description || '暂无更多介绍' }}</div>
    </template>
  </div>
</template>
