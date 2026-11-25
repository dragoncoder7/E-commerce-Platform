import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './tests',
  timeout: 60000,
  expect: { timeout: 10000 },
  workers: 1,
  use: { channel: 'chrome', headless: true, locale: 'zh-CN', screenshot: 'only-on-failure', trace: 'retain-on-failure' },
  outputDir: '../server/.runtime/browser-results',
  reporter: [['list'], ['json', { outputFile: '../server/.runtime/browser-results.json' }]],
})
