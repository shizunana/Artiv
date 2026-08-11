<template>
  <div class="page-container">
    <section class="hero">
      <h1>让每一次约稿都被认真对待</h1>
      <p>发布需求、画师应征、分阶段验收、互相评价，绘约帮你完成美术约稿全流程。</p>
      <div class="actions">
        <el-button type="primary" size="large" @click="router.push({ name: 'Projects' })">浏览企划大厅</el-button>
        <el-button size="large" @click="router.push({ name: 'Login' })">立即注册</el-button>
      </div>
    </section>

    <el-row :gutter="16" class="steps">
      <el-col v-for="step in steps" :key="step.title" :span="6">
        <el-card>
          <h3>{{ step.title }}</h3>
          <p>{{ step.desc }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const steps = [
  { title: '01 发布需求', desc: '企划方发布约稿需求，设置预算与截止时间' },
  { title: '02 画师应征', desc: '画师提交作品与报价，参与应征' },
  { title: '03 下单验收', desc: '选定画师下单，分阶段沟通与验收' },
  { title: '04 互相评价', desc: '交易完成，双方互评沉淀信誉' },
]
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 72px 0 48px;
}

.hero h1 {
  font-size: 36px;
  margin: 0 0 12px;
}

.hero p {
  color: var(--huiyue-text-secondary);
  margin: 0 0 24px;
}

.steps {
  padding-bottom: 48px;
}
</style>
