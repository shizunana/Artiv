<template>
  <header class="app-header">
    <div class="app-header__inner page-container">
      <router-link to="/" class="app-header__logo">
        <span class="logo-mark">绘</span>
        <span class="logo-text">绘约</span>
      </router-link>

      <nav class="app-header__nav">
        <router-link to="/projects">企划</router-link>
        <router-link to="/stalls">橱窗</router-link>
        <router-link to="/works">作品</router-link>
        <router-link to="/artists">画师</router-link>
      </nav>

      <div class="app-header__actions">
        <template v-if="userStore.isLoggedIn">
          <el-button type="primary" size="small" round @click="goPublish">发布需求</el-button>
          <el-badge :value="messageStore.unreadCount" :hidden="!messageStore.unreadCount" :max="99" class="msg-badge">
            <el-button circle size="small" @click="goMessages">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown @command="onCommand">
            <span class="app-header__user">
              <el-avatar :size="30" :src="userStore.user?.avatar" class="header-avatar">
                {{ avatarText }}
              </el-avatar>
              <span class="user-nickname">{{ userStore.user?.nickname || userStore.user?.username }}</span>
              <el-icon class="header-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="workbench">工作台</el-dropdown-item>
                <el-dropdown-item v-if="canSwitchRole" command="switchRole">切换身份</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="nav-btn">登录</router-link>
          <router-link to="/register" class="nav-btn is-primary">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'

const router = useRouter()
const userStore = useUserStore()
const messageStore = useMessageStore()

onMounted(() => { if (userStore.isLoggedIn) messageStore.fetchUnreadCount() })

const avatarText = computed(() => {
  const n = userStore.user?.nickname || userStore.user?.username || ''
  return n ? n.slice(0, 1) : '绘'
})

const canSwitchRole = computed(() => userStore.workbenchRole !== 3)

const workbenchHome = computed(() => {
  const role = userStore.workbenchRole
  return role === 2 ? '/painter' : role === 3 ? '/admin' : '/dashboard'
})

function goPublish() {
  if (userStore.workbenchRole === 2) {
    router.push({ name: 'PainterStalls' })
  } else {
    router.push({ name: 'ProjectNew' })
  }
}

function goMessages() {
  const base = userStore.workbenchRole === 2 ? '/painter' : '/dashboard'
  router.push(`${base}/messages`)
}

function onCommand(command) {
  if (command === 'profile') {
    router.push(userStore.workbenchRole === 2 ? '/painter/profile' : workbenchHome.value)
  } else if (command === 'workbench') {
    router.push(workbenchHome.value)
  } else if (command === 'switchRole') {
    userStore.switchRole(userStore.workbenchRole === 2 ? 1 : 2)
    router.push(userStore.workbenchRole === 2 ? '/painter' : '/dashboard')
  } else if (command === 'logout') {
    userStore.logout()
    router.push({ name: 'Home' })
  }
}
</script>

<style scoped>
.app-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: var(--huiyue-header-height);
  background: rgba(255, 255, 255, 0.97);
  border-bottom: 1px solid var(--huiyue-border);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.app-header__inner {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 100%;
}

.app-header__logo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--huiyue-text);
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--huiyue-primary);
  color: #fff;
  font-size: 17px;
}

.app-header__nav {
  display: flex;
  gap: 26px;
  flex: 1;
}

.app-header__nav a {
  color: var(--huiyue-text-secondary);
  font-size: 15px;
  transition: color 0.2s;
}

.app-header__nav a:hover,
.app-header__nav a.router-link-active {
  color: var(--huiyue-primary);
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-btn {
  padding: 7px 22px;
  border-radius: 18px;
  color: var(--huiyue-primary);
  border: 1px solid var(--huiyue-primary);
  font-size: 14px;
  transition: all 0.2s;
}

.nav-btn:hover {
  background: var(--huiyue-primary-light);
}

.nav-btn.is-primary {
  background: var(--huiyue-primary);
  color: #fff;
}

.nav-btn.is-primary:hover {
  background: var(--huiyue-primary-dark);
}

.msg-badge {
  line-height: 1;
}

.app-header__user {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
  color: var(--huiyue-text);
}

.header-avatar {
  background: var(--huiyue-primary);
  color: #fff;
}

.user-nickname {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-caret {
  font-size: 12px;
  color: var(--huiyue-text-secondary);
}

@media (max-width: 900px) {
  .app-header__nav {
    display: none;
  }
}
</style>
