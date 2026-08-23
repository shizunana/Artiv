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
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' },
  },
  // ---- 公开页面 ----
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '主页' } },
      { path: 'artists', name: 'Artists', component: () => import('@/views/artists/ArtistList.vue'), meta: { title: '画师列表' } },
      { path: 'profiles/:id', name: 'ArtistProfile', component: () => import('@/views/artists/ArtistProfile.vue'), meta: { title: '用户主页' } },
      { path: 'works', name: 'Works', component: () => import('@/views/works/WorkList.vue'), meta: { title: '作品广场' } },
      { path: 'projects', name: 'Projects', component: () => import('@/views/projects/ProjectList.vue'), meta: { title: '约稿大厅' } },
      { path: 'projects/:id', name: 'ProjectDetail', component: () => import('@/views/projects/ProjectDetail.vue'), meta: { title: '企划详情' } },
      { path: 'stalls', name: 'Stalls', component: () => import('@/views/stalls/StallList.vue'), meta: { title: '服务板' } },
      { path: 'stalls/:id', name: 'StallDetail', component: () => import('@/views/stalls/StallDetail.vue'), meta: { title: '服务板详情' } },
      { path: 'settings', name: 'AccountSettings', component: () => import('@/views/account/AccountSettings.vue'), meta: { title: '账户设置', requiresAuth: true } },
    ],
  },
  // ---- 企划方工作台 ----
  {
    path: '/dashboard',
    component: () => import('@/components/layout/WorkbenchLayout.vue'),
    meta: { requiresAuth: true, workbench: 'dashboard' },
    children: [
      { path: '', name: 'DashboardHome', component: () => import('@/views/dashboard/DashboardHome.vue'), meta: { title: '工作台主页' } },
      { path: 'projects', name: 'DashboardProjects', component: () => import('@/views/dashboard/MyProjects.vue'), meta: { title: '我的企划' } },
      { path: 'projects/new', name: 'ProjectNew', component: () => import('@/views/dashboard/ProjectNew.vue'), meta: { title: '发布企划' } },
      { path: 'projects/:id/applications', name: 'ProjectApplications', component: () => import('@/views/dashboard/Applications.vue'), meta: { title: '应征管理' } },
      { path: 'orders', name: 'DashboardOrders', component: () => import('@/views/orders/OrderList.vue'), meta: { title: '订单管理' } },
      { path: 'orders/:id', name: 'DashboardOrderDetail', component: () => import('@/views/orders/OrderDetail.vue'), meta: { title: '订单详情' } },
      { path: 'messages', name: 'DashboardMessages', component: () => import('@/views/messages/MessageList.vue'), meta: { title: '消息中心' } },
    ],
  },
  // ---- 画师工作台 ----
  {
    path: '/painter',
    component: () => import('@/components/layout/WorkbenchLayout.vue'),
    meta: { requiresAuth: true, workbench: 'painter' },
    children: [
      { path: '', name: 'PainterHome', component: () => import('@/views/painter/PainterHome.vue'), meta: { title: '画师工作台' } },
      { path: 'profile', redirect: { name: 'AccountSettings', query: { section: 'identity' } } },
      { path: 'works', name: 'PainterWorks', component: () => import('@/views/painter/MyWorks.vue'), meta: { title: '作品集管理' } },
      { path: 'stalls', name: 'PainterStalls', component: () => import('@/views/painter/MyStalls.vue'), meta: { title: '服务板管理' } },
      { path: 'applications', name: 'PainterApplications', component: () => import('@/views/painter/MyApplications.vue'), meta: { title: '我的应征' } },
      { path: 'invitations', name: 'PainterInvitations', component: () => import('@/views/painter/MyInvitations.vue'), meta: { title: '收到的邀请' } },
      { path: 'orders', name: 'PainterOrders', component: () => import('@/views/orders/OrderList.vue'), meta: { title: '订单管理' } },
      { path: 'orders/:id', name: 'PainterOrderDetail', component: () => import('@/views/orders/OrderDetail.vue'), meta: { title: '订单详情' } },
      { path: 'messages', name: 'PainterMessages', component: () => import('@/views/messages/MessageList.vue'), meta: { title: '消息中心' } },
    ],
  },
  // ---- 管理后台 ----
  {
    path: '/admin',
    component: () => import('@/components/layout/WorkbenchLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true, workbench: 'admin' },
    children: [
      { path: '', name: 'AdminHome', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '管理后台' } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/Users.vue'), meta: { title: '用户管理' } },
      { path: 'certifications', name: 'AdminCertifications', component: () => import('@/views/admin/Certifications.vue'), meta: { title: '认证审核' } },
      { path: 'projects', name: 'AdminProjects', component: () => import('@/views/admin/Projects.vue'), meta: { title: '企划审核' } },
      { path: 'disputes', name: 'AdminDisputes', component: () => import('@/views/admin/Disputes.vue'), meta: { title: '订单仲裁' } },
      { path: 'review-appeals', name: 'AdminReviewAppeals', component: () => import('@/views/admin/ReviewAppeals.vue'), meta: { title: '评价申诉' } },
      { path: 'stats', name: 'AdminStats', component: () => import('@/views/admin/Stats.vue'), meta: { title: '数据统计' } },
      { path: 'audit-logs', name: 'AdminAuditLogs', component: () => import('@/views/admin/AuditLogs.vue'), meta: { title: '操作审计' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 登录守卫：未登录访问工作台跳登录；管理员页面校验角色
router.beforeEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 艺绘` : '艺绘'
  const userStore = useUserStore()
  const token = userStore.token

  if (to.meta.requiresAuth && !token) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }
  if (to.matched.some(record => record.meta.requiresAdmin) && !userStore.isAdmin) {
    return { name: 'Home' }
  }
  if (token && userStore.isAdmin && to.name === 'AccountSettings') {
    return { name: 'AdminHome' }
  }
  // 工作台路由与角色匹配：画师访问企划方工作台、企划方访问画师工作台时引导回自己的工作台
  if (token && to.meta.workbench) {
    const role = userStore.workbenchRole
    const expected = role === 2 ? 'painter' : role === 3 ? 'admin' : 'dashboard'
    if (to.meta.workbench !== expected) {
      return { path: `/${expected}` }
    }
  }
  // 已登录用户访问登录/注册页：先退出，保证能正常看到登录表单（支持切换账号）
  if ((to.name === 'Login' || to.name === 'Register') && token) {
    userStore.logout()
    return true
  }
  return true
})

export default router
