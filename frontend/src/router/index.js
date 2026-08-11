import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'projects',
        name: 'Projects',
        component: () => import('@/views/projects/ProjectList.vue'),
        meta: { title: '企划大厅', requiresAuth: false },
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/orders/OrderList.vue'),
        meta: { title: '我的订单', requiresAuth: true },
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/messages/MessageList.vue'),
        meta: { title: '消息中心', requiresAuth: true },
      },
      {
        path: 'me',
        name: 'Me',
        component: () => import('@/views/me/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true },
      },
      {
        path: 'admin',
        name: 'Admin',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录守卫
router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 绘约` : '绘约'
  const userStore = useUserStore()
  const token = userStore.token

  if (to.meta.requiresAuth && !token) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'Login' && token) {
    return { name: 'Home' }
  }
  return true
})

export default router
