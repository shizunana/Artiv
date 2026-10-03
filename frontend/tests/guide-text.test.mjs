import { test } from 'node:test'
import assert from 'node:assert/strict'
import { guidePlainText } from '../src/utils/guideText.js'

test('guide text removes formatting but keeps paragraphs, list content and escaped emphasis', () => {
  assert.equal(guidePlainText('## 约稿需求说明\n\n**角色**：蓝发\n* **用途**：头像\n- *微笑*\n\n---\n\\*\\*待定\\*\\*：预算'),
    '约稿需求说明\n\n角色：蓝发\n• 用途：头像\n• 微笑\n\n待定：预算')
  assert.equal(guidePlainText('> __提示__：~~旧文字~~，保留内容。'), '提示：旧文字，保留内容。')
})

test('guide text handles incomplete streaming markers and preserves literal code and maths', () => {
  for (const text of ['**', '**角色', '**角色**', '\\*\\*角色\\*\\*']) {
    assert.ok(!guidePlainText(text).includes('*'))
  }
  assert.equal(guidePlainText('尺寸 3000*4000，2 * 3 = 6；image_ids，C#。'), '尺寸 3000*4000，2 * 3 = 6；image_ids，C#。')
  assert.equal(guidePlainText('2 * 3 * 4\n*.png'), '2 * 3 * 4\n*.png')
  assert.equal(guidePlainText('文件 `*.png` 和 `a**b`\n```txt\n2 * 3\n**literal**\n```'), '文件 *.png 和 a**b\n\n2 * 3\n**literal**')
  assert.equal(guidePlainText('<img src=x onerror=alert(1)>'), '<img src=x onerror=alert(1)>') // Vue 插值继续转义。
})
