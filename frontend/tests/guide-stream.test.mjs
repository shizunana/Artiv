import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createGuideEventParser, consumeGuideStream } from '../src/utils/guideStream.js'

test('SSE parser handles fragmented CRLF, heartbeat and multiline JSON', () => {
  const events = []
  const parser = createGuideEventParser((name, data) => events.push([name, data]))
  for (const chunk of ': ping\r\n\r\nevent: delta\r\ndata: {\r\ndata: "text":"你好"}\r\n\r\nevent: done\ndata: {"status":"complete"}\n\n') parser.push(chunk)
  assert.deepEqual(events, [['delta', { text: '你好' }], ['done', { status: 'complete' }]])
})
test('UTF-8 Chinese can span every byte boundary', async () => {
  const input = new TextEncoder().encode('event: delta\ndata: {"text":"你好🎨"}\n\nevent: done\ndata: {"status":"complete"}\n\n')
  const body = new ReadableStream({ start(controller) { for (const byte of input) controller.enqueue(Uint8Array.of(byte)); controller.close() } })
  const events = []
  await consumeGuideStream(new Response(body), (name, data) => events.push([name, data]))
  assert.equal(events[0][1].text, '你好🎨')
})
test('incomplete streams and oversized events fail rather than report success', async () => {
  await assert.rejects(consumeGuideStream(new Response('event: delta\ndata: {"text":"partial"}\n\n'), () => {}), /中断/)
  assert.throws(() => createGuideEventParser(() => {}).push('x'.repeat(65537)), /过长/)
})
test('error is a terminal event, no extra events mutate completed output', async () => {
  const events = []
  await consumeGuideStream(new Response('event: error\ndata: {"message":"failed"}\n\nevent: delta\ndata: {"text":"late"}\n\n'), (...event) => events.push(event))
  assert.equal(events.length, 1)
  assert.equal(events[0][0], 'error')
})
