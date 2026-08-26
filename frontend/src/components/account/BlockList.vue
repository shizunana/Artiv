<template>
  <section class="settings-card block-card">
    <div class="card-heading">
      <div>
        <h2>已屏蔽用户</h2>
        <p>屏蔽后双方仍可查看公开内容，但不能关注、邀请、应征、下单、建立会话或发送私信。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-loading="loading" class="block-list">
      <el-empty v-if="!loading && !records.length" description="暂无黑名单用户" />
      <article v-for="item in records" :key="item.userId" class="block-item">
        <el-avatar :size="52" :src="item.avatar || undefined">{{ item.nickname?.slice(0, 1) }}</el-avatar>
        <div class="block-copy">
          <div class="block-name">
            <strong>{{ item.nickname || `用户 #${item.userId}` }}</strong>
            <el-tag size="small" type="info">{{ identityText(item) }}</el-tag>
          </div>
          <span>加入时间：{{ formatDateTime(item.blockedAt) }}</span>
        </div>
        <div class="block-actions">
          <el-button link type="primary" @click="viewProfile(item)">查看主页</el-button>
          <el-button link type="danger" :loading="unblockingId === item.userId" @click="remove(item)">解除屏蔽</el-button>
        </div>
      </article>
    </div>

    <el-pagination
      v-if="total > query.size"
      v-model:current-page="query.page"
      :page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="load"
    />
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { getBlockedUsers, unblockUser } from '@/api/governance'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const query = reactive({ page: 1, size: 10 })
const records = ref([])
const total = ref(0)
const loading = ref(false)
const unblockingId = ref(null)

function identityText(item) {
  if (Number(item.certStatus) === 2) return '认证画师'
  return Number(item.role) === 2 ? '画师身份' : '企划方身份'
}

async function load() {
  loading.value = true
  try {
    const data = await getBlockedUsers(query)
    records.value = data?.records || []
    total.value = Number(data?.total || 0)
    if (!records.value.length && query.page > 1) {
      query.page -= 1
      return load()
    }
  } finally {
    loading.value = false
  }
}

function viewProfile(item) {
  router.push({ name: 'ArtistProfile', params: { id: item.userId }, query: { role: Number(item.certStatus) === 2 ? 'painter' : 'employer' } })
}

async function remove(item) {
  try {
    await ElMessageBox.confirm(
      `解除对「${item.nickname || `用户 #${item.userId}`}」的屏蔽后，双方将可以重新进行平台互动。`,
      '解除屏蔽',
      { confirmButtonText: '解除屏蔽', cancelButtonText: '取消', type: 'warning' },
    )
    unblockingId.value = item.userId
    await unblockUser(item.userId)
    ElMessage.success('已解除屏蔽')
    await load()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error
  } finally {
    unblockingId.value = null
  }
}

onMounted(load)
</script>

<style scoped>
.settings-card { margin-bottom: 16px; padding: 24px 26px; border: 1px solid #e4ebf0; border-radius: 12px; background: #fff; box-shadow: 0 7px 24px rgb(42 67 84 / 5%); }
.card-heading { display: flex; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.card-heading h2 { margin: 0 0 5px; color: #24313b; font-size: 17px; }
.card-heading p { max-width: 700px; margin: 0; color: #89949d; font-size: 13px; line-height: 1.7; }
.block-list { min-height: 160px; border-top: 1px solid #edf1f4; }
.block-item { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: 15px; min-height: 88px; padding: 14px 4px; border-bottom: 1px solid #edf1f4; }
.block-copy { min-width: 0; }
.block-name { display: flex; align-items: center; gap: 9px; }
.block-name strong { overflow: hidden; color: #25333d; text-overflow: ellipsis; white-space: nowrap; }
.block-copy > span { display: block; margin-top: 8px; color: #8b96a0; font-size: 12px; }
.block-actions { display: flex; align-items: center; }
.el-pagination { justify-content: flex-end; margin-top: 18px; }
@media (max-width: 620px) { .settings-card { padding: 20px 18px; } .card-heading { align-items: flex-start; flex-direction: column; } .block-item { grid-template-columns: auto 1fr; } .block-actions { grid-column: 1 / -1; justify-content: flex-end; } }
</style>
