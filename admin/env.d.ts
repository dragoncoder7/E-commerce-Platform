/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 应用标题 */
  readonly VITE_APP_TITLE: string
  /** 接口基础地址，mock 模式下不生效 */
  readonly VITE_API_BASE_URL: string
  /** 是否启用内置 mock 数据（true/false） */
  readonly VITE_USE_MOCK: string
  /** 开发环境接口代理目标 */
  readonly VITE_PROXY_TARGET: string
  /** 应用版本号 */
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
