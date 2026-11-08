<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Delete, Edit, Expand, Fold, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { createMenuApi, deleteMenuApi, listMenusApi, updateMenuApi } from '@/api/menu'
import type { MenuForm, MenuItem, MenuType } from '@/types/menu'
import { MENU_TYPE_LABEL, MENU_TYPE_TAG } from '@/types/menu'
import { ICON_OPTIONS, resolveIcon } from '@/utils/icons'
import { useUserStore } from '@/stores/modules/user'
import { usePermissionStore } from '@/stores/modules/permission'

const userStore = useUserStore()
const permissionStore = usePermissionStore()

/* ------------------------------- 列表状态 -------------------------------- */

const loading = ref(false)
const list = ref<MenuItem[]>([])
const expandAll = ref(true)
const tableKey = ref(0)

const query = reactive<{ title: string; type: MenuType | ''; status: '' | 0 | 1 }>({
  title: '',
  type: '',
  status: '',
})

const totalCount = computed(() => {
  const count = (items: MenuItem[]): number =>
    items.reduce((sum, item) => sum + 1 + (item.children ? count(item.children) : 0), 0)
  return count(list.value)
})

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    list.value = await listMenusApi({
      title: query.title.trim(),
      type: query.type,
      status: query.status,
    })
  } catch {
    // 错误提示已统一处理
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  void fetchList()
}

function resetQuery(): void {
  query.title = ''
  query.type = ''
  query.status = ''
  void fetchList()
}

function toggleExpand(): void {
  expandAll.value = !expandAll.value
  tableKey.value += 1
}

/* ------------------------------- 表单状态 -------------------------------- */

function createEmptyForm(parentId = 0): MenuForm {
  return {
    parentId,
    title: '',
    type: 'menu',
    path: '',
    component: '',
    icon: 'Menu',
    permission: '',
    sort: 1,
    visible: true,
    keepAlive: false,
    status: 1,
  }
}

const dialog = reactive<{ visible: boolean; mode: 'create' | 'edit'; title: string }>({
  visible: false,
  mode: 'create',
  title: '新增菜单',
})

const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive<MenuForm>(createEmptyForm())

const rules = computed<FormRules>(() => ({
  title: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path:
    form.type === 'button'
      ? []
      : [
          { required: true, message: '请输入路由路径', trigger: 'blur' },
          { pattern: /^\/[A-Za-z0-9\-_/]*$/, message: '需以 / 开头，仅支持字母、数字、- 与 /', trigger: 'blur' },
        ],
  component:
    form.type === 'menu'
      ? [
          { required: true, message: '请输入组件路径', trigger: 'blur' },
          { pattern: /^[A-Za-z0-9\-_/]+$/, message: '格式如 system/menu/index，无需 .vue 后缀', trigger: 'blur' },
        ]
      : [],
  permission:
    form.type === 'button'
      ? [{ required: true, message: '请输入权限标识', trigger: 'blur' }]
      : [],
}))

/** 上级菜单可选项：根节点 + 目录/菜单（排除按钮与自身） */
const parentOptions = computed(() => {
  const build = (items: MenuItem[]): Array<Record<string, unknown>> =>
    items
      .filter((item) => item.type !== 'button' && item.id !== form.id)
      .map((item) => ({
        value: item.id,
        label: item.title,
        children: item.children?.length ? build(item.children) : undefined,
      }))

  return [{ value: 0, label: '顶级菜单（根节点）', children: build(list.value) }]
})

function openCreate(parentId = 0): void {
  Object.assign(form, createEmptyForm(parentId))
  dialog.mode = 'create'
  dialog.title = parentId ? '新增子菜单' : '新增菜单'
  dialog.visible = true
  void nextTick(() => formRef.value?.clearValidate())
}

function openEdit(row: MenuItem): void {
  Object.assign(form, {
    id: row.id,
    parentId: row.parentId,
    name: row.name,
    title: row.title,
    type: row.type,
    path: row.path,
    component: row.component,
    icon: row.icon,
    permission: row.permission,
    sort: row.sort,
    visible: row.visible,
    keepAlive: row.keepAlive,
    status: row.status,
  })
  dialog.mode = 'edit'
  dialog.title = '编辑菜单'
  dialog.visible = true
  void nextTick(() => formRef.value?.clearValidate())
}

/** 路由 name：由路径推导，保证唯一可读，如 /system/menu -> SystemMenu */
function toRouteName(input: string): string {
  const base = input.replace(/^\/+/, '')
  const segments = base.split(/[/\-_]/).filter(Boolean)
  if (!segments.length) return `Menu${Date.now()}`
  const name = segments
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join('')
  return /^[A-Za-z]/.test(name) ? name : `Menu${Date.now()}`
}

/** 菜单变更后重建动态路由，侧边栏立即同步 */
async function syncRoutes(): Promise<void> {
  const menus = await userStore.fetchMenus()
  permissionStore.rebuild(menus)
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: MenuForm = {
      ...form,
      name: form.name || toRouteName(form.path || form.title),
    }
    if (dialog.mode === 'create') {
      await createMenuApi(payload)
      ElMessage.success('菜单已创建，路由已同步')
    } else {
      await updateMenuApi(payload.id as number, payload)
      ElMessage.success('保存成功，路由已同步')
    }
    dialog.visible = false
    await fetchList()
    await syncRoutes()
  } catch {
    // 错误提示已统一处理
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: MenuItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除菜单「${row.title}」吗？删除后对应路由将立即失效。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }

  try {
    await deleteMenuApi(row.id)
    ElMessage.success('删除成功')
    await fetchList()
    await syncRoutes()
  } catch {
    // 错误提示已统一处理
  }
}

async function handleStatusChange(row: MenuItem): Promise<void> {
  try {
    await updateMenuApi(row.id, {
      parentId: row.parentId,
      name: row.name,
      title: row.title,
      type: row.type,
      path: row.path,
      component: row.component,
      icon: row.icon,
      permission: row.permission,
      sort: row.sort,
      visible: row.visible,
      keepAlive: row.keepAlive,
      status: row.status,
    })
    ElMessage.success(row.status === 1 ? '菜单已启用' : '菜单已停用')
    await syncRoutes()
  } catch {
    // 失败回滚开关状态
    row.status = row.status === 1 ? 0 : 1
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="space-y-4">
    <!-- 查询区 -->
    <section class="app-card px-5 py-4">
      <div class="flex flex-wrap items-end gap-x-4 gap-y-3">
        <div>
          <p class="mb-1.5 text-[12px] text-slate-500">菜单名称</p>
          <el-input
            v-model="query.title"
            class="w-[200px]"
            placeholder="请输入菜单名称"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </div>

        <div>
          <p class="mb-1.5 text-[12px] text-slate-500">菜单类型</p>
          <el-select v-model="query.type" class="w-[130px]" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="目录" value="catalog" />
            <el-option label="菜单" value="menu" />
            <el-option label="按钮" value="button" />
          </el-select>
        </div>

        <div>
          <p class="mb-1.5 text-[12px] text-slate-500">状态</p>
          <el-select v-model="query.status" class="w-[120px]" placeholder="全部">
            <el-option label="全部" value="" />
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </div>

        <div class="flex items-center gap-2 pb-0.5">
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </div>
      </div>
    </section>

    <!-- 列表区 -->
    <section class="app-card overflow-hidden">
      <div class="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 px-5 py-3">
        <div class="flex items-center gap-2">
          <h3 class="text-[15px] font-semibold text-slate-800">菜单列表</h3>
          <span class="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] text-slate-500">
            共 {{ totalCount }} 项
          </span>
        </div>

        <div class="flex items-center gap-2">
          <el-button
            v-if="userStore.hasPermission('system:menu:create')"
            type="primary"
            :icon="Plus"
            @click="openCreate(0)"
          >
            新增菜单
          </el-button>
          <el-button :icon="expandAll ? Fold : Expand" @click="toggleExpand">
            {{ expandAll ? '全部折叠' : '全部展开' }}
          </el-button>
          <el-button :icon="Refresh" circle title="刷新列表" @click="fetchList" />
        </div>
      </div>

      <el-table
        :key="tableKey"
        v-loading="loading"
        :data="list"
        row-key="id"
        :tree-props="{ children: 'children' }"
        :default-expand-all="expandAll"
        class="w-full"
        empty-text="暂无菜单数据"
      >
        <el-table-column label="菜单名称" min-width="230">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-icon
                v-if="row.type !== 'button' && resolveIcon(row.icon)"
                :size="15"
                class="shrink-0 text-slate-400"
              >
                <component :is="resolveIcon(row.icon)" />
              </el-icon>
              <span v-else-if="row.type === 'button'" class="h-1 w-1 rounded-full bg-slate-300"></span>
              <span class="truncate text-[13px] font-medium text-slate-800">{{ row.title }}</span>
              <span
                v-if="!row.visible && row.type !== 'button'"
                class="rounded bg-slate-100 px-1 text-[10px] text-slate-400"
              >
                隐藏
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="84" align="center">
          <template #default="{ row }">
            <el-tag :type="MENU_TYPE_TAG[row.type as MenuType]" size="small" effect="light" round>
              {{ MENU_TYPE_LABEL[row.type as MenuType] }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="路由路径" min-width="160">
          <template #default="{ row }">
            <span class="font-mono text-[12px] text-slate-600">{{ row.path || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="组件路径" min-width="190">
          <template #default="{ row }">
            <span class="font-mono text-[12px] text-slate-500">{{ row.component || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="权限标识" min-width="180">
          <template #default="{ row }">
            <span class="font-mono text-[12px] text-slate-500">{{ row.permission || '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="排序" width="70" align="center">
          <template #default="{ row }">
            <span class="text-[12.5px] text-slate-600">{{ row.sort }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              size="small"
              :disabled="!userStore.hasPermission('system:menu:update')"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            <span class="text-[12px] text-slate-500">{{ row.createTime }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="196" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center">
              <el-button
                v-if="userStore.hasPermission('system:menu:update')"
                link
                type="primary"
                size="small"
                :icon="Edit"
                @click="openEdit(row)"
              >
                修改
              </el-button>
              <el-button
                v-if="row.type !== 'button' && userStore.hasPermission('system:menu:create')"
                link
                type="primary"
                size="small"
                :icon="Plus"
                @click="openCreate(row.id)"
              >
                子菜单
              </el-button>
              <el-button
                v-if="userStore.hasPermission('system:menu:delete')"
                link
                type="danger"
                size="small"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <!-- 新增 / 编辑 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.title"
      width="660px"
      align-center
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="pt-1">
        <el-form-item label="上级菜单" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            check-strictly
            :render-after-expand="false"
            node-key="value"
            default-expand-all
            class="w-full"
            placeholder="请选择上级菜单"
          />
        </el-form-item>

        <el-form-item label="菜单类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button value="catalog">目录</el-radio-button>
            <el-radio-button value="menu">菜单</el-radio-button>
            <el-radio-button value="button">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="title">
              <el-input v-model="form.title" placeholder="如：订单管理" maxlength="20" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type !== 'button'" label="菜单图标" prop="icon">
              <el-select v-model="form.icon" filterable clearable class="w-full" placeholder="请选择图标">
                <el-option v-for="icon in ICON_OPTIONS" :key="icon" :label="icon" :value="icon">
                  <div class="flex items-center gap-2">
                    <el-icon><component :is="resolveIcon(icon)" /></el-icon>
                    <span class="text-[12.5px]">{{ icon }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-if="form.type !== 'button'" :gutter="16">
          <el-col :span="12">
            <el-form-item label="路由路径" prop="path">
              <el-input v-model="form.path" placeholder="如：/order/list" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.type === 'menu'" label="组件路径" prop="component">
              <el-input v-model="form.component" placeholder="如：order/list/index" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="权限标识" prop="permission">
              <el-input v-model="form.permission" placeholder="如：order:list:query" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="sort">
              <el-input-number v-model="form.sort" :min="1" :max="999" controls-position="right" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item v-if="form.type !== 'button'" label="显示设置">
          <div class="flex flex-wrap items-center gap-x-8 gap-y-2">
            <label class="flex items-center gap-2 text-[12.5px] text-slate-600">
              <el-switch v-model="form.visible" size="small" />
              在侧边栏显示
            </label>
            <label class="flex items-center gap-2 text-[12.5px] text-slate-600">
              <el-switch v-model="form.keepAlive" size="small" />
              缓存页面（保留状态）
            </label>
          </div>
        </el-form-item>

        <el-form-item label="菜单状态">
          <label class="flex items-center gap-2 text-[12.5px] text-slate-600">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" size="small" />
            {{ form.status === 1 ? '正常（可访问）' : '停用（不生成路由）' }}
          </label>
        </el-form-item>

        <div class="rounded-lg bg-slate-50 px-4 py-3 text-[11.5px] leading-relaxed text-slate-500">
          提示：组件路径相对 <code class="text-slate-700">src/views</code>，不需要
          <code class="text-slate-700">.vue</code>
          后缀；保存后会自动重建动态路由并同步左侧导航。若填写的组件不存在，将降级显示 404 页面。
        </div>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-2">
          <el-button @click="dialog.visible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ dialog.mode === 'create' ? '确认新增' : '保存修改' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
