<template>
  <div class="project-edit wb-card" v-loading="loading">
    <header class="page-header">
      <div>
        <el-button link @click="back">← 返回企划面板</el-button>
        <h2>编辑企划内容</h2>
        <p>仅修改企划展示与创作要求；截稿日期、上下架和删除请在企划面板使用“变更企划”。</p>
      </div>
    </header>

    <el-alert
      v-if="project && project.contentEditable === false"
      title="该企划已存在应征、有效邀请或历史订单，不能再修改完整内容。"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-form v-if="project" class="edit-form" label-position="top" :disabled="project.contentEditable === false">
      <section class="form-section">
        <h3>基本信息</h3>
        <el-form-item label="企划标题（最多 20 字）" required>
          <el-input v-model="form.title" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="需求描述（最多 1000 字）" required>
          <el-input v-model="form.description" type="textarea" :rows="7" maxlength="1000" show-word-limit />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="类型">
            <el-select v-model="form.category" placeholder="请选择类型">
              <el-option v-for="item in CATEGORY_OPTIONS" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="预算区间（元）" required>
            <div class="budget-range">
              <el-input-number v-model="form.budgetMin" :min="0.01" :precision="2" />
              <span>—</span>
              <el-input-number v-model="form.budgetMax" :min="0.01" :precision="2" />
            </div>
          </el-form-item>
        </div>
        <el-form-item label="题材风格">
          <el-checkbox-group v-model="form.subjects">
            <el-checkbox v-for="item in SUBJECT_OPTIONS" :key="item" :value="item">{{ item }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </section>

      <section class="form-section">
        <h3>参考图片</h3>
        <el-upload
          v-model:file-list="imageFileList"
          list-type="picture-card"
          :limit="6"
          :http-request="doUpload"
          :on-remove="handleImageRemove"
          :on-success="handleImageSuccess"
          :disabled="project.contentEditable === false"
        >
          <span class="upload-plus">＋</span>
        </el-upload>
        <p class="field-tip">最多 6 张，第一张作为企划封面。</p>
      </section>

      <section class="form-section">
        <h3>稿件要求</h3>
        <div class="form-grid">
          <el-form-item label="尺寸规格"><el-input v-model="form.requirements.size" /></el-form-item>
          <el-form-item label="颜色模式"><el-input v-model="form.requirements.colorMode" /></el-form-item>
        </div>
        <el-form-item label="稿件格式">
          <el-checkbox-group v-model="form.requirements.formats">
            <el-checkbox v-for="item in FORMAT_OPTIONS" :key="item" :value="item">{{ item }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="发布权限">
          <el-radio-group v-model="form.requirements.publishPermission">
            <el-radio v-for="item in PERMISSION_OPTIONS" :key="item" :value="item">{{ item }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </section>

      <section class="form-section">
        <h3>创作阶段</h3>
        <CreativePhaseEditor v-model="form.phases" />
      </section>

      <footer class="form-actions">
        <el-button @click="back">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="project.contentEditable === false" @click="save">保存修改</el-button>
      </footer>
    </el-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProjectPanel, updateProjectContent } from '@/api/project'
import { uploadFile } from '@/api/upload'
import { CATEGORY_OPTIONS, SUBJECT_OPTIONS } from '@/constants/subjects'
import CreativePhaseEditor from '@/components/common/CreativePhaseEditor.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const project = ref(null)
const imageFileList = ref([])
const uploadedImageByUid = new Map()
const FORMAT_OPTIONS = ['JPG', 'PNG', 'PSD', 'GIF', 'AI', 'SVG', 'TIF', '其他']
const PERMISSION_OPTIONS = ['可以自由发布', '需经委托方同意发布', '不可以公开发布']
const form = reactive({
  title: '', description: '', category: '', budgetMin: 0, budgetMax: 0,
  subjects: [], images: [],
  requirements: { size: '', colorMode: '', formats: [], publishPermission: '' },
  phases: [],
})

function split(value) { return String(value || '').split(',').map((item) => item.trim()).filter(Boolean) }
function parseRequirements(value) {
  try { return { ...form.requirements, ...(JSON.parse(value || '{}') || {}) } } catch { return { ...form.requirements } }
}
function parsePhases(value) {
  try {
    const parsed = JSON.parse(value || '[]')
    if (Array.isArray(parsed) && parsed.length) return parsed.map((item) => ({ name: String(item.name || '') }))
  } catch { /* 使用默认阶段 */ }
  return [{ name: '初稿' }, { name: '修改' }, { name: '终稿' }]
}
function back() { router.push({ name: 'ProjectPanel', params: { id: route.params.id } }) }
function uploadedUrl(file) {
  const response = file?.response
  return [typeof response === 'string' ? response : response?.url, uploadedImageByUid.get(file?.uid), file?.url]
    .find((value) => typeof value === 'string' && value && !value.startsWith('blob:')) || ''
}
function syncImageOrder(_file, files = imageFileList.value) {
  imageFileList.value = files
  form.images = files.map(uploadedUrl).filter(Boolean)
}
function handleImageRemove(_file, files) { syncImageOrder(null, files) }
function handleImageSuccess(_response, _file, files) { syncImageOrder(null, files) }
async function doUpload({ file, onSuccess, onError }) {
  try {
    const url = await uploadFile(file)
    uploadedImageByUid.set(file.uid, url)
    onSuccess(url)
    syncImageOrder()
  } catch (error) { onError(error) }
}
async function load() {
  loading.value = true
  try {
    project.value = await getProjectPanel(route.params.id)
    form.title = project.value.title || ''
    form.description = project.value.description || ''
    form.category = project.value.category || ''
    form.budgetMin = Number(project.value.budgetMin ?? project.value.budget ?? 0)
    form.budgetMax = Number(project.value.budgetMax ?? project.value.budget ?? 0)
    form.subjects = split(project.value.subject)
    form.images = split(project.value.images)
    form.requirements = parseRequirements(project.value.requirements)
    form.phases = parsePhases(project.value.phases)
    imageFileList.value = form.images.map((url, index) => ({ name: `参考图${index + 1}`, url, uid: `existing-${index}` }))
  } finally { loading.value = false }
}
async function save() {
  if (!form.title.trim() || !form.description.trim()) return ElMessage.warning('请填写企划标题和需求描述')
  if (!form.budgetMin || !form.budgetMax || form.budgetMin > form.budgetMax) return ElMessage.warning('请填写正确的预算区间')
  if (!form.phases.length || form.phases.some((item) => !item.name.trim())) return ElMessage.warning('请填写完整的创作阶段')
  saving.value = true
  try {
    await updateProjectContent(route.params.id, {
      title: form.title, description: form.description, category: form.category,
      budgetMin: form.budgetMin, budgetMax: form.budgetMax, subject: form.subjects,
      images: form.images.length ? form.images.join(',') : null,
      requirements: form.requirements,
      phases: JSON.stringify(form.phases.map((item) => ({ name: item.name.trim() }))),
    })
    ElMessage.success('企划内容已更新')
    back()
  } finally { saving.value = false }
}
onMounted(load)
</script>

<style scoped>
.wb-card { min-width: 0; padding: 28px; border-radius: 8px; background: #fff; }
.page-header h2 { margin: 8px 0; font-size: 22px; }
.page-header p,.field-tip { color: var(--artiv-text-secondary); font-size: 13px; }
.edit-form { max-width: 820px; margin: 24px auto 0; }
.form-section { margin-top: 20px; padding: 22px; border: 1px solid var(--artiv-border); border-radius: 8px; }
.form-section h3 { margin: 0 0 20px; font-size: 17px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.budget-range { display: flex; align-items: center; gap: 8px; }
.upload-plus { font-size: 28px; color: var(--artiv-text-secondary); }
.form-actions { display: flex; justify-content: center; gap: 12px; margin-top: 24px; }
@media(max-width:720px){.form-grid{grid-template-columns:1fr}.wb-card{padding:18px}.form-section{padding:16px}.budget-range{flex-wrap:wrap}}
</style>
