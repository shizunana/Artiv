<template>
  <el-container class="layout">
    <el-header class="layout-header">
      <div class="page-container header-inner">
        <router-link to="/" class="logo">绘约 HuiYue</router-link>
        <nav class="nav">
          <router-link to="/artists">画师</router-link>
          <router-link to="/works">作品</router-link>
          <router-link to="/projects">企划大厅</router-link>
          <router-link to="/stalls">服务板</router-link>
          <router-link v-if="userStore.isLoggedIn" :to="workbenchHome">我的工作台</router-link>
          <router-link v-if="userStore.isAdmin" to="/admin">管理后台</router-link>
        </nav>
        <div v-if="userStore.isLoggedIn" class="user-area">
          <el-dropdown @command="onCommand">
            <span class="user-name">
              <el-avatar :size="28" :src="userStore.user?.avatar">{{ nickname }}</el-avatar>
              <span class="nickname">{{ userStore.user?.nickname || userStore.user?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="me">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <el-button v-else type="primary" @click="router.push({ name: 'Login' })">登录 / 注册</el-button>
      </div>
    </el-header>

    <el-main class="layout-main">
      <router-view />
    </el-main>

    <el-footer class="layout-footer">绘约 HuiYue · 美术约稿交易平台 · 毕业设计作品</el-footer>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const workbenchHome = computed(() => {
  const role = userStore.user?.role
  if (role === 2) return '/painter'
  if (role === 3) return '/admin'
  return '/dashboard'
})

const nickname = computed(() => {
  const n = userStore.user?.nickname || ''
  return n ? n.slice(0, 1) : '绘'
})

function onCommand(command) {
  if (command === 'me') {
    router.push(workbenchHome.value)
  } else if (command === 'logout') {
    userStore.logout()
    router.push({ name: 'Home' })
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}

.layout-header {
  background: #fff;
  border-bottom: 1px solid var(--huiyue-border);
  height: 60px;
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100%;
}

.logo {
  font-size: 18px;
  font-weight: 700;
  color: var(--huiyue-text);
}

.nav {
  display: flex;
  gap: 18px;
  flex: 1;
}

.nav a {
  color: var(--huiyue-text-secondary);
}

.nav a.router-link-active {
  color: var(--huiyue-primary);
}

.user-area {
  cursor: pointer;
}

.user-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.layout-main {
  padding: 24px 0;
}

.layout-footer {
  text-align: center;
  color: var(--huiyue-text-secondary);
  border-top: 1px solid var(--huiyue-border);
  background: #fff;
}
</style>
