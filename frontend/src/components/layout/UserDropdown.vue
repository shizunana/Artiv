<template>
  <el-dropdown @command="onCommand">
    <slot />
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="workbench">{{ userStore.isAdmin ? '工作台' : '我的工作台' }}</el-dropdown-item>
        <el-dropdown-item v-if="!userStore.isAdmin" command="profile">账户设置</el-dropdown-item>
        <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

function onCommand(command) {
  if (command === 'workbench') {
    if (userStore.isAdmin) router.push('/admin')
    else if (route.path.startsWith('/painter')) router.push('/painter')
    else if (route.path.startsWith('/dashboard')) router.push('/dashboard')
    else router.push(userStore.workbenchRole === 2 ? '/painter' : '/dashboard')
  } else if (command === 'profile') {
    router.push({ name: 'AccountSettings' })
  } else if (command === 'logout') {
    userStore.logout()
    router.push({ name: 'Home' })
  }
}
</script>
