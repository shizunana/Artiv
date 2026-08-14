<template>
  <header class="app-header">
    <div class="app-header__inner page-container">
      <router-link to="/" class="app-header__logo">
        <span class="logo-mark">艺</span>
        <span class="logo-text">艺绘</span>
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
          <el-dropdown @command="onCommand" @visible-change="onAccountMenuVisible">
            <span class="app-header__user">
              <el-avatar :size="30" :src="userStore.user?.avatar" class="header-avatar">
                {{ avatarText }}
              </el-avatar>
              <span class="user-nickname">{{ userStore.user?.nickname || userStore.user?.username }}</span>
              <el-icon class="header-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="account-menu">
                <div class="account-menu__funds">
                  <div class="account-menu__funds-title">资金管理</div>
                  <div class="account-menu__balance">
                    <span>账户余额</span>
                    <strong>{{ formattedBalance }}</strong>
                    <div>
                      <button type="button" @click.stop="onCommand('withdraw')">提现</button>
                      <button type="button" @click.stop="onCommand('recharge')">充值</button>
                    </div>
                  </div>
                </div>
                <el-dropdown-item command="home">首页</el-dropdown-item>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
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
import { ElMessage } from 'element-plus'
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

const workbenchHome = computed(() => {
  const role = userStore.workbenchRole
  return role === 2 ? '/painter' : role === 3 ? '/admin' : '/dashboard'
})

const formattedBalance = computed(() => {
  const amount = Number(userStore.user?.balance ?? 0)
  return `¥${Number.isFinite(amount) ? amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '0.00'}`
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

async function onAccountMenuVisible(visible) {
  if (!visible) return
  try {
    await userStore.fetchMe()
  } catch (_) {
    // Keep the cached account summary when the refresh request is temporarily unavailable.
  }
}

function onCommand(command) {
  if (command === 'home') {
    ElMessage.info('首页正在开发中')
  } else if (command === 'recharge' || command === 'withdraw') {
    ElMessage.info('资金管理功能正在开发中')
  } else if (command === 'profile') {
    router.push(userStore.workbenchRole === 2 ? '/painter/profile' : workbenchHome.value)
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
  height: var(--artiv-header-height);
  background: rgba(255, 255, 255, 0.97);
  border-bottom: 1px solid var(--artiv-border);
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
  color: var(--artiv-text);
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--artiv-primary);
  color: #fff;
  font-size: 17px;
}

.app-header__nav {
  display: flex;
  gap: 26px;
  flex: 1;
}

.app-header__nav a {
  color: var(--artiv-text-secondary);
  font-size: 15px;
  transition: color 0.2s;
}

.app-header__nav a:hover,
.app-header__nav a.router-link-active {
  color: var(--artiv-primary);
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-btn {
  padding: 7px 22px;
  border-radius: 18px;
  color: var(--artiv-primary);
  border: 1px solid var(--artiv-primary);
  font-size: 14px;
  transition: all 0.2s;
}

.nav-btn:hover {
  background: var(--artiv-primary-light);
}

.nav-btn.is-primary {
  background: var(--artiv-primary);
  color: #fff;
}

.nav-btn.is-primary:hover {
  background: var(--artiv-primary-dark);
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
  color: var(--artiv-text);
}

.header-avatar {
  background: var(--artiv-primary);
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
  color: var(--artiv-text-secondary);
}

:global(.account-menu) { min-width: 190px; padding: 0 0 6px; }
:global(.account-menu__funds) { padding: 12px; border-bottom: 1px solid var(--artiv-border); }
:global(.account-menu__funds-title) { margin-bottom: 8px; color: var(--artiv-text); font-size: 14px; font-weight: 600; }
:global(.account-menu__balance) { display: grid; grid-template-columns: 1fr auto; align-items: end; gap: 4px 8px; padding: 9px 10px; border-radius: 8px; background: var(--artiv-primary-light); }
:global(.account-menu__balance span) { color: var(--artiv-text-secondary); font-size: 12px; }
:global(.account-menu__balance strong) { color: var(--artiv-text); font-size: 18px; }
:global(.account-menu__balance div) { grid-column: 1 / -1; display: flex; justify-content: flex-end; gap: 12px; }
:global(.account-menu__balance button) { border: 0; padding: 0; color: var(--artiv-primary); background: transparent; cursor: pointer; font-size: 12px; }

@media (max-width: 900px) {
  .app-header__nav {
    display: none;
  }
}
</style>
