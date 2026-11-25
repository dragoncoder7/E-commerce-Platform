import { api } from './http'

export interface Member { id: number; username: string | null; phone: string | null; email: string | null; nickname: string; phoneVerified: boolean }
export interface Session { token: string; expiresIn: number; member: Member }
export interface Registration { type: 'account' | 'phone' | 'email'; account: string; password: string; code?: string }
export const memberApi = {
  capabilities: () => api.get<{ emailRegistration: boolean; mailMode: string }>('/auth/capabilities'),
  register: (data: Registration) => api.post<Member>('/auth/register', data),
  login: (account: string, password: string) => api.post<Session>('/auth/login', { account, password }),
  code: (email: string) => api.post<void>('/auth/email-code', { email }),
  me: () => api.get<Member>('/auth/me'),
  update: (nickname: string) => api.put<Member>('/auth/me', { nickname }),
  password: (currentPassword: string, newPassword: string) => api.put<void>('/auth/password', { currentPassword, newPassword }),
  logout: () => api.post<void>('/auth/logout'),
}
