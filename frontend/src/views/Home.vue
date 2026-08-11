<template>
  <div class="home-page">
    <!-- HERO 首屏（参考站 hero-unit） -->
    <section class="hero-unit">
      <div class="hero-unit__mask"></div>
      <div class="hero-unit__content page-container">
        <h1>找好画师，完成需求，就这么简单！</h1>
        <div class="hero-unit__desc">仅需 3 分钟发布需求，即可收到数十位画师应征。<br />精选画师在此等待，只为创作属于您的精美画作。</div>
        <div class="hero-unit__actions">
          <button class="pill-button pill-button--primary" @click="go('projects')">我是需求方</button>
          <button class="pill-button pill-button--outline" @click="go('artists')">我是画师</button>
        </div>
        <div class="hero-unit__stats">
          <div class="stat"><strong>10+</strong><span>功能模块</span></div>
          <div class="stat"><strong>100%</strong><span>模拟托管担保</span></div>
          <div class="stat"><strong>24h</strong><span>实时沟通</span></div>
        </div>
      </div>
    </section>

    <!-- F01 简单易用的管理工具 -->
    <section class="landing-section is-white">
      <div class="page-container">
        <h2 class="section-title">简单易用的管理工具，帮您高效合作</h2>
        <div class="feature-grid">
          <div v-for="f in features" :key="f.title" class="feature-card">
            <div class="feature-icon" :style="{ background: f.bg }">{{ f.icon }}</div>
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- G01 作品展示（万名画师 → 作品网格） -->
    <section class="landing-section is-alt">
      <div class="page-container">
        <h2 class="section-title">精选作品，涵盖各式风格</h2>
        <p class="section-subtitle">来自平台认证画师的优选作品</p>
        <div class="category-tabs">
          <span
            v-for="cat in categories"
            :key="cat"
            class="category-tab"
            :class="{ 'is-active': cat === activeCategory }"
            @click="activeCategory = cat"
          >
            {{ cat }}
          </span>
        </div>
        <div class="artwork-grid">
          <div v-for="(work, i) in filteredWorks" :key="work.id" class="artwork-cell" @click="go('works')">
            <div class="artwork-thumb" :style="gradient(i)">
              <span class="artwork-emoji">{{ emojis[i % emojis.length] }}</span>
            </div>
            <div class="artwork-info">
              <h4>{{ work.title }}</h4>
              <span class="artwork-category">{{ work.category }}</span>
            </div>
          </div>
        </div>
        <div class="more-link">
          <el-button text type="primary" @click="go('works')">更多作品 →</el-button>
        </div>
      </div>
    </section>

    <!-- F02 告别传统美术外包 -->
    <section class="landing-section is-white">
      <div class="page-container">
        <h2 class="section-title">告别传统美术外包，迎接全新的解决办法</h2>
        <p class="section-subtitle">流畅的约稿体验，绘约全面保证您的交易安全与质量</p>
        <div class="solution-grid">
          <div v-for="s in solutions" :key="s.title" class="solution-card">
            <div class="solution-icon">{{ s.icon }}</div>
            <h3>{{ s.title }}</h3>
            <p>{{ s.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- T01 用户评价 -->
    <section class="landing-section is-alt">
      <div class="page-container">
        <h2 class="section-title">使用过绘约的人怎么说？</h2>
        <p class="section-subtitle">发现并敢于尝试一个新事物确实非常困难，您可以先看看我们的用户怎么说。</p>
        <div class="comment-grid">
          <div v-for="(c, i) in comments" :key="i" class="comment-cell">
            <div class="comment-cell__quote">“</div>
            <p class="comment-cell__content">{{ c.content }}</p>
            <div class="comment-cell__header">
              <span class="comment-cell__role">{{ c.role }}</span>
              <span class="comment-cell__author">{{ c.author }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- B01 底部 CTA -->
    <section class="cta-banner">
      <h2>立即尝试完全不同于传统美术外包的解决方案</h2>
      <button class="pill-button pill-button--primary" @click="go(userStore.isLoggedIn ? workbenchHome : 'register')">
        {{ userStore.isLoggedIn ? '进入工作台' : '免费注册并使用' }}
      </button>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getWorks } from '@/api/work'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const works = ref([])
const loading = ref(false)
const activeCategory = ref('全部')

const categories = ['全部', '国潮', '插画', '立绘', '场景', '原画', '同人']

const emojis = ['🎨', '🖌️', '✨', '🖼️', '🎭', '🌊', '🌸', '🐉']

const gradients = [
  'linear-gradient(135deg, #4facfe, #00f2fe)',
  'linear-gradient(135deg, #f093fb, #f5576c)',
  'linear-gradient(135deg, #43e97b, #38f9d7)',
  'linear-gradient(135deg, #fa709a, #fee140)',
  'linear-gradient(135deg, #30cfd0, #330867)',
  'linear-gradient(135deg, #a8edea, #fed6e3)',
  'linear-gradient(135deg, #ff9a9e, #fecfef)',
  'linear-gradient(135deg, #667eea, #764ba2)',
]

const features = [
  { icon: '👥', bg: '#d5f3ff', title: '海量画师应征', desc: '发布需求后可获得数十位画师应征，作品、信誉透明，供您全面评估。' },
  { icon: '📋', bg: '#fff7f1', title: '流畅合作管理', desc: '轻松管理合作画师，分段担保资金、分步审查稿件，告别跑单拖稿。' },
  { icon: '💬', bg: '#e8f8f4', title: '在线实时沟通', desc: '随时随地反馈修改意见，与合作画师一对一直接沟通，共同创作优秀作品。' },
]

const solutions = [
  { icon: '💬', title: '简单便捷的沟通', desc: '在线即时聊天，与画师一对一沟通，需求意向实时传达，提高合作效率。' },
  { icon: '🗂️', title: '轻松高效的管理', desc: '自定义稿件验收阶段和详细报表，轻松管理多位合作画师。' },
  { icon: '🔒', title: '可信赖的担保服务', desc: '分阶段验收驱动模拟资金托管，保证项目资料及信息不会泄露。' },
  { icon: '⭐', title: '专业的评价体系', desc: '交易完成后双方互评，沉淀画师信誉，帮助您找到更合适的合作对象。' },
]

const comments = [
  { content: '页面设计很直观，画师风格鲜明，约稿面板的流程清晰，约稿很方便，客服热心。', role: '约稿负责人', author: '蓝诺' },
  { content: '画师众多，找画师也方便，基本上各种风格的画师都能在这里找到，流程清晰。', role: '约稿负责人', author: 'EARVA' },
  { content: '专业的平台，优质的服务。画师资源十分丰富，风格多样，操作方便。', role: '项目组', author: '岚风' },
]

const filteredWorks = computed(() => {
  if (activeCategory.value === '全部') return works.value
  return works.value.filter((w) => w.category === activeCategory.value)
})

const workbenchHome = computed(() => {
  const role = userStore.user?.role
  if (role === 2) return '/painter'
  if (role === 3) return '/admin'
  return '/dashboard'
})

function gradient(i) {
  return { background: gradients[i % gradients.length] }
}

function go(target) {
  const routes = {
    projects: { name: 'Projects' },
    artists: { name: 'Artists' },
    works: { name: 'Works' },
    stalls: { name: 'Stalls' },
    register: { name: 'Register' },
  }
  router.push(typeof target === 'string' && routes[target] ? routes[target] : target)
}

onMounted(async () => {
  loading.value = true
  try {
    const data = await getWorks({ page: 1, size: 8 })
    works.value = data.records || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* ---------- HERO ---------- */
.hero-unit {
  position: relative;
  min-height: 560px;
  display: flex;
  align-items: center;
  background: linear-gradient(120deg, #0f2027 0%, #203a43 55%, #2c5364 100%);
  color: #fff;
  overflow: hidden;
}

.hero-unit__mask {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 30%, rgba(0, 168, 233, 0.28), transparent 55%),
    radial-gradient(ellipse at 80% 70%, rgba(246, 161, 29, 0.18), transparent 50%);
}

.hero-unit__content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.hero-unit__content h1 {
  font-size: 34px;
  font-weight: 500;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
  letter-spacing: 5px;
  line-height: 1.8;
  margin: 0 0 16px;
}

.hero-unit__desc {
  font-size: 16px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.85);
}

.hero-unit__actions {
  margin-top: 48px;
  display: flex;
  justify-content: center;
  gap: 60px;
}

.hero-unit__stats {
  display: flex;
  justify-content: center;
  gap: 80px;
  margin-top: 56px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat strong {
  font-size: 26px;
  font-weight: 600;
  color: var(--huiyue-primary-light);
}

.stat span {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

/* ---------- 通用分区 ---------- */
.landing-section {
  padding: 72px 0;
}

.landing-section.is-white {
  background: #fff;
}

.landing-section.is-alt {
  background: var(--huiyue-section-alt);
}

/* ---------- 特性区 ---------- */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  margin-top: 36px;
}

.feature-card {
  background: #fff;
  border: 1px solid var(--huiyue-border);
  border-radius: 12px;
  padding: 32px 28px;
  text-align: center;
  transition: transform 0.2s, box-shadow 0.2s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.feature-icon {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  margin: 0 auto 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
}

.feature-card h3 {
  margin: 0 0 10px;
  color: var(--huiyue-text);
  font-size: 18px;
}

.feature-card p {
  margin: 0;
  color: var(--huiyue-text-secondary);
  line-height: 1.8;
}

/* ---------- 作品展示 ---------- */
.category-tabs {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.category-tab {
  padding: 8px 22px;
  border-radius: 20px;
  background: #fff;
  color: var(--huiyue-text-secondary);
  cursor: pointer;
  border: 1px solid var(--huiyue-border);
  font-size: 14px;
  transition: all 0.2s;
}

.category-tab:hover {
  color: var(--huiyue-primary);
  border-color: var(--huiyue-primary);
}

.category-tab.is-active {
  background: var(--huiyue-primary);
  color: #fff;
  border-color: var(--huiyue-primary);
}

.artwork-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.artwork-cell {
  cursor: pointer;
  transition: transform 0.2s;
}

.artwork-cell:hover {
  transform: translateY(-4px);
}

.artwork-thumb {
  aspect-ratio: 4 / 3;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.artwork-emoji {
  font-size: 52px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.25));
}

.artwork-info {
  padding: 12px 4px 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.artwork-info h4 {
  margin: 0;
  font-size: 15px;
  color: var(--huiyue-text);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artwork-category {
  font-size: 12px;
  color: var(--huiyue-primary);
  background: var(--huiyue-primary-light);
  padding: 2px 10px;
  border-radius: 10px;
  flex-shrink: 0;
}

.more-link {
  text-align: center;
  margin-top: 24px;
}

/* ---------- 方案区 ---------- */
.solution-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-top: 32px;
}

.solution-card {
  background: var(--huiyue-section-alt);
  border-radius: 12px;
  padding: 28px 22px;
  text-align: center;
}

.solution-icon {
  font-size: 34px;
  margin-bottom: 14px;
}

.solution-card h3 {
  margin: 0 0 10px;
  font-size: 17px;
  color: var(--huiyue-text);
}

.solution-card p {
  margin: 0;
  font-size: 13px;
  color: var(--huiyue-text-secondary);
  line-height: 1.8;
}

/* ---------- 评价区 ---------- */
.comment-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.comment-cell {
  background: #fff;
  border-radius: 12px;
  padding: 28px 24px;
  position: relative;
  border: 1px solid var(--huiyue-border);
}

.comment-cell__quote {
  font-size: 48px;
  line-height: 1;
  color: var(--huiyue-primary);
  font-family: Georgia, serif;
  margin-bottom: 6px;
}

.comment-cell__content {
  color: var(--huiyue-text);
  line-height: 1.9;
  font-size: 14px;
  min-height: 84px;
  margin: 0 0 16px;
}

.comment-cell__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--huiyue-border);
  padding-top: 14px;
}

.comment-cell__role {
  color: var(--huiyue-accent);
  font-size: 13px;
}

.comment-cell__author {
  color: var(--huiyue-text-secondary);
  font-size: 13px;
}

/* ---------- CTA ---------- */
.cta-banner {
  background: linear-gradient(120deg, #203a43, #2c5364);
  color: #fff;
  text-align: center;
  padding: 72px 16px;
}

.cta-banner h2 {
  font-size: 28px;
  font-weight: 500;
  margin: 0 0 36px;
  letter-spacing: 2px;
}

@media (max-width: 900px) {
  .feature-grid,
  .comment-grid {
    grid-template-columns: 1fr;
  }

  .solution-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .artwork-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .hero-unit__actions {
    flex-direction: column;
    gap: 20px;
    align-items: center;
  }

  .hero-unit__stats {
    gap: 32px;
  }
}
</style>
