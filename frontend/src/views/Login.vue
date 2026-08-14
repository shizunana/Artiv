<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="auth-logo">绘</span>
        <h1>艺绘</h1>
        <p>带给你自由、高效、专业的美术约稿体验</p>
      </div>

      <el-form :model="loginForm" :rules="rules" ref="formRef" label-position="top" size="large" @keyup.enter="onLogin">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" :prefix-icon="Lock" />
        </el-form-item>
        <div class="auth-links">
          <router-link to="/register">注册新账号</router-link>
          <span class="forget">忘记密码？</span>
        </div>
        <el-button type="primary" class="auth-submit" :loading="loading" @click="onLogin">登 录</el-button>
      </el-form>

      <div class="auth-switch">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)

const loginForm = reactive({
  username: route.query.username || '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onLogin() {
  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    // 登录后按角色进入对应工作台（除非有明确 redirect）
    const redirect = route.query.redirect
    const workbenchHome = userStore.user?.role === 2 ? '/painter' : userStore.user?.role === 3 ? '/admin' : '/dashboard'
    router.push(redirect || workbenchHome)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eaf7ff 0%, #f5f8ff 50%, #fdf5ec 100%);
  padding: 20px;
}

.auth-card {
  width: 420px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(0, 168, 233, 0.12);
  padding: 44px 40px 36px;
}

.auth-brand {
  text-align: center;
  margin-bottom: 32px;
}

.auth-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--artiv-primary);
  color: #fff;
  font-size: 28px;
  margin-bottom: 14px;
}

.auth-brand h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--artiv-text);
  letter-spacing: 1px;
}

.auth-brand p {
  margin: 0;
  font-size: 13px;
  color: var(--artiv-text-secondary);
}

.auth-links {
  display: flex;
  justify-content: space-between;
  margin: 4px 0 20px;
  font-size: 13px;
}

.forget {
  color: var(--artiv-text-secondary);
  cursor: pointer;
}

.auth-submit {
  width: 100%;
  border-radius: 22px;
  letter-spacing: 6px;
  font-size: 16px;
  padding: 22px 0;
}

.auth-switch {
  margin-top: 24px;
  text-align: center;
  font-size: 13px;
  color: var(--artiv-text-secondary);
}
</style>
