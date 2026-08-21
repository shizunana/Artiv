<template>
  <div class="painter-profile">
    <div class="wb-card">
      <div class="profile-title-row">
        <h3 class="card-title">个人资料与认证</h3>
        <el-button v-if="!editing" type="primary" plain @click="startEdit">编辑主页资料</el-button>
      </div>

      <section class="avatar-editor-section" aria-label="头像设置">
        <button type="button" class="avatar-editor" aria-label="更换头像" @click="chooseAvatar">
          <el-avatar :size="96" :src="userStore.user?.avatar || undefined" class="profile-avatar">
            {{ avatarText }}
          </el-avatar>
          <span class="avatar-editor__mask">更换头像</span>
        </button>
        <div>
          <strong>个人头像</strong>
          <p>点击头像上传图片，并选择要展示的区域。</p>
        </div>
        <input ref="avatarFileInput" class="avatar-file-input" type="file" accept="image/png,image/jpeg,image/webp" @change="onAvatarFileChange" />
      </section>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ userStore.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">
          <el-input v-if="editing" v-model="form.nickname" maxlength="50" show-word-limit placeholder="请输入唯一昵称" />
          <span v-else>{{ userStore.user?.nickname }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="角色">画师</el-descriptions-item>
        <el-descriptions-item label="认证状态">
          <el-tag :type="certType" size="small">{{ certText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="个人简介" :span="2">
          <el-input v-if="editing" v-model="form.bio" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="介绍你的擅长方向、接稿状态等" />
          <span v-else>{{ userStore.user?.bio || '未填写' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="风格标签" :span="2">
          <el-select v-if="editing" v-model="form.tags" multiple filterable class="tag-select" placeholder="请选择题材风格（可多选）">
            <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <span v-else>{{ userStore.user?.tags || '未填写' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="editing" class="profile-edit-actions">
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存展示资料</el-button>
      </div>

      <div class="cert-actions">
        <template v-if="certStatus === 0">
          <el-button type="primary" round :loading="eligibilityLoading" :disabled="!canApplyCertification" @click="openCertification">申请画师认证</el-button>
          <span class="cert-tip">{{ certificationRequirementText }}</span>
        </template>
        <template v-else-if="certStatus === 1">
          <el-alert title="认证审核中，请耐心等待管理员审核" type="warning" :closable="false" show-icon />
        </template>
        <template v-else-if="certStatus === 3">
          <el-alert title="认证被驳回，请完善资料后重新申请" type="error" :closable="false" show-icon />
          <el-button type="primary" round class="mt16" :loading="eligibilityLoading" :disabled="!canApplyCertification" @click="openCertification">重新申请</el-button>
          <span class="cert-tip mt16">{{ certificationRequirementText }}</span>
        </template>
        <template v-else>
          <el-alert title="您已通过画师认证，可以应征企划、开设橱窗" type="success" :closable="false" show-icon />
        </template>
      </div>
    </div>

    <el-dialog v-model="certVisible" title="画师认证申请" width="680px" destroy-on-close>
      <el-steps :active="2" finish-status="success" align-center class="cert-steps">
        <el-step title="阅读须知" /><el-step title="功能介绍" /><el-step title="填写认证信息" /><el-step title="等待审核" />
      </el-steps>
      <el-alert title="管理员将查看你的公开主页、主页简介、公开作品和本次提交信息。申请时必须至少有 4 件公开作品。" type="info" :closable="false" show-icon />
      <el-form label-position="top" class="cert-form">
        <el-form-item label="主页简介"><div class="profile-preview">{{ userStore.user?.bio || '尚未填写，请先关闭窗口并编辑主页资料' }}</div></el-form-item>
        <el-form-item label="常用ID" required><el-input v-model="certForm.commonId" maxlength="100" show-word-limit placeholder="签名或常用昵称" /></el-form-item>
        <el-form-item label="常用站（选填）"><el-input v-model="certForm.commonSite" maxlength="500" placeholder="其他平台个人主页，需以 http:// 或 https:// 开头" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="certVisible=false">取消</el-button><el-button type="primary" :loading="certSubmitting" @click="submitCertificationForm">提交申请</el-button></template>
    </el-dialog>

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
          <span class="avatar-crop-ring" aria-hidden="true"></span>
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
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { submitCertification, updateMyAvatar, updateMyPainterProfile } from '@/api/user'
import { uploadFile } from '@/api/upload'
import { getMyWorks } from '@/api/work'
import { SUBJECT_OPTIONS } from '@/constants/subjects'
import { CERT_STATUS, statusText, statusType } from '@/utils/status'

const userStore = useUserStore()

const certStatus = computed(() => userStore.user?.certStatus ?? 0)
const certText = computed(() => statusText(CERT_STATUS, certStatus.value))
const certType = computed(() => statusType(CERT_STATUS, certStatus.value))
const subjectOptions = SUBJECT_OPTIONS
const editing = ref(false)
const saving = ref(false)
const form = reactive({ nickname: '', bio: '', tags: [] })
const certVisible = ref(false)
const certSubmitting = ref(false)
const certForm = reactive({ commonId: '', commonSite: '' })
const publicWorkCount = ref(0)
const eligibilityLoading = ref(true)
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
const canApplyCertification = computed(() => Boolean(userStore.user?.bio?.trim()) && publicWorkCount.value >= 4)
const certificationRequirementText = computed(() => {
  if (eligibilityLoading.value) return '正在核对主页资料与公开作品…'
  if (!userStore.user?.bio?.trim()) return `请先填写主页简介；当前公开作品 ${publicWorkCount.value} 件。`
  if (publicWorkCount.value < 4) return `当前公开作品 ${publicWorkCount.value} 件，还需公开 ${4 - publicWorkCount.value} 件后才能申请。`
  return `申请条件已满足：主页简介完整，当前公开作品 ${publicWorkCount.value} 件。`
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

function startEdit() {
  form.nickname = userStore.user?.nickname || ''
  form.bio = userStore.user?.bio || ''
  form.tags = (userStore.user?.tags || '').split(',').map(tag => tag.trim()).filter(tag => subjectOptions.includes(tag))
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function saveProfile() {
  const nickname = form.nickname.trim()
  if (!nickname) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    const user = await updateMyPainterProfile({ nickname, bio: form.bio, tags: form.tags })
    userStore.updateCurrentUser(user)
    editing.value = false
    ElMessage.success('展示资料已保存')
  } finally {
    saving.value = false
  }
}

async function refreshCertificationEligibility() {
  eligibilityLoading.value = true
  try {
    const result = await getMyWorks({ page: 1, size: 100 })
    publicWorkCount.value = (result.records || []).filter(work => work.visibility === 1).length
  } finally {
    eligibilityLoading.value = false
  }
}

async function openCertification() {
  await refreshCertificationEligibility()
  if (publicWorkCount.value < 4) {
    ElMessage.warning(`至少公开 4 件作品后才能申请，当前为 ${publicWorkCount.value} 件`)
    return
  }
  if (!userStore.user?.bio?.trim()) {
    ElMessage.warning('请先编辑主页资料并填写个人简介')
    return
  }
  certForm.commonId = userStore.user?.nickname || ''
  certForm.commonSite = ''
  certVisible.value = true
}

async function submitCertificationForm() {
  await refreshCertificationEligibility()
  if (publicWorkCount.value < 4) {
    certVisible.value = false
    ElMessage.warning(`公开作品不足 4 件，当前为 ${publicWorkCount.value} 件，不能提交认证申请`)
    return
  }
  if (!userStore.user?.bio?.trim()) {
    ElMessage.warning('请先编辑主页资料并填写个人简介')
    return
  }
  if (!certForm.commonId.trim()) {
    ElMessage.warning('请填写常用ID')
    return
  }
  certSubmitting.value = true
  try {
    await submitCertification({ commonId: certForm.commonId, commonSite: certForm.commonSite || null })
    await userStore.fetchMe()
    certVisible.value = false
    ElMessage.success('认证申请已提交，请等待管理员审核')
  } finally {
    certSubmitting.value = false
  }
}

function chooseAvatar() {
  avatarFileInput.value?.click()
}

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
  cropDrag.active = true
  cropDrag.pointerId = event.pointerId
  cropDrag.startX = event.clientX
  cropDrag.startY = event.clientY
  cropDrag.offsetX = cropOffset.x
  cropDrag.offsetY = cropOffset.y
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
    const sourceX = -imageLeft / scale
    const sourceY = -imageTop / scale
    const canvas = document.createElement('canvas')
    canvas.width = 512
    canvas.height = 512
    const context = canvas.getContext('2d')
    context.drawImage(cropImageElement.value, sourceX, sourceY, sourceSize, sourceSize, 0, 0, 512, 512)
    const blob = await canvasToBlob(canvas)
    const avatarFile = new File([blob], `avatar-${Date.now()}.jpg`, { type: 'image/jpeg' })
    const avatar = await uploadFile(avatarFile)
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

onMounted(refreshCertificationEligibility)
</script>

<style scoped>
.wb-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

.card-title {
  margin: 0 0 18px;
  font-size: 17px;
}

.profile-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.profile-title-row .card-title {
  margin-bottom: 18px;
}

.avatar-editor-section {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 22px;
  padding: 18px;
  border: 1px solid #e6ebf0;
  border-radius: 8px;
  background: #fafcff;
}

.avatar-editor-section strong {
  color: var(--artiv-text);
  font-size: 15px;
}

.avatar-editor-section p {
  margin: 7px 0 0;
  color: var(--artiv-text-secondary);
  font-size: 13px;
}

.avatar-editor {
  position: relative;
  width: 96px;
  height: 96px;
  flex: 0 0 96px;
  overflow: hidden;
  padding: 0;
  border: 0;
  border-radius: 50%;
  cursor: pointer;
  background: transparent;
  box-shadow: 0 3px 12px rgb(43 73 95 / 16%);
}

.profile-avatar {
  color: #fff;
  background: var(--artiv-primary);
  font-size: 28px;
}

.avatar-editor__mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgb(0 0 0 / 48%);
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.avatar-editor:hover .avatar-editor__mask,
.avatar-editor:focus-visible .avatar-editor__mask {
  opacity: 1;
}

.avatar-file-input {
  display: none;
}

.avatar-crop-dialog {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-crop-stage {
  position: relative;
  width: 280px;
  height: 280px;
  overflow: hidden;
  border-radius: 50%;
  cursor: grab;
  touch-action: none;
  background: #e8edf2;
  box-shadow: 0 0 0 10px #f3f6f9;
  user-select: none;
}

.avatar-crop-stage:active {
  cursor: grabbing;
}

.avatar-crop-stage img {
  position: absolute;
  max-width: none;
  pointer-events: none;
  user-select: none;
}

.avatar-crop-ring {
  position: absolute;
  inset: 1px;
  border: 2px solid rgb(255 255 255 / 92%);
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px rgb(0 0 0 / 10%);
  pointer-events: none;
}

.avatar-zoom-row {
  display: grid;
  width: 320px;
  grid-template-columns: auto 1fr auto;
  gap: 14px;
  align-items: center;
  margin-top: 24px;
  color: var(--artiv-text-secondary);
  font-size: 12px;
}

.avatar-crop-tip {
  margin: 4px 0 0;
  color: var(--artiv-text-secondary);
  font-size: 12px;
}

.tag-select {
  width: 100%;
}

.profile-edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.cert-actions {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.cert-tip {
  color: var(--artiv-text-secondary);
  font-size: 13px;
}

.cert-steps { margin: 4px 0 24px; }
.cert-form { margin-top: 20px; }
.profile-preview { width: 100%; padding: 12px; border-radius: 5px; background: #f5f7fa; color: var(--artiv-text-secondary); }

.mt16 {
  margin-top: 16px;
}

@media (max-width: 600px) {
  .avatar-editor-section {
    align-items: flex-start;
  }

  .avatar-zoom-row {
    width: 100%;
  }
}
</style>
