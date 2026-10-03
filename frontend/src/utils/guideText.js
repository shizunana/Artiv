/** 助手回复的纯文本展示；不改服务器历史，不解析 HTML，不处理用户原文。 */
export function guidePlainText(value = '') {
  const text = String(value).replace(/\r\n?/g, '\n')
  // 代码内容保持字面值，只移除围栏，避免误删乘号、文件通配符等有效字符。
  let fence = null
  return text.split('\n').map(line => {
    const marker = line.match(/^\s{0,3}(`{3,}|~{3,})(.*)$/)
    if (marker && !fence) { fence = marker[1][0]; return '' }
    if (fence) {
      if (marker && marker[1][0] === fence && !marker[2].trim()) { fence = null; return '' }
      return line
    }
    const code = []
    let result = line.replace(/`([^`\n]+)`/g, (_, content) => {
      code.push(content)
      return `\u0000${code.length - 1}\u0000`
    })
    // 兼容模型将 Markdown 标记本身加反斜杠转义的情况。
    result = result.replace(/\\([*_~`#])/g, '$1')
    if (/^\s{0,3}(?:(?:\*\s*){3,}|(?:-\s*){3,}|(?:_\s*){3,})$/.test(result)) return ''
    result = result.replace(/^\s{0,3}#{1,6}\s+/, '').replace(/\s+#+\s*$/, '')
      .replace(/^\s{0,3}(?:>\s*)+/, '')
      .replace(/^(\s*)[-+*]\s+/, '$1• ')
      .replace(/\*\*([^\n]+?)\*\*/g, '$1')
      .replace(/(^|[^\w])__([^\n]+?)__(?=$|[^\w])/g, '$1$2')
      .replace(/~~([^\n]+?)~~/g, '$1')
      .replace(/(^|[\s（(，。：:；;])\*([^*\s](?:[^*\n]*?[^*\s])?)\*(?=$|[\s，。！？、；：,.!?:;）)])/g, '$1$2')
    // 流式输出可能暂时只有半个加粗标记，也不把它展示给用户。
    result = result.replace(/\*{2,}/g, '')
    return result.replace(/\u0000(\d+)\u0000/g, (_, index) => code[Number(index)] ?? '')
  }).join('\n').replace(/\n{3,}/g, '\n\n').trim()
}
