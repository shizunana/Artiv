<template>
  <div class="my-works">
    <div class="wb-card">
      <div class="wb-card__head">
        <div>
          <h3>我的作品</h3>
          <p>每页展示 12 件作品；设为公开后会出现在作品广场。</p>
        </div>
        <el-button type="primary" round size="small" @click="openDialog">+ 上传作品</el-button>
      </div>

      <div v-loading="loading" class="my-work-grid">
        <article v-for="work in works" :key="work.id" class="work-card">
          <div class="work-cover">
            <el-image v-if="work.image" :src="work.image" fit="cover" class="work-image" :preview-src-list="[work.image]" preview-teleported />
            <div v-else class="work-placeholder">🎨</div>
            <el-tag class="visibility-tag" size="small" :type="work.visibility === 1 ? 'success' : 'info'">
              {{ work.visibility === 1 ? '公开' : '仅自己可见' }}
            </el-tag>
          </div>
          <div class="work-info">
            <h4 :title="work.title">{{ work.title }}</h4>
            <p>{{ work.category }} · {{ work.subject || '未设置题材' }}</p>
            <el-button link type="danger" size="small" @click="remove(work.id)">删除</el-button>
          </div>
        </article>
      </div>

      <EmptyState v-if="!loading && works.length === 0" description="还没有作品，上传 4 张完成度最高的作品有助于通过认证" />
      <div v-if="total > size" class="pager">
        <Pagination v-model:page="page" v-model:size="size" :total="total" @change="load" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="上传作品" width="680px" top="5vh" append-to-body destroy-on-close class="work-dialog" @closed="resetForm">
      <el-form label-position="top">
        <el-form-item label="作品标题" required>
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="输入作品标题" />
        </el-form-item>
        <div class="form-two-columns">
          <el-form-item label="类型" required>
            <el-select v-model="form.category" placeholder="选择类型">
              <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
            </el-select>
          </el-form-item>
          <el-form-item label="作品可见范围" required>
            <el-radio-group v-model="form.visibility" class="visibility-options">
              <el-radio-button :value="1">公开</el-radio-button>
              <el-radio-button :value="0">仅自己可见</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
        <el-form-item label="题材风格" required>
          <el-select v-model="form.subjects" multiple filterable collapse-tags collapse-tags-tooltip placeholder="选择题材风格（可多选）">
            <el-option v-for="subject in subjectOptions" :key="subject" :label="subject" :value="subject" />
          </el-select>
        </el-form-item>
        <el-form-item label="作品说明">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="介绍作品的创作内容、用途或风格特点" />
        </el-form-item>
        <el-form-item label="作品图" required>
          <el-upload :show-file-list="false" accept="image/png,image/jpeg,image/gif,image/webp" :http-request="uploadImage">
            <el-image v-if="form.image" :src="form.image" fit="cover" class="work-upload-preview" />
            <div v-else class="work-upload-placeholder">
              <el-icon class="upload-icon"><Plus /></el-icon>
              <span>上传作品图</span>
              <small>仅限 1 张，支持 PNG/JPG/GIF/WEBP</small>
            </div>
          </el-upload>
          <span v-if="!form.image" class="form-tip">* 请上传 1 张作品图片</span>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyWorks, createWork, deleteWork } from '@/api/work'
import { uploadFile } from '@/api/upload'
import { CATEGORY_OPTIONS, SUBJECT_OPTIONS } from '@/constants/subjects'
import EmptyState from '@/components/common/EmptyState.vue'
import Pagination from '@/components/common/Pagination.vue'

const works = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const page = ref(1)
const size = ref(12)
const total = ref(0)
const categories = CATEGORY_OPTIONS
const subjectOptions = SUBJECT_OPTIONS
const form = reactive({ title: '', category: '', subjects: [], description: '', image: '', visibility: 1 })

function openDialog() {
  resetForm()
  dialogVisible.value = true
}

function resetForm() {
  form.title = ''
  form.category = ''
  form.subjects = []
  form.description = ''
  form.image = ''
  form.visibility = 1
}

async function uploadImage({ file, onSuccess, onError }) {
  try {
    form.image = await uploadFile(file)
    onSuccess?.({ url: form.image })
    ElMessage.success('作品图片上传成功')
  } catch (error) {
    onError?.(error)
    ElMessage.error('作品图片上传失败')
  }
}

async function load() {
  loading.value = true
  try {
    const data = await getMyWorks({ page: page.value, size: size.value })
    works.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.title || !form.category || !form.subjects.length || !form.image) {
    ElMessage.warning('请完整填写标题、类型、题材风格并上传作品图')
    return
  }
  submitting.value = true
  try {
    await createWork({ ...form, subject: form.subjects.join(',') })
    ElMessage.success(form.visibility === 1 ? '上传成功，作品已公开展示' : '上传成功，作品仅自己可见')
    dialogVisible.value = false
    page.value = 1
    await load()
  } finally {
    submitting.value = false
  }
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除该作品吗？删除后不可恢复。', '删除作品', { type: 'warning' })
  await deleteWork(id)
  ElMessage.success('已删除')
  if (works.value.length === 1 && page.value > 1) page.value -= 1
  await load()
}

onMounted(load)
</script>

<style scoped>
.wb-card { background: #fff; border-radius: 4px; box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08); padding: 24px; }
.wb-card__head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 18px; }
.wb-card__head h3 { margin: 0; font-size: 17px; }
.wb-card__head p { margin: 7px 0 0; color: var(--huiyue-text-secondary); font-size: 13px; }
.my-work-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; min-height: 120px; }
.work-card { border: 1px solid var(--huiyue-border); border-radius: 8px; overflow: hidden; background: #fff; }
.work-cover { position: relative; aspect-ratio: 4 / 3; background: var(--huiyue-section-alt); }
.work-image { width: 100%; height: 100%; display: block; }
.work-placeholder { width: 100%; height: 100%; display: grid; place-items: center; font-size: 42px; }
.visibility-tag { position: absolute; top: 10px; right: 10px; }
.work-info { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 5px 10px; padding: 12px; align-items: center; }
.work-info h4 { margin: 0; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.work-info p { grid-column: 1 / 2; margin: 0; color: var(--huiyue-text-secondary); font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.work-info .el-button { grid-column: 2; grid-row: 1 / 3; }
.pager { display: flex; justify-content: center; margin-top: 28px; }
.form-two-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-two-columns :deep(.el-select) { width: 100%; }
.visibility-options { display: flex; flex-wrap: nowrap; }
.work-upload-placeholder, .work-upload-preview { width: 180px; height: 135px; border-radius: 8px; }
.work-upload-placeholder { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; border: 1px dashed var(--huiyue-border); color: var(--huiyue-text-secondary); }
.work-upload-placeholder small { font-size: 11px; color: #a8abb2; }
.upload-icon { font-size: 32px; }
.form-tip { margin-left: 12px; color: var(--huiyue-danger, #f56c6c); font-size: 12px; }
@media (max-width: 820px) { .my-work-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .form-two-columns { grid-template-columns: 1fr; gap: 0; } }
@media (max-width: 520px) { .my-work-grid { grid-template-columns: 1fr; } .wb-card__head { align-items: stretch; flex-direction: column; } }
</style>
