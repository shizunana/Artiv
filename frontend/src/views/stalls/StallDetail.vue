<template>
  <div class="detail-page page-container" v-loading="loading">
    <div v-if="stall" class="stall-detail">
      <!-- 头部：封面 + 基本信息 -->
      <div class="detail-header">
        <div class="detail-cover">
          <el-image
            v-if="stall.cover"
            :src="stall.cover"
            :preview-src-list="[stall.cover]"
            preview-teleported
            fit="cover"
          />
          <div v-else class="cover-placeholder">🛍️</div>
        </div>
        <div class="detail-info">
          <div class="detail-category">{{ stall.category }}</div>
          <h1 class="detail-name">{{ stall.title }}</h1>
          <div class="detail-price-row">
            <span class="detail-price">￥{{ stall.price }}</span>
            <el-tag size="small" :type="stall.status === 1 ? 'success' : 'info'">
              {{ stall.status === 1 ? '上架中' : '已下架' }}
            </el-tag>
          </div>
          <div class="detail-stats">
            <div class="stat"><strong>{{ stall.painterId ? '画师' : '-' }}</strong><span>创作者</span></div>
            <div class="stat"><strong>0</strong><span>已售</span></div>
            <div class="stat"><strong>100%</strong><span>需求完成率</span></div>
          </div>
          <el-button
            type="primary"
            size="large"
            round
            class="buy-btn"
            :loading="buying"
            @click="buy"
          >
            立即下单
          </el-button>
          <p class="buy-tip">下单后进入订单流程，分阶段验收后完成结算</p>
        </div>
      </div>

      <!-- 橱窗详情 -->
      <section class="detail-section">
        <h2 class="detail-title">橱窗详情</h2>
        <p class="detail-desc">{{ stall.description || '暂无详细说明' }}</p>
      </section>

      <!-- 创作阶段 -->
      <section class="detail-section">
        <h2 class="detail-title">创作阶段</h2>
        <div class="phase-row">
          <div class="phase-item">
            <span class="phase-percent">40%</span>
            <span class="phase-name">初稿</span>
          </div>
          <div class="phase-item">
            <span class="phase-percent">30%</span>
            <span class="phase-name">修改</span>
          </div>
          <div class="phase-item">
            <span class="phase-percent">30%</span>
            <span class="phase-name">终稿交付</span>
          </div>
        </div>
        <p class="phase-tip">画师按阶段提交稿件，企划方确认后再进入下一阶段</p>
      </section>

      <!-- 稿件参数 -->
      <section class="detail-section">
        <h2 class="detail-title">稿件参数</h2>
        <div class="params">
          <div class="param-item"><span class="param-label">橱窗类型</span><span>{{ stall.category }}</span></div>
          <div class="param-item"><span class="param-label">交付方式</span><span>在线交付（分阶段验收）</span></div>
          <div class="param-item"><span class="param-label">价格</span><span>￥{{ stall.price }}</span></div>
          <div class="param-item"><span class="param-label">接单状态</span><span>{{ stall.status === 1 ? '接单中' : '暂停接单' }}</span></div>
        </div>
      </section>
    </div>

    <EmptyState v-else-if="!loading" description="橱窗不存在或已下架" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getStall } from '@/api/stall'
import EmptyState from '@/components/common/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const stall = ref(null)
const loading = ref(false)
const buying = ref(false)

async function load() {
  loading.value = true
  try {
    stall.value = await getStall(route.params.id)
  } finally {
    loading.value = false
  }
}

async function buy() {
  buying.value = true
  try {
    const { purchaseStall } = await import('@/api/stall')
    await purchaseStall(stall.value.id)
    ElMessage.success('下单成功，请到订单管理查看')
    router.push('/dashboard/orders')
  } catch (e) {
    ElMessage.error('下单失败：' + (e.message || '请先登录'))
  } finally {
    buying.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail-page {
  min-height: 60vh;
  padding: 24px 16px 56px;
  max-width: 1000px;
}

.detail-header {
  display: flex;
  gap: 32px;
  background: #fff;
  border: 1px solid var(--huiyue-border);
  border-radius: 14px;
  padding: 28px;
  margin-bottom: 20px;
}

.detail-cover {
  width: 320px;
  height: 320px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--huiyue-section-alt);
  flex-shrink: 0;
}

.detail-cover .el-image {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
  color: var(--huiyue-text-secondary);
}

.detail-info {
  flex: 1;
  min-width: 0;
}

.detail-category {
  color: var(--huiyue-primary);
  font-size: 13px;
  margin-bottom: 8px;
}

.detail-name {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 600;
}

.detail-price-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.detail-price {
  color: var(--huiyue-accent);
  font-size: 26px;
  font-weight: 700;
}

.detail-stats {
  display: flex;
  gap: 36px;
  margin-bottom: 24px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat strong {
  font-size: 18px;
  color: var(--huiyue-text);
}

.stat span {
  font-size: 12px;
  color: var(--huiyue-text-secondary);
}

.buy-btn {
  min-width: 180px;
  font-size: 16px;
}

.buy-tip {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--huiyue-text-secondary);
}

.detail-section {
  background: #fff;
  border: 1px solid var(--huiyue-border);
  border-radius: 12px;
  padding: 24px 28px;
  margin-bottom: 16px;
}

.detail-title {
  margin: 0 0 16px;
  font-size: 17px;
  font-weight: 600;
  padding-left: 10px;
  border-left: 3px solid var(--huiyue-primary);
}

.detail-desc {
  margin: 0;
  color: var(--huiyue-text);
  line-height: 1.9;
  white-space: pre-wrap;
}

.phase-row {
  display: flex;
  gap: 16px;
}

.phase-item {
  flex: 1;
  background: var(--huiyue-section-alt);
  border-radius: 10px;
  padding: 18px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.phase-percent {
  font-size: 22px;
  font-weight: 700;
  color: var(--huiyue-primary);
}

.phase-name {
  font-size: 14px;
  color: var(--huiyue-text);
}

.phase-tip {
  margin: 14px 0 0;
  font-size: 12px;
  color: var(--huiyue-text-secondary);
}

.params {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.param-item {
  display: flex;
  justify-content: space-between;
  background: var(--huiyue-section-alt);
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
}

.param-label {
  color: var(--huiyue-text-secondary);
}

@media (max-width: 760px) {
  .detail-header {
    flex-direction: column;
  }

  .detail-cover {
    width: 100%;
  }
}
</style>
