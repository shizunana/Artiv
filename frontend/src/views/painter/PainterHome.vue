<template>
  <div class="painter-home">
    <!-- 认证引导（参考站 dashboard_artist-verified） -->
    <section v-if="certStatus !== 2" class="wb-card cert-card">
      <h2>您尚未完成画师认证</h2>
      <p>暂时无法使用画师身份的全部功能。在绘约，您可以通过应征企划、售卖橱窗与个人或企业合作，完成约稿需求。</p>
      <div class="cert-steps">
        <div class="cert-step">
          <span class="cert-step__icon">1</span>
          <div>
            <strong>完善个人资料</strong>
            <p>上传完成度最高的作品，有助于快速通过认证。</p>
          </div>
        </div>
        <div class="cert-step">
          <span class="cert-step__icon">2</span>
          <div>
            <strong>申请画师认证</strong>
            <p>提交后由管理员审核，通过即可接稿与开设橱窗。</p>
          </div>
        </div>
      </div>
      <div class="cert-actions">
        <el-button type="primary" round @click="router.push({ name: 'PainterWorks' })">完善资料与作品</el-button>
        <el-button round @click="router.push({ name: 'PainterProfile' })">进行画师认证</el-button>
      </div>
    </section>

    <section v-else class="wb-card welcome-card">
      <h2>欢迎您，{{ userStore.user?.nickname || userStore.user?.username }}</h2>
      <p>您已通过画师认证，可以应征企划、开设橱窗开始接稿。</p>
      <div class="welcome-actions">
        <el-button type="primary" round @click="router.push({ name: 'Projects' })">浏览企划</el-button>
        <el-button round @click="router.push({ name: 'PainterStalls' })">管理橱窗</el-button>
      </div>
    </section>

    <!-- 数据概览 -->
    <section class="stat-row">
      <div class="stat-card">
        <strong>{{ stats.works }}</strong>
        <span>我的作品</span>
      </div>
      <div class="stat-card">
        <strong>{{ stats.stalls }}</strong>
        <span>上架橱窗</span>
      </div>
      <div class="stat-card">
        <strong>{{ stats.applications }}</strong>
        <span>我的应征</span>
      </div>
      <div class="stat-card">
        <strong>{{ stats.invitations }}</strong>
        <span>收到的邀请</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { getMyWorks } from '@/api/work'
import { getMyStalls } from '@/api/stall'
import { getMyApplications } from '@/api/project'
import { getInvitations } from '@/api/invitation'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const stats = reactive({ works: 0, stalls: 0, applications: 0, invitations: 0 })

const certStatus = computed(() => userStore.user?.certStatus ?? 0)

onMounted(async () => {
  const [works, stalls, apps, invites] = await Promise.all([
    getMyWorks({ page: 1, size: 100 }).catch(() => ({ total: 0 })),
    getMyStalls({ page: 1, size: 100 }).catch(() => ({ total: 0 })),
    getMyApplications({ page: 1, size: 100 }).catch(() => ({ total: 0 })),
    getInvitations({ page: 1, size: 100 }).catch(() => ({ total: 0 })),
  ])
  stats.works = works.total || 0
  stats.stalls = stalls.total || 0
  stats.applications = apps.total || 0
  stats.invitations = invites.total || 0
})
</script>

<style scoped>
.painter-home {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.wb-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 28px;
}

.cert-card h2 {
  margin: 0 0 10px;
  font-size: 20px;
}

.cert-card > p {
  color: var(--huiyue-text-secondary);
  line-height: 1.8;
  margin: 0 0 20px;
}

.cert-steps {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.cert-step {
  display: flex;
  gap: 14px;
  background: var(--huiyue-section-alt);
  border-radius: 8px;
  padding: 18px;
}

.cert-step__icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--huiyue-primary);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.cert-step strong {
  font-size: 14px;
}

.cert-step p {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--huiyue-text-secondary);
  line-height: 1.7;
}

.cert-actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}

.welcome-card h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.welcome-card p {
  color: var(--huiyue-text-secondary);
  margin: 0 0 18px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
}

.stat-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-card strong {
  font-size: 24px;
  color: var(--huiyue-primary);
}

.stat-card span {
  font-size: 13px;
  color: var(--huiyue-text-secondary);
}

@media (max-width: 900px) {
  .cert-steps,
  .stat-row {
    grid-template-columns: 1fr;
  }
}
</style>
