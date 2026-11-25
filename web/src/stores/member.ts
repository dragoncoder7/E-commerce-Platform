import { defineStore } from 'pinia'
import { ref } from 'vue'
import { memberApi, type Member } from '@/api/member'

export const useMemberStore = defineStore('member', () => {
  const member = ref<Member | null>(null)
  const ready = ref(false)
  let pending: Promise<void> | undefined
  function clear() {
    localStorage.removeItem('store:token')
    member.value = null
    ready.value = true
  }
  async function restore() {
    if (ready.value) return
    if (pending) return pending
    pending = (async () => {
      try { if (localStorage.getItem('store:token')) member.value = await memberApi.me() }
      catch { clear() }
      finally { ready.value = true; pending = undefined }
    })()
    return pending
  }
  async function login(account: string, password: string) {
    const session = await memberApi.login(account, password)
    localStorage.setItem('store:token', session.token)
    member.value = session.member
    ready.value = true
  }
  async function logout() {
    await memberApi.logout()
    clear()
  }
  window.addEventListener('store:unauthorized', clear)
  window.addEventListener('storage', event => {
    if (event.key === 'store:token') { member.value = null; ready.value = false; void restore() }
  })
  return { member, ready, clear, restore, login, logout }
})
