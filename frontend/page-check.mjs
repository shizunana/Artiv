import { chromium } from 'playwright-core'
const browser = await chromium.launch({ channel: 'msedge', headless: true })
const page = await browser.newPage({ viewport: { width: 1440, height: 900 } })
const errors = []
page.on('pageerror', (e) => errors.push(String(e)))
await page.goto('http://localhost:5173/projects', { waitUntil: 'networkidle' })
await page.waitForTimeout(1500)

const page1 = await page.evaluate(() => ({
  cards: document.querySelectorAll('.project-card').length,
  total: document.querySelector('.el-pagination__total')?.textContent?.trim(),
  current: document.querySelector('.el-pager li.is-active')?.textContent?.trim(),
  firstTitle: document.querySelector('.project-card h3')?.textContent?.trim(),
}))

// 点击下一页
await page.click('.el-pagination .btn-next')
await page.waitForTimeout(1200)

const page2 = await page.evaluate(() => ({
  cards: document.querySelectorAll('.project-card').length,
  current: document.querySelector('.el-pager li.is-active')?.textContent?.trim(),
  firstTitle: document.querySelector('.project-card h3')?.textContent?.trim(),
}))

// 回到上一页
await page.click('.el-pagination .btn-prev')
await page.waitForTimeout(1000)
const back = await page.evaluate(() => document.querySelector('.el-pager li.is-active')?.textContent?.trim())

console.log('第1页:', JSON.stringify(page1))
console.log('第2页:', JSON.stringify(page2))
console.log('返回第1页:', back)
console.log('errors:', errors.length ? errors[0].slice(0, 150) : '无')
await browser.close()
