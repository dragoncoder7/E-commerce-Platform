import { api } from './http'

export interface AddressInput {
  recipient: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  defaultAddress: boolean
}
export interface Address extends AddressInput { id: number }
export const addressApi = {
  list: () => api.get<Address[]>('/addresses'),
  save: (data: AddressInput, id?: number) => id === undefined
    ? api.post<Address>('/addresses', data) : api.put<Address>(`/addresses/${id}`, data),
  setDefault: (id: number) => api.put<void>(`/addresses/${id}/default`),
  delete: (id: number) => api.delete<void>(`/addresses/${id}`),
}
