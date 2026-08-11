<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1 class="title">绘约 HuiYue</h1>
      <p class="subtitle">美术约稿交易平台</p>
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="rules" ref="loginRef" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" @keyup.enter="onLogin" />
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="onLogin">登 录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerRef" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="registerForm.username" placeholder="3-20 位字符" />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="展示给其他用户的名称" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="6-32 位" />
            </el-form-item>
            <el-form-item label="角色" prop="role">
              <el-radio-group v-model="registerForm.role">
                <el-radio :value="1">我是企划方</el-radio>
                <el-radio :value="2">我是画师</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="onRegister">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { register as registerApi } from '@/api/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', nickname: '', password: '', role: 2 })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度需在 3-20 位之间', trigger: 'blur' },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '长度需在 6-32 位之间', trigger: 'blur' },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
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

async function onRegister() {
  loading.value = true
  try {
    await registerApi(registerForm)
    ElMessage.success('注册成功，请登录')
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
    activeTab.value = 'login'
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
</style>
