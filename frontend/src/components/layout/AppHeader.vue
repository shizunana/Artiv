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
          <el-button v-if="!userStore.isAdmin" type="primary" round class="publish-button" @click="goPublish">发布需求</el-button>
          <el-dropdown v-if="!userStore.isAdmin" trigger="hover" placement="bottom" @command="onMessageCommand">
            <el-badge :value="messageStore.unreadCount" :hidden="!messageStore.unreadCount" :max="99" class="msg-badge">
              <button type="button" class="message-entry" aria-label="打开消息中心" @click.stop="goMessages('conversations')">
                <el-icon><MessageIcon /></el-icon>
                <span>消息</span>
              </button>
            </el-badge>
            <template #dropdown>
              <el-dropdown-menu class="message-quick-menu">
                <el-dropdown-item command="conversations">我的消息</el-dropdown-item>
                <el-dropdown-item command="notifications">系统消息</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown @command="onCommand" @visible-change="onAccountMenuVisible">
            <span class="app-header__user" role="button" tabindex="0">
              <el-avatar :size="32" :src="userStore.user?.avatar" class="header-avatar">
                {{ avatarText }}
              </el-avatar>
              <span class="user-nickname">{{ userStore.user?.nickname || userStore.user?.username }}</span>
              <el-icon class="header-caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="account-menu">
                <div v-if="!userStore.isAdmin" class="account-menu__funds">
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
                <el-dropdown-item command="home">主页</el-dropdown-item>
                <el-dropdown-item command="workbench">{{ userStore.isAdmin ? '工作台' : '我的工作台' }}</el-dropdown-item>
                <el-dropdown-item v-if="!userStore.isAdmin" command="profile">账户设置</el-dropdown-item>
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
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Message as MessageIcon } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const messageStore = useMessageStore()

onMounted(() => { if (userStore.isLoggedIn) messageStore.fetchUnreadCount() })

const avatarText = computed(() => {
  const n = userStore.user?.nickname || userStore.user?.username || ''
  return n ? n.slice(0, 1) : '绘'
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

function goMessages(section = 'conversations') {
  router.push({
    name: 'Messages',
    query: section === 'notifications' ? { tab: 'notifications' } : {},
  })
}

function onMessageCommand(command) {
  goMessages(command)
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
    if (userStore.isAdmin) {
      router.push({ name: 'Home' })
    } else {
      router.push({
        name: 'ArtistProfile',
        params: { id: userStore.user.id },
        query: { role: userStore.workbenchRole === 2 ? 'painter' : 'employer' },
      })
    }
  } else if (command === 'workbench') {
    if (userStore.isAdmin) {
      router.push('/admin')
    } else if (route.path.startsWith('/painter')) {
      router.push('/painter')
    } else if (route.path.startsWith('/dashboard')) {
      router.push('/dashboard')
    } else {
      router.push(userStore.workbenchRole === 2 ? '/painter' : '/dashboard')
    }
  } else if (command === 'recharge' || command === 'withdraw') {
    ElMessage.info('资金管理功能正在开发中')
  } else if (command === 'profile') {
    router.push({ name: 'AccountSettings' })
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
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(210, 219, 227, 0.78);
  box-shadow: 0 4px 18px rgba(48, 75, 92, 0.06);
  backdrop-filter: blur(14px);
}

.app-header__inner {
  display: flex;
  align-items: center;
  gap: 32px;
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
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--artiv-primary);
  color: #fff;
  font-size: 17px;
}

.app-header__nav {
  display: flex;
  align-self: stretch;
  gap: 28px;
  flex: 1;
}

.app-header__nav a {
  position: relative;
  display: inline-flex;
  align-items: center;
  color: var(--artiv-text-secondary);
  font-size: 15px;
  font-weight: 500;
  transition: color 0.2s, font-weight 0.2s;
}

.app-header__nav a:hover,
.app-header__nav a.router-link-active {
  color: var(--artiv-primary);
  font-weight: 600;
}

.app-header__nav a::after {
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
  background: var(--artiv-primary);
  content: '';
  transform: translateX(-50%);
  transition: width 0.2s;
}

.app-header__nav a.router-link-active::after {
  width: 24px;
}

.app-header__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.publish-button {
  height: 36px;
  padding: 0 20px;
  border: 0;
  font-weight: 600;
  box-shadow: 0 5px 14px rgba(0, 168, 233, 0.18);
}

.message-entry {
  display: inline-flex;
  min-width: 48px;
  height: 44px;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 3px 7px;
  border: 0;
  border-radius: 8px;
  color: var(--artiv-text);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: color .2s, background .2s;
}

.message-entry .el-icon {
  font-size: 18px;
}

.message-entry:hover,
.message-entry:focus-visible {
  color: var(--artiv-primary);
  background: var(--artiv-primary-light);
  outline: none;
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

.msg-badge :deep(.el-badge__content) {
  font-weight: 700;
}

:global(.message-quick-menu) { min-width: 146px; padding: 6px; }
:global(.message-quick-menu .el-dropdown-menu__item) { justify-content: center; border-radius: 6px; padding: 10px 18px; }

.app-header__user {
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  gap: 8px;
  padding: 4px 8px 4px 5px;
  border-radius: 999px;
  cursor: pointer;
  outline: none;
  color: var(--artiv-text);
  transition: background 0.2s;
}

.app-header__user:hover,
.app-header__user:focus-visible {
  background: var(--artiv-primary-light);
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
  .app-header__inner {
    gap: 16px;
  }

  .app-header__nav {
    display: none;
  }
}

@media (max-width: 640px) {
  .app-header__inner {
    gap: 10px;
  }

  .logo-text,
  .user-nickname {
    display: none;
  }

  .app-header__actions {
    margin-left: auto;
    gap: 6px;
  }

  .publish-button {
    height: 34px;
    padding: 0 14px;
  }

  .message-entry {
    min-width: 36px;
    padding-inline: 5px;
  }

  .message-entry span {
    display: none;
  }

  .app-header__user {
    min-height: 38px;
    padding-right: 5px;
  }
}
</style>
