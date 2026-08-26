<template>
  <div class="project-new">
    <div class="wb-card">
      <h3 class="page-title">发布企划</h3>

      <el-steps :active="step" finish-status="success" align-center class="wizard-steps">
        <el-step title="稿件用途" />
        <el-step title="需求详情" />
        <el-step title="高级选项" />
        <el-step title="预览企划" />
      </el-steps>

      <div v-if="step === 0" class="wizard-body">
        <h4>类型</h4>
        <div class="type-tabs">
          <button
            v-for="category in CATEGORY_OPTIONS"
            :key="category"
            type="button"
            class="category-tab"
            :class="{ 'is-active': form.category === category }"
            @click="form.category = category"
          >
            {{ category }}
          </button>
        </div>

        <h4>稿件用途</h4>
        <div class="usage-group">
          <button
            type="button"
            class="usage-card"
            :class="{ 'is-active': form.usage === 'personal' }"
            @click="form.usage = 'personal'"
          >
            <strong>个人用途</strong>
            <p>稿件仅供个人使用、展示和娱乐，不涉及印刷传播、大规模销售等盈利用途。</p>
            <el-tag size="small" type="success">个人收藏 / 社交展示 / 壁纸头像</el-tag>
          </button>
          <button
            type="button"
            class="usage-card"
            :class="{ 'is-active': form.usage === 'commercial' }"
            @click="form.usage = 'commercial'"
          >
            <strong>商业用途</strong>
            <p>稿件用于商业目的，如游戏宣传、周边贩售、虚拟主播运营等营利性活动。</p>
            <el-tag size="small" type="warning">游戏用图 / 商业宣传 / 周边贩售</el-tag>
          </button>
        </div>

        <div class="form-row">
          <el-form-item label="截稿日期" required class="inline-item">
            <el-date-picker
              v-model="form.deadline"
              type="date"
              placeholder="选择截稿日期（至少 2 天后）"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
            />
          </el-form-item>
          <el-form-item label="稿酬预算（元/张）" required class="inline-item">
            <el-input-number v-model="form.budgetMin" :min="1" :max="100000" placeholder="最低" />
            <span class="range-sep">—</span>
            <el-input-number v-model="form.budgetMax" :min="1" :max="100000" placeholder="最高" />
          </el-form-item>
        </div>
      </div>

      <div v-else-if="step === 1" class="wizard-body">
        <el-form label-position="top">
          <el-form-item label="企划标题" required>
            <el-input v-model="form.title" maxlength="20" show-word-limit placeholder="一句话描述您的需求（最多 20 字）" />
          </el-form-item>
          <el-form-item label="需求描述" required>
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="6"
              maxlength="1000"
              show-word-limit
              placeholder="详细描述风格、内容、尺寸、参考等要求"
            />
          </el-form-item>
          <el-form-item label="参考图">
            <el-upload
              list-type="picture-card"
              :limit="6"
              accept="image/png,image/jpeg,image/gif,image/webp"
              :file-list="imageFileList"
              :http-request="doUpload"
              :on-change="syncImageOrder"
              :on-success="onImageUploadSuccess"
              :on-remove="onImageRemove"
              :on-exceed="onImageExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <p class="field-tip">可上传多张参考图，第一张会作为企划大厅的封面。</p>
          </el-form-item>

          <el-form-item label="创作阶段" required>
            <CreativePhaseEditor v-model="form.phases" />
            <p class="field-tip">画师按顺序提交阶段稿件；最后一个阶段确认后即完成订单并交付原图。</p>
          </el-form-item>

          <section class="collaboration-section">
            <h4>合作方式</h4>
            <el-radio-group v-model="form.mode" @change="handleModeChange">
              <el-radio :value="0">公开招募（全员可见）</el-radio>
              <el-radio :value="1">定向邀请（仅被邀画师可见）</el-radio>
            </el-radio-group>
            <button
              v-if="form.mode === 1"
              type="button"
              class="invite-picker-trigger"
              @click="openInvitePainterDialog"
            >
              <span class="invite-picker-trigger__icon">
                <el-avatar v-if="selectedInvitePainter" :size="40" :src="selectedInvitePainter.avatar || undefined">
                  {{ (selectedInvitePainter.nickname || selectedInvitePainter.username || '画').slice(0, 1) }}
                </el-avatar>
                <el-icon v-else><User /></el-icon>
              </span>
              <span class="invite-picker-trigger__content">
                <strong>{{ selectedInvitePainter ? invitedPainterName : '可选择一位画师发布定向企划' }}</strong>
                <small>{{ selectedInvitePainter ? '企划内容仅被该画师可见，点击可更换' : '企划内容仅被选择画师可见' }}</small>
              </span>
              <el-icon class="invite-picker-trigger__arrow"><ArrowRight /></el-icon>
            </button>
          </section>
        </el-form>
      </div>

      <div v-else-if="step === 2" class="wizard-body advanced-body">
        <div class="advanced-tip">在大多数情况下，使用默认设置即可满足您的需求。</div>

        <section class="option-section">
          <h4>您需要哪些种题材风格</h4>
          <div class="option-grid subject-grid">
            <button
              v-for="subject in subjectCards"
              :key="subject"
              type="button"
              class="option-card"
              :class="{ 'is-active': isSubjectActive(subject) }"
              @click="toggleSubject(subject)"
            >
              {{ subject }}
            </button>
          </div>
        </section>

        <section class="option-section">
          <h4>您需要哪几种稿件格式</h4>
          <div class="option-grid">
            <button
              v-for="format in formatOptions"
              :key="format"
              type="button"
              class="option-card"
              :class="{ 'is-active': form.requirements.formats.includes(format) }"
              @click="toggleFormat(format)"
            >
              {{ format }}
            </button>
          </div>
        </section>

        <section class="option-section">
          <h4>选择画师公开稿件的权限</h4>
          <div class="permission-grid">
            <button
              v-for="permission in permissionOptions"
              :key="permission"
              type="button"
              class="option-card"
              :class="{ 'is-active': form.requirements.publishPermission === permission }"
              @click="form.requirements.publishPermission = permission"
            >
              {{ permission }}
            </button>
          </div>
        </section>
      </div>

      <div v-else class="wizard-body">
        <el-descriptions :column="1" border class="preview-box">
          <el-descriptions-item label="类型">{{ form.category }}</el-descriptions-item>
          <el-descriptions-item label="用途">{{ form.usage === 'personal' ? '个人用途' : '商业用途' }}</el-descriptions-item>
          <el-descriptions-item label="截稿日期">{{ form.deadline || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预算">¥{{ form.budgetMin }} — ¥{{ form.budgetMax }} / 张</el-descriptions-item>
          <el-descriptions-item label="标题">{{ form.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需求描述">{{ form.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="参考图">{{ form.images.length ? `已上传 ${form.images.length} 张` : '未上传' }}</el-descriptions-item>
          <el-descriptions-item label="创作阶段">{{ phaseSummary }}</el-descriptions-item>
          <el-descriptions-item label="合作方式">{{ form.mode === 1 ? '定向邀请' : '公开招募' }}</el-descriptions-item>
          <el-descriptions-item v-if="form.mode === 1" label="受邀画师">{{ invitedPainterName }}</el-descriptions-item>
          <el-descriptions-item label="题材风格">{{ subjectSummary }}</el-descriptions-item>
          <el-descriptions-item label="稿件格式">{{ formatSummary }}</el-descriptions-item>
          <el-descriptions-item label="公开权限">{{ form.requirements.publishPermission }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="wizard-actions">
        <el-button v-if="step > 0" round @click="step -= 1">上一步</el-button>
        <el-button v-if="step < 3" type="primary" round @click="next">下一步</el-button>
        <el-button v-else type="primary" round :loading="submitting" @click="submit">发布企划</el-button>
      </div>
    </div>

    <el-dialog
      v-model="inviteDialogVisible"
      title="选择定向邀请画师"
      width="600px"
      top="8vh"
      append-to-body
      destroy-on-close
      @closed="resetInvitePainterDialog"
    >
      <template #header>
        <div class="invite-dialog-header">
          <span>选择定向邀请画师</span>
          <el-button
            class="followed-painters-button"
            type="primary"
            plain
            round
            size="small"
            :loading="invitePaintersLoading && inviteResultMode === 'following'"
            @click="loadFollowedInvitePainters"
          >
            <el-icon><Star /></el-icon>
            关注的画师
          </el-button>
        </div>
      </template>
      <div class="invite-dialog-body">
        <el-input
          v-model="inviteSearchKeyword"
          clearable
          maxlength="50"
          placeholder="搜索画师名字"
          @keyup.enter="searchInvitePainters"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
          <template #append>
            <el-button :loading="invitePaintersLoading" @click="searchInvitePainters">搜索</el-button>
          </template>
        </el-input>

        <section class="invite-search-section" v-loading="invitePaintersLoading">
          <h4 v-if="inviteResultMode !== 'idle'">
            {{ inviteResultMode === 'following' ? '关注的画师' : '搜索结果' }}
          </h4>
          <div v-if="invitePainters.length" class="invite-painter-results">
            <button
              v-for="painter in invitePainters"
              :key="painter.id"
              type="button"
              class="invite-painter-option"
              :class="{ 'is-selected': pendingInvitePainterId === painter.id }"
              @click="pendingInvitePainterId = painter.id"
            >
              <el-avatar :size="44" :src="painter.avatar">
                {{ (painter.nickname || painter.username || '画').slice(0, 1) }}
              </el-avatar>
              <span class="invite-painter-option__content">
                <strong>{{ painter.nickname || painter.username }}</strong>
                <small>{{ painter.bio || '暂无个人简介' }}</small>
              </span>
              <el-icon v-if="pendingInvitePainterId === painter.id" class="invite-painter-option__check"><Check /></el-icon>
            </button>
          </div>
          <el-empty
            v-else-if="inviteResultMode !== 'idle' && !invitePaintersLoading"
            :image-size="72"
            :description="inviteResultMode === 'following' ? '暂未关注可邀请的认证画师' : '未找到对应的已认证画师'"
          />
          <p v-else class="invite-search-placeholder">可以搜索画师名字，或直接查看已关注的可邀请画师。</p>
        </section>
      </div>

      <template #footer>
        <el-button @click="inviteDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!pendingInvitePainterId" @click="confirmInvitePainter">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Check, Plus, Search, Star, User } from '@element-plus/icons-vue'
import { createProject } from '@/api/project'
import { uploadFile } from '@/api/upload'
import { disableBeforeProjectDeadline, minimumProjectDeadlineText } from '@/utils/projectDeadline'
import { getArtists, getPublicProfile } from '@/api/user'
import { getFollowedPainters } from '@/api/social'
import { useUserStore } from '@/stores/user'
import { SUBJECT_OPTIONS, CATEGORY_OPTIONS } from '@/constants/subjects'
import CreativePhaseEditor from '@/components/common/CreativePhaseEditor.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const step = ref(0)
const submitting = ref(false)
const invitePainters = ref([])
const invitePaintersLoading = ref(false)
const inviteDialogVisible = ref(false)
const inviteSearchKeyword = ref('')
const inviteResultMode = ref('idle')
const pendingInvitePainterId = ref(null)
const selectedInvitePainter = ref(null)
let inviteSearchVersion = 0

const subjectCards = ['不限', ...SUBJECT_OPTIONS]
const formatOptions = ['JPG', 'PNG', 'PSD', 'GIF', 'AI', 'SVG', 'TIF', '其他']
const permissionOptions = ['可以自由发布', '需经委托方同意发布', '不可以公开发布']

const form = reactive({
  category: '头像',
  usage: 'personal',
  deadline: '',
  budgetMin: 300,
  budgetMax: 500,
  title: '',
  description: '',
  tags: [],
  subjects: [],
  requirements: {
    size: '与画师商议',
    colorMode: 'RGB',
    formats: ['JPG', 'PNG'],
    publishPermission: '需经委托方同意发布',
  },
  mode: 0,
  invitePainterId: null,
  images: [],
  phases: [{ name: '初稿' }, { name: '修改' }, { name: '终稿' }],
})

const imageFileList = ref([])
const uploadedImageByUid = new Map()
const subjectSummary = computed(() => form.subjects.length ? form.subjects.join(' / ') : '不限')
const formatSummary = computed(() => form.requirements.formats.length ? form.requirements.formats.join(' / ') : '-')
const phaseSummary = computed(() => form.phases.map((phase) => phase.name.trim()).filter(Boolean).join(' → '))
const invitedPainterName = computed(() => {
  const painter = selectedInvitePainter.value
  return painter ? (painter.nickname || painter.username) : '-'
})

onMounted(async () => {
  const painterId = Number(route.query.invitePainterId)
  if (!Number.isInteger(painterId) || painterId <= 0) return
  try {
    const painter = await getPublicProfile(painterId, 'painter')
    if (Number(painter?.certStatus) !== 2) {
      ElMessage.warning('只能邀请已认证画师')
      return
    }
    form.mode = 1
    form.invitePainterId = painterId
    selectedInvitePainter.value = { ...painter, id: painterId }
  } catch {
    ElMessage.warning('无法读取该画师资料，请重新选择')
  }
})

function uploadedImageUrl(file) {
  const response = file?.response
  const uploadedUrl = uploadedImageByUid.get(file?.uid)
  const candidates = [
    typeof response === 'string' ? response : response?.url,
    uploadedUrl,
    file?.url,
  ]
  return candidates.find((url) => typeof url === 'string' && url && !url.startsWith('blob:')) || ''
}

async function doUpload({ file, onSuccess, onError }) {
  try {
    const url = await uploadFile(file)
    uploadedImageByUid.set(file.uid, url)
    syncImageOrder(null, imageFileList.value)
    onSuccess(url)
  } catch (error) {
    ElMessage.error('图片上传失败')
    onError(error)
  }
}

function syncImageOrder(_file, fileList) {
  imageFileList.value = fileList
  form.images = fileList.map(uploadedImageUrl).filter(Boolean)
}

function onImageUploadSuccess(_response, _file, fileList) {
  syncImageOrder(null, fileList)
}

function onImageRemove(file, fileList) {
  uploadedImageByUid.delete(file?.uid)
  syncImageOrder(null, fileList)
}

function onImageExceed() {
  ElMessage.warning('最多上传 6 张参考图')
}

function handleModeChange(mode) {
  if (mode !== 0) return
  form.invitePainterId = null
  selectedInvitePainter.value = null
}

function openInvitePainterDialog() {
  resetInvitePainterDialog()
  inviteDialogVisible.value = true
}

function resetInvitePainterDialog() {
  inviteSearchVersion += 1
  inviteSearchKeyword.value = ''
  inviteResultMode.value = 'idle'
  invitePainters.value = []
  pendingInvitePainterId.value = null
  invitePaintersLoading.value = false
}

async function searchInvitePainters() {
  const keyword = inviteSearchKeyword.value.trim()
  if (!keyword) {
    inviteResultMode.value = 'idle'
    invitePainters.value = []
    pendingInvitePainterId.value = null
    return
  }
  const version = ++inviteSearchVersion
  invitePaintersLoading.value = true
  inviteResultMode.value = 'search'
  invitePainters.value = []
  pendingInvitePainterId.value = null
  try {
    const data = await getArtists({ page: 1, size: 20, keyword })
    if (version !== inviteSearchVersion) return
    const currentUserId = Number(userStore.user?.id || 0)
    invitePainters.value = (data.records || []).filter((painter) => Number(painter.id) !== currentUserId)
  } finally {
    if (version === inviteSearchVersion) invitePaintersLoading.value = false
  }
}

async function loadFollowedInvitePainters() {
  const version = ++inviteSearchVersion
  inviteSearchKeyword.value = ''
  invitePaintersLoading.value = true
  inviteResultMode.value = 'following'
  invitePainters.value = []
  pendingInvitePainterId.value = null
  try {
    const data = await getFollowedPainters({ page: 1, size: 50 })
    if (version !== inviteSearchVersion) return
    const currentUserId = Number(userStore.user?.id || 0)
    invitePainters.value = (data.records || []).filter((painter) => Number(painter.id) !== currentUserId)
  } finally {
    if (version === inviteSearchVersion) invitePaintersLoading.value = false
  }
}

function confirmInvitePainter() {
  const painter = invitePainters.value.find((item) => item.id === pendingInvitePainterId.value)
  if (!painter) return
  selectedInvitePainter.value = painter
  form.invitePainterId = painter.id
  inviteDialogVisible.value = false
}

function isSubjectActive(subject) {
  return subject === '不限' ? form.subjects.length === 0 : form.subjects.includes(subject)
}

function toggleSubject(subject) {
  if (subject === '不限') {
    form.subjects = []
    return
  }
  const index = form.subjects.indexOf(subject)
  if (index === -1) form.subjects.push(subject)
  else form.subjects.splice(index, 1)
}

function toggleFormat(format) {
  const index = form.requirements.formats.indexOf(format)
  if (index === -1) form.requirements.formats.push(format)
  else form.requirements.formats.splice(index, 1)
}

function disabledDate(date) {
  return disableBeforeProjectDeadline(date)
}

function next() {
  if (step.value === 0) {
    if (!form.deadline) {
      ElMessage.warning('请选择截稿日期')
      return
    }
    if (form.deadline < minimumProjectDeadlineText()) {
      ElMessage.warning('截稿日期至少需要选择当前日期的 2 天后')
      return
    }
    if (!form.budgetMin || !form.budgetMax || form.budgetMin <= 0 || form.budgetMax <= 0 || form.budgetMin > form.budgetMax) {
      ElMessage.warning('请设置正确的预算区间')
      return
    }
  }
  if (step.value === 1) {
    if (!form.title.trim()) {
      ElMessage.warning('请填写企划标题')
      return
    }
    if (!form.description.trim()) {
      ElMessage.warning('请填写需求描述')
      return
    }
    if (form.mode === 1 && !form.invitePainterId) {
      ElMessage.warning('请选择被邀请的画师')
      return
    }
    if (!form.phases.length || form.phases.some((phase) => !phase.name.trim())) {
      ElMessage.warning('请填写完整的创作阶段名称')
      return
    }
  }
  if (step.value === 2 && (!form.requirements.formats.length || !form.requirements.publishPermission)) {
    ElMessage.warning('请至少选择一种稿件格式和公开权限')
    return
  }
  step.value += 1
}

async function submit() {
  submitting.value = true
  try {
    await createProject({
      title: form.title,
      description: form.description,
      budget: form.budgetMax,
      budgetMin: form.budgetMin,
      budgetMax: form.budgetMax,
      deadline: form.deadline,
      tags: form.tags,
      images: form.images.length ? form.images.join(',') : null,
      category: form.category,
      subject: form.subjects,
      requirements: form.requirements,
      phases: JSON.stringify(form.phases.map((phase) => ({ name: phase.name.trim() }))),
      mode: form.mode,
      invitePainterId: form.mode === 1 ? form.invitePainterId : null,
    })
    ElMessage.success('企划发布成功！等待画师应征。')
    router.push({ name: 'DashboardProjects' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.wb-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 28px;
}

.page-title {
  margin: 0 0 24px;
  font-size: 18px;
  color: var(--artiv-text);
}

.wizard-steps {
  margin-bottom: 32px;
}

.wizard-body {
  max-width: 760px;
  margin: 0 auto;
}

.wizard-body h4 {
  margin: 24px 0 14px;
  font-size: 15px;
  color: var(--artiv-text);
}

.type-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.category-tab,
.usage-card,
.option-card {
  font: inherit;
}

.category-tab {
  padding: 8px 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid var(--artiv-border);
  color: var(--artiv-text-secondary);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.category-tab:hover,
.category-tab.is-active {
  color: #fff;
  border-color: var(--artiv-primary);
  background: var(--artiv-primary);
}

.usage-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.usage-card {
  text-align: left;
  border: 1px solid var(--artiv-border);
  border-radius: 8px;
  padding: 18px;
  background: #fff;
  color: var(--artiv-text);
  cursor: pointer;
  transition: all 0.2s;
}

.usage-card:hover,
.usage-card.is-active {
  border-color: var(--artiv-primary);
}

.usage-card.is-active {
  background: var(--artiv-primary-light);
}

.usage-card strong {
  font-size: 15px;
}

.usage-card p {
  color: var(--artiv-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.form-row {
  display: flex;
  gap: 32px;
  margin-top: 24px;
}

.inline-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-sep {
  margin: 0 8px;
  color: var(--artiv-text-secondary);
}

.field-tip {
  width: 100%;
  margin: 8px 0 0;
  color: var(--artiv-text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.collaboration-section {
  margin-top: 8px;
  padding-top: 8px;
}

.collaboration-section h4 {
  margin: 0 0 14px;
}

.invite-picker-trigger {
  display: flex;
  align-items: center;
  width: 400px;
  max-width: 100%;
  margin-top: 14px;
  padding: 12px 14px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: #f7f8fa;
  color: var(--artiv-text);
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: border-color .18s, background .18s;
}

.invite-picker-trigger:hover {
  border-color: var(--artiv-primary);
  background: #f4fbff;
}

.invite-picker-trigger__icon {
  display: grid;
  flex: 0 0 40px;
  width: 40px;
  height: 40px;
  margin-right: 12px;
  place-items: center;
  border-radius: 50%;
  background: #fff;
  font-size: 21px;
}

.invite-picker-trigger__content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.invite-picker-trigger__content strong {
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invite-picker-trigger__content small {
  color: var(--artiv-text-secondary);
  font-size: 12px;
}

.invite-picker-trigger__arrow {
  margin-left: 12px;
  color: var(--artiv-text-secondary);
}

.invite-dialog-body {
  min-height: 390px;
}

.invite-dialog-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-right: 28px;
  color: var(--artiv-text);
  font-size: 18px;
  font-weight: 600;
}

.followed-painters-button {
  font-weight: 500;
}

.invite-search-section {
  min-height: 310px;
  margin-top: 18px;
}

.invite-search-section h4 {
  margin: 0 0 12px;
  font-size: 14px;
}

.invite-search-placeholder {
  margin: 38px 0 0;
  color: var(--artiv-text-secondary);
  font-size: 13px;
  text-align: center;
}

.invite-painter-results {
  display: flex;
  max-height: 300px;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.invite-painter-option {
  display: flex;
  align-items: center;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--artiv-border);
  border-radius: 6px;
  background: #fff;
  color: var(--artiv-text);
  cursor: pointer;
  font: inherit;
  text-align: left;
  transition: border-color .18s, background .18s;
}

.invite-painter-option:hover,
.invite-painter-option.is-selected {
  border-color: var(--artiv-primary);
  background: #f4fbff;
}

.invite-painter-option__content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
  margin-left: 12px;
}

.invite-painter-option__content strong,
.invite-painter-option__content small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invite-painter-option__content strong {
  font-size: 14px;
}

.invite-painter-option__content small {
  color: var(--artiv-text-secondary);
  font-size: 12px;
}

.invite-painter-option__check {
  margin-left: 12px;
  color: var(--artiv-primary);
}

.advanced-body {
  max-width: 664px;
}

.advanced-tip {
  margin: 6px 0 28px;
  padding: 12px 16px;
  border-radius: 6px;
  background: #f5f8fc;
  color: var(--artiv-text-secondary);
  font-size: 13px;
}

.option-section + .option-section {
  margin-top: 30px;
}

.option-section h4 {
  margin: 0 0 14px;
  font-size: 16px;
}

.option-grid,
.permission-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.permission-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.option-card {
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid #d9e0e8;
  border-radius: 6px;
  background: #fff;
  color: var(--artiv-text);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.18s;
}

.option-card:hover,
.option-card.is-active {
  border-color: var(--artiv-primary);
  background: #eff9ff;
  color: var(--artiv-primary);
}

.preview-box {
  max-width: 640px;
  margin: 0 auto;
}

.wizard-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 28px;
}

@media (max-width: 900px) {
  .usage-group,
  .form-row {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .wb-card {
    padding: 20px 16px;
  }

  .option-grid,
  .permission-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
