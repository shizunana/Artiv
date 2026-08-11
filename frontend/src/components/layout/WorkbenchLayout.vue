<template>
  <div class="workbench-page">
    <!-- 顶部导航（登录后状态，参考站 mhs-header） -->
    <header class="wb-header">
      <div class="wb-header__inner page-container">
        <router-link to="/" class="wb-header__logo">绘约</router-link>
        <nav class="wb-header__nav">
          <router-link to="/projects">企划</router-link>
          <router-link to="/stalls">橱窗</router-link>
          <router-link to="/works">作品</router-link>
          <router-link to="/artists">画师</router-link>
        </nav>
        <div class="wb-header__actions">
          <el-button type="primary" size="small" round @click="goPublish">发布需求</el-button>
          <el-badge :value="0" :max="99" class="msg-badge">
            <el-button circle size="small" @click="goMessages">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown @command="onCommand">
            <span class="wb-header__user">
              <el-avatar :size="30" :src="userStore.user?.avatar" class="wb-avatar">{{ avatarText }}</el-avatar>
              <span class="wb-user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
              <el-icon class="wb-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="switchRole">切换身份</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

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

        <div class="wb-sidebar__foot">
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
import { ArrowDown, Bell, Right } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = {
  dashboard: [
    { path: '/dashboard', title: '工作台首页' },
    { path: '/dashboard/projects', title: '我的企划' },
    { path: '/dashboard/projects/new', title: '发布企划' },
    { path: '/dashboard/orders', title: '订单管理' },
    { path: '/dashboard/messages', title: '消息中心' },
  ],
  painter: [
    { path: '/painter', title: '工作台首页' },
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
    { path: '/admin/projects', title: '企划审核' },
    { path: '/admin/stats', title: '数据统计' },
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
  const role = userStore.user?.role
  return role === 1 ? '企划方' : role === 2 ? '画师' : role === 3 ? '管理员' : '-'
})

const oppositeRoleText = computed(() => {
  return userStore.user?.role === 2 ? '企划方' : '画师'
})

function goPublish() {
  if (userStore.user?.role === 2) {
    router.push({ name: 'PainterStalls' })
  } else {
    router.push({ name: 'ProjectNew' })
  }
}

function goMessages() {
  router.push({ path: `${workbench.value}/messages` })
}

function switchRole() {
  // 演示环境：企划方与画师工作台互相切换
  router.push(userStore.user?.role === 2 ? '/dashboard' : '/painter')
}

function onCommand(command) {
  if (command === 'profile') {
    const path = userStore.user?.role === 2 ? '/painter/profile' : '/dashboard'
    router.push(path)
  } else if (command === 'switchRole') {
    switchRole()
  } else if (command === 'logout') {
    userStore.logout()
    router.push({ name: 'Home' })
  }
}
</script>

<style scoped>
.workbench-page {
  min-height: 100vh;
  background: #f5f6f8;
  padding-bottom: 48px;
}

/* ---------- 顶部导航 ---------- */
.wb-header {
  position: sticky;
  top: 0;
  z-index: 90;
  background: #fff;
  border-bottom: 1px solid var(--huiyue-border);
  height: 56px;
}

.wb-header__inner {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 100%;
}

.wb-header__logo {
  font-size: 19px;
  font-weight: 700;
  color: var(--huiyue-text);
}

.wb-header__nav {
  display: flex;
  gap: 22px;
  flex: 1;
}

.wb-header__nav a {
  color: var(--huiyue-text-secondary);
  font-size: 15px;
}

.wb-header__nav a:hover,
.wb-header__nav a.router-link-active {
  color: var(--huiyue-primary);
}

.wb-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.msg-badge {
  line-height: 1;
}

.wb-header__user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
  color: var(--huiyue-text);
}

.wb-avatar {
  background: var(--huiyue-primary);
  color: #fff;
}

.wb-user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-caret {
  font-size: 12px;
  color: var(--huiyue-text-secondary);
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
  background: var(--huiyue-primary);
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
  color: var(--huiyue-primary);
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
  color: var(--huiyue-text);
  font-size: 14px;
  transition: background 0.2s, color 0.2s;
}

.wb-sidebar__tab:hover {
  background: var(--huiyue-primary-light);
  color: var(--huiyue-primary);
}

.wb-sidebar__tab.is-active {
  background: var(--huiyue-primary-light);
  color: var(--huiyue-primary);
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
  border: 1px solid var(--huiyue-primary);
  border-radius: 18px;
  background: #fff;
  color: var(--huiyue-primary);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.wb-switch:hover {
  background: var(--huiyue-primary-light);
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

  .wb-header__nav {
    display: none;
  }
}
</style>
