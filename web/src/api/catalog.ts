import { api, type Page } from './http'
export interface Option { id: number; parentId: number; name: string }
export interface Sku { id: number; specification: string; price: number; stock: number }
export interface Product { id: number; name: string; subtitle: string; image: string; description: string; price: number; stock: number; sales: number; skus: Sku[]; images: string[] }
export const listProducts = (params: object) => api.get<Page<Product>>('/catalog/products', params)
export const getProduct = (id: string) => api.get<Product>(`/catalog/products/${id}`)
export const categories = () => api.get<Option[]>('/catalog/categories')
export const brands = () => api.get<Option[]>('/catalog/brands')
