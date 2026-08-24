<template>
  <div class="workbench-page">
    <!-- 统一顶部导航 -->
    <AppHeader />

    <div class="page-container wb-body">
      <!-- 左侧工作台卡片（参考站 dashboard-sidebar） -->
      <div class="wb-side-column">
        <aside class="wb-sidebar">
        <div class="wb-sidebar__user">
          <el-avatar :size="46" :src="userStore.user?.avatar" class="wb-sidebar-avatar">{{ avatarText }}</el-avatar>
          <div class="wb-sidebar__user-info">
            <span class="wb-sidebar__role">当前工作台 · {{ roleText }}</span>
            <span class="wb-sidebar__name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
          </div>
        </div>

        <div class="wb-sidebar__tabs">
          <template v-for="item in menuItems" :key="item.path">
            <div v-if="item.children" class="wb-sidebar__group">
              <button
                type="button"
                class="wb-sidebar__tab wb-sidebar__group-toggle"
                :aria-expanded="orderMenuExpanded"
                @click="orderMenuExpanded = !orderMenuExpanded"
              >
                <span>{{ item.title }}</span>
                <el-icon class="wb-sidebar__chevron" :class="{ 'is-open': orderMenuExpanded }"><ArrowDown /></el-icon>
              </button>
              <div v-show="orderMenuExpanded" class="wb-sidebar__children">
                <router-link
                  v-for="child in item.children"
                  :key="child.participantRole"
                  :to="{ path: child.path, query: { participantRole: child.participantRole } }"
                  class="wb-sidebar__subtab"
                  :class="{ 'is-active': isOrderChildActive(child) }"
                >
                  {{ child.title }}
                </router-link>
              </div>
            </div>
            <router-link
              v-else
              :to="item.path"
              class="wb-sidebar__tab"
              :class="{ 'is-active': isMenuActive(item) }"
            >
              <span>{{ item.title }}</span>
              <el-icon v-if="item.soon" class="wb-tab-soon"><Right /></el-icon>
            </router-link>
          </template>
        </div>

        <div v-if="workbench !== 'admin'" class="wb-sidebar__foot">
          <button class="wb-switch" :disabled="switchingRole" @click="switchRole">
            {{ switchingRole ? '正在切换…' : `切换到${oppositeRoleText}工作台` }}
          </button>
        </div>
        </aside>
        <FavoriteShortcutCard v-if="workbench !== 'admin'" :workbench="workbench" />
      </div>

      <!-- 主内容区（参考站 dashboard__main） -->
      <main class="wb-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Right } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import AppHeader from './AppHeader.vue'
import FavoriteShortcutCard from '@/components/favorites/FavoriteShortcutCard.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const switchingRole = ref(false)
const orderMenuExpanded = ref(false)

const menus = {
  dashboard: [
    { path: '/dashboard', title: '工作台主页' },
    { path: '/dashboard/projects', title: '我的企划' },
    { path: '/dashboard/projects/new', title: '发布企划' },
    {
      path: '/dashboard/orders',
      title: '订单管理',
      children: [
        { path: '/dashboard/orders', title: '我委托的订单', participantRole: 'employer' },
        { path: '/dashboard/orders', title: '我承接的订单', participantRole: 'painter' },
      ],
    },
  ],
  painter: [
    { path: '/painter', title: '工作台主页' },
    { path: '/painter/works', title: '我的作品' },
    { path: '/painter/stalls', title: '我的橱窗' },
    { path: '/painter/applications', title: '我的应征' },
    { path: '/painter/invitations', title: '收到的邀请' },
    {
      path: '/painter/orders',
      title: '订单管理',
      children: [
        { path: '/painter/orders', title: '我委托的订单', participantRole: 'employer' },
        { path: '/painter/orders', title: '我承接的订单', participantRole: 'painter' },
      ],
    },
  ],
  admin: [
    { path: '/admin', title: '概览' },
    { path: '/admin/users', title: '用户管理' },
    { path: '/admin/certifications', title: '认证审核' },
    { path: '/admin/projects', title: '企划治理' },
    { path: '/admin/disputes', title: '订单仲裁' },
    { path: '/admin/review-appeals', title: '评价申诉' },
    { path: '/admin/stats', title: '数据统计' },
    { path: '/admin/audit-logs', title: '操作审计' },
  ],
}

const workbench = computed(() => route.meta.workbench || 'dashboard')
const menuItems = computed(() => menus[workbench.value] || [])
const isOrdersRoute = computed(() => /^\/(dashboard|painter)\/orders(?:\/|$)/.test(route.path))
const selectedOrderRole = computed(() => {
  if (route.query.participantRole === 'employer' || route.query.participantRole === 'painter') {
    return route.query.participantRole
  }
  return workbench.value === 'painter' ? 'painter' : 'employer'
})
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

function isMenuActive(item) {
  return route.path === item.path
}

function isOrderChildActive(child) {
  return isOrdersRoute.value && selectedOrderRole.value === child.participantRole
}

watch(
  () => route.path,
  () => {
    if (isOrdersRoute.value) orderMenuExpanded.value = true
  },
  { immediate: true },
)

async function switchRole() {
  if (switchingRole.value) return
  switchingRole.value = true
  try {
    const nextRole = userStore.workbenchRole === 2 ? 1 : 2
    await userStore.switchRole(nextRole)
    await router.push(nextRole === 2 ? '/painter' : '/dashboard')
  } finally {
    switchingRole.value = false
  }
}
</script>

<style scoped>
.workbench-page {
  min-height: 100vh;
  background: #f4f7f9;
  padding-top: var(--artiv-header-height);
  padding-bottom: 64px;
}

/* ---------- 主体布局 ---------- */
.wb-body {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  margin-top: 34px;
}

/* ---------- 左侧工作台（参考站 dashboard-sidebar：282px 白卡片） ---------- */
.wb-side-column {
  flex: 0 0 250px;
  width: 250px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: calc(var(--artiv-header-height) + 24px);
}

.wb-sidebar {
  width: 100%;
  background: #fff;
  border: 1px solid #e4ebf0;
  border-radius: 12px;
  box-shadow: 0 8px 28px rgb(42 67 84 / 6%);
  padding: 10px;
  display: flex;
  flex-direction: column;
}

.wb-sidebar__user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 14px 18px;
  border-bottom: 1px solid #edf1f4;
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
  color: #1d2a34;
  font-size: 16px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-sidebar__tabs {
  display: flex;
  flex-direction: column;
  margin-top: 8px;
  flex: 1;
}

.wb-sidebar__tab {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding: 14px;
  border-radius: 8px;
  color: #53616d;
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
  margin-top: 8px;
  border-top: 1px solid #edf1f4;
  padding: 16px 14px 4px;
}

.wb-sidebar__group {
  width: 100%;
}

.wb-sidebar__group-toggle {
  width: 100%;
  border: 0;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}

.wb-sidebar__chevron {
  color: #8996a1;
  font-size: 14px;
  transition: transform 0.2s ease;
}

.wb-sidebar__chevron.is-open {
  transform: rotate(180deg);
}

.wb-sidebar__children {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 0 0 16px;
}

.wb-sidebar__subtab {
  padding: 10px 14px;
  border-radius: 8px;
  color: #66737e;
  font-size: 13px;
  text-decoration: none;
  transition: background 0.2s, color 0.2s;
}

.wb-sidebar__subtab:hover,
.wb-sidebar__subtab.is-active {
  background: var(--artiv-primary-light);
  color: var(--artiv-primary);
}

.wb-sidebar__subtab.is-active {
  font-weight: 600;
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

.wb-switch:disabled {
  cursor: wait;
  opacity: 0.65;
}

/* ---------- 主内容区 ---------- */
.wb-main {
  flex: 1;
  min-width: 0;
  filter: none;
}

@media (max-width: 900px) {
  .wb-body {
    flex-direction: column;
  }

  .wb-side-column {
    flex: none;
    width: 100%;
    position: static;
  }

  .wb-sidebar__tabs {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .wb-sidebar__group {
    width: 100%;
  }

  .wb-sidebar__children {
    flex-direction: row;
    padding-left: 0;
  }

  .wb-sidebar__subtab {
    flex: 1;
    text-align: center;
  }

}
</style>
