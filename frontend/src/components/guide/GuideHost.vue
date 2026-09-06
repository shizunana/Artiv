<template>
  <template v-if="visible">
    <button v-show="!opened" ref="launcher" class="guide-launcher" type="button" aria-haspopup="dialog" aria-controls="artiv-guide-window" :aria-expanded="opened" @click="opened = true">
      <el-icon :size="20"><ChatDotRound /></el-icon>
      <span>艺绘向导</span>
      <span class="guide-launcher__badge">测试</span>
    </button>
    <ArtivGuideDrawer v-model="opened" :capabilities="capabilities" @close="focusLauncher" />
  </template>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useGuideStore } from '@/stores/guide'
import { getGuideCapabilities } from '@/api/guide'
import ArtivGuideDrawer from './ArtivGuideDrawer.vue'

const userStore = useUserStore()
const guideStore = useGuideStore()
const route = useRoute()
const opened = ref(false)
const launcher = ref(null)
const capabilities = ref(null)
const eligible = computed(() => Boolean(userStore.token && userStore.isAdmin
  && !['Login', 'Register'].includes(route.name)))
const visible = computed(() => eligible.value && capabilities.value?.enabled === true)

async function focusLauncher() {
  await nextTick()
  launcher.value?.focus({ preventScroll: true })
}

watch([() => userStore.token, () => userStore.user?.id, () => userStore.isAdmin, eligible], async (_, __, onCleanup) => {
  // 仅身份/可访问性变化时清理。普通路由切换不重置、不卸载窗口，保留局部状态。
  // 旧账号的迟到响应不能重新打开入口。
  opened.value = false
  guideStore.reset()
  capabilities.value = null
  const controller = new AbortController()
  onCleanup(() => controller.abort())
  if (!eligible.value) return
  try {
    const result = await getGuideCapabilities(controller.signal)
    if (!controller.signal.aborted) {
      capabilities.value = result
      if (result.chatEnabled) await guideStore.activate(userStore.user.id, userStore.token)
    }
  } catch (_) {
    // 可选功能失败时隐藏，不干扰原有页面；重新登录或刷新后重试。
  }
}, { immediate: true })
onBeforeUnmount(() => guideStore.reset())
</script>

<style scoped>
.guide-launcher {
  position: fixed;
  right: 24px;
  bottom: 28px;
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 46px;
  padding: 10px 16px;
  border: 1px solid #bde9f8;
  border-radius: 24px;
  background: #effaff;
  color: #087da6;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 6px 22px #1686ab18;
}
.guide-launcher:hover { background: #dff5ff; }
.guide-launcher:focus-visible { outline: 3px solid #00aeef; outline-offset: 3px; }
.guide-launcher__badge { font-size: 11px; color: #69808e; font-weight: 400; }
@media (max-width: 600px) { .guide-launcher { right: 16px; bottom: 18px; } }
</style>
