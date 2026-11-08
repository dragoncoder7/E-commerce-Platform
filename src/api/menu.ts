import { http } from '@/utils/request'
import type { MenuForm, MenuItem, MenuQuery } from '@/types/menu'

/** 菜单列表（树结构） */
export function listMenusApi(params: MenuQuery = {}) {
  return http.get<MenuItem[]>('/menu/list', { ...params })
}

/** 新增菜单 */
export function createMenuApi(data: MenuForm) {
  return http.post<MenuItem>('/menu', data)
}

/** 修改菜单 */
export function updateMenuApi(id: number, data: MenuForm) {
  return http.put<MenuItem>(`/menu/${id}`, data)
}

/** 删除菜单 */
export function deleteMenuApi(id: number) {
  return http.delete<void>(`/menu/${id}`)
}
