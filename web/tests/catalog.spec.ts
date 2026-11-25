import { test, expect } from '@playwright/test'
import fs from 'node:fs'
const result = JSON.parse(fs.readFileSync('../server/.runtime/catalog-result.json', 'utf8')) as { productId: number }

for (const width of [1440, 1200, 390]) {
  test(`商城浏览与规格选择 ${width}px`, async ({ page }) => {
    const errors: string[] = []
    page.on('pageerror', error => errors.push(error.message))
    page.on('response', response => { if (response.status() >= 400 && response.url().startsWith('http://127.0.0.1')) errors.push(`${response.status()} ${response.url()}`) })
    await page.setViewportSize({ width, height: 900 })
    await page.goto('http://127.0.0.1:5174/')
    await expect(page.getByRole('heading', { name: '为你精选' })).toBeVisible()
    await expect(page.locator('.product-card').first()).toBeVisible()
    await page.screenshot({ path: `../server/.runtime/store-home-${width}.png`, fullPage: true })
    await page.getByRole('link', { name: '全部好物', exact: true }).first().click()
    await expect(page.getByRole('heading', { name: '发现好物' })).toBeVisible()
    await page.getByRole('textbox', { name: '搜索商品' }).fill('素色')
    await page.getByRole('button', { name: '搜索', exact: true }).click()
    await expect(page.locator('.product-card').first()).toContainText('素色')
    await page.goto(`http://127.0.0.1:5174/products/${result.productId}`)
    await expect(page.getByRole('heading', { name: '素色陶瓷马克杯' })).toBeVisible()
    await page.getByText('雾蓝色 / 350ml', { exact: true }).click()
    await expect(page.getByRole('radio', { name: '雾蓝色 / 350ml' })).toBeChecked()
    await expect(page.locator('.detail .price')).toContainText('59.90')
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth + 1)).toBeTruthy()
    await page.screenshot({ path: `../server/.runtime/store-product-${width}.png`, fullPage: true })
    expect(errors).toEqual([])
  })
}

test('ERP 登录与商品、分类、品牌、规格库存页面', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', error => errors.push(error.message))
  await page.setViewportSize({ width: 1440, height: 1000 })
  await page.goto('http://127.0.0.1:5173/login')
  await page.getByPlaceholder('请输入账号').fill('catalog_admin')
  await page.getByPlaceholder('请输入密码').fill('CatalogTest123!')
  await page.getByRole('button', { name: /登\s*录/ }).click()
  await expect(page).toHaveURL(/dashboard/)
  await page.goto('http://127.0.0.1:5173/product')
  await expect(page.getByRole('heading', { name: '商品管理', exact: true })).toBeVisible()
  await page.getByRole('button', { name: '规格库存', exact: true }).first().click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('button', { name: '流水', exact: true }).first().click()
  await expect(page.getByRole('heading', { name: /库存流水/ })).toBeVisible()
  await page.screenshot({ path: '../server/.runtime/erp-stock-history.png', fullPage: true })
  await page.getByRole('button', { name: '关闭此对话框' }).click()
  await page.goto('http://127.0.0.1:5173/product/brand')
  await expect(page.getByRole('heading', { name: '品牌管理' })).toBeVisible()
  await page.goto('http://127.0.0.1:5173/product/category')
  await expect(page.getByText('日用好物', { exact: true }).first()).toBeVisible()
  expect(errors).toEqual([])
})
