import { http } from '@/utils/request'

export interface ProductForm {
  id?: number
  productCode: string
  productName: string
  categoryId: number | null
  brandId: number | null
  mainImage: string
  subTitle: string
  description: string
  originalPrice: number | null
  salePrice: number | null
  stock: number
  unit: string
  status: number
  isHot: number
  isNew: number
  isRecommend: number
  sortOrder: number
}

export interface Product extends ProductForm {
  id: number
  salesCount: number
  viewCount: number
  createTime: string
  updateTime: string
}

export interface ProductQuery {
  pageNum: number
  pageSize: number
  productName?: string
  productCode?: string
  categoryId?: number
  brandId?: number
  status?: number
}

export interface ProductPage {
  records: Product[]
  total: number
  current: number
  size: number
  pages: number
}

export interface ProductOptions {
  categories: { id: number; categoryName: string; categoryCode: string; parentId: number; status: number }[]
  brands: { id: number; brandName: string; status: number }[]
}

export const listProducts = (query: ProductQuery) =>
  http.get<ProductPage>('/product/list', { ...query }, { showError: false })
export const getProduct = (id: number) => http.get<Product>(`/product/${id}`)
export const getProductOptions = () => http.get<ProductOptions>('/product/options')
export const createProduct = (form: ProductForm) => http.post<Product>('/product', form)
export const updateProduct = (form: ProductForm) => http.put<Product>('/product', form)
export const deleteProduct = (id: number) => http.delete<void>(`/product/${id}`)
export const createProductCategory = (categoryName: string, categoryCode: string) =>
  http.post('/product/category', { categoryName, categoryCode, parentId: 0, level: 1, status: 1, sortOrder: 0 })
