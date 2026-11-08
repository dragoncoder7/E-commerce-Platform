import { ref } from 'vue'
import { defineStore } from 'pinia'

const SIDEBAR_KEY = 'erp:sidebar-collapsed'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(localStorage.getItem(SIDEBAR_KEY) === '1')

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem(SIDEBAR_KEY, sidebarCollapsed.value ? '1' : '0')
  }

  function setSidebarCollapsed(value: boolean): void {
    sidebarCollapsed.value = value
    localStorage.setItem(SIDEBAR_KEY, value ? '1' : '0')
  }

  return { sidebarCollapsed, toggleSidebar, setSidebarCollapsed }
})
