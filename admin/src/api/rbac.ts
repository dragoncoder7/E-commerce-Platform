import { http } from '@/utils/request'

export interface StaffInput { username: string; nickname: string; password?: string; status: number; roleIds: number[] }
export interface StaffRow extends StaffInput { id: number; createTime: string }
export interface RoleInput { name: string; code: string; status: number; sort: number; remark: string; menuIds: number[] }
export interface RoleRow extends RoleInput { id: number }
export const rbacApi = {
  staff: (params: { pageNum: number; pageSize: number; keyword: string }) => http.get<{ records: StaffRow[]; total: number }>('/staff', params),
  saveStaff: (data: StaffInput, id?: number) => id === undefined ? http.post<StaffRow>('/staff', data) : http.put<StaffRow>(`/staff/${id}`, data),
  deleteStaff: (id: number) => http.delete<void>(`/staff/${id}`),
  roles: () => http.get<RoleRow[]>('/roles'),
  saveRole: (data: RoleInput, id?: number) => id === undefined ? http.post<RoleRow>('/roles', data) : http.put<RoleRow>(`/roles/${id}`, data),
  deleteRole: (id: number) => http.delete<void>(`/roles/${id}`),
}
