<template>
  <section class="avatar-editor-section" aria-label="头像设置">
    <button type="button" class="avatar-editor" aria-label="更换头像" @click="chooseAvatar">
      <el-avatar :size="88" :src="userStore.user?.avatar || undefined" class="profile-avatar">
        {{ avatarText }}
      </el-avatar>
      <span class="avatar-editor__mask">更换头像</span>
    </button>
    <div class="avatar-copy">
      <strong>个人头像</strong>
      <p>支持 PNG、JPG、WebP，上传后可裁剪展示区域。</p>
      <el-button type="primary" plain size="small" @click="chooseAvatar">上传新头像</el-button>
    </div>
    <input ref="avatarFileInput" class="avatar-file-input" type="file" accept="image/png,image/jpeg,image/webp" @change="onAvatarFileChange" />
  </section>

  <el-dialog
    v-model="cropVisible"
    title="裁剪头像"
    width="480px"
    append-to-body
    destroy-on-close
    :close-on-click-modal="!avatarSaving"
    :close-on-press-escape="!avatarSaving"
    @closed="resetCrop"
  >
    <div class="avatar-crop-dialog">
      <div
        ref="cropStage"
        class="avatar-crop-stage"
        @pointerdown.prevent="startCropDrag"
        @pointermove.prevent="moveCropDrag"
        @pointerup="endCropDrag"
        @pointercancel="endCropDrag"
      >
        <img
          v-if="cropImageUrl"
          ref="cropImageElement"
          :src="cropImageUrl"
          :style="cropImageStyle"
          draggable="false"
          alt="待裁剪头像"
          @load="onCropImageLoad"
        />
        <span class="avatar-crop-ring" aria-hidden="true" />
      </div>
      <div class="avatar-zoom-row">
        <span>缩小</span>
        <el-slider v-model="cropZoom" :min="1" :max="3" :step="0.01" :show-tooltip="false" @input="clampCropOffset" />
        <span>放大</span>
      </div>
      <p class="avatar-crop-tip">拖动图片调整位置，圆形区域即最终头像范围。</p>
    </div>
    <template #footer>
      <el-button :disabled="avatarSaving" @click="cropVisible = false">取消</el-button>
      <el-button type="primary" :loading="avatarSaving" @click="saveAvatar">保存头像</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { updateMyAvatar } from '@/api/user'
import { uploadFile } from '@/api/upload'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const avatarFileInput = ref(null)
const cropStage = ref(null)
const cropImageElement = ref(null)
const cropVisible = ref(false)
const cropImageUrl = ref('')
const cropZoom = ref(1)
const avatarSaving = ref(false)
const cropNatural = reactive({ width: 0, height: 0 })
const cropOffset = reactive({ x: 0, y: 0 })
const cropDrag = reactive({ active: false, pointerId: null, startX: 0, startY: 0, offsetX: 0, offsetY: 0 })
const cropStageSize = 280
const cropBaseScale = ref(1)

const avatarText = computed(() => {
  const name = userStore.user?.nickname || userStore.user?.username || ''
  return name ? name.slice(0, 1) : '绘'
})
const cropScale = computed(() => cropBaseScale.value * cropZoom.value)
const cropRenderedSize = computed(() => ({
  width: cropNatural.width * cropScale.value,
  height: cropNatural.height * cropScale.value,
}))
const cropImageStyle = computed(() => ({
  width: `${cropRenderedSize.value.width}px`,
  height: `${cropRenderedSize.value.height}px`,
  left: `${(cropStageSize - cropRenderedSize.value.width) / 2 + cropOffset.x}px`,
  top: `${(cropStageSize - cropRenderedSize.value.height) / 2 + cropOffset.y}px`,
}))

function chooseAvatar() { avatarFileInput.value?.click() }
function onAvatarFileChange(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
    ElMessage.warning('请选择 PNG、JPG 或 WebP 图片')
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('原始图片不能超过 20MB')
    return
  }
  resetCrop()
  cropImageUrl.value = URL.createObjectURL(file)
  cropVisible.value = true
}
function onCropImageLoad(event) {
  cropNatural.width = event.target.naturalWidth
  cropNatural.height = event.target.naturalHeight
  cropBaseScale.value = Math.max(cropStageSize / cropNatural.width, cropStageSize / cropNatural.height)
  cropZoom.value = 1
  cropOffset.x = 0
  cropOffset.y = 0
}
function clampCropOffset() {
  const maxX = Math.max(0, (cropRenderedSize.value.width - cropStageSize) / 2)
  const maxY = Math.max(0, (cropRenderedSize.value.height - cropStageSize) / 2)
  cropOffset.x = Math.min(maxX, Math.max(-maxX, cropOffset.x))
  cropOffset.y = Math.min(maxY, Math.max(-maxY, cropOffset.y))
}
function startCropDrag(event) {
  if (!cropNatural.width || avatarSaving.value) return
  Object.assign(cropDrag, {
    active: true, pointerId: event.pointerId, startX: event.clientX, startY: event.clientY,
    offsetX: cropOffset.x, offsetY: cropOffset.y,
  })
  cropStage.value?.setPointerCapture(event.pointerId)
}
function moveCropDrag(event) {
  if (!cropDrag.active || cropDrag.pointerId !== event.pointerId) return
  cropOffset.x = cropDrag.offsetX + event.clientX - cropDrag.startX
  cropOffset.y = cropDrag.offsetY + event.clientY - cropDrag.startY
  clampCropOffset()
}
function endCropDrag(event) {
  if (cropDrag.pointerId !== event.pointerId) return
  cropDrag.active = false
  if (cropStage.value?.hasPointerCapture(event.pointerId)) cropStage.value.releasePointerCapture(event.pointerId)
  cropDrag.pointerId = null
}
function canvasToBlob(canvas) {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => blob ? resolve(blob) : reject(new Error('头像裁剪失败')), 'image/jpeg', 0.9)
  })
}
async function saveAvatar() {
  if (!cropImageElement.value || !cropNatural.width) return
  avatarSaving.value = true
  try {
    const scale = cropScale.value
    const rendered = cropRenderedSize.value
    const imageLeft = (cropStageSize - rendered.width) / 2 + cropOffset.x
    const imageTop = (cropStageSize - rendered.height) / 2 + cropOffset.y
    const sourceSize = cropStageSize / scale
    const canvas = document.createElement('canvas')
    canvas.width = 512
    canvas.height = 512
    canvas.getContext('2d').drawImage(
      cropImageElement.value, -imageLeft / scale, -imageTop / scale, sourceSize, sourceSize, 0, 0, 512, 512,
    )
    const blob = await canvasToBlob(canvas)
    const avatar = await uploadFile(new File([blob], `avatar-${Date.now()}.jpg`, { type: 'image/jpeg' }))
    const user = await updateMyAvatar({ avatar })
    userStore.updateCurrentUser(user)
    cropVisible.value = false
    ElMessage.success('头像已更新')
  } finally {
    avatarSaving.value = false
  }
}
function resetCrop() {
  if (cropImageUrl.value) URL.revokeObjectURL(cropImageUrl.value)
  cropImageUrl.value = ''
  cropNatural.width = 0
  cropNatural.height = 0
  cropZoom.value = 1
  cropOffset.x = 0
  cropOffset.y = 0
  cropDrag.active = false
  cropDrag.pointerId = null
}
</script>

<style scoped>
.avatar-editor-section { display: flex; align-items: center; gap: 20px; }
.avatar-editor { position: relative; width: 88px; height: 88px; flex: 0 0 88px; overflow: hidden; padding: 0; border: 0; border-radius: 50%; cursor: pointer; background: transparent; box-shadow: 0 5px 18px rgb(43 73 95 / 16%); }
.profile-avatar { color: #fff; background: var(--artiv-primary); font-size: 26px; }
.avatar-editor__mask { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: #fff; background: rgb(0 0 0 / 48%); font-size: 13px; opacity: 0; transition: opacity .2s; }
.avatar-editor:hover .avatar-editor__mask, .avatar-editor:focus-visible .avatar-editor__mask { opacity: 1; }
.avatar-copy strong { display: block; color: var(--artiv-text); font-size: 15px; }
.avatar-copy p { margin: 7px 0 12px; color: var(--artiv-text-secondary); font-size: 13px; }
.avatar-file-input { display: none; }
.avatar-crop-dialog { display: flex; flex-direction: column; align-items: center; }
.avatar-crop-stage { position: relative; width: 280px; height: 280px; overflow: hidden; border-radius: 50%; cursor: grab; touch-action: none; background: #e8edf2; box-shadow: 0 0 0 10px #f3f6f9; user-select: none; }
.avatar-crop-stage:active { cursor: grabbing; }
.avatar-crop-stage img { position: absolute; max-width: none; pointer-events: none; user-select: none; }
.avatar-crop-ring { position: absolute; inset: 1px; border: 2px solid rgb(255 255 255 / 92%); border-radius: 50%; box-shadow: inset 0 0 0 1px rgb(0 0 0 / 10%); pointer-events: none; }
.avatar-zoom-row { display: grid; width: 320px; grid-template-columns: auto 1fr auto; gap: 14px; align-items: center; margin-top: 24px; color: var(--artiv-text-secondary); font-size: 12px; }
.avatar-crop-tip { margin: 4px 0 0; color: var(--artiv-text-secondary); font-size: 12px; }
@media (max-width: 560px) { .avatar-editor-section { align-items: flex-start; } .avatar-zoom-row { width: 100%; } }
</style>
