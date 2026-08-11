<template>
  <div class="page-container">
    <h2>服务板</h2>
    <el-row :gutter="16">
      <el-col v-for="stall in stalls" :key="stall.id" :span="6">
        <el-card class="stall-card">
          <h3>{{ stall.title }}</h3>
          <p>{{ stall.description }}</p>
          <div class="meta">¥{{ stall.price }} · {{ stall.category }}</div>
          <el-button type="primary" size="small" class="buy" @click="goDetail(stall.id)">立即下单</el-button>
        </el-card>
      </el-col>
    </el-row>
    <EmptyState v-if="!loading && stalls.length === 0" description="暂无上架服务" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStalls } from '@/api/stall'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const stalls = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const data = await getStalls({ page: 1, size: 8 })
    stalls.value = data.records || []
  } finally {
    loading.value = false
  }
})

function goDetail(id) {
  router.push({ name: 'StallDetail', params: { id } })
}
</script>

<style scoped>
.stall-card h3 {
  margin: 0 0 8px;
}

.stall-card p {
  color: var(--huiyue-text-secondary);
  min-height: 42px;
}

.meta {
  font-size: 13px;
  color: var(--huiyue-text);
  margin-bottom: 12px;
}

.buy {
  width: 100%;
}
</style>
