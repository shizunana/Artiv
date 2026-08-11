<template>
  <div class="my-works">
    <div class="wb-card">
      <div class="wb-card__head">
        <h3>我的作品</h3>
        <el-button type="primary" round size="small" @click="dialogVisible = true">+ 上传作品</el-button>
      </div>

      <el-row :gutter="16" v-loading="loading">
        <el-col v-for="work in works" :key="work.id" :span="8" class="work-col">
          <div class="work-card">
            <div class="work-thumb" :style="gradient(work.id)">
              <span class="work-emoji">🎨</span>
            </div>
            <div class="work-info">
              <h4>{{ work.title }}</h4>
              <span class="work-cat">{{ work.category }}</span>
              <el-button link type="danger" size="small" @click="remove(work.id)">删除</el-button>
            </div>
          </div>
        </el-col>
      </el-row>
      <EmptyState v-if="!loading && works.length === 0" description="还没有作品，上传 4 张完成度最高的作品有助于通过认证" />
    </div>

    <el-dialog v-model="dialogVisible" title="上传作品" width="460px">
      <el-form label-position="top">
        <el-form-item label="作品标题" required>
          <el-input v-model="form.title" placeholder="作品名称" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="作品说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="作品图">
          <el-upload drag :auto-upload="false" accept="image/*">
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div>将图片拖到此处，或点击上传</div>
          </el-upload>
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
import { UploadFilled } from '@element-plus/icons-vue'
import { getMyWorks, createWork, deleteWork } from '@/api/work'
import EmptyState from '@/components/common/EmptyState.vue'

const works = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const categories = ['国潮', '插画', '立绘', '场景', '原画', '同人', 'Q版', '古风']
const gradients = [
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
]

const form = reactive({ title: '', category: '', description: '' })

function gradient(id) {
  return { background: gradients[id % gradients.length] }
}

async function load() {
  loading.value = true
  try {
    const data = await getMyWorks({ page: 1, size: 30 })
    works.value = data.records || []
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.title || !form.category) {
    ElMessage.warning('请填写标题与分类')
    return
  }
  submitting.value = true
  try {
    await createWork({ ...form })
    ElMessage.success('上传成功')
    dialogVisible.value = false
    form.title = ''
    form.category = ''
    form.description = ''
    load()
  } finally {
    submitting.value = false
  }
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除该作品吗？', '提示', { type: 'warning' })
  await deleteWork(id)
  ElMessage.success('已删除')
  load()
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

.wb-card__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.wb-card__head h3 {
  margin: 0;
  font-size: 17px;
}

.work-col {
  margin-bottom: 16px;
}

.work-card {
  border: 1px solid var(--huiyue-border);
  border-radius: 8px;
  overflow: hidden;
}

.work-thumb {
  aspect-ratio: 4 / 3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.work-emoji {
  font-size: 44px;
}

.work-info {
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.work-info h4 {
  margin: 0;
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.work-cat {
  font-size: 12px;
  color: var(--huiyue-primary);
  background: var(--huiyue-primary-light);
  padding: 2px 8px;
  border-radius: 10px;
}

.upload-icon {
  font-size: 40px;
  color: var(--huiyue-text-secondary);
}
</style>
