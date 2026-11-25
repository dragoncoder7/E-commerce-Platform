import { http } from '@/utils/request'

export interface MemberRow { id: number; username: string | null; phone: string | null; email: string | null; nickname: string; status: number; createTime: string }
export const memberApi = {
  list: (params: { pageNum: number; pageSize: number; keyword?: string; status?: number }) => http.get<{ records: MemberRow[]; total: number }>('/members', params),
  status: (id: number, status: number) => http.put<void>(`/members/${id}/status`, { status }),
}
