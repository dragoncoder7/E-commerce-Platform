import { http } from '@/utils/request'

export interface StatCard {
  key: string
  label: string
  value: number
  /** 单位后缀，如 元 / 单 / 人 */
  unit: string
  /** 环比变化百分比，正数上升，负数下降 */
  trend: number
  /** 统计口径说明 */
  hint: string
}

export interface TodoItem {
  id: number
  title: string
  level: 'high' | 'normal' | 'low'
  deadline: string
}

export interface NoticeItem {
  id: number
  title: string
  type: string
  time: string
}

export interface DashboardSummary {
  stats: StatCard[]
  todos: TodoItem[]
  notices: NoticeItem[]
  serverTime: string
}

/** 工作台概览数据 */
export function getDashboardSummaryApi() {
  return http.get<DashboardSummary>('/dashboard/summary')
}
