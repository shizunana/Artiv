<template>
  <Teleport to="body">
    <!-- 只隐藏而不销毁：收起/普通路由切换保留内容；不创建遮罩、滚动锁或焦点陷阱。 -->
    <section v-show="opened" id="artiv-guide-window" ref="windowRef" class="guide-window"
      role="dialog" aria-modal="false" aria-labelledby="artiv-guide-title" tabindex="-1"
      @keydown.esc.stop="close">
      <header class="guide-window__header">
        <h2 id="artiv-guide-title" class="guide-window__title">艺绘向导</h2>
        <el-button v-if="capabilities.chatEnabled && guide.sessionId" text size="small" :disabled="guide.stopping" @click="clearChat">清空</el-button>
        <button class="guide-window__close" type="button" aria-label="收起艺绘向导" @click="close">
          <el-icon :size="20"><Close /></el-icon>
        </button>
      </header>
      <div ref="scrollArea" class="guide-window__body">
    <section v-if="!guide.messages.length" class="guide-empty">
      <span class="guide-label">管理员测试版 · {{ capabilities.chatEnabled ? '约稿需求助手' : '基础框架' }}</span>
      <div class="guide-symbol"><el-icon :size="32"><ChatDotRound /></el-icon></div>
      <h2>你的约稿小向导</h2>
      <p>{{ capabilities.chatEnabled ? '发来你的角色图和动作、服装等参考图，一次最多 4 张。说说每张图想参考什么，我会和你确认，再整理成约稿需求说明。' : '未来可协助了解平台规则、整理约稿需求和理解参考图。' }}</p>
      <div class="guide-status" role="status">
        <strong>{{ capabilities.chatEnabled ? '像聊天一样，把需求聊清楚' : '聊天与图片上传尚未开放' }}</strong>
        <p>{{ capabilities.chatEnabled ? '例如：“图 1 是我的 OC，保留外观，只参考图 2 的动作，想约一张全身图。”也可咨询规则、查找公开企划、橱窗和画师，或询问自己企划、订单及收到邀请的状态。' : '模型尚未配置或适配器尚未接入，当前可查看入口和布局。' }}</p>
      </div>
      <p class="guide-disclaimer">向导使用模型辅助理解信息，回答可能出错。询问本人业务时，仅将权限内的必要状态摘要交给模型；不会替你发布、删除或执行交易操作。</p>
    </section>
        <div v-else class="guide-messages" role="log" aria-label="向导对话记录" aria-live="polite" aria-relevant="additions">
          <article v-for="message in guide.messages" :key="message.id" class="guide-message" :class="`guide-message--${message.role}`">
            <span class="guide-message__role">{{ message.role === 'user' ? '你' : '艺绘向导' }}</span>
            <div v-if="messageImages(message).length" class="guide-message__images">
              <figure v-for="(id, index) in messageImages(message)" :key="id">
                <GuidePrivateImage :id="id" :label="`图 ${index + 1}`" />
                <figcaption>图 {{ index + 1 }}</figcaption>
              </figure>
            </div>
            <div v-if="message.text || !messageImages(message).length" class="guide-message__text">{{ message.text || (message.status === 'generating' ? '正在思考…' : '未返回内容') }}</div>
            <span v-if="message.status === 'generating'" class="guide-message__hint">{{ message.toolStatus || '正在生成…' }}</span>
            <span v-else-if="message.status === 'cancelled'" class="guide-message__hint">已停止 · 内容可能不完整</span>
            <span v-else-if="message.status === 'truncated'" class="guide-message__hint">已达到单次输出上限 · 可继续追问</span>
            <span v-else-if="message.status === 'error'" class="guide-message__hint">{{ message.error || '回答中断，请手动重试' }}</span>
            <button v-if="message.role === 'assistant' && message.status === 'complete' && message.text" type="button" class="guide-copy" @click="copyReply(message)">{{ copied === message.id ? '已复制' : message.text.includes('约稿需求说明') ? '复制需求说明' : '复制回复' }}</button>
            <nav v-if="navigableReferences(message).length" class="guide-related" aria-label="相关资源卡片">
              <GuideResourceCard v-for="source in navigableReferences(message)" :key="`${source.kind}:${source.id}`"
                :source="source" :disabled="openingReference" @open="openReference" />
            </nav>
          </article>
        </div>
      </div>
      <footer class="guide-window__footer">
        <p v-if="guide.error" class="guide-error" role="alert">{{ guide.error }}</p>
        <template v-if="capabilities.chatEnabled">
          <div v-if="guide.pendingImages.length" class="guide-attachments" aria-label="待发送参考图">
            <div v-for="(attachment, index) in guide.pendingImages" :key="attachment.key" class="guide-attachment">
              <img :src="attachment.url" :alt="`待发送图 ${index + 1}（仅本地预览）`" />
              <span :title="attachment.file.name">图 {{ index + 1 }}<small>{{ attachment.file.name }}</small></span>
              <button type="button" :aria-label="`移除图 ${index + 1}`" :disabled="guide.busy || guide.stopping" @click="guide.removeImage(attachment.key)">移除</button>
            </div>
          </div>
          <textarea v-model="guide.draft" class="guide-input" aria-label="输入向导消息" :placeholder="guide.pendingImages.length > 1 ? '例如：图 1 是我的 OC，保留外观，只参考图 2 的动作…' : '说说想画什么，或补充需要保留的角色特征…'"
            rows="3" :maxlength="capabilities.maxInputChars || 4000" @keydown.enter="handleEnter" />
          <div class="guide-input__actions">
            <input ref="fileInput" type="file" multiple accept="image/png,image/jpeg" class="guide-file" aria-label="选择角色参考图" :disabled="guide.busy || guide.stopping || guide.pendingImages.length >= 4" @change="selectFile" />
            <el-button v-if="capabilities.imageUploadEnabled" :disabled="guide.busy || guide.stopping || guide.pendingImages.length >= 4" @click="fileInput?.click()"><el-icon><Picture /></el-icon><span>附上参考图 {{ guide.pendingImages.length }}/4</span></el-button>
            <el-button v-if="guide.busy || guide.stopping" :loading="guide.stopping" @click="guide.stop">停止生成</el-button>
            <el-button v-else type="primary" :disabled="!guide.draft.trim() && !guide.pendingImages.length" @click="guide.send">{{ guide.pendingImages.length ? '发送并分析' : '发送' }}</el-button>
          </div>
          <p v-if="guide.pendingImages.length" class="guide-footnote">发送即同意将本条参考图交给 {{ capabilities.provider }} 分析。每次最多 4 张，每张 PNG/JPG ≤ 5 MB；会话内最多 12 张。可按图号说明用途。</p>
          <p v-else class="guide-footnote">回答仅供参考 · 可直接聊天修改、复制需求说明 · 重启后清空</p>
        </template>
        <el-button v-else-if="!capabilities.chatEnabled" @click="close">知道了</el-button>
      </footer>
    </section>
  </Teleport>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { ChatDotRound, Close, Picture } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useGuideStore } from '@/stores/guide'
import { useRouter } from 'vue-router'
import { guideReferenceRoute } from '@/utils/guideReferences'
import { scrollGuideToLatest } from '@/utils/guideScroll'
import GuidePrivateImage from './GuidePrivateImage.vue'
import GuideResourceCard from './GuideResourceCard.vue'
defineProps({ capabilities: { type: Object, default: () => ({}) } })
const guide = useGuideStore()
const router = useRouter()
const openingReference = ref(false)
const opened = defineModel({ type: Boolean, default: false })
const emit = defineEmits(['close'])
const windowRef = ref(null)
const scrollArea = ref(null)
const fileInput = ref(null)
const copied = ref('')
function selectFile(event) {
  guide.selectImages(event.target.files)
  event.target.value = ''
}
const messageImages = message => message.imageIds?.length ? message.imageIds : message.imageId ? [message.imageId] : []
const navigableReferences = message => (message.references || []).filter(source => guideReferenceRoute(source))
async function copyReply(message) {
  const cards = navigableReferences(message).map(source => [source.title, source.card?.category,
    ...(source.card?.fields || []).filter(field => field.value?.trim()).map(field => `${field.label}：${field.value}`),
    source.card?.summary].filter(Boolean).join('\n'))
  try { await navigator.clipboard.writeText([message.text, ...cards].filter(Boolean).join('\n\n')); copied.value = message.id }
  catch (_) { ElMessage.warning('浏览器未允许复制，请选中回复文字手动复制') }
}
async function openReference(source) {
  if (openingReference.value) return
  openingReference.value = true
  try {
    const checked = await guide.resolveReference(source)
    const target = guideReferenceRoute(checked)
    if (target) await router.push(target)
  } finally { openingReference.value = false }
}

function handleEnter(event) {
  if (event.isComposing || event.keyCode === 229 || event.shiftKey) return
  event.preventDefault()
  guide.send()
}

async function clearChat() {
  try { await ElMessageBox.confirm('清空聊天及参考图？正在生成的回答也会停止，清空后无法恢复。', '清空会话', { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' }) }
  catch (_) { return }
  await guide.clear()
}

watch(() => [guide.messages.length, guide.messages.at(-1)?.text, guide.messages.at(-1)?.references], async () => {
  const area = scrollArea.value
  const follow = !area || area.scrollHeight - area.scrollTop - area.clientHeight < 100
  await nextTick()
  if (follow) scrollGuideToLatest(scrollArea.value)
})

function close() {
  opened.value = false
  emit('close')
}

watch(opened, async value => {
  if (!value) return
  // v-show 恢复布局后再定位；第二个 tick 覆盖同轮恢复历史消息的情况。
  await nextTick()
  if (!opened.value) return
  scrollGuideToLatest(scrollArea.value)
  await nextTick()
  if (opened.value) {
    scrollGuideToLatest(scrollArea.value)
    windowRef.value?.focus({ preventScroll: true })
  }
})
</script>

<style scoped>
.guide-window {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  width: min(420px, calc(100vw - 48px));
  height: min(640px, calc(100dvh - 48px));
  box-sizing: border-box;
  background: #fff;
  border: 1px solid #e5edf2;
  border-radius: 16px;
  box-shadow: 0 12px 40px #243b4926;
  overflow: hidden;
  outline: none;
}
.guide-window__header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 20px; flex-shrink: 0; }
.guide-window__title { margin: 0; font-size: 18px; font-weight: 500; }
.guide-window__title { margin-right: auto; }
.guide-copy { margin-top: 6px; border: 0; border-radius: 6px; padding: 5px 8px; color: #087da6; background: #edf8fc; cursor: pointer; font: inherit; font-size: 12px; }
.guide-attachments { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; margin-bottom: 8px; max-height: 132px; overflow-y: auto; }
.guide-attachment { display: flex; align-items: center; gap: 6px; padding: 6px; border: 1px solid #e5edf2; border-radius: 10px; text-align: left; min-width: 0; }
.guide-attachment img { width: 36px; height: 40px; object-fit: contain; border-radius: 6px; flex-shrink: 0; }
.guide-attachment span { flex: 1; min-width: 0; font-size: 12px; overflow-wrap: anywhere; }
.guide-attachment small { display: block; margin-top: 4px; color: #7b8c96; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.guide-attachment button { flex-shrink: 0; border: 0; padding: 6px; color: #087da6; background: transparent; cursor: pointer; }
.guide-attachment button:disabled { opacity: .5; cursor: default; }
.guide-file { display: none; }
.guide-message__images { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; margin-bottom: 8px; }
.guide-message__images figure { margin: 0; min-width: 0; padding: 6px; border: 1px solid #e5edf2; border-radius: 10px; }
.guide-message__images figcaption { color: #7b8c96; font-size: 12px; text-align: center; }
.guide-window__close { display: grid; place-items: center; width: 32px; height: 32px; padding: 0; border: 0; border-radius: 6px; background: transparent; color: #7b8c96; cursor: pointer; }
.guide-window__close:hover { background: #f5f8fa; }
.guide-window__close:focus-visible { outline: 2px solid #00aeef; outline-offset: 2px; }
.guide-window__body { flex: 1; min-height: 0; overflow-y: auto; overscroll-behavior-y: contain; padding: 0 20px; }
.guide-window__footer { flex-shrink: 0; padding: 20px; text-align: right; }
.guide-messages { display: flex; flex-direction: column; gap: 18px; padding: 8px 0 20px; }
.guide-message { max-width: 95%; align-self: flex-start; }
.guide-message--assistant { width: 95%; }
.guide-message--user { align-self: flex-end; }
.guide-message__role { display: block; margin-bottom: 5px; font-size: 12px; color: #7b8c96; }
.guide-message__text { padding: 10px 12px; border-radius: 12px; background: #f5f8fa; color: #344d5c; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.7; font-size: 14px; }
.guide-message--user .guide-message__text { background: #e5f7ff; }
.guide-message__hint { display: block; margin-top: 4px; font-size: 12px; color: #8a7272; }
.guide-related { display: grid; gap: 10px; margin-top: 10px; }
.guide-input { display: block; box-sizing: border-box; width: 100%; max-height: 150px; min-height: 72px; padding: 10px; resize: vertical; border: 1px solid #d8e5ec; border-radius: 10px; font: inherit; font-size: 14px; line-height: 1.5; }
.guide-input:focus { outline: 2px solid #bde9f8; border-color: #00aeef; }
.guide-input__actions { display: flex; align-items: center; gap: 10px; justify-content: space-between; margin-top: 10px; }
.guide-footnote { margin: 8px 0 0; font-size: 11px; text-align: left; color: #8c9ba5; }
.guide-error { margin: 0 0 8px; font-size: 12px; color: #bd5454; text-align: left; }
@media (max-width: 600px) {
  .guide-window { right: 12px; bottom: 12px; width: calc(100vw - 24px); height: min(640px, calc(100dvh - 24px)); }
}
.guide-empty { padding: 12px 4px; color: #526675; line-height: 1.8; }
.guide-label { padding: 5px 10px; border-radius: 14px; background: #edf8fc; color: #1585aa; font-size: 12px; }
.guide-symbol { display: grid; place-items: center; width: 64px; height: 64px; margin-top: 36px; border-radius: 20px; background: #e5f7ff; color: #00a9df; }
h2 { margin: 20px 0 8px; color: #243b49; font-size: 24px; }
p { margin: 8px 0; }
.guide-status { margin-top: 28px; padding: 16px; background: #f5f8fa; border: 1px solid #e5edf2; border-radius: 14px; font-size: 14px; }
.guide-status strong { color: #3a5260; }
.guide-disclaimer { margin-top: 28px; color: #7b8c96; font-size: 13px; }
</style>
