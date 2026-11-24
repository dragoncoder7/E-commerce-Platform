import { http } from '@/utils/request'

export interface CategoryForm {
  id?: number
  parentId: number
  categoryName: string
  categoryCode: string
  sortOrder: number
  description: string
  status: number
}

export interface ProductCategory extends CategoryForm {
  id: number
  level: number
}

export const listCategories = () => http.get<ProductCategory[]>('/product/category/all', undefined, { showError: false })
export const saveCategory = (form: CategoryForm) => form.id
  ? http.put<ProductCategory>('/product/category', form)
  : http.post<ProductCategory>('/product/category', form)
export const deleteCategory = (id: number) => http.delete<void>(`/product/category/${id}`)
