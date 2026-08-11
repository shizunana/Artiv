<template>
  <div class="page-container">
    <h2>个人中心</h2>
    <el-card v-if="userStore.user">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ userStore.user.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userStore.user.nickname }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleText }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatDateTime(userStore.user.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/date'

const userStore = useUserStore()

const roleText = computed(() => {
  const role = userStore.user?.role
  return role === 1 ? '企划方' : role === 2 ? '画师' : role === 3 ? '管理员' : '-'
})
</script>
