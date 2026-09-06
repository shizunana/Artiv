// 在 Node/Vite SSR 中测试状态机，不启动浏览器，不连接后端，不读取真实账号。
import { test, after } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { createPinia, setActivePinia } from 'pinia'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('../', import.meta.url))
const server = await createServer({ configFile: false, root,
  resolve: { alias: { '@': `${root}src` } },
  plugins: [{ name: 'isolated-guide-api', enforce: 'pre',
    resolveId(id) { if (/\/api\/guide(?:\.js)?$/.test(id)) return '\0guide-test-api' },
    load(id) { if (id === '\0guide-test-api') return 'export const guideJson = (...args) => globalThis.guideFakeApi.json(...args); export const sendGuideStream = (...args) => globalThis.guideFakeApi.stream(...args)' },
  }],
})
after(() => server.close())
const { useGuideStore } = await server.ssrLoadModule('/src/stores/guide.js')
function setup(api) {
  const data = new Map()
  globalThis.sessionStorage = { getItem: key => data.get(key) || null, setItem: (key, value) => data.set(key, value), removeItem: key => data.delete(key) }
  globalThis.guideFakeApi = api
  setActivePinia(createPinia())
  return useGuideStore()
}

test('send streams deltas, preserves next draft and stores only session ID', async () => {
  const guide = setup({ json: async () => ({ id: 'session-1', messages: [] }), stream: async (id, payload, options) => {
    assert.equal(id, 'session-1'); assert.equal(payload.text, 'hello')
    assert.equal(options.token, 'fake-a')
    options.onEvent('delta', { text: '你好' })
    guide.draft = '下一条草稿'
    options.onEvent('done', { status: 'complete' })
  } })
  await guide.activate(1, 'fake-a'); guide.draft = 'hello'; await guide.send()
  assert.equal(guide.messages.at(-1).text, '你好'); assert.equal(guide.messages.at(-1).status, 'complete')
  assert.equal(guide.draft, '下一条草稿'); assert.equal(guide.busy, false)
  assert.equal(sessionStorage.getItem('artiv_guide_session_1'), 'session-1')
})
test('account reset aborts old stream and ignores late callbacks', async () => {
  let release, oldOptions
  const guide = setup({ json: async () => ({ id: 'old-session', messages: [] }), stream: async (id, payload, options) => {
    oldOptions = options; await new Promise(resolve => { release = resolve }); options.onEvent('delta', { text: 'private-old-data' })
  } })
  await guide.activate(1, 'fake-a'); guide.draft = 'old'; const pending = guide.send()
  await new Promise(resolve => setImmediate(resolve))
  guide.reset(); await guide.activate(2, 'fake-b'); release(); await pending
  assert.equal(oldOptions.signal.aborted, true); assert.equal(guide.messages.length, 0); assert.equal(guide.sessionId, '')
})
test('stop aborts stream, explicitly cancels on server and does not auto-retry', async () => {
  const calls = []
  const guide = setup({ json: async (path, options) => { calls.push(path); return { id: 'session-1', messages: [] } }, stream: async (id, payload, options) => {
    await new Promise((resolve, reject) => options.signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError'))))
  } })
  await guide.activate(1, 'fake-a'); guide.draft = 'hello'; const pending = guide.send()
  await new Promise(resolve => setImmediate(resolve)); await guide.stop(); await pending
  assert.equal(guide.messages.at(-1).status, 'cancelled'); assert.equal(guide.busy, false)
  assert.equal(calls.filter(path => path.endsWith('/cancel')).length, 1)
})
test('restore retrieves server history; clear removes session and draft', async () => {
  const guide = setup({ json: async () => ({ id: 'session-1', messages: [{ role: 'assistant', text: '历史', status: 'complete' }] }) })
  sessionStorage.setItem('artiv_guide_session_1', 'session-1')
  await guide.activate(1, 'fake-a'); assert.equal(guide.messages[0].text, '历史')
  guide.draft = 'draft'; await guide.clear(); assert.equal(guide.messages.length, 0); assert.equal(guide.draft, '')
  assert.equal(sessionStorage.getItem('artiv_guide_session_1'), null)
})

test('tool progress and backend references attach to the current answer', async () => {
  const source = { kind: 'artist', id: '21', title: '公开画师', detail: '测试', checkedAt: 'today' }
  const guide = setup({ json: async () => ({ id: 'session-1', messages: [] }), stream: async (id, payload, options) => {
    options.onEvent('tool_status', { message: '正在检索' }); options.onEvent('references', [source]); options.onEvent('done', { status: 'complete' })
  } })
  await guide.activate(1, 'fake-a'); guide.draft = '查找画师'; await guide.send()
  assert.equal(guide.messages.at(-1).toolStatus, '正在检索')
  assert.deepEqual(guide.messages.at(-1).references, [source])
})

test('late reference validation cannot navigate after account switch', async () => {
  let release
  const guide = setup({ json: async path => path.includes('/references/') ? new Promise(resolve => { release = resolve }) : ({ id: 'session-1', messages: [] }) })
  sessionStorage.setItem('artiv_guide_session_1', 'session-1'); await guide.activate(1, 'fake-a')
  const pending = guide.resolveReference({ kind: 'artist', id: '21' })
  guide.reset(); await guide.activate(2, 'fake-b'); release({ kind: 'artist', id: '21' })
  assert.equal(await pending, null)
})

test('connection errors show a Chinese message and never retry automatically', async () => {
  let calls = 0
  const guide = setup({ json: async () => { calls++; throw new TypeError('Failed to fetch') } })
  await guide.activate(1, 'fake-a'); guide.draft = 'hello'; await guide.send()
  assert.match(guide.error, /连接向导服务失败/); assert.equal(calls, 1); assert.equal(guide.busy, false)
})
