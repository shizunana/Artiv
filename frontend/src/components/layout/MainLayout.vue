<template>
  <div class="landing-layout">
    <!-- 统一顶部导航 -->
    <AppHeader />

    <main class="landing-layout__main">
      <router-view />
    </main>

    <!-- 页脚（参考站 page-footer） -->
    <footer class="page-footer">
      <div class="page-container">
        <div class="page-footer__nav-list">
          <router-link to="/projects">企划大厅</router-link>
          <router-link to="/stalls">服务板</router-link>
          <router-link to="/works">作品广场</router-link>
          <router-link to="/artists">画师列表</router-link>
          <router-link to="/login">登录</router-link>
          <router-link to="/register">注册</router-link>
        </div>
        <div class="page-footer__copyright">
          <p>艺绘 · 带给你自由、高效、专业的美术约稿体验</p>
          <p class="records">Copyright © 2026 艺绘 保留所有权利 · 毕业设计作品</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import AppHeader from './AppHeader.vue'
</script>

<style scoped>
.landing-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.landing-layout__main {
  flex: 1;
  padding-top: var(--artiv-header-height);
}

/* ---------- 页脚 ---------- */
.page-footer {
  /* 暂时隐藏，保留完整页脚结构以便后续恢复。 */
  display: none;
  background: #23262b;
  color: #9b9b9b;
  padding: 40px 0 28px;
  margin-top: 40px;
}

.page-footer__nav-list {
  display: flex;
  justify-content: center;
  gap: 28px;
  margin-bottom: 24px;
}

.page-footer__nav-list a {
  color: #c8ccd4;
  font-size: 14px;
}

.page-footer__nav-list a:hover {
  color: var(--artiv-primary);
}

.page-footer__copyright {
  text-align: center;
  font-size: 13px;
}

.page-footer__copyright p {
  margin: 4px 0;
}

.records {
  color: #707070;
}
</style>
