// 独立解析器便于测试中文跨分块、CRLF、多行 data 和断流；不执行模型返回的 HTML。
export function createGuideEventParser(onEvent) {
  let pending = ''
  let total = 0
  return {
    push(text) {
      total += text.length
      if (total > 2_000_000) throw new Error('回答超过接收容量限制')
      pending += text
      let match
      while ((match = /\r?\n\r?\n/.exec(pending))) {
        const frame = pending.slice(0, match.index)
        pending = pending.slice(match.index + match[0].length)
        if (frame.length > 65536) throw new Error('向导事件过长')
        let name = 'message'
        const data = []
        for (const line of frame.split(/\r?\n/)) {
          if (line.startsWith('event:')) name = line.slice(6).trim()
          if (line.startsWith('data:')) data.push(line.slice(5).replace(/^ /, ''))
        }
        if (data.length) onEvent(name, JSON.parse(data.join('\n')))
      }
      if (pending.length > 65536) throw new Error('向导事件过长')
    },
  }
}

export async function consumeGuideStream(response, onEvent) {
  if (!response.body) throw new Error('浏览器未提供流式响应')
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let terminal = false
  const parser = createGuideEventParser((name, data) => {
    if (terminal) return
    if (name === 'done' || name === 'error') terminal = true
    onEvent(name, data)
  })
  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      parser.push(decoder.decode(value, { stream: true }))
    }
    parser.push(decoder.decode())
    if (!terminal) throw new Error('连接已中断，回答可能不完整；不会自动重发')
  } finally {
    await reader.cancel().catch(() => {})
    reader.releaseLock()
  }
}
