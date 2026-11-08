import { createApp, type Component } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { setupStore } from './stores'
import { httpInstance } from './utils/request'
import { setupMock } from './mock'
import { USE_MOCK } from './config'
import './styles/index.css'
import './router/guard'

// mock 必须在任何请求发出前安装
if (USE_MOCK) {
  setupMock(httpInstance)
}

const app = createApp(App)

// 全量注册 Element Plus 图标，菜单图标可只写名称
const icons = ElementPlusIcons as unknown as Record<string, Component>
Object.entries(icons).forEach(([name, component]) => {
  app.component(name, component)
})

app.use(ElementPlus, { locale: zhCn, zIndex: 3000 })
setupStore(app)
app.use(router)
app.mount('#app')
