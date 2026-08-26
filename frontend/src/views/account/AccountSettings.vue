<template>
  <div class="settings-page">
    <div class="page-container settings-shell">
      <aside class="settings-nav" aria-label="账户设置菜单">
        <div class="settings-nav__title">
          <span>账户设置</span>
          <small>账号与身份资料统一维护</small>
        </div>
        <button :class="{ active: activeSection === 'account' }" type="button" @click="setSection('account')">
          <strong>账号安全与资料设置</strong>
          <span>基础资料、头像与密码</span>
        </button>
        <button :class="{ active: activeSection === 'identity' }" type="button" @click="setSection('identity')">
          <strong>身份认证</strong>
          <span>画师信息与认证资料</span>
        </button>
        <button :class="{ active: activeSection === 'blocks' }" type="button" @click="setSection('blocks')">
          <strong>黑名单</strong>
          <span>查看与解除已屏蔽用户</span>
        </button>
      </aside>

      <main v-loading="pageLoading" class="settings-main">
        <template v-if="activeSection === 'account'">
          <header class="settings-heading">
            <div>
              <span class="heading-kicker">ACCOUNT</span>
              <h1>账号安全与资料设置</h1>
              <p>管理两种身份共用的账户资料，以及企划方公开主页简介。</p>
            </div>
          </header>

          <section class="settings-card">
            <h2>个人头像</h2>
            <AvatarEditor />
          </section>

          <section class="settings-card">
            <div class="card-heading">
              <div><h2>基础资料</h2><p>昵称全站唯一；企划方简介只显示在企划方主页。</p></div>
            </div>
            <el-form label-position="top" class="settings-form" @submit.prevent="saveAccountProfile">
              <div class="form-grid">
                <el-form-item label="用户名">
                  <el-input :model-value="userStore.user?.username" disabled />
                </el-form-item>
                <el-form-item label="昵称" required>
                  <el-input v-model="accountForm.nickname" maxlength="50" show-word-limit placeholder="请输入全站唯一昵称" />
                </el-form-item>
              </div>
              <el-form-item label="企划方个人简介">
                <el-input v-model="accountForm.employerBio" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="介绍你的企划方向、合作偏好等" />
              </el-form-item>
              <div class="form-grid placeholder-fields">
                <el-form-item label="手机号码">
                  <el-input model-value="后期开放" disabled />
                </el-form-item>
                <el-form-item label="邮箱地址">
                  <el-input model-value="后期开放" disabled />
                </el-form-item>
              </div>
              <div class="form-actions">
                <el-button type="primary" :loading="accountSaving" @click="saveAccountProfile">保存基础资料</el-button>
              </div>
            </el-form>
          </section>

          <section class="settings-card">
            <div class="card-heading">
              <div><h2>修改密码</h2><p>修改前需要验证当前密码，密码长度为 6–32 位。</p></div>
            </div>
            <el-form label-position="top" class="settings-form password-form" @submit.prevent="savePassword">
              <el-form-item label="当前密码" required>
                <el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" />
              </el-form-item>
              <div class="form-grid">
                <el-form-item label="新密码" required>
                  <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
                </el-form-item>
                <el-form-item label="确认新密码" required>
                  <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
                </el-form-item>
              </div>
              <div class="form-actions">
                <el-button type="primary" plain :loading="passwordSaving" @click="savePassword">更新密码</el-button>
              </div>
            </el-form>
          </section>
        </template>

        <template v-else-if="activeSection === 'identity'">
          <header class="settings-heading">
            <div>
              <span class="heading-kicker">IDENTITY</span>
              <h1>身份认证 · 画师信息</h1>
              <p>画师简介和题材风格可直接编辑；笔名和常用站变更需要管理员审核。</p>
            </div>
          </header>

          <section class="settings-card certification-summary">
            <div class="certification-summary__top">
              <div>
                <span class="summary-label">画师认证</span>
                <div class="summary-status">
                  <h2>{{ certStatusText }}</h2>
                  <el-tag :type="certStatusType" effect="light">{{ certStatusText }}</el-tag>
                </div>
              </div>
              <el-button v-if="certStatus === 0" type="primary" @click="openInitialApplication">前往申请</el-button>
              <el-button v-else-if="certStatus === 2" type="primary" plain :disabled="Boolean(certification.pendingChangeApplication)" @click="openChangeApplication">修改认证信息</el-button>
            </div>

            <dl class="certification-fields">
              <div>
                <dt>已生效笔名</dt>
                <dd>{{ certification.artistName || '—' }}</dd>
              </div>
              <div>
                <dt>已生效常用站</dt>
                <dd>
                  <a v-if="certification.artistSite" :href="certification.artistSite" target="_blank" rel="noopener noreferrer">{{ certification.artistSite }}</a>
                  <span v-else>—</span>
                </dd>
              </div>
            </dl>

            <el-alert v-if="certStatus === 1" title="初次认证申请正在审核，审核期间不能重复提交。" type="warning" :closable="false" show-icon>
              <template #default>
                <p class="pending-copy">提交笔名：{{ certification.pendingInitialApplication?.commonId || '—' }}</p>
                <p class="pending-copy">提交常用站：{{ certification.pendingInitialApplication?.commonSite || '—' }}</p>
              </template>
            </el-alert>
            <el-alert v-if="certification.pendingChangeApplication" title="认证资料修改审核中，原认证资料继续生效。" type="warning" :closable="false" show-icon>
              <template #default>
                <p class="pending-copy">新笔名：{{ certification.pendingChangeApplication.commonId }}</p>
                <p class="pending-copy">新常用站：{{ certification.pendingChangeApplication.commonSite }}</p>
              </template>
            </el-alert>
          </section>

          <section ref="painterProfileCard" class="settings-card">
            <div class="card-heading">
              <div><h2>画师公开资料</h2><p>保存后立即更新画师主页，不触发认证复审。</p></div>
            </div>
            <el-form label-position="top" class="settings-form" @submit.prevent="savePainterProfile">
              <el-form-item label="画师个人简介">
                <el-input v-model="painterForm.bio" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="介绍擅长方向、接稿状态等" />
              </el-form-item>
              <el-form-item label="题材风格">
                <el-select v-model="painterForm.tags" multiple filterable class="full-width" placeholder="请选择题材风格（可多选）">
                  <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
              <div class="form-actions">
                <el-button type="primary" :loading="painterSaving" @click="savePainterProfile">保存画师资料</el-button>
              </div>
            </el-form>
          </section>

          <section class="settings-card eligibility-card">
            <div>
              <h2>认证申请条件</h2>
              <p>后端会在提交和管理员通过时再次核验。</p>
            </div>
            <div class="eligibility-items">
              <span :class="{ complete: certification.painterBioCompleted }">
                <el-icon><Check v-if="certification.painterBioCompleted" /><Close v-else /></el-icon>
                画师个人简介
              </span>
              <span :class="{ complete: Number(certification.publicWorkCount || 0) >= 4 }">
                <el-icon><Check v-if="Number(certification.publicWorkCount || 0) >= 4" /><Close v-else /></el-icon>
                公开作品 {{ certification.publicWorkCount || 0 }} / 4
              </span>
            </div>
          </section>
        </template>

        <template v-else>
          <header class="settings-heading">
            <div>
              <span class="heading-kicker">PRIVACY</span>
              <h1>黑名单管理</h1>
              <p>管理账号级屏蔽关系；公开内容仍然可见，平台互动将被双向限制。</p>
            </div>
          </header>
          <BlockList />
        </template>
      </main>
    </div>

    <el-dialog v-model="initialVisible" title="申请画师认证" width="620px" append-to-body destroy-on-close>
      <el-steps :active="initialStep" align-center class="application-steps">
        <el-step title="资格检查" />
        <el-step title="填写认证资料" />
      </el-steps>
      <template v-if="initialStep === 0">
        <div class="qualification-list">
          <div :class="{ passed: certification.painterBioCompleted }">
            <el-icon><Check v-if="certification.painterBioCompleted" /><Close v-else /></el-icon>
            <span><strong>画师个人简介</strong><small>{{ certification.painterBioCompleted ? '已填写' : '尚未填写' }}</small></span>
          </div>
          <div :class="{ passed: Number(certification.publicWorkCount || 0) >= 4 }">
            <el-icon><Check v-if="Number(certification.publicWorkCount || 0) >= 4" /><Close v-else /></el-icon>
            <span><strong>公开作品</strong><small>{{ certification.publicWorkCount || 0 }} / 4 件</small></span>
          </div>
        </div>
        <el-alert v-if="!certification.eligibleForInitialApplication" title="请先完成全部申请条件" type="info" :closable="false" />
        <el-button v-if="!certification.painterBioCompleted" link type="primary" class="complete-profile-link" @click="focusPainterProfile">前往完善画师个人简介</el-button>
      </template>
      <el-form v-else label-position="top" class="application-form">
        <el-form-item label="笔名" required>
          <el-input v-model="initialForm.commonId" maxlength="100" show-word-limit placeholder="填写对外使用的画师笔名" />
        </el-form-item>
        <el-form-item label="常用站" required>
          <el-input v-model="initialForm.commonSite" maxlength="500" placeholder="例如 https://www.pixiv.net/users/12345" />
          <p class="field-help">请填写支持平台中的个人主页、作者主页或作品集地址，平台首页无法提交。</p>
        </el-form-item>
        <p class="supported-sites">支持：Pixiv、Lofter、微博、小红书、哔哩哔哩、DeviantArt、站酷、涂鸦王国</p>
      </el-form>
      <template #footer>
        <el-button v-if="initialStep === 0" @click="initialVisible = false">取消</el-button>
        <el-button v-else @click="initialStep = 0">上一步</el-button>
        <el-button v-if="initialStep === 0" type="primary" :disabled="!certification.eligibleForInitialApplication" @click="initialStep = 1">下一步</el-button>
        <el-button v-else type="primary" :loading="initialSubmitting" @click="submitInitialApplication">提交认证</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="changeVisible" title="修改认证资料" width="620px" append-to-body destroy-on-close>
      <el-alert title="新资料审核通过前，当前已生效资料不会改变。" type="info" :closable="false" show-icon />
      <el-form label-position="top" class="application-form">
        <div class="current-values">
          <div><span>当前笔名</span><strong>{{ certification.artistName || '—' }}</strong></div>
          <div><span>当前常用站</span><strong>{{ certification.artistSite || '—' }}</strong></div>
        </div>
        <el-form-item label="新笔名" required>
          <el-input v-model="changeForm.commonId" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="新常用站" required>
          <el-input v-model="changeForm.commonSite" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeVisible = false">取消</el-button>
        <el-button type="primary" :loading="changeSubmitting" @click="submitChangeApplication">提交修改审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Check, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AvatarEditor from '@/components/account/AvatarEditor.vue'
import BlockList from '@/components/account/BlockList.vue'
import { SUBJECT_OPTIONS } from '@/constants/subjects'
import {
  getCertificationStatus,
  submitCertification,
  submitCertificationChange,
  updateMyAccountProfile,
  updateMyPainterProfile,
  updateMyPassword,
} from '@/api/user'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const subjectOptions = SUBJECT_OPTIONS
const pageLoading = ref(true)
const accountSaving = ref(false)
const passwordSaving = ref(false)
const painterSaving = ref(false)
const initialVisible = ref(false)
const initialStep = ref(0)
const initialSubmitting = ref(false)
const changeVisible = ref(false)
const changeSubmitting = ref(false)
const painterProfileCard = ref(null)
const certification = ref({
  certStatus: 0,
  publicWorkCount: 0,
  painterBioCompleted: false,
  eligibleForInitialApplication: false,
})
const accountForm = reactive({ nickname: '', employerBio: '' })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const painterForm = reactive({ bio: '', tags: [] })
const initialForm = reactive({ commonId: '', commonSite: '' })
const changeForm = reactive({ commonId: '', commonSite: '' })

const activeSection = computed(() => ['identity', 'blocks'].includes(route.query.section) ? route.query.section : 'account')
const certStatus = computed(() => Number(certification.value.certStatus || 0))
const certStatusText = computed(() => ({ 0: '未认证', 1: '审核中', 2: '已认证' }[certStatus.value] || '未认证'))
const certStatusType = computed(() => ({ 0: 'info', 1: 'warning', 2: 'success' }[certStatus.value] || 'info'))

onMounted(loadPage)
watch(() => userStore.user, syncForms, { deep: true })

async function loadPage() {
  pageLoading.value = true
  try {
    await userStore.fetchMe()
    await refreshCertification()
    syncForms()
  } finally {
    pageLoading.value = false
  }
}
async function refreshCertification() {
  certification.value = await getCertificationStatus()
}
function syncForms() {
  const user = userStore.user || {}
  accountForm.nickname = user.nickname || ''
  accountForm.employerBio = user.employerBio || ''
  painterForm.bio = user.bio || ''
  painterForm.tags = (user.tags || '').split(',').map((tag) => tag.trim()).filter((tag) => subjectOptions.includes(tag))
}
function setSection(section) {
  router.replace({ name: 'AccountSettings', query: section === 'account' ? {} : { section } })
}
async function saveAccountProfile() {
  const nickname = accountForm.nickname.trim()
  if (!nickname) return ElMessage.warning('昵称不能为空')
  accountSaving.value = true
  try {
    const user = await updateMyAccountProfile({ nickname, employerBio: accountForm.employerBio })
    userStore.updateCurrentUser(user)
    ElMessage.success('基础资料已保存')
  } finally {
    accountSaving.value = false
  }
}
async function savePassword() {
  if (!passwordForm.currentPassword || !passwordForm.newPassword) return ElMessage.warning('请完整填写密码信息')
  if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 32) return ElMessage.warning('新密码长度应为 6–32 位')
  if (passwordForm.newPassword !== passwordForm.confirmPassword) return ElMessage.warning('两次输入的新密码不一致')
  passwordSaving.value = true
  try {
    await updateMyPassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword })
    Object.assign(passwordForm, { currentPassword: '', newPassword: '', confirmPassword: '' })
    ElMessage.success('密码已更新')
  } finally {
    passwordSaving.value = false
  }
}
async function savePainterProfile() {
  painterSaving.value = true
  try {
    const user = await updateMyPainterProfile({ bio: painterForm.bio, tags: painterForm.tags })
    userStore.updateCurrentUser(user)
    await refreshCertification()
    ElMessage.success('画师资料已保存')
  } finally {
    painterSaving.value = false
  }
}
async function openInitialApplication() {
  await refreshCertification()
  initialStep.value = 0
  initialForm.commonId = certification.value.pendingInitialApplication?.commonId || userStore.user?.nickname || ''
  initialForm.commonSite = certification.value.pendingInitialApplication?.commonSite || ''
  initialVisible.value = true
}
async function focusPainterProfile() {
  initialVisible.value = false
  await nextTick()
  painterProfileCard.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}
async function submitInitialApplication() {
  if (!initialForm.commonId.trim() || !initialForm.commonSite.trim()) return ElMessage.warning('请填写笔名和常用站')
  initialSubmitting.value = true
  try {
    await submitCertification({ commonId: initialForm.commonId, commonSite: initialForm.commonSite })
    await Promise.all([userStore.fetchMe(), refreshCertification()])
    initialVisible.value = false
    ElMessage.success('认证申请已提交，请等待管理员审核')
  } finally {
    initialSubmitting.value = false
  }
}
function openChangeApplication() {
  changeForm.commonId = certification.value.artistName || userStore.user?.nickname || ''
  changeForm.commonSite = certification.value.artistSite || ''
  changeVisible.value = true
}
async function submitChangeApplication() {
  if (!changeForm.commonId.trim() || !changeForm.commonSite.trim()) return ElMessage.warning('请填写新笔名和新常用站')
  changeSubmitting.value = true
  try {
    await submitCertificationChange({ commonId: changeForm.commonId, commonSite: changeForm.commonSite })
    await refreshCertification()
    changeVisible.value = false
    ElMessage.success('认证资料修改已提交审核')
  } finally {
    changeSubmitting.value = false
  }
}
</script>

<style scoped>
.settings-page { min-height: calc(100vh - var(--artiv-header-height)); padding: 34px 0 64px; background: #f4f7f9; }
.settings-shell { display: grid; grid-template-columns: 250px minmax(0, 1fr); gap: 24px; align-items: start; }
.settings-nav { position: sticky; top: calc(var(--artiv-header-height) + 24px); overflow: hidden; padding: 10px; border: 1px solid #e4ebf0; border-radius: 12px; background: #fff; box-shadow: 0 8px 28px rgb(42 67 84 / 6%); }
.settings-nav__title { padding: 16px 14px 18px; border-bottom: 1px solid #edf1f4; }
.settings-nav__title span { display: block; color: #1d2a34; font-size: 19px; font-weight: 700; }
.settings-nav__title small { display: block; margin-top: 6px; color: #8a96a0; line-height: 1.5; }
.settings-nav button { width: 100%; margin-top: 8px; padding: 14px; border: 0; border-radius: 8px; color: #53616d; background: transparent; text-align: left; cursor: pointer; transition: .2s; }
.settings-nav button:hover { background: #f4f9fc; }
.settings-nav button.active { color: var(--artiv-primary); background: var(--artiv-primary-light); }
.settings-nav button strong, .settings-nav button span { display: block; }
.settings-nav button strong { font-size: 14px; }
.settings-nav button span { margin-top: 5px; color: #929da6; font-size: 12px; }
.settings-main { min-width: 0; }
.settings-heading { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 20px; padding: 4px 2px; }
.heading-kicker { color: var(--artiv-primary); font-size: 11px; font-weight: 700; letter-spacing: .14em; }
.settings-heading h1 { margin: 6px 0 8px; color: #1b2934; font-size: 25px; }
.settings-heading p { margin: 0; color: #7a8791; font-size: 14px; }
.settings-card { margin-bottom: 16px; padding: 24px 26px; border: 1px solid #e4ebf0; border-radius: 12px; background: #fff; box-shadow: 0 7px 24px rgb(42 67 84 / 5%); }
.settings-card h2 { margin: 0 0 18px; color: #24313b; font-size: 17px; }
.card-heading { display: flex; justify-content: space-between; margin-bottom: 18px; }
.card-heading h2 { margin-bottom: 5px; }
.card-heading p { margin: 0; color: #89949d; font-size: 13px; }
.settings-form { max-width: 760px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.placeholder-fields { margin-top: 6px; }
.form-actions { display: flex; justify-content: flex-end; padding-top: 4px; }
.password-form { max-width: 620px; }
.full-width { width: 100%; }
.certification-summary { padding: 26px; }
.certification-summary__top { display: flex; justify-content: space-between; align-items: center; gap: 20px; }
.summary-label { color: #89949d; font-size: 12px; }
.summary-status { display: flex; align-items: center; gap: 10px; margin-top: 6px; }
.summary-status h2 { margin: 0; font-size: 22px; }
.certification-fields { display: grid; grid-template-columns: 1fr 1.6fr; gap: 12px; margin: 22px 0; }
.certification-fields div { min-width: 0; padding: 15px 16px; border-radius: 8px; background: #f6f9fb; }
.certification-fields dt { color: #89949d; font-size: 12px; }
.certification-fields dd { overflow: hidden; margin: 7px 0 0; color: #293844; text-overflow: ellipsis; white-space: nowrap; }
.certification-fields a { color: var(--artiv-primary); }
.pending-copy { margin: 5px 0 0; word-break: break-all; }
.eligibility-card { display: flex; justify-content: space-between; align-items: center; gap: 24px; }
.eligibility-card h2 { margin-bottom: 6px; }
.eligibility-card p { margin: 0; color: #89949d; font-size: 13px; }
.eligibility-items { display: flex; flex-wrap: wrap; gap: 10px; }
.eligibility-items span { display: inline-flex; align-items: center; gap: 7px; padding: 9px 13px; border-radius: 999px; color: #a25b59; background: #fff3f2; font-size: 13px; }
.eligibility-items span.complete { color: #3c8b68; background: #edf8f2; }
.application-steps { margin: 4px 0 28px; }
.qualification-list { display: grid; gap: 12px; margin-bottom: 18px; }
.qualification-list > div { display: flex; align-items: center; gap: 14px; padding: 16px; border: 1px solid #f0d7d4; border-radius: 9px; color: #b35d55; background: #fff9f8; }
.qualification-list > div.passed { border-color: #d7ebdf; color: #3d8a67; background: #f4fbf7; }
.qualification-list .el-icon { font-size: 20px; }
.qualification-list span, .qualification-list strong, .qualification-list small { display: block; }
.qualification-list small { margin-top: 4px; color: #89949d; }
.application-form { margin-top: 20px; }
.field-help, .supported-sites { color: #89949d; font-size: 12px; line-height: 1.6; }
.field-help { margin: 7px 0 0; }
.supported-sites { margin: -3px 0 0; padding: 11px 13px; border-radius: 7px; background: #f6f9fb; }
.complete-profile-link { margin-top: 10px; }
.current-values { display: grid; grid-template-columns: 1fr; gap: 8px; margin-bottom: 18px; }
.current-values div { display: grid; grid-template-columns: 90px minmax(0, 1fr); gap: 12px; padding: 11px 13px; border-radius: 7px; background: #f6f9fb; }
.current-values span { color: #89949d; }
.current-values strong { overflow-wrap: anywhere; }
@media (max-width: 860px) { .settings-shell { grid-template-columns: 1fr; } .settings-nav { position: static; display: grid; grid-template-columns: 1fr 1fr; } .settings-nav__title { grid-column: 1 / -1; } }
@media (max-width: 620px) { .settings-page { padding-top: 18px; } .settings-shell { padding: 0 14px; } .settings-nav { grid-template-columns: 1fr; } .settings-nav__title { grid-column: auto; } .settings-card { padding: 20px 18px; } .form-grid, .certification-fields { grid-template-columns: 1fr; } .certification-summary__top, .eligibility-card { align-items: flex-start; flex-direction: column; } }
</style>
