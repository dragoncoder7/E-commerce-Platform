import { test, expect } from '@playwright/test'
import fs from 'node:fs'
const result = JSON.parse(fs.readFileSync('../server/.runtime/member-management-result.json', 'utf8')) as { reader: string; member: string }

test('ERP 会员启停与只读权限', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', error => errors.push(error.message))
  await page.setViewportSize({ width: 1440, height: 980 })
  await page.goto('http://127.0.0.1:5173/login')
  await page.getByPlaceholder('请输入账号').fill('catalog_admin')
  await page.getByPlaceholder('请输入密码').fill('CatalogTest123!')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await expect(page).toHaveURL(/dashboard/)
  await page.getByRole('menuitem', { name: '商城会员' }).click()
  await page.getByPlaceholder('账号 / 手机 / 邮箱 / 昵称').fill(result.member)
  await expect(page.getByRole('button', { name: '查询', exact: true })).toBeEnabled()
  const searched = page.waitForResponse(response => response.url().includes('/members?') && response.url().includes('keyword=' + result.member))
  await page.getByRole('button', { name: '查询', exact: true }).click()
  await searched
  await expect(page.getByRole('row').filter({ hasText: result.member })).toHaveCount(1)
  await page.getByRole('row').filter({ hasText: result.member }).getByRole('button', { name: '禁用', exact: true }).click()
  await page.getByRole('button', { name: '确认', exact: true }).click()
  await expect(page.getByRole('row').filter({ hasText: result.member }).getByRole('button', { name: '启用', exact: true })).toBeVisible()
  await page.getByRole('row').filter({ hasText: result.member }).getByRole('button', { name: '启用', exact: true }).click()
  await page.getByRole('button', { name: '确认', exact: true }).click()
  await expect(page.getByRole('row').filter({ hasText: result.member }).getByRole('button', { name: '禁用', exact: true })).toBeVisible()
  await page.screenshot({ path: '../server/.runtime/erp-members.png', fullPage: true })
  expect(errors).toEqual([])
})

test('只读后台人员看不到会员状态按钮和商品导航', async ({ page }) => {
  await page.goto('http://127.0.0.1:5173/login')
  await page.getByPlaceholder('请输入账号').fill(result.reader)
  await page.getByPlaceholder('请输入密码').fill('ReaderTest123!')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await expect(page).toHaveURL(/forbidden/)
  await page.getByRole('menuitem', { name: '商城会员' }).click()
  await expect(page.getByRole('heading', { name: '商城会员' })).toBeVisible()
  await expect(page.getByRole('row').filter({ hasText: result.member }).getByRole('button', { name: '禁用', exact: true })).toHaveCount(0)
  await expect(page.getByRole('menuitem', { name: '商品管理' })).toHaveCount(0)
  await page.goto('http://127.0.0.1:5173/product')
  await expect(page).toHaveURL(/forbidden/)
})
