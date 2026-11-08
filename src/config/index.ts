/** 应用级常量配置 */
export const APP_TITLE = import.meta.env.VITE_APP_TITLE || '云商 ERP'
export const APP_VERSION = import.meta.env.VITE_APP_VERSION || '0.1.0'
export const APP_SUBTITLE = '电商中台管理系统'
export const COPYRIGHT = `© ${new Date().getFullYear()} 云商 ERP · E-commerce Platform`

/** 是否启用内置 mock */
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

/** 演示账号（mock 模式下可用） */
export const DEMO_ACCOUNT = {
  username: 'admin',
  password: '123456',
}
