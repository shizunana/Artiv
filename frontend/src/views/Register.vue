<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="auth-logo">绘</span>
        <h1>注册绘约</h1>
        <p>仅需 3 分钟，开启高效的美术约稿体验</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="large">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20 位字符" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="展示给其他用户的名称" :prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-32 位" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item label="我是" prop="role">
          <div class="role-picker">
            <div
              class="role-option"
              :class="{ 'is-active': form.role === 1 }"
              @click="form.role = 1"
            >
              <span class="role-icon">📢</span>
              <strong>企划方</strong>
              <small>发布需求，寻找画师</small>
            </div>
            <div
              class="role-option"
              :class="{ 'is-active': form.role === 2 }"
              @click="form.role = 2"
            >
              <span class="role-icon">🎨</span>
              <strong>画师</strong>
              <small>接稿创作，展示作品</small>
            </div>
          </div>
        </el-form-item>
        <el-button type="primary" class="auth-submit" :loading="loading" @click="onRegister">注 册</el-button>
      </el-form>

      <div class="auth-switch">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue'
import { register as registerApi } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '', role: 2 })

const rules = {
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

async function onRegister() {
  loading.value = true
  try {
    await registerApi(form)
    ElMessage.success('注册成功，请登录')
    router.push({ name: 'Login', query: { username: form.username } })
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
  width: 440px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(0, 168, 233, 0.12);
  padding: 40px 40px 32px;
}

.auth-brand {
  text-align: center;
  margin-bottom: 26px;
}

.auth-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--huiyue-primary);
  color: #fff;
  font-size: 28px;
  margin-bottom: 12px;
}

.auth-brand h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 6px;
  color: var(--huiyue-text);
}

.auth-brand p {
  margin: 0;
  font-size: 13px;
  color: var(--huiyue-text-secondary);
}

.role-picker {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  width: 100%;
}

.role-option {
  border: 1px solid var(--huiyue-border);
  border-radius: 10px;
  padding: 16px 12px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.role-option:hover {
  border-color: var(--huiyue-primary);
}

.role-option.is-active {
  border-color: var(--huiyue-primary);
  background: var(--huiyue-primary-light);
  box-shadow: 0 0 0 1px var(--huiyue-primary);
}

.role-icon {
  font-size: 26px;
}

.role-option strong {
  color: var(--huiyue-text);
  font-size: 15px;
}

.role-option small {
  color: var(--huiyue-text-secondary);
  font-size: 12px;
}

.auth-submit {
  width: 100%;
  border-radius: 22px;
  letter-spacing: 6px;
  font-size: 16px;
  padding: 22px 0;
  margin-top: 4px;
}

.auth-switch {
  margin-top: 22px;
  text-align: center;
  font-size: 13px;
  color: var(--huiyue-text-secondary);
}
</style>
