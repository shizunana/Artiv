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
      <span class="guide-label">管理员测试版 · {{ capabilities.chatEnabled ? '文本对话' : '基础框架' }}</span>
      <div class="guide-symbol"><el-icon :size="32"><ChatDotRound /></el-icon></div>
      <h2>你的约稿小向导</h2>
      <p>{{ capabilities.chatEnabled ? '可以先聊聊你的约稿想法，我会协助你理清需求。' : '未来可协助了解平台规则、整理约稿需求和理解参考图。' }}</p>
      <div class="guide-status" role="status">
        <strong>{{ capabilities.chatEnabled ? '当前支持多轮文字对话' : '聊天与图片上传尚未开放' }}</strong>
        <p>{{ capabilities.chatEnabled ? '可咨询平台规则、查找公开企划和认证画师。回答下方可查看真实来源；不执行发布、删除或交易操作，图片上传尚未开放。' : '模型尚未配置或适配器尚未接入，当前可查看入口和布局。' }}</p>
      </div>
      <p class="guide-disclaimer">向导使用模型辅助理解信息，回答可能出错。不会替你发布、删除或执行交易操作。</p>
    </section>
        <div v-else class="guide-messages" role="log" aria-label="向导对话记录" aria-live="polite" aria-relevant="additions">
          <article v-for="message in guide.messages" :key="message.id" class="guide-message" :class="`guide-message--${message.role}`">
            <span class="guide-message__role">{{ message.role === 'user' ? '你' : '艺绘向导' }}</span>
            <div class="guide-message__text">{{ message.text || (message.status === 'generating' ? '正在思考…' : '未返回内容') }}</div>
            <span v-if="message.status === 'generating'" class="guide-message__hint">{{ message.toolStatus || '正在生成…' }}</span>
            <span v-else-if="message.status === 'cancelled'" class="guide-message__hint">已停止 · 内容可能不完整</span>
            <span v-else-if="message.status === 'truncated'" class="guide-message__hint">已达到单次输出上限 · 可继续追问</span>
            <span v-else-if="message.status === 'error'" class="guide-message__hint">{{ message.error || '回答中断，请手动重试' }}</span>
            <details v-if="message.references?.length" class="guide-references">
              <summary>本次查询依据（{{ message.references.length }}）</summary>
              <div v-for="source in message.references" :key="`${source.kind}:${source.id}`" class="guide-reference">
                <strong>{{ source.title }}</strong>
                <p>{{ source.detail }}</p>
                <small>{{ source.kind === 'rule' ? '规则核对' : '查询时间' }}：{{ source.checkedAt }}</small>
                <button v-if="guideReferenceRoute(source)" class="guide-reference__link" type="button" :disabled="openingReference" @click="openReference(source)">{{ source.kind === 'project' ? '查看企划' : '查看画师主页' }}</button>
              </div>
            </details>
          </article>
        </div>
      </div>
      <footer class="guide-window__footer">
        <p v-if="guide.error" class="guide-error" role="alert">{{ guide.error }}</p>
        <template v-if="capabilities.chatEnabled">
          <textarea v-model="guide.draft" class="guide-input" aria-label="输入向导消息" placeholder="聊聊你的约稿想法，Shift+Enter 换行"
            rows="3" :maxlength="capabilities.maxInputChars || 4000" @keydown.enter="handleEnter" />
          <div class="guide-input__actions">
            <span class="guide-model" :title="capabilities.textModel">{{ capabilities.provider }} · {{ capabilities.textModel }}</span>
            <el-button v-if="guide.busy || guide.stopping" :loading="guide.stopping" @click="guide.stop">停止生成</el-button>
            <el-button v-else type="primary" :disabled="!guide.draft.trim()" @click="guide.send">发送</el-button>
          </div>
          <p class="guide-footnote">回答仅供参考 · 后端重启后清空 · 图片上传尚未开放</p>
        </template>
        <el-button v-else @click="close">知道了</el-button>
      </footer>
    </section>
  </Teleport>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { ChatDotRound, Close } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useGuideStore } from '@/stores/guide'
import { useRouter } from 'vue-router'
import { guideReferenceRoute } from '@/utils/guideReferences'
defineProps({ capabilities: { type: Object, default: () => ({}) } })
const guide = useGuideStore()
const router = useRouter()
const openingReference = ref(false)
const opened = defineModel({ type: Boolean, default: false })
const emit = defineEmits(['close'])
const windowRef = ref(null)
const scrollArea = ref(null)
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
  try { await ElMessageBox.confirm('清空当前向导会话？正在生成的回答也会停止。', '清空会话', { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' }) }
  catch (_) { return }
  await guide.clear()
}

watch(() => [guide.messages.length, guide.messages.at(-1)?.text], async () => {
  const area = scrollArea.value
  const follow = !area || area.scrollHeight - area.scrollTop - area.clientHeight < 100
  await nextTick()
  if (follow && scrollArea.value) scrollArea.value.scrollTop = scrollArea.value.scrollHeight
})

function close() {
  opened.value = false
  emit('close')
}

watch(opened, async value => {
  if (!value) return
  await nextTick()
  if (opened.value) windowRef.value?.focus({ preventScroll: true })
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
.guide-window__close { display: grid; place-items: center; width: 32px; height: 32px; padding: 0; border: 0; border-radius: 6px; background: transparent; color: #7b8c96; cursor: pointer; }
.guide-window__close:hover { background: #f5f8fa; }
.guide-window__close:focus-visible { outline: 2px solid #00aeef; outline-offset: 2px; }
.guide-window__body { flex: 1; min-height: 0; overflow-y: auto; overscroll-behavior-y: contain; padding: 0 20px; }
.guide-window__footer { flex-shrink: 0; padding: 20px; text-align: right; }
.guide-messages { display: flex; flex-direction: column; gap: 18px; padding: 8px 0 20px; }
.guide-message { max-width: 95%; align-self: flex-start; }
.guide-message--user { align-self: flex-end; }
.guide-message__role { display: block; margin-bottom: 5px; font-size: 12px; color: #7b8c96; }
.guide-message__text { padding: 10px 12px; border-radius: 12px; background: #f5f8fa; color: #344d5c; white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.7; font-size: 14px; }
.guide-message--user .guide-message__text { background: #e5f7ff; }
.guide-message__hint { display: block; margin-top: 4px; font-size: 12px; color: #8a7272; }
.guide-references { margin-top: 8px; font-size: 12px; color: #536c7b; }
.guide-references summary { cursor: pointer; color: #087da6; }
.guide-reference { margin-top: 8px; padding: 10px; border: 1px solid #e5edf2; border-radius: 8px; overflow-wrap: anywhere; line-height: 1.6; }
.guide-reference small { color: #7b8c96; }
.guide-reference__link { display: block; margin-top: 8px; border: 0; padding: 4px 0; color: #087da6; background: transparent; font: inherit; cursor: pointer; }
.guide-reference__link:disabled { opacity: .5; cursor: wait; }
.guide-input { display: block; box-sizing: border-box; width: 100%; max-height: 150px; min-height: 72px; padding: 10px; resize: vertical; border: 1px solid #d8e5ec; border-radius: 10px; font: inherit; font-size: 14px; line-height: 1.5; }
.guide-input:focus { outline: 2px solid #bde9f8; border-color: #00aeef; }
.guide-input__actions { display: flex; align-items: center; gap: 10px; justify-content: space-between; margin-top: 10px; }
.guide-model { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 11px; color: #7b8c96; }
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
