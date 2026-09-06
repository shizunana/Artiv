// 独立无头浏览器 + 临时 Vite 服务，所有 API 均模拟，不访问真实账户/数据库。
// node tests/guide.browser.mjs；需 Playwright（或通过 ARTIV_PLAYWRIGHT_MODULE 指向已有安装）。
import assert from 'node:assert/strict'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'

const { chromium } = await import(process.env.ARTIV_PLAYWRIGHT_MODULE
  ? pathToFileURL(process.env.ARTIV_PLAYWRIGHT_MODULE).href : 'playwright')
const root = fileURLToPath(new URL('../', import.meta.url))
const server = await createServer({
  configFile: false, root, plugins: [vue()],
  resolve: { alias: { '@': fileURLToPath(new URL('../src', import.meta.url)) } },
  server: { host: '127.0.0.1', port: 0 },
})
let browser
try {
  await server.listen()
  const base = `http://127.0.0.1:${server.httpServer.address().port}`
  browser = await chromium.launch({ headless: true, ...(process.env.ARTIV_BROWSER_CHANNEL ? { channel: process.env.ARTIV_BROWSER_CHANNEL } : {}) })
  const context = await browser.newContext({ viewport: { width: 1280, height: 900 } })
  await context.routeWebSocket(/\/ws(?:\?|$)/, socket => socket.close())
  const page = await context.newPage()
  const navigate = async path => page.evaluate(async path => {
    const { default: router } = await import('/src/router/index.js')
    await router.push(path)
  }, path)
  const errors = []
  page.on('pageerror', e => errors.push(e.message))
  let capabilities = { enabled: true, modelConfigured: false, chatEnabled: false, imageUploadEnabled: false }
  let capabilityCalls = 0
  let deferred = null
  await context.route('**/api/v1/**', async route => {
    if (route.request().url().endsWith('/admin/guide/capabilities')) {
      capabilityCalls++
      const snapshot = { ...capabilities }
      if (deferred) await deferred
      await route.fulfill({ json: { code: 200, data: snapshot } }).catch(() => {})
    } else {
      await route.fulfill({ json: { code: 200, data: { records: [], total: 0, unreadCount: 0 } } })
    }
  })
  await context.addInitScript(() => {
    localStorage.setItem('artiv_token', 'isolated-test-token')
    localStorage.setItem('artiv_user', JSON.stringify({ id: 99999, username: '测试管理员', nickname: '测试管理员', role: 3 }))
  })
  await page.goto(`${base}/guides/employer`)
  const launcher = page.locator('.guide-launcher')
  await launcher.waitFor().catch(async error => {
    console.error('Browser diagnostics:', errors, await page.locator('body').innerText())
    throw error
  })
  await launcher.click()
  const drawer = page.getByRole('dialog')
  await drawer.waitFor()
  assert.match(await drawer.innerText(), /模型尚未配置/)
  assert.match(await drawer.innerText(), /聊天与图片上传尚未开放/)
  assert.equal(await drawer.locator('textarea,input[type=file]').count(), 0)
  assert.equal(Math.round((await drawer.boundingBox()).width), 420)
  assert.equal(await drawer.getAttribute('aria-modal'), 'false')
  assert.equal(await page.locator('.el-overlay').count(), 0)
  assert.equal(await page.evaluate(() => getComputedStyle(document.body).overflow === 'hidden'), false)
  await drawer.evaluate(el => { window.guideTestWindow = el })
  const initialBox = await drawer.boundingBox()
  await page.mouse.move(150, 500)
  await page.mouse.wheel(0, 500)
  await page.waitForFunction(() => scrollY > 0)
  assert.equal(Math.round((await drawer.boundingBox()).y), Math.round(initialBox.y))
  // 原页面链接可点；跨页面窗口保持打开，且组件 DOM 未被重建。
  const callsBeforeNavigation = capabilityCalls
  await page.getByRole('link', { name: '橱窗', exact: true }).click()
  await page.waitForURL('**/stalls')
  assert.equal(await drawer.isVisible(), true)
  assert.equal(await drawer.evaluate(el => el === window.guideTestWindow), true)
  assert.equal(capabilityCalls, callsBeforeNavigation)
  // 焦点在原页面时，Escape 不应误收起另一个独立窗口。
  await page.keyboard.press('Escape')
  assert.equal(await drawer.isVisible(), true)
  console.log('PASS non-modal / page scrolling and clicking / route preserves window instance')
  await page.getByRole('button', { name: '知道了' }).click()
  await drawer.waitFor({ state: 'hidden' })
  await launcher.click()
  assert.equal(await drawer.evaluate(el => el === window.guideTestWindow), true)
  await page.keyboard.press('Escape')
  await drawer.waitFor({ state: 'hidden' })
  console.log('PASS administrator / empty state / 420px / footer and Escape close')

  await page.setViewportSize({ width: 375, height: 812 })
  await launcher.click()
  await drawer.waitFor()
  assert.equal(Math.round((await drawer.boundingBox()).width), 351)
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true)
  await page.keyboard.press('Escape')
  await drawer.waitFor({ state: 'hidden' })
  console.log('PASS mobile drawer fits viewport')

  capabilities.enabled = false
  const disabledResponse = page.waitForResponse(r => r.url().endsWith('/admin/guide/capabilities'))
  await page.reload()
  await disabledResponse
  assert.equal(await launcher.count(), 0)
  console.log('PASS disabled feature hides entry')

  capabilities.enabled = true
  capabilities.modelConfigured = true
  await page.reload()
  await launcher.waitFor()
  await launcher.click()
  await drawer.waitFor()
  assert.match(await drawer.innerText(), /聊天与图片上传尚未开放/)
  await page.evaluate(async () => {
    const { useUserStore } = await import('/src/stores/user.js')
    useUserStore().updateCurrentUser({ id: 99998, role: 2 })
  })
  await launcher.waitFor({ state: 'hidden' })
  await drawer.waitFor({ state: 'hidden' })
  const before = capabilityCalls
  await navigate('/guides/painter-certification')
  assert.equal(capabilityCalls, before)
  console.log('PASS account/role switch clears drawer; ordinary account makes no guide request')

  let release
  deferred = new Promise(resolve => { release = resolve })
  const pendingCapability = page.waitForRequest(r => r.url().endsWith('/admin/guide/capabilities'))
  await page.evaluate(async () => {
    const { useUserStore } = await import('/src/stores/user.js')
    useUserStore().updateCurrentUser({ id: 99997, role: 3 })
  })
  await pendingCapability
  await navigate('/login')
  const lateResponse = page.waitForEvent('requestfailed', {
    predicate: request => request.url().endsWith('/admin/guide/capabilities'), timeout: 3000,
  }).catch(() => null)
  release()
  deferred = null
  await lateResponse
  await page.waitForURL('**/login')
  assert.equal(await launcher.count(), 0)
  assert.equal(await page.evaluate(() => localStorage.getItem('artiv_token')), null)
  await navigate('/register')
  assert.equal(await launcher.count(), 0)
  console.log('PASS login/register/logout hide guide and ignore stale capability response')
  assert.deepEqual(errors, [])
  console.log('PASS no browser runtime errors; all API data above is simulated')
} finally {
  await browser?.close()
  await server.close()
}
