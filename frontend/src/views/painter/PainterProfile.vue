<template>
  <div class="painter-profile">
    <div class="wb-card">
      <div class="profile-title-row">
        <h3 class="card-title">个人资料与认证</h3>
        <el-button v-if="certStatus === 2 && !editing" type="primary" plain @click="startEdit">编辑展示资料</el-button>
      </div>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ userStore.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userStore.user?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="角色">画师</el-descriptions-item>
        <el-descriptions-item label="认证状态">
          <el-tag :type="certType" size="small">{{ certText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="个人简介" :span="2">
          <el-input v-if="editing" v-model="form.bio" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="介绍你的擅长方向、接稿状态等" />
          <span v-else>{{ userStore.user?.bio || '未填写' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="风格标签" :span="2">
          <el-select v-if="editing" v-model="form.tags" multiple filterable class="tag-select" placeholder="请选择题材风格（可多选）">
            <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <span v-else>{{ userStore.user?.tags || '未填写' }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="editing" class="profile-edit-actions">
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存展示资料</el-button>
      </div>

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
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { applyCert as applyCertApi, updateMyPainterProfile } from '@/api/user'
import { SUBJECT_OPTIONS } from '@/constants/subjects'
import { CERT_STATUS, statusText, statusType } from '@/utils/status'

const userStore = useUserStore()

const certStatus = computed(() => userStore.user?.certStatus ?? 0)
const certText = computed(() => statusText(CERT_STATUS, certStatus.value))
const certType = computed(() => statusType(CERT_STATUS, certStatus.value))
const subjectOptions = SUBJECT_OPTIONS
const editing = ref(false)
const saving = ref(false)
const form = reactive({ bio: '', tags: [] })

function startEdit() {
  form.bio = userStore.user?.bio || ''
  form.tags = (userStore.user?.tags || '').split(',').map(tag => tag.trim()).filter(tag => subjectOptions.includes(tag))
  editing.value = true
}

function cancelEdit() {
  editing.value = false
}

async function saveProfile() {
  saving.value = true
  try {
    const user = await updateMyPainterProfile({ bio: form.bio, tags: form.tags })
    userStore.updateCurrentUser(user)
    editing.value = false
    ElMessage.success('展示资料已保存')
  } finally {
    saving.value = false
  }
}

async function applyCert() {
  const user = await applyCertApi()
  userStore.updateCurrentUser(user)
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

.profile-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.profile-title-row .card-title {
  margin-bottom: 18px;
}

.tag-select {
  width: 100%;
}

.profile-edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
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
