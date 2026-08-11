<template>
  <div class="register-page">
    <el-card class="register-card">
      <h1 class="title">注册绘约</h1>
      <p class="subtitle">美术约稿交易平台 · 模拟注册</p>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20 位字符" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="展示给其他用户的名称" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="我是" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio :value="1">企划方</el-radio>
            <el-radio :value="2">画师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onRegister">注 册</el-button>
        <div class="tip">
          已有账号？<router-link to="/login">去登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2ff, #fdf2f8);
}

.register-card {
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
