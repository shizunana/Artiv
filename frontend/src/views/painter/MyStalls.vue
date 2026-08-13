<template>
  <div class="my-stalls">
    <div class="wb-card">
      <div class="wb-card__head">
        <h3>我的橱窗</h3>
        <el-button type="primary" round size="small" @click="dialogVisible = true">+ 创建橱窗</el-button>
      </div>

      <el-table :data="stalls" v-loading="loading" empty-text="还没有橱窗，创建一个明码标价的服务吧">
        <el-table-column label="封面" width="70">
          <template #default="{ row }">
            <el-image
              v-if="row.cover"
              :src="row.cover"
              fit="cover"
              class="cover-thumb"
              :preview-src-list="[row.cover]"
              preview-teleported
            />
            <span v-else class="cover-none">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="服务名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="category" label="类型" width="80" show-overflow-tooltip />
        <el-table-column label="价格" width="80">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column label="剩余库存" width="90">
          <template #default="{ row }">{{ row.stock ?? 0 }} 件</template>
        </el-table-column>
        <el-table-column prop="description" label="橱窗详情" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="76">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 && row.stock > 0 ? 'success' : 'info'" size="small">
              {{ row.status !== 1 ? '已下架' : row.stock > 0 ? '上架中' : '无库存' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row.id)">详情</el-button>
            <el-button link type="primary" size="small" @click="toggle(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-tooltip
              :disabled="!row.hasUnfinishedOrders"
              content="该橱窗存在未完成订单，暂时无法删除"
              placement="top"
            >
              <span>
                <el-button
                  link
                  type="danger"
                  size="small"
                  :disabled="row.hasUnfinishedOrders"
                  @click="remove(row.id)"
                >删除</el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="创建橱窗" width="640px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="服务名称" required>
          <el-input v-model="form.title" placeholder="如：国潮插画约稿（单张）" />
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
              <el-input-number v-model="p.percent" :min="0" :max="100" class="phase-percent" />
              <span class="phase-unit">%</span>
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMyStalls, createStall, changeStallStatus, deleteStall } from '@/api/stall'
import { uploadFile } from '@/api/upload'
import { SUBJECT_OPTIONS } from '@/constants/subjects'

const stalls = ref([])
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const categories = ['国潮插画', '角色立绘', '场景原画', '头像', 'Q版', 'Live2d']
const subjectOptions = SUBJECT_OPTIONS
const form = reactive({
  title: '',
  category: '',
  price: 100,
  stock: 3,
  deliverTime: '',
  cover: '',
  images: [],
  phases: [
    { name: '初稿', percent: 40 },
    { name: '修改', percent: 30 },
    { name: '终稿', percent: 30 },
  ],
  params: { colorMode: 'RGB', spec: '', format: [], subject: [] },
  description: '',
})

// 橱窗详情图：文件列表（UI）与 URL 列表（提交）
const detailFileList = ref([])

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
    form.cover = ''
    form.images = []
    detailFileList.value = []
    load()
  } finally {
    submitting.value = false
  }
}

function addPhase() {
  form.phases.push({ name: '', percent: 0 })
}

function removePhase(i) {
  form.phases.splice(i, 1)
}

async function toggle(row) {
  await changeStallStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  load()
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除该橱窗吗？', '提示', { type: 'warning' })
  await deleteStall(id)
  // 删除成功后先同步更新当前页面，避免等待重新请求期间仍显示已删除橱窗。
  stalls.value = stalls.value.filter((item) => item.id !== id)
  ElMessage.success('橱窗已删除并下架')
  await load()
}

function goDetail(id) {
  router.push({ name: 'StallDetail', params: { id } })
}

onMounted(load)
</script>

<style scoped>
.wb-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

:deep(.el-table__inner-wrapper::before) {
  display: none;
}

:deep(.el-scrollbar__bar.is-horizontal) {
  display: none;
}

.wb-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.wb-card__head h3 {
  margin: 0;
  font-size: 17px;
}

.cover-thumb {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  display: block;
}

.cover-none {
  font-size: 12px;
  color: var(--huiyue-text-secondary);
}

.cover-upload {
  width: 110px;
  height: 110px;
  border-radius: 8px;
  display: block;
}

.cover-upload-placeholder {
  width: 110px;
  height: 110px;
  border: 1px dashed var(--huiyue-border);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--huiyue-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.cover-upload-placeholder:hover {
  border-color: var(--huiyue-primary);
  color: var(--huiyue-primary);
}

.upload-icon {
  font-size: 24px;
}

.form-tip {
  font-size: 12px;
  color: var(--huiyue-accent);
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

.phase-percent {
  width: 120px;
}

.phase-unit {
  color: var(--huiyue-text-secondary);
  font-size: 13px;
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
  color: var(--huiyue-text-secondary);
  flex-shrink: 0;
}

.param-input {
  flex: 1;
}
</style>
