<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-orb hero-orb--one"></div>
      <div class="hero-orb hero-orb--two"></div>
      <div class="page-container hero-layout">
        <div class="hero-copy">
          <p class="hero-eyebrow">ARTIV · 艺绘</p>
          <h1>让每一个创作需求，<br /><span>遇见合适的画师</span></h1>
          <p class="hero-description">浏览正在招募的企划、画师橱窗与公开作品，找到适合自己的合作方式。</p>
          <div class="hero-actions">
            <button class="action-button action-button--primary" @click="go(primaryAction.to)">
              {{ primaryAction.label }}
              <el-icon><ArrowRight /></el-icon>
            </button>
            <button class="action-button action-button--secondary" @click="go(secondaryAction.to)">
              {{ secondaryAction.label }}
            </button>
          </div>
          <nav class="hero-nav" aria-label="内容快捷入口">
            <button v-for="item in contentLinks" :key="item.label" @click="go(item.to)">
              <span>{{ item.label }}</span>
              <small>{{ item.note }}</small>
            </button>
          </nav>
        </div>

        <div class="hero-gallery" aria-label="创作图片展示">
          <div
            v-for="(image, index) in heroShowcaseImages"
            :key="image.src"
            class="hero-work"
            :class="`hero-work--${index + 1}`"
          >
            <img :src="image.src" :alt="image.alt" loading="eager" />
          </div>
        </div>
      </div>
    </section>

    <main>
      <section class="content-section projects-section">
        <div class="page-container">
          <SectionHeading
            eyebrow="正在招募"
            title="发现值得参与的企划"
            description="从最新公开需求中寻找感兴趣的创作方向"
            action-label="查看全部企划"
            @action="go({ name: 'Projects' })"
          />
          <div v-if="sections.projects.loading" class="project-grid" aria-label="企划加载中">
            <ContentSkeleton v-for="index in 4" :key="index" variant="project" />
          </div>
          <SectionError v-else-if="sections.projects.error" description="企划暂时无法加载" @retry="loadProjects" />
          <div v-else-if="projects.length" class="project-grid">
            <article
              v-for="project in projects"
              :key="project.id"
              class="project-card"
              tabindex="0"
              @click="goProject(project.id)"
              @keyup.enter="goProject(project.id)"
            >
              <div class="project-card__image">
                <img v-if="firstImage(project.images)" :src="firstImage(project.images)" :alt="project.title" loading="lazy" />
                <div v-else class="image-placeholder"><el-icon><Picture /></el-icon></div>
                <span class="content-badge">{{ project.category || '未分类' }}</span>
              </div>
              <div class="project-card__body">
                <div class="project-card__topline">
                  <span>公开招募</span>
                  <strong>¥{{ formatMoney(project.budget) }}</strong>
                </div>
                <h3>{{ project.title }}</h3>
                <p>{{ project.description || '发布者暂未填写企划说明。' }}</p>
                <div class="project-meta">
                  <span><el-icon><Calendar /></el-icon>{{ formatDeadline(project.deadline) }}</span>
                  <span><span class="applicant-pawn" aria-hidden="true">♟︎</span>{{ applicationText(project.applicationCount) }}</span>
                </div>
              </div>
            </article>
          </div>
          <ContentEmpty v-else title="暂无正在招募的企划" action-label="浏览企划大厅" @action="go({ name: 'Projects' })" />
        </div>
      </section>

      <section class="content-section stalls-section">
        <div class="page-container">
          <SectionHeading
            eyebrow="画师橱窗"
            title="从明确的服务开始合作"
            description="查看画师已经上架的服务内容、价格与交付时间"
            action-label="浏览全部橱窗"
            @action="go({ name: 'Stalls' })"
          />
          <div v-if="sections.stalls.loading" class="stall-grid" aria-label="橱窗加载中">
            <ContentSkeleton v-for="index in 4" :key="index" variant="image" />
          </div>
          <SectionError v-else-if="sections.stalls.error" description="橱窗暂时无法加载" @retry="loadStalls" />
          <div v-else-if="stalls.length" class="stall-grid">
            <article
              v-for="stall in stalls"
              :key="stall.id"
              class="stall-card"
              tabindex="0"
              @click="goStall(stall.id)"
              @keyup.enter="goStall(stall.id)"
            >
              <div class="stall-card__cover">
                <img v-if="stall.cover" :src="stall.cover" :alt="stall.title" loading="lazy" />
                <div v-else class="image-placeholder"><el-icon><Picture /></el-icon></div>
                <span class="content-badge">{{ stall.category || '未分类' }}</span>
              </div>
              <div class="stall-card__body">
                <h3>{{ stall.title }}</h3>
                <div class="stall-card__artist">
                  <span>{{ stall.painterName || `画师 #${stall.painterId}` }}</span>
                  <strong>¥{{ formatMoney(stall.price) }}</strong>
                </div>
                <div class="stall-card__meta">
                  <span>{{ formatDeliverTime(stall.deliverTime) }}</span>
                  <span>剩余 {{ stall.stock }} 份</span>
                </div>
              </div>
            </article>
          </div>
          <ContentEmpty v-else title="暂无正在出售的橱窗" action-label="浏览服务板" @action="go({ name: 'Stalls' })" />
        </div>
      </section>

      <section class="content-section works-section">
        <div class="page-container">
          <SectionHeading
            eyebrow="精选作品"
            title="从作品认识不同的创作风格"
            description="查看画师公开展示的近期作品"
            action-label="进入作品广场"
            @action="go({ name: 'Works' })"
          />
          <div v-if="sections.works.loading" class="work-grid" aria-label="作品加载中">
            <ContentSkeleton v-for="index in 8" :key="index" variant="image" />
          </div>
          <SectionError v-else-if="sections.works.error" description="作品暂时无法加载" @retry="loadWorks" />
          <div v-else-if="works.length" class="work-grid">
            <button
              v-for="work in works"
              :key="work.id"
              class="work-card"
              :aria-label="`查看 ${work.title} 的作者主页`"
              @click="goPainter(work.userId)"
            >
              <img v-if="work.image" :src="work.image" :alt="work.title" loading="lazy" />
              <div v-else class="image-placeholder"><el-icon><Picture /></el-icon></div>
              <span class="work-card__overlay">
                <strong>{{ work.title }}</strong>
                <small>{{ work.category || '未分类' }}</small>
              </span>
            </button>
          </div>
          <ContentEmpty v-else title="暂无公开作品" action-label="进入作品广场" @action="go({ name: 'Works' })" />
        </div>
      </section>

      <section class="content-section artists-section">
        <div class="page-container">
          <SectionHeading
            eyebrow="认证画师"
            title="找到与你想法契合的创作者"
            description="通过简介、题材风格和公开作品了解画师"
            action-label="认识更多画师"
            @action="go({ name: 'Artists' })"
          />
          <div v-if="sections.artists.loading" class="artist-grid" aria-label="画师加载中">
            <ContentSkeleton v-for="index in 3" :key="index" variant="artist" />
          </div>
          <SectionError v-else-if="sections.artists.error" description="画师暂时无法加载" @retry="loadArtists" />
          <div v-else-if="artists.length" class="artist-grid">
            <article
              v-for="artist in artists"
              :key="artist.id"
              class="artist-card"
              tabindex="0"
              @click="goPainter(artist.id)"
              @keyup.enter="goPainter(artist.id)"
            >
              <header class="artist-card__header">
                <el-avatar :size="54" :src="artist.avatar || undefined" class="artist-avatar">
                  {{ (artist.nickname || artist.username || '绘').slice(0, 1) }}
                </el-avatar>
                <div>
                  <div class="artist-card__name">
                    <h3>{{ artist.nickname || artist.username }}</h3>
                    <span>已认证</span>
                  </div>
                  <p>@{{ artist.username }}</p>
                </div>
              </header>
              <p class="artist-card__bio">{{ artist.bio || '这位画师暂未填写个人简介。' }}</p>
              <div class="artist-card__tags">
                <span v-for="tag in splitTags(artist.tags).slice(0, 3)" :key="tag">{{ tag }}</span>
                <span v-if="splitTags(artist.tags).length === 0" class="muted-tag">暂未设置风格</span>
              </div>
              <div class="artist-card__works">
                <div v-for="work in (artist.works || []).slice(0, 3)" :key="work.id">
                  <img :src="work.image" :alt="work.title" loading="lazy" />
                </div>
                <div v-for="index in Math.max(0, 3 - (artist.works || []).slice(0, 3).length)" :key="`empty-${index}`" class="artist-work-empty">
                  <el-icon><Picture /></el-icon>
                </div>
              </div>
              <span class="artist-card__link">进入画师主页 <el-icon><ArrowRight /></el-icon></span>
            </article>
          </div>
          <ContentEmpty v-else title="暂无认证画师" action-label="浏览画师列表" @action="go({ name: 'Artists' })" />
        </div>
      </section>

      <section class="closing-section">
        <div class="page-container closing-layout">
          <div>
            <p>{{ closingEyebrow }}</p>
            <h2>{{ closingTitle }}</h2>
          </div>
          <button class="action-button action-button--light" @click="go(closingAction.to)">
            {{ closingAction.label }}
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Calendar, Picture, Refresh } from '@element-plus/icons-vue'
import { ElIcon, ElSkeleton, ElSkeletonItem } from 'element-plus'
import { getApplications, getProjects } from '@/api/project'
import { getStalls } from '@/api/stall'
import { getWorks } from '@/api/work'
import { getArtists } from '@/api/user'
import { useUserStore } from '@/stores/user'
import heroShowcase01 from '@/assets/home/hero-showcase-01.png'
import heroShowcase02 from '@/assets/home/hero-showcase-02.png'
import heroShowcase03 from '@/assets/home/hero-showcase-03.png'
import heroShowcase04 from '@/assets/home/hero-showcase-04.jpg'
import heroShowcase05 from '@/assets/home/hero-showcase-05.jpg'

const router = useRouter()
const userStore = useUserStore()
const projects = ref([])
const stalls = ref([])
const works = ref([])
const artists = ref([])
const sections = reactive({
  projects: { loading: true, error: false },
  stalls: { loading: true, error: false },
  works: { loading: true, error: false },
  artists: { loading: true, error: false },
})

const heroShowcaseImages = [
  { src: heroShowcase01, alt: '蓝色水面中的人物插画' },
  { src: heroShowcase02, alt: '乐队角色组合插画' },
  { src: heroShowcase03, alt: '蓝绿色废墟场景插画' },
  { src: heroShowcase04, alt: '花丛中的人物插画' },
  { src: heroShowcase05, alt: '双人角色插画' },
]

const contentLinks = [
  { label: '企划', note: '浏览公开需求', to: { name: 'Projects' } },
  { label: '橱窗', note: '查看画师服务', to: { name: 'Stalls' } },
  { label: '作品', note: '发现创作风格', to: { name: 'Works' } },
  { label: '画师', note: '认识认证创作者', to: { name: 'Artists' } },
]
const activeRole = computed(() => userStore.workbenchRole)

const primaryAction = computed(() => {
  if (!userStore.isLoggedIn) return { label: '浏览正在招募的企划', to: { name: 'Projects' } }
  if (activeRole.value === 3) return { label: '进入管理后台', to: { name: 'AdminHome' } }
  if (activeRole.value === 2) return { label: '查看可应征企划', to: { name: 'Projects' } }
  return { label: '发布一个企划', to: { name: 'ProjectNew' } }
})

const secondaryAction = computed(() => {
  if (!userStore.isLoggedIn) return { label: '浏览画师橱窗', to: { name: 'Stalls' } }
  if (activeRole.value === 3) return { label: '浏览平台内容', to: { name: 'Works' } }
  if (activeRole.value === 2) return { label: '管理我的橱窗', to: { name: 'PainterStalls' } }
  return { label: '浏览画师橱窗', to: { name: 'Stalls' } }
})

const closingAction = computed(() => {
  if (!userStore.isLoggedIn) return { label: '注册并开始使用', to: { name: 'Register' } }
  if (activeRole.value === 3) return { label: '进入管理后台', to: { name: 'AdminHome' } }
  if (activeRole.value === 2) return { label: '进入画师工作台', to: { name: 'PainterHome' } }
  return { label: '进入企划工作台', to: { name: 'DashboardHome' } }
})

const closingEyebrow = computed(() => userStore.isLoggedIn ? '欢迎回来' : '从这里开始')
const closingTitle = computed(() => {
  if (!userStore.isLoggedIn) return '去发现作品，也让新的创作在这里发生'
  return `继续探索艺绘，${userStore.user?.nickname || userStore.user?.username || '你好'}`
})

const SectionHeading = defineComponent({
  props: { eyebrow: String, title: String, description: String, actionLabel: String },
  emits: ['action'],
  setup(props, { emit }) {
    return () => h('header', { class: 'section-heading' }, [
      h('div', { class: 'section-heading__copy' }, [h('p', props.eyebrow), h('h2', props.title), h('span', props.description)]),
      h('button', { class: 'section-more', onClick: () => emit('action') }, [
        props.actionLabel,
        h(ElIcon, null, { default: () => h(ArrowRight) }),
      ]),
    ])
  },
})

const SectionError = defineComponent({
  props: { description: String },
  emits: ['retry'],
  setup(props, { emit }) {
    return () => h('div', { class: 'section-state section-state--error' }, [
      h('p', props.description),
      h('button', { onClick: () => emit('retry') }, [h(ElIcon, null, { default: () => h(Refresh) }), '重新加载']),
    ])
  },
})

const ContentEmpty = defineComponent({
  props: { title: String, actionLabel: String },
  emits: ['action'],
  setup(props, { emit }) {
    return () => h('div', { class: 'section-state' }, [
      h(ElIcon, { class: 'section-state__icon' }, { default: () => h(Picture) }),
      h('p', props.title),
      h('button', { onClick: () => emit('action') }, props.actionLabel),
    ])
  },
})

const ContentSkeleton = defineComponent({
  props: { variant: String },
  setup(props) {
    return () => h(ElSkeleton, { animated: true, class: ['content-skeleton', `content-skeleton--${props.variant}`] }, {
      template: () => [
        h(ElSkeletonItem, { variant: 'image', class: 'content-skeleton__image' }),
        h(ElSkeletonItem, { variant: 'h3', style: 'width: 68%; margin-top: 14px' }),
        h(ElSkeletonItem, { variant: 'text', style: 'width: 88%; margin-top: 10px' }),
        h(ElSkeletonItem, { variant: 'text', style: 'width: 48%; margin-top: 8px' }),
      ],
    })
  },
})

async function loadProjects() {
  sections.projects.loading = true
  sections.projects.error = false
  try {
    const data = await getProjects({ page: 1, size: 4, sort: 'newest' })
    const records = data.records || []
    projects.value = await Promise.all(records.map(async (project) => {
      try {
        const applications = await getApplications(project.id, { page: 1, size: 1 })
        return { ...project, applicationCount: applications.total ?? 0 }
      } catch {
        return { ...project, applicationCount: null }
      }
    }))
  } catch {
    projects.value = []
    sections.projects.error = true
  } finally {
    sections.projects.loading = false
  }
}

async function loadStalls() {
  sections.stalls.loading = true
  sections.stalls.error = false
  try {
    const data = await getStalls({ page: 1, size: 4, sort: 'newest' })
    stalls.value = data.records || []
  } catch {
    stalls.value = []
    sections.stalls.error = true
  } finally {
    sections.stalls.loading = false
  }
}

async function loadWorks() {
  sections.works.loading = true
  sections.works.error = false
  try {
    const data = await getWorks({ page: 1, size: 8, sort: 'newest' })
    works.value = data.records || []
  } catch {
    works.value = []
    sections.works.error = true
  } finally {
    sections.works.loading = false
  }
}

async function loadArtists() {
  sections.artists.loading = true
  sections.artists.error = false
  try {
    const data = await getArtists({ page: 1, size: 3 })
    artists.value = data.records || []
  } catch {
    artists.value = []
    sections.artists.error = true
  } finally {
    sections.artists.loading = false
  }
}

function go(target) { router.push(target) }
function goProject(id) { go({ name: 'ProjectDetail', params: { id } }) }
function goStall(id) { go({ name: 'StallDetail', params: { id } }) }
function goPainter(id) {
  if (id) go({ name: 'ArtistProfile', params: { id }, query: { role: 'painter' } })
}
function firstImage(value) { return String(value || '').split(',').map((item) => item.trim()).find(Boolean) || '' }
function splitTags(value) { return String(value || '').split(',').map((item) => item.trim()).filter(Boolean) }
function formatMoney(value) {
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toLocaleString('zh-CN', { maximumFractionDigits: 2 }) : '-'
}
function formatDeadline(value) { return value ? `${value} 截稿` : '截稿时间待定' }
function formatDeliverTime(value) {
  if (!value) return '交付时间待商议'
  const text = String(value)
  return /^\d+$/.test(text) ? `${text} 小时交付` : text
}
function applicationText(value) { return value === null ? '应征人数暂不可用' : `${value} 人应征` }

onMounted(() => {
  loadProjects()
  loadStalls()
  loadWorks()
  loadArtists()
})
</script>

<style scoped>
/* 首页内容门户重构 */
.home-page { overflow: hidden; color: #27313b; background: #f7f8fa; }
button { font: inherit; }
.hero-section { position: relative; min-height: 620px; overflow: hidden; background: linear-gradient(135deg, rgba(255,255,255,.98), rgba(242,250,253,.95)), #f5fbfd; }
.hero-section::before { position: absolute; inset: 0; background-image: linear-gradient(rgba(25,71,91,.035) 1px, transparent 1px), linear-gradient(90deg, rgba(25,71,91,.035) 1px, transparent 1px); background-size: 44px 44px; content: ''; mask-image: linear-gradient(to bottom, #000, transparent 84%); }
.hero-orb { position: absolute; border-radius: 50%; filter: blur(4px); pointer-events: none; }
.hero-orb--one { top: -160px; right: -80px; width: 440px; height: 440px; background: rgba(0,168,233,.09); }
.hero-orb--two { bottom: -180px; left: 34%; width: 360px; height: 360px; background: rgba(246,161,29,.08); }
.hero-layout { position: relative; z-index: 1; display: grid; grid-template-columns: minmax(0,.92fr) minmax(520px,1.08fr); gap: 72px; align-items: center; min-height: 620px; padding-top: 60px; padding-bottom: 60px; }
.hero-copy { min-width: 0; }
.hero-eyebrow, .closing-layout > div > p { margin: 0 0 14px; color: var(--artiv-primary-dark); font-size: 12px; font-weight: 700; letter-spacing: .2em; }
.home-page :deep(.section-heading__copy > p) { margin: 0 0 14px; color: var(--artiv-primary-dark); font-size: 12px; font-weight: 700; letter-spacing: .2em; }
.hero-copy h1 { margin: 0; color: #17232d; font-size: clamp(38px,4.4vw,58px); font-weight: 700; letter-spacing: -.045em; line-height: 1.18; }
.hero-copy h1 span { color: var(--artiv-primary-dark); }
.hero-description { max-width: 540px; margin: 24px 0 0; color: #65717c; font-size: 17px; line-height: 1.85; }
.hero-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 34px; }
.action-button { display: inline-flex; min-height: 48px; align-items: center; justify-content: center; gap: 10px; padding: 0 24px; border: 1px solid transparent; border-radius: 999px; cursor: pointer; font-size: 14px; font-weight: 600; transition: transform .2s ease, box-shadow .2s ease, border-color .2s ease, background .2s ease; }
.action-button:hover { transform: translateY(-2px); }
.action-button--primary { color: #fff; background: var(--artiv-primary); box-shadow: 0 10px 24px rgba(0,168,233,.22); }
.action-button--primary:hover { background: var(--artiv-primary-dark); box-shadow: 0 13px 30px rgba(0,168,233,.3); }
.action-button--secondary { color: var(--artiv-primary); border-color: var(--artiv-primary); background: rgba(255,255,255,.9); }
.action-button--secondary:hover { color: #fff; background: var(--artiv-primary); }
.hero-nav { display: grid; grid-template-columns: repeat(4,1fr); margin-top: 48px; border-top: 1px solid rgba(85,111,123,.16); }
.hero-nav button { display: flex; flex-direction: column; align-items: flex-start; padding: 18px 12px 0 0; border: 0; color: #34434d; background: transparent; cursor: pointer; text-align: left; }
.hero-nav button:hover span { color: var(--artiv-primary-dark); }
.hero-nav span { font-weight: 600; transition: color .2s ease; }
.hero-nav small { margin-top: 5px; color: #8a959d; font-size: 11px; }
.hero-gallery { position: relative; min-height: 475px; }
.hero-work { position: absolute; overflow: hidden; margin: 0; padding: 0; border: 5px solid rgba(255,255,255,.96); border-radius: 18px; background: #e6edf1; box-shadow: 0 18px 45px rgba(37,64,77,.16); pointer-events: none; }
.hero-work img { width: 100%; height: 100%; object-fit: cover; }
.hero-work--1 { top: 8px; left: 5%; z-index: 3; width: 47%; height: 288px; transform: rotate(-2.5deg); }
.hero-work--1 img { object-position: center 18%; }
.hero-work--2 { top: 34px; right: 0; z-index: 2; width: 42%; height: 218px; transform: rotate(2.5deg); }
.hero-work--3 { bottom: 0; left: 10%; z-index: 4; width: 34%; height: 188px; transform: rotate(1.7deg); }
.hero-work--4 { right: 4%; bottom: 18px; z-index: 3; width: 43%; height: 210px; transform: rotate(-2deg); }
.hero-work--5 { top: 205px; left: 43%; z-index: 5; width: 28%; height: 158px; transform: rotate(3.5deg); }
.hero-work--5 img { object-position: center 24%; }
.content-section { padding: 88px 0; }
.projects-section, .works-section { background: #fff; }
.stalls-section, .artists-section { background: #f5f7f9; }
.home-page :deep(.section-heading) { display: flex; align-items: flex-end; justify-content: space-between; gap: 32px; margin-bottom: 32px; }
.home-page :deep(.section-heading__copy h2) { margin: 0; color: #202c35; font-size: 31px; font-weight: 650; letter-spacing: -.025em; }
.home-page :deep(.section-heading__copy > span) { display: block; margin-top: 10px; color: #7a858e; font-size: 14px; }
.home-page :deep(.section-more) { display: inline-flex; min-height: 36px; flex: 0 0 auto; align-items: center; gap: 7px; padding: 0 16px; border: 1px solid var(--artiv-primary); border-radius: 999px; color: #fff; background: var(--artiv-primary); box-shadow: 0 7px 18px rgba(0,168,233,.16); cursor: pointer; font-weight: 600; transition: transform .2s ease, background .2s ease, box-shadow .2s ease; }
.home-page :deep(.section-more:hover) { color: #fff; background: var(--artiv-primary-dark); box-shadow: 0 10px 22px rgba(0,168,233,.24); transform: translateY(-1px); }
.project-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 18px; }
.project-card { display: grid; grid-template-columns: 168px minmax(0,1fr); min-height: 190px; overflow: hidden; border: 1px solid #e3e8eb; border-radius: 13px; background: #fff; cursor: pointer; transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease; }
.project-card:hover, .project-card:focus-visible { border-color: #badbe8; outline: none; transform: translateY(-3px); box-shadow: 0 14px 34px rgba(49,71,82,.1); }
.project-card__image, .stall-card__cover { position: relative; overflow: hidden; background: #e8edf0; }
.project-card__image img, .stall-card__cover img { width: 100%; height: 100%; object-fit: cover; transition: transform .28s ease; }
.project-card:hover img, .stall-card:hover img { transform: scale(1.035); }
.image-placeholder { display: grid; width: 100%; height: 100%; place-items: center; color: #9eaab1; background: linear-gradient(145deg,#eef3f5,#e0eaee); font-size: 26px; }
.content-badge { position: absolute; top: 12px; left: 12px; padding: 5px 9px; border-radius: 6px; color: #fff; background: rgba(21,35,43,.74); backdrop-filter: blur(8px); font-size: 11px; }
.project-card__body { display: flex; min-width: 0; flex-direction: column; padding: 19px 20px 17px; }
.project-card__topline { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #83a0ae; font-size: 11px; }
.project-card__topline strong { color: #ef8d11; font-size: 16px; }
.project-card h3, .stall-card h3 { overflow: hidden; margin: 11px 0 7px; color: #27323b; font-size: 17px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.project-card__body > p { display: -webkit-box; margin: 0; overflow: hidden; color: #77828b; font-size: 13px; line-height: 1.65; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.project-meta { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-top: auto; padding-top: 14px; color: #697680; font-size: 12px; }
.project-meta span { display: inline-flex; align-items: center; gap: 5px; }
.applicant-pawn { font-family: "Segoe UI Symbol",serif; font-size: 13px; line-height: 1; }
.stall-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 20px; }
.stall-card { overflow: hidden; border: 1px solid #e2e7ea; border-radius: 13px; background: #fff; cursor: pointer; transition: transform .22s ease, box-shadow .22s ease; }
.stall-card:hover, .stall-card:focus-visible { outline: none; transform: translateY(-4px); box-shadow: 0 14px 34px rgba(49,71,82,.11); }
.stall-card__cover { aspect-ratio: 1.16/1; }
.stall-card__body { padding: 16px 17px 17px; }
.stall-card h3 { margin: 0 0 11px; }
.stall-card__artist, .stall-card__meta { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.stall-card__artist { color: #69757e; font-size: 12px; }
.stall-card__artist span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stall-card__artist strong { flex: 0 0 auto; color: #ef8d11; font-size: 16px; }
.stall-card__meta { margin-top: 13px; padding-top: 12px; border-top: 1px solid #eef1f3; color: #929ba2; font-size: 11px; }
.work-grid { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 17px; }
.work-card { position: relative; aspect-ratio: 1/1.08; overflow: hidden; padding: 0; border: 0; border-radius: 11px; background: #e7ecef; cursor: pointer; }
.work-card img { width: 100%; height: 100%; object-fit: cover; transition: transform .32s ease; }
.work-card:hover img, .work-card:focus-visible img { transform: scale(1.045); }
.work-card:focus-visible { outline: 3px solid rgba(0,168,233,.35); outline-offset: 3px; }
.work-card__overlay { position: absolute; right: 0; bottom: 0; left: 0; display: flex; flex-direction: column; align-items: flex-start; padding: 58px 16px 15px; color: #fff; background: linear-gradient(transparent,rgba(13,22,27,.8)); text-align: left; }
.work-card__overlay strong { max-width: 100%; overflow: hidden; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
.work-card__overlay small { margin-top: 5px; color: rgba(255,255,255,.75); }
.artist-grid { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 20px; }
.artist-card { padding: 22px; border: 1px solid #e1e7ea; border-radius: 13px; background: #fff; cursor: pointer; transition: transform .22s ease, box-shadow .22s ease; }
.artist-card:hover, .artist-card:focus-visible { outline: none; transform: translateY(-4px); box-shadow: 0 14px 34px rgba(49,71,82,.1); }
.artist-card__header { display: flex; align-items: center; gap: 13px; }
.artist-avatar { flex: 0 0 auto; color: #fff; background: linear-gradient(145deg,#78cfed,#159bc9); }
.artist-card__header > div { min-width: 0; }
.artist-card__name { display: flex; align-items: center; gap: 8px; }
.artist-card__name h3 { overflow: hidden; margin: 0; color: #29343d; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.artist-card__name span { flex: 0 0 auto; padding: 2px 6px; border-radius: 5px; color: #23845d; background: #eaf8f2; font-size: 10px; }
.artist-card__header p { margin: 5px 0 0; color: #9aa3aa; font-size: 11px; }
.artist-card__bio { display: -webkit-box; min-height: 44px; margin: 17px 0 13px; overflow: hidden; color: #6f7a83; font-size: 13px; line-height: 1.7; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.artist-card__tags { display: flex; min-height: 25px; flex-wrap: wrap; gap: 6px; margin-bottom: 17px; }
.artist-card__tags span { padding: 4px 8px; border: 1px solid #dfe8ec; border-radius: 12px; color: #60717b; background: #f8fafb; font-size: 10px; }
.artist-card__tags .muted-tag { color: #9ba4aa; }
.artist-card__works { display: grid; grid-template-columns: repeat(3,1fr); gap: 7px; }
.artist-card__works > div { aspect-ratio: 1; overflow: hidden; border-radius: 7px; background: #edf1f3; }
.artist-card__works img { width: 100%; height: 100%; object-fit: cover; }
.artist-work-empty { display: grid; place-items: center; color: #b0b8bd; }
.artist-card__link { display: inline-flex; align-items: center; gap: 6px; margin-top: 18px; color: var(--artiv-primary-dark); font-size: 12px; font-weight: 600; }
.home-page :deep(.section-state) { display: flex; min-height: 210px; flex-direction: column; align-items: center; justify-content: center; gap: 12px; border: 1px dashed #d7e0e4; border-radius: 13px; color: #849098; background: rgba(255,255,255,.62); }
.home-page :deep(.section-state p) { margin: 0; }
.home-page :deep(.section-state button) { display: inline-flex; align-items: center; gap: 6px; padding: 8px 16px; border: 1px solid var(--artiv-primary); border-radius: 999px; color: #fff; background: var(--artiv-primary); cursor: pointer; }
.home-page :deep(.section-state button:hover) { border-color: var(--artiv-primary-dark); color: #fff; background: var(--artiv-primary-dark); }
.home-page :deep(.section-state__icon) { font-size: 30px; }
.home-page :deep(.content-skeleton) { box-sizing: border-box; min-height: 220px; padding: 0 0 18px; overflow: hidden; border: 1px solid #e4e9ec; border-radius: 13px; background: #fff; }
.home-page :deep(.content-skeleton .el-skeleton__item:not(.content-skeleton__image)) { margin-right: 16px; margin-left: 16px; }
.home-page :deep(.content-skeleton__image) { width: 100%; height: 155px; border-radius: 0; }
.home-page :deep(.content-skeleton--project) { min-height: 190px; }
.home-page :deep(.content-skeleton--artist) { padding: 18px; }
.home-page :deep(.content-skeleton--artist .content-skeleton__image) { height: 88px; border-radius: 8px; }
.closing-section { padding: 62px 0; color: #fff; background: #172d38; }
.closing-layout { display: flex; align-items: center; justify-content: space-between; gap: 32px; }
.closing-layout > div > p { color: #67c5e7; }
.closing-layout h2 { margin: 0; font-size: 27px; font-weight: 600; letter-spacing: -.02em; }
.action-button--light { flex: 0 0 auto; color: #19303b; background: #fff; }
@media (max-width: 1060px) {
  .hero-layout { grid-template-columns: minmax(0,.9fr) minmax(430px,1.1fr); gap: 36px; }
  .stall-grid, .work-grid { grid-template-columns: repeat(2,minmax(0,1fr)); }
  .artist-grid { gap: 14px; }
}
@media (max-width: 820px) {
  .hero-layout { grid-template-columns: 1fr; padding-top: 66px; padding-bottom: 72px; }
  .hero-copy { text-align: center; }
  .hero-description { margin-right: auto; margin-left: auto; }
  .hero-actions { justify-content: center; }
  .hero-nav { max-width: 560px; margin-right: auto; margin-left: auto; }
  .hero-gallery { width: min(620px,100%); margin: 0 auto; }
  .project-grid { grid-template-columns: 1fr; }
  .artist-grid { grid-template-columns: 1fr; }
  .artist-card__works { max-width: 360px; }
}
@media (max-width: 560px) {
  .hero-section, .hero-layout { min-height: auto; }
  .hero-layout { gap: 46px; padding-top: 48px; padding-bottom: 54px; }
  .hero-copy h1 { font-size: 36px; line-height: 1.25; }
  .hero-description { font-size: 15px; }
  .hero-actions { flex-direction: column; }
  .action-button { width: 100%; }
  .hero-nav { grid-template-columns: repeat(2,1fr); row-gap: 8px; }
  .hero-gallery { min-height: 365px; }
  .hero-work--1 { width: 55%; height: 220px; }
  .hero-work--2 { width: 42%; height: 165px; }
  .hero-work--3 { left: 3%; width: 37%; height: 142px; }
  .hero-work--4 { right: 1%; width: 48%; height: 168px; }
  .hero-work--5 { top: 160px; left: 45%; width: 31%; height: 125px; }
  .content-section { padding: 62px 0; }
  .home-page :deep(.section-heading) { align-items: flex-start; margin-bottom: 24px; }
  .home-page :deep(.section-heading__copy h2) { font-size: 25px; line-height: 1.35; }
  .home-page :deep(.section-heading__copy > span) { line-height: 1.65; }
  .home-page :deep(.section-more) { display: none; }
  .project-card { grid-template-columns: 112px minmax(0,1fr); min-height: 168px; }
  .project-card__body { padding: 14px 14px 13px; }
  .project-card__body > p { -webkit-line-clamp: 1; }
  .project-meta { align-items: flex-start; flex-direction: column; gap: 5px; padding-top: 9px; }
  .stall-grid, .work-grid { gap: 12px; }
  .stall-card__body { padding: 13px; }
  .stall-card__artist, .stall-card__meta { align-items: flex-start; flex-direction: column; gap: 7px; }
  .artist-card { padding: 18px; }
  .closing-layout { align-items: flex-start; flex-direction: column; }
  .closing-layout h2 { font-size: 23px; line-height: 1.45; }
}
@media (prefers-reduced-motion: reduce) {
  .action-button, .section-more, .project-card, .stall-card, .work-card img, .artist-card { transition: none; }
}
</style>
