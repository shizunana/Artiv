<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1 class="title">绘约 HuiYue</h1>
      <p class="subtitle">美术约稿交易平台</p>
      <el-form :model="loginForm" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" @keyup.enter="onLogin" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onLogin">登 录</el-button>
        <div class="tip">
          还没有账号？<router-link to="/register">去注册</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function onLogin() {
  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push(route.query.redirect || { name: 'Home' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2ff, #fdf2f8);
}

.login-card {
  width: 400px;
  padding: 8px 16px 24px;
}

.title {
  text-align: center;
  margin: 8px 0 4px;
}

.subtitle {
  text-align: center;
  color: var(--huiyue-text-secondary);
  margin: 0 0 16px;
}

.submit {
  width: 100%;
}

.tip {
  margin-top: 12px;
  text-align: center;
  color: var(--huiyue-text-secondary);
}
</style>
