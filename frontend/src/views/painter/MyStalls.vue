<template>
  <div class="my-stalls">
    <div class="wb-card">
      <header class="stall-header">
        <div>
          <h2>我的橱窗</h2>
          <p>统一查看和管理我发布的绘画服务。</p>
        </div>
        <div class="header-actions">
          <DeleteManageButton @click="deleteVisible = true">删除橱窗</DeleteManageButton>
          <el-button v-if="permissionReady && isCertifiedPainter" type="primary" round @click="openCreateDialog">+ 创建橱窗</el-button>
        </div>
      </header>

      <el-alert
        v-if="permissionReady && !isCertifiedPainter"
        class="certification-alert"
        title="完成画师认证后才能创建橱窗"
        type="warning"
        :closable="false"
        show-icon
      />

      <div v-loading="loading" class="stall-cards">
        <el-empty v-if="!stalls.length" description="还没有橱窗，创建一个明码标价的服务吧" />
        <article
          v-for="stall in stalls"
          :key="stall.id"
          class="stall-card"
          role="link"
          tabindex="0"
          :aria-label="`查看橱窗 ${stall.title}`"
          @click="goDetail(stall.id)"
          @keydown.enter.self="goDetail(stall.id)"
          @keydown.space.self.prevent="goDetail(stall.id)"
        >
          <img v-if="stall.cover" :src="stall.cover" :alt="`${stall.title}封面`" />
          <div v-else class="stall-cover">橱窗</div>

          <div class="stall-main">
            <div class="stall-title-row">
              <h3 :title="stall.title">{{ truncate(stall.title) }}</h3>
              <el-tag :type="stallStatus(stall).type">{{ stallStatus(stall).text }}</el-tag>
            </div>
            <p>类型：{{ stall.category || '未分类' }}</p>
            <p class="stall-detail" :title="stall.description || ''">
              库存：{{ stall.stock ?? 0 }} 件 · 橱窗简介：{{ truncate(stall.description || '暂无简介') }}
            </p>
          </div>

          <div class="stall-price">
            <small>价格</small>
            <strong>¥{{ money(stall.price) }}</strong>
            <div class="stall-actions">
              <el-button type="primary" link @click.stop="goEdit(stall.id)">编辑</el-button>
            </div>
          </div>
        </article>
      </div>
    </div>

    <StallDeleteDialog v-model="deleteVisible" @deleted="handleDeletedStalls" />

    <el-dialog v-model="dialogVisible" title="创建橱窗" width="640px" append-to-body @closed="resetForm">
      <el-form label-position="top">
        <el-form-item label="服务名称" required>
          <el-input v-model="form.title" placeholder="请输入橱窗的服务介绍" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.category" placeholder="选择类型">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图">
          <el-upload
            :show-file-list="false"
            accept="image/png,image/jpeg,image/gif,image/webp"
            :http-request="uploadCover"
          >
            <el-image v-if="form.cover" :src="form.cover" fit="cover" class="cover-upload" />
            <div v-else class="cover-upload-placeholder">
              <el-icon class="upload-icon"><Plus /></el-icon>
              <span>上传封面</span>
            </div>
          </el-upload>
          <span v-if="!form.cover" class="form-tip">* 必填，请上传封面图</span>
        </el-form-item>
        <el-form-item label="橱窗详情图">
          <el-upload
            list-type="picture-card"
            :limit="9"
            accept="image/png,image/jpeg,image/gif,image/webp"
            :file-list="detailFileList"
            :http-request="uploadDetailImage"
            :on-remove="onDetailImageRemove"
            :on-exceed="onDetailImageExceed"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <span v-if="form.images.length === 0" class="form-tip">* 必填，至少上传 1 张橱窗详情图</span>
        </el-form-item>
        <el-form-item label="价格（元）" required>
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="库存（件）" required>
          <el-input-number v-model="form.stock" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="截稿时间" required>
          <el-input v-model="form.deliverTime" placeholder="如：画师接单后 24 小时" />
        </el-form-item>
        <el-form-item label="创作阶段" required>
          <div class="phase-editor">
            <div v-for="(p, i) in form.phases" :key="i" class="phase-row">
              <el-input v-model="p.name" placeholder="阶段名称" class="phase-name" />
              <el-button link type="danger" @click="removePhase(i)">删除</el-button>
            </div>
            <el-button size="small" @click="addPhase">+ 添加阶段</el-button>
          </div>
        </el-form-item>
        <el-form-item label="稿件参数" required>
          <div class="param-editor">
            <div class="param-line">
              <span class="param-label">颜色模式</span>
              <el-select v-model="form.params.colorMode" placeholder="选择颜色模式" class="param-input">
                <el-option label="RGB模式" value="RGB" />
                <el-option label="CMYK模式" value="CMYK" />
              </el-select>
            </div>
            <div class="param-line">
              <span class="param-label">规格</span>
              <el-input v-model="form.params.spec" placeholder="如：800*800 px" class="param-input" />
            </div>
            <div class="param-line">
              <span class="param-label">稿件格式</span>
              <el-select v-model="form.params.format" multiple placeholder="选择格式" class="param-input">
                <el-option v-for="f in ['JPG','PNG','PSD','GIF','WEBP']" :key="f" :label="f" :value="f" />
              </el-select>
            </div>
            <div class="param-line">
              <span class="param-label">题材风格</span>
              <el-select v-model="form.params.subject" multiple placeholder="选择题材风格（可多选）" class="param-input">
                <el-option v-for="s in subjectOptions" :key="s" :label="s" :value="s" />
              </el-select>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="橱窗详情（文字描述）" required>
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="详细介绍成稿内容、偏好类型、不接类型、修改次数、交付周期等" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyStalls, createStall } from '@/api/stall'
import StallDeleteDialog from '@/components/stalls/StallDeleteDialog.vue'
import DeleteManageButton from '@/components/common/DeleteManageButton.vue'
import { uploadFile } from '@/api/upload'
import { CATEGORY_OPTIONS, SUBJECT_OPTIONS } from '@/constants/subjects'
import { useUserStore } from '@/stores/user'

const stalls = ref([])
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const deleteVisible = ref(false)
const categories = CATEGORY_OPTIONS
const subjectOptions = SUBJECT_OPTIONS
function createDefaultForm() {
  return {
    title: '',
    category: '',
    price: 100,
    stock: 3,
    deliverTime: '',
    cover: '',
    images: [],
    phases: [
      { name: '初稿' },
      { name: '修改' },
      { name: '终稿' },
    ],
    params: { colorMode: 'RGB', spec: '', format: [], subject: [] },
    description: '',
  }
}

const form = reactive(createDefaultForm())
const userStore = useUserStore()
const permissionReady = ref(false)
const isCertifiedPainter = computed(() => Number(userStore.user?.certStatus || 0) === 2)

// 橱窗详情图：文件列表（UI）与 URL 列表（提交）
const detailFileList = ref([])

function resetForm() {
  Object.assign(form, createDefaultForm())
  detailFileList.value = []
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

async function uploadDetailImage({ file, onSuccess, onError }) {
  try {
    const url = await uploadFile(file)
    form.images.push(url)
    onSuccess(url)
  } catch {
    ElMessage.error('详情图上传失败')
    onError()
  }
}

function onDetailImageRemove(file) {
  const url = file.response || file.url
  const idx = form.images.indexOf(url)
  if (idx >= 0) form.images.splice(idx, 1)
}

function onDetailImageExceed() {
  ElMessage.warning('最多上传 9 张详情图')
}

async function uploadCover({ file }) {
  try {
    form.cover = await uploadFile(file)
    ElMessage.success('封面上传成功')
  } catch {
    ElMessage.error('封面上传失败')
  }
}

async function load() {
  loading.value = true
  try {
    const data = await getMyStalls({ page: 1, size: 30 })
    stalls.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!isCertifiedPainter.value) {
    ElMessage.error('完成画师认证后才能创建橱窗')
    return
  }
  if (!form.title || !form.category) {
    ElMessage.warning('请填写服务名称与类型')
    return
  }
  if (!form.cover) {
    ElMessage.warning('必须上传封面图')
    return
  }
  if (!form.images.length) {
    ElMessage.warning('必须上传至少一张橱窗详情图')
    return
  }
  if (!form.deliverTime) {
    ElMessage.warning('请填写截稿时间')
    return
  }
  if (form.phases.some((p) => !p.name)) {
    ElMessage.warning('请填写完整的创作阶段名称')
    return
  }
  if (!form.params.spec || !form.params.format.length || !form.params.subject.length) {
    ElMessage.warning('稿件参数（规格/格式/题材风格）为必填')
    return
  }
  if (!form.description.trim()) {
    ElMessage.warning('请填写橱窗详情文字描述')
    return
  }
  submitting.value = true
  try {
    await createStall({
      ...form,
      images: form.images.join(','),
      phases: JSON.stringify(form.phases),
      params: JSON.stringify(form.params),
    })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    resetForm()
    await load()
  } finally {
    submitting.value = false
  }
}

function addPhase() {
  form.phases.push({ name: '' })
}

function removePhase(i) {
  form.phases.splice(i, 1)
}

function goEdit(id) {
  router.push({ name: 'StallDetail', params: { id }, query: { edit: '1' } })
}

function goDetail(id) {
  router.push({ name: 'StallDetail', params: { id } })
}

function handleDeletedStalls(ids) {
  stalls.value = stalls.value.filter((stall) => !ids.includes(stall.id))
}

function truncate(value, limit = 10) {
  const characters = Array.from(String(value || ''))
  return characters.length > limit ? `${characters.slice(0, limit).join('')}...` : characters.join('')
}

function money(value) {
  const amount = Number(value || 0)
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function stallStatus(stall) {
  if (Number(stall.status) !== 1) return { text: '已下架', type: 'info' }
  if (Number(stall.stock || 0) <= 0) return { text: '无库存', type: 'warning' }
  return { text: '上架中', type: 'success' }
}

onMounted(async () => {
  await userStore.fetchMe().catch(() => null)
  permissionReady.value = true
  await load()
})
</script>

<style scoped>
.wb-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}

.stall-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.stall-header h2 { margin: 0 0 7px; font-size: 21px; }
.stall-header p { margin: 0; color: var(--artiv-text-secondary); }
.header-actions { display: flex; align-items: center; gap: 10px; }
.header-actions > .el-button { margin-left: 0; }

.certification-alert {
  margin-top: 18px;
}

.stall-cards { min-width: 0; min-height: 240px; padding-top: 16px; }
.stall-card { display: grid; width: 100%; box-sizing: border-box; grid-template-columns: 92px minmax(0, 1fr) minmax(200px, 240px); align-items: center; gap: 18px; padding: 16px 8px; border-bottom: 1px solid var(--artiv-border); border-radius: 6px; outline: none; cursor: pointer; transition: background-color .18s ease, box-shadow .18s ease; }
.stall-card:hover, .stall-card:focus-visible { background: #f8fcfe; box-shadow: inset 3px 0 0 color-mix(in srgb, var(--artiv-primary) 55%, transparent); }
.stall-card:hover .stall-title-row h3 { color: var(--artiv-primary); }
.stall-card img, .stall-cover { width: 92px; height: 72px; border-radius: 7px; object-fit: cover; }
.stall-cover { display: grid; place-items: center; background: var(--artiv-section-alt); color: var(--artiv-text-secondary); }
.stall-main { min-width: 0; }
.stall-title-row { display: flex; min-width: 0; align-items: center; gap: 10px; }
.stall-title-row h3 { min-width: 0; margin: 0; overflow: hidden; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.stall-main p { margin: 8px 0 0; overflow: hidden; color: var(--artiv-text-secondary); font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.stall-main .stall-detail { color: var(--artiv-text); }
.stall-price { display: grid; min-width: 0; justify-items: end; gap: 7px; }
.stall-price small { color: var(--artiv-text-secondary); }
.stall-price strong { color: var(--artiv-accent); font-size: 18px; }
.stall-actions { display: flex; width: 100%; flex-wrap: wrap; justify-content: flex-end; gap: 4px 8px; }
.stall-actions :deep(.el-button + .el-button) { margin-left: 0; }

.cover-upload {
  width: 110px;
  height: 110px;
  border-radius: 8px;
  display: block;
}

.cover-upload-placeholder {
  width: 110px;
  height: 110px;
  border: 1px dashed var(--artiv-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--artiv-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.cover-upload-placeholder:hover {
  border-color: var(--artiv-primary);
  color: var(--artiv-primary);
}

.upload-icon {
  font-size: 24px;
}

.form-tip {
  font-size: 12px;
  color: var(--artiv-accent);
  margin-left: 12px;
}

.phase-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.phase-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.phase-name {
  width: 200px;
}

.param-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.param-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.param-label {
  width: 70px;
  font-size: 13px;
  color: var(--artiv-text-secondary);
  flex-shrink: 0;
}

.param-input {
  flex: 1;
}

@media (max-width: 720px) {
  .stall-header { flex-direction: column; }
  .stall-card { grid-template-columns: 70px minmax(0, 1fr); }
  .stall-card img, .stall-cover { width: 70px; }
  .stall-price { grid-column: 2; justify-items: start; }
  .stall-actions { justify-content: flex-start; }
}
</style>
