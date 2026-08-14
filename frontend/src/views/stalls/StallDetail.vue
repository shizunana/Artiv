<template>
  <div class="detail-page page-container" v-loading="loading">
    <div v-if="stall" class="stall-detail">
      <!-- 页内导航（参考站 stall-detail__nav） -->
      <nav class="detail-nav">
        <a href="#detail" class="nav-item is-active">橱窗详情</a>
        <a href="#phases" class="nav-item">创作阶段</a>
        <a href="#params" class="nav-item">稿件参数</a>
      </nav>

      <div class="detail-body">
        <!-- 左侧内容 -->
        <main class="detail-main">
          <!-- 封面图（参考站 stall-detail__header） -->
          <div class="stall-cover">
            <el-image
              v-if="stall.cover"
              :src="stall.cover"
              :preview-src-list="[stall.cover]"
              preview-teleported
              fit="contain"
              class="cover-image"
            />
            <div v-else class="cover-placeholder">🛍️</div>
          </div>

          <!-- 橱窗详情 -->
          <section id="detail" class="detail-section">
            <h2 class="detail-title"><span class="title-icon">🖼️</span>橱窗详情</h2>
            <p class="detail-desc">{{ stall.description || '暂无详细说明' }}</p>
            <!-- 示例图（参考站 stall-detail__example-images） -->
            <div class="example-images">
              <el-image
                v-for="(img, i) in detailImages"
                :key="i"
                :src="img"
                :preview-src-list="detailImages"
                :initial-index="i"
                preview-teleported
                fit="contain"
                class="example-image"
              />
            </div>
          </section>

          <!-- 创作阶段 -->
          <section id="phases" class="detail-section">
            <h2 class="detail-title"><span class="title-icon">🎨</span>创作阶段</h2>
            <p class="section-tip">
              画师承诺按以下阶段提供阶段性稿件供企划方审核确认。<br />
              每个阶段均可根据沟通结果再次提交；最后一个阶段确认后完成交易并开放原图。
            </p>
            <ul class="phase-list">
              <li v-for="(p, i) in phaseList" :key="i" class="phase-item">
                <span class="phase-badge">{{ i + 1 }}</span>
                <span class="phase-name">{{ p.name }}</span>
              </li>
            </ul>
          </section>

          <!-- 稿件参数 -->
          <section id="params" class="detail-section">
            <h2 class="detail-title"><span class="title-icon">📋</span>稿件参数</h2>
            <div class="param-grid">
              <div class="param-item"><span class="param-label">橱窗类型</span><span>{{ stall.category }}</span></div>
              <div class="param-item"><span class="param-label">颜色模式</span><span>{{ params.colorMode }}</span></div>
              <div class="param-item"><span class="param-label">规格</span><span>{{ params.spec }}</span></div>
              <div class="param-item"><span class="param-label">稿件格式</span><span>{{ formatText }}</span></div>
              <div class="param-item"><span class="param-label">题材风格</span><span>{{ subjectText }}</span></div>
              <div class="param-item"><span class="param-label">发布权限</span><span>画师可公开发布带水印稿件</span></div>
              <div class="param-item"><span class="param-label">交付方式</span><span>在线交付（分阶段验收）</span></div>
            </div>
          </section>
        </main>

        <!-- 右侧：画师信息 + 橱窗信息（参考站 aside） -->
        <aside class="detail-aside">
          <!-- 橱窗名 + 分类（参考站 stall-info__name） -->
          <div class="stall-name">
            <h2 class="stall-name__text">{{ stall.title }}</h2>
            <el-tag type="primary" size="small" effect="light">{{ stall.category }}</el-tag>
          </div>

          <!-- 画师信息（参考站 stall-info__owner，评分等按需求隐藏） -->
          <div class="painter-card">
            <el-avatar :size="56" class="painter-avatar">绘</el-avatar>
            <div class="painter-info">
              <span class="painter-name">{{ stall.painterName || '画师 #' + stall.painterId }}</span>
              <span class="painter-role">认证画师</span>
            </div>
          </div>

          <div class="stall-info-card">
            <h4>橱窗信息</h4>
            <div class="info-row price-row">
              <span>橱窗价格</span>
              <strong class="price">￥{{ stall.price }}</strong>
            </div>
            <div class="info-row"><span>已售</span><span>0 件</span></div>
            <div class="info-row"><span>库存</span><span :class="{ 'stock-empty': !hasStock }">{{ hasStock ? `${stall.stock} 件` : '无库存' }}</span></div>
            <div class="info-row"><span>截稿时间</span><span>{{ stall.deliverTime || '-' }}</span></div>
          </div>

          <el-button
            type="primary"
            size="large"
            round
            class="buy-btn"
            :disabled="!isOwner && !canPurchase"
            @click="handlePrimaryAction"
          >
            {{ isOwner ? '修改橱窗' : canPurchase ? '立即下单' : !hasStock ? '无库存' : '已下架' }}
          </el-button>
          <p class="buy-tip">{{ isOwner ? '仅可调整库存并追加题材风格，其他交易信息保持不变' : '确认订单后托管固定金额，并建立与画师的沟通会话' }}</p>
        </aside>
      </div>
    </div>

    <EmptyState v-else-if="!loading" description="橱窗不存在或已下架" />
    <el-dialog v-model="confirmVisible" title="确认订单" width="520px">
      <div class="confirm-order">
        <img v-if="stall?.cover" :src="stall.cover" alt="橱窗封面" />
        <div><h3>{{ stall?.title }}</h3><p>画师：{{ stall?.painterName || `#${stall?.painterId}` }}</p><p>创作阶段：{{ phaseList.map(p => p.name).join(' → ') }}</p></div>
      </div>
      <div class="confirm-amount"><span>订单固定金额</span><strong>¥{{ Number(stall?.price || 0).toFixed(2) }}</strong></div>
      <p class="confirm-tip">确认后将从账户余额托管订单金额并扣减 1 件库存。待画师接单前可取消。</p>
      <template #footer><el-button @click="confirmVisible=false">再想想</el-button><el-button type="primary" :loading="buying" @click="buy">确认下单</el-button></template>
    </el-dialog>
    <el-dialog v-model="editVisible" title="修改橱窗" width="520px">
      <el-form label-position="top">
        <el-form-item label="剩余库存">
          <el-input-number v-model="editForm.stock" :min="0" :max="9999" />
          <span class="edit-tip">库存为 0 时，橱窗会从橱窗大厅隐藏。</span>
        </el-form-item>
        <el-form-item label="题材风格">
          <el-select v-model="editForm.subjects" multiple filterable class="edit-subjects" placeholder="选择要追加的题材风格">
            <el-option
              v-for="item in subjectOptions"
              :key="item"
              :label="item"
              :value="item"
              :disabled="currentSubjects.includes(item)"
            />
          </el-select>
          <span class="edit-tip">已有题材风格不可删除，只能继续追加。</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="edit-footer">
          <div class="edit-footer__danger-zone">
            <el-button :loading="changingStatus" @click="toggleStatus">
              {{ stall?.status === 1 ? '下架橱窗' : '上架橱窗' }}
            </el-button>
            <el-tooltip
              :disabled="!stall?.hasUnfinishedOrders"
              content="该橱窗存在未完成订单，暂时无法删除"
              placement="top"
            >
              <span>
                <el-button
                  type="danger"
                  plain
                  :disabled="stall?.hasUnfinishedOrders"
                  :loading="deleting"
                  @click="removeStall"
                >删除橱窗</el-button>
              </span>
            </el-tooltip>
          </div>
          <div>
            <el-button @click="editVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="saveLimitedEdit">保存修改</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changeStallStatus, deleteStall, getStall, updateStall } from '@/api/stall'
import EmptyState from '@/components/common/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { SUBJECT_OPTIONS } from '@/constants/subjects'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const stall = ref(null)
const loading = ref(false)
const buying = ref(false)
const confirmVisible = ref(false)
const editVisible = ref(false)
const saving = ref(false)
const changingStatus = ref(false)
const deleting = ref(false)
const subjectOptions = SUBJECT_OPTIONS
const editForm = ref({ stock: 0, subjects: [] })

const phaseList = computed(() => {
  try {
    const list = JSON.parse(stall.value?.phases || '[]')
    return Array.isArray(list) ? list : []
  } catch {
    return []
  }
})

const params = computed(() => {
  try {
    return JSON.parse(stall.value?.params || '{}')
  } catch {
    return {}
  }
})

const detailImages = computed(() => {
  const imgs = stall.value?.images
  if (!imgs) return []
  return String(imgs).split(',').filter(Boolean)
})

const formatText = computed(() => {
  const f = params.value.format
  return Array.isArray(f) ? f.join('、') : f || '-'
})

const currentSubjects = computed(() => {
  const value = params.value.subject
  if (Array.isArray(value)) return value
  return value ? String(value).split(',').filter(Boolean) : []
})
const subjectText = computed(() => currentSubjects.value.join('、') || '-')
const isOwner = computed(() => Number(userStore.user?.id) === Number(stall.value?.painterId))
const hasStock = computed(() => Number(stall.value?.stock || 0) > 0)
const canPurchase = computed(() => stall.value?.status === 1 && hasStock.value)

async function load() {
  loading.value = true
  try {
    stall.value = await getStall(route.params.id)
    document.title = `${stall.value.title} - 艺绘`
    if (route.query.edit === '1' && Number(userStore.user?.id) === Number(stall.value.painterId)) {
      openEdit()
    }
  } finally {
    loading.value = false
  }
}

async function buy() {
  buying.value = true
  try {
    const { purchaseStall } = await import('@/api/stall')
    const order = await purchaseStall(stall.value.id)
    confirmVisible.value = false
    await userStore.fetchMe()
    ElMessage.success('下单成功，金额已托管并通知画师接单')
    router.push({
      name: 'DashboardOrderDetail',
      params: { id: order.id },
    })
  } catch (e) {
    ElMessage.error('下单失败：' + (e.message || '请先登录'))
  } finally {
    buying.value = false
  }
}

function openConfirm() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'Login', query: { redirect: route.fullPath } })
    return
  }
  confirmVisible.value = true
}

function handlePrimaryAction() {
  if (isOwner.value) {
    openEdit()
    return
  }
  if (canPurchase.value) openConfirm()
}

function openEdit() {
  editForm.value = { stock: Number(stall.value.stock || 0), subjects: [...currentSubjects.value] }
  editVisible.value = true
}

async function saveLimitedEdit() {
  saving.value = true
  try {
    const updated = await updateStall(stall.value.id, editForm.value)
    stall.value = {
      ...stall.value,
      ...updated,
      hasUnfinishedOrders: stall.value.hasUnfinishedOrders,
    }
    editVisible.value = false
    ElMessage.success('橱窗库存与题材风格已更新')
  } finally {
    saving.value = false
  }
}

async function toggleStatus() {
  changingStatus.value = true
  try {
    const nextStatus = stall.value.status === 1 ? 0 : 1
    const updated = await changeStallStatus(stall.value.id, nextStatus)
    stall.value = {
      ...stall.value,
      ...updated,
      hasUnfinishedOrders: stall.value.hasUnfinishedOrders,
    }
    ElMessage.success(nextStatus === 1 ? '橱窗已上架' : '橱窗已下架')
  } finally {
    changingStatus.value = false
  }
}

async function removeStall() {
  await ElMessageBox.confirm(
    '确定删除该橱窗吗？删除后会同时下架，且无法恢复。',
    '删除橱窗',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
  )
  deleting.value = true
  try {
    await deleteStall(stall.value.id)
    ElMessage.success('橱窗已删除并下架')
    editVisible.value = false
    router.replace({ name: 'PainterStalls' })
  } finally {
    deleting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail-page {
  min-height: 60vh;
  padding: 20px 16px 56px;
  max-width: 1100px;
}

/* 页内导航 */
.detail-nav {
  display: flex;
  gap: 28px;
  background: #fff;
  border: 1px solid var(--artiv-border);
  border-radius: 10px;
  padding: 0 24px;
  margin-bottom: 16px;
}

.nav-item {
  padding: 14px 4px;
  font-size: 15px;
  color: var(--artiv-text-secondary);
  border-bottom: 2px solid transparent;
}

.nav-item:hover,
.nav-item.is-active {
  color: var(--artiv-primary);
  border-bottom-color: var(--artiv-primary);
}

/* 主体两栏 */
.detail-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.detail-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stall-cover {
  background: #fff;
  border: 1px solid var(--artiv-border);
  border-radius: 12px;
  padding: 10px;
}

.cover-image {
  width: 100%;
  border-radius: 8px;
  cursor: zoom-in;
}

.cover-image :deep(.el-image__inner) {
  width: 100%;
  height: auto;
  max-height: 480px;
  object-fit: contain;
  display: block;
}

.cover-placeholder {
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: var(--artiv-text-secondary);
}

.detail-section {
  background: #fff;
  border: 1px solid var(--artiv-border);
  border-radius: 12px;
  padding: 24px 28px;
  scroll-margin-top: 80px;
}

.detail-title {
  margin: 0 0 18px;
  font-size: 17px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 18px;
}

.example-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 10px;
  border-radius: 10px;
  background: var(--artiv-section-alt);
  margin-bottom: 16px;
  padding: 10px;
}

.example-image {
  width: 100%;
  border-radius: 8px;
  cursor: zoom-in;
}

.example-image :deep(.el-image__inner) {
  width: 100%;
  max-height: 300px;
  object-fit: contain;
  display: block;
}

.detail-desc {
  margin: 0;
  color: var(--artiv-text);
  line-height: 1.9;
  white-space: pre-wrap;
}

.section-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--artiv-text-secondary);
}

.phase-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.phase-item {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  position: relative;
}

.phase-item::after {
  content: '';
  position: absolute;
  left: 26px;
  top: calc(100% + 2px);
  width: 1px;
  height: 10px;
  background: var(--artiv-primary);
}

.phase-item:last-child::after {
  display: none;
}

.phase-badge {
  color: var(--artiv-primary);
  border: 1px solid var(--artiv-primary);
  border-radius: 12px;
  font-size: 12px;
  padding: 3px 10px;
  flex-shrink: 0;
}

.phase-name {
  font-size: 15px;
  color: var(--artiv-text);
}

.param-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.param-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  background: var(--artiv-section-alt);
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
}

.param-label {
  color: var(--artiv-text-secondary);
  flex-shrink: 0;
}

/* 右侧 aside */
.detail-aside {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: sticky;
  top: 80px;
}

.painter-card,
.stall-info-card {
  background: #fff;
  border: 1px solid var(--artiv-border);
  border-radius: 12px;
  padding: 20px;
}

.stall-name {
  background: #fff;
  border: 1px solid var(--artiv-border);
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.stall-name__text {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: var(--artiv-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.painter-card {
  display: flex;
  align-items: center;
  gap: 14px;
}

.painter-avatar {
  background: var(--artiv-primary);
  color: #fff;
  font-size: 22px;
}

.painter-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.painter-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--artiv-text);
}

.painter-role {
  font-size: 12px;
  color: var(--artiv-primary);
  background: var(--artiv-primary-light);
  padding: 1px 8px;
  border-radius: 8px;
  align-self: flex-start;
}

.stall-info-card h4 {
  margin: 0 0 14px;
  font-size: 15px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 9px 0;
  font-size: 14px;
  color: var(--artiv-text);
  border-bottom: 1px solid var(--artiv-border);
}

.info-row:last-of-type {
  border-bottom: none;
}

.info-row > span:first-child {
  color: var(--artiv-text-secondary);
}

.price-row {
  align-items: baseline;
}

.price {
  color: var(--artiv-accent);
  font-size: 24px;
  font-weight: 800;
}

.buy-btn {
  width: 100%;
  font-size: 16px;
}

.buy-tip {
  text-align: center;
  margin: 0;
  font-size: 12px;
  color: var(--artiv-text-secondary);
}
.stock-empty { color: var(--artiv-accent); font-weight: 600; }
.edit-tip { margin-left: 10px; color: var(--artiv-text-secondary); font-size: 12px; }
.edit-subjects { width: 100%; }
.edit-footer { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.edit-footer__danger-zone { display: flex; align-items: center; gap: 8px; }
.confirm-order { display:flex; gap:14px; padding-bottom:16px; border-bottom:1px solid var(--artiv-border); }
.confirm-order img { width:96px; height:76px; border-radius:7px; object-fit:cover; }
.confirm-order h3 { margin:3px 0 10px; }
.confirm-order p { margin:5px 0; color:var(--artiv-text-secondary); }
.confirm-amount { display:flex; justify-content:space-between; align-items:center; padding:20px 0 12px; }
.confirm-amount strong { color:var(--artiv-accent); font-size:23px; }
.confirm-tip { margin:0; padding:10px 12px; border-radius:6px; background:#fff7e8; color:#8a6226; font-size:13px; line-height:1.6; }

@media (max-width: 900px) {
  .detail-body {
    flex-direction: column;
  }

  .detail-aside {
    width: 100%;
    position: static;
  }
}
</style>
