<template>
  <div class="page-container">
    <div class="wb-card">
      <h3 class="card-title">用户管理</h3>
      <el-alert
        title="用户管理接口（/admin/users）将在阶段 5 接入：封禁 / 解封、画师认证审核"
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
