<template>
  <div class="landing-layout">
    <!-- 吸顶导航（参考站 header-bar） -->
    <header class="header-bar" :class="{ 'is-scrolled': scrolled }">
      <div class="header-bar__inner page-container">
        <router-link to="/" class="header-bar__logo">
          <span class="logo-mark">绘</span>
          <span class="logo-text">绘约 HuiYue</span>
        </router-link>

        <nav class="header-bar__nav">
          <router-link to="/projects">企划</router-link>
          <router-link to="/stalls">橱窗</router-link>
          <router-link to="/works">作品</router-link>
          <router-link to="/artists">画师</router-link>
          <router-link v-if="userStore.isLoggedIn" :to="workbenchHome">我的工作台</router-link>
          <router-link v-if="userStore.isAdmin" to="/admin">管理后台</router-link>
        </nav>

        <div class="header-bar__actions">
          <template v-if="userStore.isLoggedIn">
            <el-dropdown @command="onCommand">
              <span class="user-chip">
                <el-avatar :size="30" :src="userStore.user?.avatar" class="user-avatar">
                  {{ avatarText }}
                </el-avatar>
                <span class="user-nickname">{{ userStore.user?.nickname || userStore.user?.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="me">个人中心</el-dropdown-item>
                  <el-dropdown-item command="workbench">工作台</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <router-link to="/login" class="header-bar__nav-button">登录</router-link>
            <router-link to="/register" class="header-bar__nav-button is-primary">注册</router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="landing-layout__main">
      <router-view />
    </main>

    <!-- 页脚（参考站 page-footer） -->
    <footer class="page-footer">
      <div class="page-container">
        <div class="page-footer__nav-list">
          <router-link to="/projects">企划大厅</router-link>
          <router-link to="/stalls">服务板</router-link>
          <router-link to="/works">作品广场</router-link>
          <router-link to="/artists">画师列表</router-link>
          <router-link to="/login">登录</router-link>
          <router-link to="/register">注册</router-link>
        </div>
        <div class="page-footer__copyright">
          <p>绘约 HuiYue · 带给你自由、高效、专业的美术约稿体验</p>
          <p class="records">Copyright © 2026 HuiYue.com 保留所有权利 · 毕业设计作品</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const scrolled = ref(false)

const workbenchHome = computed(() => {
  const role = userStore.user?.role
  if (role === 2) return '/painter'
  if (role === 3) return '/admin'
  return '/dashboard'
})

const avatarText = computed(() => {
  const n = userStore.user?.nickname || userStore.user?.username || ''
  return n ? n.slice(0, 1) : '绘'
})

function onScroll() {
  scrolled.value = window.scrollY > 8
}

function onCommand(command) {
  if (command === 'me') {
    router.push({ path: `${workbenchHome.value}/profile`, query: { tab: 'profile' } })
  } else if (command === 'workbench') {
    router.push(workbenchHome.value)
  } else if (command === 'logout') {
    userStore.logout()
    router.push({ name: 'Home' })
  }
}

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.landing-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.landing-layout__main {
  flex: 1;
}

/* ---------- 吸顶导航 ---------- */
.header-bar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  height: var(--huiyue-header-height);
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid var(--huiyue-border);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-bar__inner {
  display: flex;
  align-items: center;
  gap: 32px;
  height: 100%;
}

.header-bar__logo {
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

.header-bar__nav {
  display: flex;
  gap: 26px;
  flex: 1;
}

.header-bar__nav a {
  color: var(--huiyue-text-secondary);
  font-size: 15px;
  transition: color 0.2s;
}

.header-bar__nav a:hover,
.header-bar__nav a.router-link-active {
  color: var(--huiyue-primary);
}

.header-bar__actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.header-bar__nav-button {
  padding: 7px 22px;
  border-radius: 18px;
  color: var(--huiyue-primary);
  border: 1px solid var(--huiyue-primary);
  font-size: 14px;
  transition: all 0.2s;
}

.header-bar__nav-button:hover {
  background: var(--huiyue-primary-light);
}

.header-bar__nav-button.is-primary {
  background: var(--huiyue-primary);
  color: #fff;
}

.header-bar__nav-button.is-primary:hover {
  background: var(--huiyue-primary-dark);
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--huiyue-text);
  outline: none;
}

.user-avatar {
  background: var(--huiyue-primary);
  color: #fff;
}

.user-nickname {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 页脚 ---------- */
.page-footer {
  background: #23262b;
  color: #9b9b9b;
  padding: 40px 0 28px;
  margin-top: 40px;
}

.page-footer__nav-list {
  display: flex;
  justify-content: center;
  gap: 28px;
  margin-bottom: 24px;
}

.page-footer__nav-list a {
  color: #c8ccd4;
  font-size: 14px;
}

.page-footer__nav-list a:hover {
  color: var(--huiyue-primary);
}

.page-footer__copyright {
  text-align: center;
  font-size: 13px;
}

.page-footer__copyright p {
  margin: 4px 0;
}

.records {
  color: #707070;
}
</style>
