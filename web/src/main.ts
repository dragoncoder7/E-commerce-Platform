import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElAlert } from 'element-plus/es/components/alert/index.mjs'
import { ElBreadcrumb, ElBreadcrumbItem } from 'element-plus/es/components/breadcrumb/index.mjs'
import { ElButton } from 'element-plus/es/components/button/index.mjs'
import { ElCarousel, ElCarouselItem } from 'element-plus/es/components/carousel/index.mjs'
import { ElConfigProvider } from 'element-plus/es/components/config-provider/index.mjs'
import { ElEmpty } from 'element-plus/es/components/empty/index.mjs'
import { ElForm, ElFormItem } from 'element-plus/es/components/form/index.mjs'
import { ElImage } from 'element-plus/es/components/image/index.mjs'
import { ElInput } from 'element-plus/es/components/input/index.mjs'
import { ElInputNumber } from 'element-plus/es/components/input-number/index.mjs'
import { ElLoading } from 'element-plus/es/components/loading/index.mjs'
import { ElOption, ElSelect } from 'element-plus/es/components/select/index.mjs'
import { ElPagination } from 'element-plus/es/components/pagination/index.mjs'
import { ElRadioButton, ElRadioGroup } from 'element-plus/es/components/radio/index.mjs'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)
const components = [
  ElAlert, ElBreadcrumb, ElBreadcrumbItem, ElButton, ElCarousel, ElCarouselItem,
  ElConfigProvider, ElEmpty, ElForm, ElFormItem, ElImage, ElInput, ElInputNumber,
  ElOption, ElPagination, ElRadioButton, ElRadioGroup, ElSelect,
]
for (const component of components) app.component(component.name!, component)
app.use(createPinia()).use(router).use(ElLoading).mount('#app')
