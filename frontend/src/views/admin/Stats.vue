<template>
  <div class="page-container">
    <div class="wb-card">
      <h3 class="card-title">数据统计</h3>
      <el-alert
        title="统计接口（/admin/stats）将在阶段 5 接入：注册量 / 订单量 / 成交额"
        type="info"
        :closable="false"
        show-icon
      />
    </div>
  </div>
</template>

<script setup>
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
</style>
