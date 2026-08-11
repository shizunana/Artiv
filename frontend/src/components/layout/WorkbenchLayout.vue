<template>
  <el-container class="workbench">
    <el-aside width="200px" class="sidebar">
      <div class="logo">
        <router-link to="/">绘约</router-link>
      </div>
      <el-menu :default-active="activePath" router class="menu">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          {{ item.title }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span>{{ currentTitle }}</span>
        <el-dropdown @command="onCommand">
          <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
    { path: '/painter/works', title: '作品集管理' },
    { path: '/painter/stalls', title: '服务板管理' },
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
const currentTitle = computed(() => route.meta.title || '绘约')
const activePath = computed(() => route.path)

function onCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push({ name: 'Home' })
  }
}
</script>

<style scoped>
.workbench {
  min-height: 100vh;
}

.sidebar {
  background: #1f2329;
  color: #fff;
}

.logo {
  padding: 18px 16px;
  font-size: 17px;
  font-weight: 700;
}

.logo a {
  color: #fff;
}

.menu {
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-text-color: #c8ccd4;
  --el-menu-active-color: #fff;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.08);
}

.menu .el-menu-item {
  color: #c8ccd4;
}

.menu .el-menu-item.is-active {
  background: rgba(79, 124, 255, 0.35);
  color: #fff;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid var(--huiyue-border);
}

.user-name {
  cursor: pointer;
  color: var(--huiyue-text);
}
</style>
