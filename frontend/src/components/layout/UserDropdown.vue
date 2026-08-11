<template>
  <el-dropdown @command="onCommand">
    <slot />
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="profile">个人中心</el-dropdown-item>
        <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

function onCommand(command) {
  if (command === 'profile') {
    router.push({ name: 'Me' })
  } else if (command === 'logout') {
    userStore.logout()
    router.push({ name: 'Home' })
  }
}
</script>
