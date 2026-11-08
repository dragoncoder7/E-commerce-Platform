import type { Component } from 'vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'

const iconRegistry = ElementPlusIcons as unknown as Record<string, Component>

/** 按名称解析 Element Plus 图标组件 */
export function resolveIcon(name?: string): Component | undefined {
  if (!name) return undefined
  return iconRegistry[name]
}

/** 菜单管理页图标可选项 */
export const ICON_OPTIONS: string[] = [
  'HomeFilled',
  'Setting',
  'Menu',
  'User',
  'UserFilled',
  'Avatar',
  'Goods',
  'ShoppingCart',
  'ShoppingBag',
  'Box',
  'Tickets',
  'Document',
  'Files',
  'Folder',
  'Collection',
  'List',
  'Grid',
  'DataLine',
  'PieChart',
  'TrendCharts',
  'Money',
  'Wallet',
  'Coin',
  'CreditCard',
  'Monitor',
  'Operation',
  'Tools',
  'Management',
  'Promotion',
  'Shop',
  'Van',
  'Bell',
  'Message',
  'Star',
  'Location',
  'Checked',
]
