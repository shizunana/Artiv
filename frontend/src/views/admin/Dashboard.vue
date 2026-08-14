<template>
  <div class="admin-dashboard">
    <div class="wb-card">
      <h3 class="card-title">平台概览</h3>
      <div class="stat-row">
        <div class="stat-card">
          <strong>3</strong>
          <span>注册用户</span>
        </div>
        <div class="stat-card">
          <strong>2</strong>
          <span>企划总数</span>
        </div>
        <div class="stat-card">
          <strong>1</strong>
          <span>订单总数</span>
        </div>
        <div class="stat-card">
          <strong>¥1,200</strong>
          <span>成交金额</span>
        </div>
      </div>
      <el-alert
        title="数据统计接口（/admin/stats）将在阶段 5 接入，当前为演示占位数据"
        type="info"
        :closable="false"
        show-icon
      />
    </div>
  </div>
</template>

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

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.stat-card {
  background: var(--artiv-section-alt);
  border-radius: 8px;
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-card strong {
  font-size: 24px;
  color: var(--artiv-primary);
}

.stat-card span {
  font-size: 13px;
  color: var(--artiv-text-secondary);
}

@media (max-width: 900px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
