<template>
  <div class="painter-profile">
    <div class="wb-card">
      <h3 class="card-title">个人资料与认证</h3>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ userStore.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userStore.user?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="角色">画师</el-descriptions-item>
        <el-descriptions-item label="认证状态">
          <el-tag :type="certType" size="small">{{ certText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="个人简介" :span="2">{{ userStore.user?.bio || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="风格标签" :span="2">{{ userStore.user?.tags || '未填写' }}</el-descriptions-item>
      </el-descriptions>

      <div class="cert-actions">
        <template v-if="certStatus === 0">
          <el-button type="primary" round @click="applyCert">申请画师认证</el-button>
          <span class="cert-tip">需要完善个人资料并上传作品，将有助于快速通过认证。</span>
        </template>
        <template v-else-if="certStatus === 1">
          <el-alert title="认证审核中，请耐心等待管理员审核" type="warning" :closable="false" show-icon />
        </template>
        <template v-else-if="certStatus === 3">
          <el-alert title="认证被驳回，请完善资料后重新申请" type="error" :closable="false" show-icon />
          <el-button type="primary" round class="mt16" @click="applyCert">重新申请</el-button>
        </template>
        <template v-else>
          <el-alert title="您已通过画师认证，可以应征企划、开设橱窗" type="success" :closable="false" show-icon />
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { applyCert as applyCertApi } from '@/api/user'
import { CERT_STATUS, statusText, statusType } from '@/utils/status'

const userStore = useUserStore()

const certStatus = computed(() => userStore.user?.certStatus ?? 0)
const certText = computed(() => statusText(CERT_STATUS, certStatus.value))
const certType = computed(() => statusType(CERT_STATUS, certStatus.value))

async function applyCert() {
  const user = await applyCertApi()
  userStore.user = user
  ElMessage.success('认证申请已提交，请等待管理员审核')
}
</script>

<style scoped>
.wb-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 24px;
}

.card-title {
  margin: 0 0 18px;
  font-size: 17px;
}

.cert-actions {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.cert-tip {
  color: var(--huiyue-text-secondary);
  font-size: 13px;
}

.mt16 {
  margin-top: 16px;
}
</style>
