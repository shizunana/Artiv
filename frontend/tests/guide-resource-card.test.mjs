import { test, after } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import * as compiler from 'vue/compiler-sfc'
import { createSSRApp } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { fileURLToPath } from 'node:url'

const server = await createServer({ configFile: false, root: fileURLToPath(new URL('../', import.meta.url)), plugins: [vue({ compiler })] })
after(() => server.close())
await server.pluginContainer.buildStart({})
const { default: Card } = await server.ssrLoadModule('/src/components/guide/GuideResourceCard.vue')
const render = (source, disabled = false) => renderToString(createSSRApp(Card, { source, disabled }))

test('resource cards render public fields, escape text and never render raw evidence', async () => {
  const html = await render({ kind: 'project', id: '11', title: '<script>alert(1)</script>',
    detail: 'INTERNAL_QUERY_MARKER', checkedAt: 'INTERNAL_TIME_MARKER', url: 'javascript:alert(1)',
    card: { category: '头像', fields: [{ label: '预算', value: '290～7100 元' }, { label: 'EMPTY_LABEL', value: '' }], summary: '公开需求摘要' } })
  for (const value of ['290～7100 元', '头像', '公开需求摘要', '查看企划', '&lt;script&gt;']) assert.ok(html.includes(value))
  for (const value of ['INTERNAL_QUERY_MARKER', 'INTERNAL_TIME_MARKER', 'EMPTY_LABEL', 'javascript:', '<script>']) assert.ok(!html.includes(value))
  assert.match(html, /<button[^>]*type="button"/)
})

test('legacy references without card data remain clickable and busy state disables navigation', async () => {
  const html = await render({ kind: 'stall', id: '31', title: '头像橱窗', detail: 'DO_NOT_DISPLAY' })
  assert.ok(html.includes('头像橱窗') && html.includes('查看橱窗') && !html.includes('DO_NOT_DISPLAY'))
  const busy = await render({ kind: 'artist', id: '21', title: '画师' }, true)
  assert.match(busy, /<button[^>]*disabled/)
  assert.ok(busy.includes('正在确认…'))
  assert.ok((await render({ kind: 'artist', id: '21', title: '画师' })).includes('查看画师主页'))
})

test('recommendation cards display covers and artist avatars with usable fallbacks', async () => {
  for (const kind of ['project', 'stall', 'artist']) {
    const html = await render({ kind, title: '测试资源', card: {
      coverUrl: '/uploads/cover.png', avatarUrl: '/uploads/avatar.png', creatorName: '测试画师', fields: []
    } })
    assert.ok(html.includes('src="/uploads/cover.png"'))
    assert.equal(html.includes('src="/uploads/avatar.png"'), kind !== 'project')
    assert.ok(html.includes('loading="lazy"') && html.includes('referrerpolicy="no-referrer"'))
  }
  const missing = await render({ kind: 'artist', title: '画师甲', card: { fields: [] } })
  assert.ok(missing.includes('作品预览') && missing.includes('认证画师'))
  assert.ok(!missing.includes('<img'))
  for (const url of ['javascript:alert(1)', 'data:image/svg+xml,evil', '//evil.example/a.png', '/api/v1/admin/private']) {
    const html = await render({ kind: 'artist', title: '画师', card: { coverUrl: url, avatarUrl: url, fields: [] } })
    assert.ok(!html.includes('<img'))
  }
})
