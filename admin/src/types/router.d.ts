import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题，同时用于面包屑与 document.title */
    title?: string
    /** Element Plus 图标名 */
    icon?: string
    /** 是否在侧边栏隐藏 */
    hidden?: boolean
    /** 是否开启 keep-alive 缓存 */
    keepAlive?: boolean
    /** 固定标签，不可关闭（预留） */
    affix?: boolean
    /** 访问所需权限标识，为空表示不校验 */
    permission?: string
  }
}
