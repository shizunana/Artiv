<template>
  <div class="my-stalls">
    <div class="wb-card">
      <div class="wb-card__head">
        <h3>我的橱窗</h3>
        <el-button type="primary" round size="small" @click="dialogVisible = true">+ 创建橱窗</el-button>
      </div>

      <el-table :data="stalls" v-loading="loading" empty-text="还没有橱窗，创建一个明码标价的服务吧">
        <el-table-column prop="title" label="服务名称" min-width="160" />
        <el-table-column prop="category" label="类型" width="110" />
        <el-table-column label="价格" width="110">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="description" label="交付说明" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架中' : '已下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="toggle(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" size="small" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="创建橱窗" width="480px">
      <el-form label-position="top">
        <el-form-item label="服务名称" required>
          <el-input v-model="form.title" placeholder="如：国潮插画约稿（单张）" />
        </el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.category" placeholder="选择类型">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格（元）" required>
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="交付说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="交付内容、修改次数、周期等" />
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
import { getMyStalls, createStall, changeStallStatus } from '@/api/stall'

const stalls = ref([])
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const categories = ['国潮插画', '角色立绘', '场景原画', '头像', 'Q版', 'Live2d']
const form = reactive({ title: '', category: '', price: 100, description: '' })

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
  submitting.value = true
  try {
    await createStall({ ...form })
    ElMessage.success('创建成功')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function toggle(row) {
  await changeStallStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  load()
}

async function remove(id) {
  await ElMessageBox.confirm('确定删除该橱窗吗？', '提示', { type: 'warning' })
  await changeStallStatus(id, 0)
  ElMessage.success('已下架')
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
  margin-bottom: 16px;
}

.wb-card__head h3 {
  margin: 0;
  font-size: 17px;
}
</style>
