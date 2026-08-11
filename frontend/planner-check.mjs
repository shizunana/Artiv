import { chromium } from 'playwright-core'

const BASE = 'http://localhost:5173'
const API = 'http://localhost:8080/api/v1'
const stamp = Date.now().toString().slice(-8)
const username = `pl${stamp}`

const browser = await chromium.launch({ channel: 'msedge', headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
const errors = []
page.on('pageerror', (e) => errors.push(String(e)))

// 1. 注册企划方
console.log('=== 1. 注册企划方:', username, '===')
await page.goto(`${BASE}/register`, { waitUntil: 'networkidle' })
await page.fill('input[placeholder="3-20 位字符"]', username)
await page.fill('input[placeholder="展示给其他用户的名称"]', '新企划方')
await page.fill('input[placeholder="6-32 位"]', 'abc123')
await page.click('.role-option:has-text("企划方")')
await page.click('button:has-text("注 册")')
await page.waitForTimeout(1200)
console.log('注册后 url:', page.url())

// 2. 登录
await page.fill('input[placeholder="请输入用户名"]', username)
await page.fill('input[placeholder="请输入密码"]', 'abc123')
await page.click('button:has-text("登 录")')
await page.waitForTimeout(1200)
console.log('登录后 url:', page.url())

// 3. 首页导航对比
const homeNav = await page.evaluate(() => ({
  url: location.pathname,
  navLinks: [...document.querySelectorAll('.header-bar__nav a')].map((e) => e.textContent.trim()),
  actions: [...document.querySelectorAll('.header-bar__actions *')].map((e) => e.textContent.trim()).filter(Boolean),
  heroTitle: document.querySelector('.hero-unit h1')?.textContent?.trim(),
}))
console.log('首页导航:', JSON.stringify(homeNav, null, 2))

// 4. 工作台各功能页
const pages = [
  '/dashboard',
  '/dashboard/projects',
  '/dashboard/projects/new',
  '/dashboard/projects/1/applications',
  '/dashboard/orders',
  '/dashboard/messages',
]
for (const p of pages) {
  errors.length = 0
  await page.goto(`${BASE}${p}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(900)
  const info = await page.evaluate(() => ({
    url: location.pathname,
    h2: document.querySelector('h2, h3')?.textContent?.trim(),
    sidebar: [...document.querySelectorAll('.wb-sidebar__tab')].map((e) => e.textContent.trim()).slice(0, 4),
    empty: document.querySelector('.el-table__empty-text')?.textContent?.trim() || document.querySelector('.el-empty__description')?.textContent?.trim(),
    alert: document.querySelector('.el-alert__title')?.textContent?.trim(),
    rows: document.querySelectorAll('.el-table__row').length,
  }))
  console.log(`${p}: url=${info.url} h2=${info.h2} rows=${info.rows} empty=${info.empty} alert=${info.alert} errs=${errors.length}`)
  if (errors.length) console.log('   ERR:', errors[0].slice(0, 160))
}

// 5. 对照：画师登录首页
console.log('=== 5. 对照：画师登录首页 ===')
await page.evaluate(() => localStorage.clear())
await page.goto(`${BASE}/login`, { waitUntil: 'networkidle' })
await page.fill('input[placeholder="请输入用户名"]', 'artist')
await page.fill('input[placeholder="请输入密码"]', '123456')
await page.click('button:has-text("登 录")')
await page.waitForTimeout(1000)
const artistNav = await page.evaluate(() => ({
  url: location.pathname,
  navLinks: [...document.querySelectorAll('.header-bar__nav a')].map((e) => e.textContent.trim()),
  actions: [...document.querySelectorAll('.header-bar__actions *')].map((e) => e.textContent.trim()).filter(Boolean),
}))
console.log('画师首页导航:', JSON.stringify(artistNav, null, 2))

await browser.close()
console.log('DONE')
