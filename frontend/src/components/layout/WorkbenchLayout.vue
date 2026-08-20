<template>
  <div class="workbench-page">
    <!-- 统一顶部导航 -->
    <AppHeader />

    <div class="page-container wb-body">
      <!-- 左侧工作台卡片（参考站 dashboard-sidebar） -->
      <aside class="wb-sidebar">
        <div class="wb-sidebar__user">
          <el-avatar :size="46" :src="userStore.user?.avatar" class="wb-sidebar-avatar">{{ avatarText }}</el-avatar>
          <div class="wb-sidebar__user-info">
            <span class="wb-sidebar__role">{{ roleText }}</span>
            <span class="wb-sidebar__name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
          </div>
        </div>

        <div class="wb-sidebar__tabs">
          <router-link
            v-for="item in menuItems"
            :key="item.path"
            :to="item.path"
            class="wb-sidebar__tab"
            :class="{ 'is-active': activePath === item.path }"
          >
            <span>{{ item.title }}</span>
            <el-icon v-if="item.soon" class="wb-tab-soon"><Right /></el-icon>
          </router-link>
        </div>

        <div v-if="workbench !== 'admin'" class="wb-sidebar__foot">
          <button class="wb-switch" @click="switchRole">
            切换为{{ oppositeRoleText }}
          </button>
        </div>
      </aside>

      <!-- 主内容区（参考站 dashboard__main） -->
      <main class="wb-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Right } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AppHeader from './AppHeader.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = {
  dashboard: [
    { path: '/dashboard', title: '工作台主页' },
    { path: '/dashboard/projects', title: '我的企划' },
    { path: '/dashboard/projects/new', title: '发布企划' },
    { path: '/dashboard/orders', title: '订单管理' },
    { path: '/dashboard/messages', title: '消息中心' },
  ],
  painter: [
    { path: '/painter', title: '工作台主页' },
    { path: '/painter/profile', title: '资料与认证' },
    { path: '/painter/works', title: '我的作品' },
    { path: '/painter/stalls', title: '我的橱窗' },
    { path: '/painter/applications', title: '我的应征' },
    { path: '/painter/invitations', title: '收到的邀请' },
    { path: '/painter/orders', title: '订单管理' },
    { path: '/painter/messages', title: '消息中心' },
  ],
  admin: [
    { path: '/admin', title: '概览' },
    { path: '/admin/users', title: '用户管理' },
    { path: '/admin/certifications', title: '认证审核' },
    { path: '/admin/projects', title: '企划治理' },
    { path: '/admin/disputes', title: '订单仲裁' },
    { path: '/admin/stats', title: '数据统计' },
    { path: '/admin/audit-logs', title: '操作审计' },
  ],
}

const workbench = computed(() => route.meta.workbench || 'dashboard')
const menuItems = computed(() => menus[workbench.value] || [])
const activePath = computed(() => route.path)
const avatarText = computed(() => {
  const n = userStore.user?.nickname || userStore.user?.username || ''
  return n ? n.slice(0, 1) : '绘'
})

const roleText = computed(() => {
  const role = userStore.workbenchRole
  return role === 1 ? '企划方' : role === 2 ? '画师' : role === 3 ? '管理员' : '-'
})

const oppositeRoleText = computed(() => {
  return userStore.workbenchRole === 2 ? '企划方' : '画师'
})

function switchRole() {
  // 演示环境：真正切换工作台身份
  userStore.switchRole(userStore.workbenchRole === 2 ? 1 : 2)
  router.push(userStore.workbenchRole === 2 ? '/painter' : '/dashboard')
}
</script>

<style scoped>
.workbench-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-top: var(--artiv-header-height);
  padding-bottom: 48px;
}

/* ---------- 主体布局 ---------- */
.wb-body {
  display: flex;
  align-items: flex-start;
  gap: 15px;
  margin-top: 20px;
}

/* ---------- 左侧工作台（参考站 dashboard-sidebar：282px 白卡片） ---------- */
.wb-sidebar {
  flex: 0 0 282px;
  width: 282px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 76px;
}

.wb-sidebar__user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 18px;
  border-bottom: 1px solid #ececec;
}

.wb-sidebar-avatar {
  background: var(--artiv-primary);
  color: #fff;
  flex-shrink: 0;
}

.wb-sidebar__user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.wb-sidebar__role {
  color: var(--artiv-primary);
  font-size: 12px;
}

.wb-sidebar__name {
  color: #707070;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-sidebar__tabs {
  display: flex;
  flex-direction: column;
  margin-top: 12px;
  flex: 1;
}

.wb-sidebar__tab {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 10px;
  border-radius: 4px;
  color: var(--artiv-text);
  font-size: 14px;
  transition: background 0.2s, color 0.2s;
}

.wb-sidebar__tab:hover {
  background: var(--artiv-primary-light);
  color: var(--artiv-primary);
}

.wb-sidebar__tab.is-active {
  background: var(--artiv-primary-light);
  color: var(--artiv-primary);
  font-weight: 600;
}

.wb-tab-soon {
  font-size: 12px;
  color: #c0c4cc;
}

.wb-sidebar__foot {
  border-top: 1px solid #ececec;
  padding-top: 16px;
}

.wb-switch {
  width: 100%;
  padding: 9px 0;
  border: 1px solid var(--artiv-primary);
  border-radius: 18px;
  background: #fff;
  color: var(--artiv-primary);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.wb-switch:hover {
  background: var(--artiv-primary-light);
}

/* ---------- 主内容区 ---------- */
.wb-main {
  flex: 1;
  min-width: 0;
  filter: drop-shadow(0 1px 3px rgba(0, 0, 0, 0.08));
}

@media (max-width: 900px) {
  .wb-body {
    flex-direction: column;
  }

  .wb-sidebar {
    flex: none;
    width: 100%;
    position: static;
  }

  .wb-sidebar__tabs {
    flex-direction: row;
    flex-wrap: wrap;
  }

}
</style>
