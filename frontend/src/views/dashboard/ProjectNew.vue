<template>
  <div class="project-new">
    <div class="wb-card">
      <h3 class="page-title">发布企划</h3>

      <!-- 步骤条（参考站 4 步向导） -->
      <el-steps :active="step" finish-status="success" align-center class="wizard-steps">
        <el-step title="稿件用途" />
        <el-step title="需求详情" />
        <el-step title="高级选项" />
        <el-step title="预览企划" />
      </el-steps>

      <!-- 第 1 步：稿件用途 -->
      <div v-if="step === 0" class="wizard-body">
        <h4>我准备把稿件用作…</h4>
        <div class="choice-grid">
          <div
            v-for="t in types"
            :key="t.value"
            class="choice-card"
            :class="{ 'is-active': form.manuscriptType === t.value }"
            @click="form.manuscriptType = t.value"
          >
            <span class="choice-emoji">{{ t.icon }}</span>
            <strong>{{ t.label }}</strong>
          </div>
        </div>

        <h4>稿件用途</h4>
        <div class="usage-grid">
          <div class="usage-group">
            <div
              class="usage-card"
              :class="{ 'is-active': form.usage === 'personal' }"
              @click="form.usage = 'personal'"
            >
              <strong>个人用途</strong>
              <p>稿件仅供个人使用、展示和娱乐，不涉及印刷传播、大规模贩售等盈利用途。</p>
              <el-tag size="small" type="success">个人收藏 / 社交展示 / 壁纸头像</el-tag>
            </div>
            <div
              class="usage-card"
              :class="{ 'is-active': form.usage === 'commercial' }"
              @click="form.usage = 'commercial'"
            >
              <strong>商业用途</strong>
              <p>稿件用于商业目的，如游戏宣传、周边贩卖、虚拟主播运营等营利性活动。</p>
              <el-tag size="small" type="warning">游戏用图 / 商业宣传 / 周边贩售</el-tag>
            </div>
          </div>
        </div>

        <div class="form-row">
          <el-form-item label="截稿日期" required class="inline-item">
            <el-date-picker
              v-model="form.deadline"
              type="date"
              placeholder="选择截稿日期（至少 7 天后）"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
            />
          </el-form-item>
          <el-form-item label="稿酬预算（元/张）" required class="inline-item">
            <el-input-number v-model="form.budgetMin" :min="0" :max="100000" placeholder="最低" />
            <span class="range-sep">—</span>
            <el-input-number v-model="form.budgetMax" :min="0" :max="100000" placeholder="最高" />
          </el-form-item>
        </div>

        <el-checkbox v-model="form.directed">可选择一位画师发布定向企划（内容仅被选画师可见）</el-checkbox>
      </div>

      <!-- 第 2 步：需求详情 -->
      <div v-else-if="step === 1" class="wizard-body">
        <el-form label-position="top">
          <el-form-item label="企划标题" required>
            <el-input v-model="form.title" maxlength="20" show-word-limit placeholder="一句话描述您的需求（最多 20 字）" />
          </el-form-item>
          <el-form-item label="题材风格">
            <el-select
              v-model="form.subjects"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入题材风格（可多选）"
              class="subject-select"
            >
              <el-option v-for="s in SUBJECT_OPTIONS" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item label="需求描述" required>
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="6"
              maxlength="1000"
              show-word-limit
              placeholder="详细描述风格、内容、尺寸、参考等要求"
            />
          </el-form-item>
          <el-form-item label="参考图">
            <el-upload
              list-type="picture-card"
              :limit="6"
              accept="image/png,image/jpeg,image/gif,image/webp"
              :file-list="imageFileList"
              :http-request="doUpload"
              :on-remove="onImageRemove"
              :on-exceed="onImageExceed"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="form.tags" multiple filterable allow-create default-first-option placeholder="选择或输入标签">
              <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <!-- 第 3 步：高级选项 -->
      <div v-else-if="step === 2" class="wizard-body">
        <el-form label-position="top">
          <el-form-item label="题材">
            <el-select v-model="form.subject" multiple placeholder="选择题材">
              <el-option v-for="s in subjectOptions" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item label="交付格式">
            <el-checkbox-group v-model="form.formats">
              <el-checkbox v-for="f in formatOptions" :key="f" :label="f" />
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="合作方式">
            <el-radio-group v-model="form.mode">
              <el-radio :value="0">公开招募（全员可见）</el-radio>
              <el-radio :value="1">定向邀请（仅被邀画师可见）</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.mode === 1" label="定向邀请画师">
            <el-select v-model="form.invitePainterId" placeholder="选择被邀请的画师">
              <el-option label="画师阿绘（已认证）" :value="2" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <!-- 第 4 步：预览 -->
      <div v-else class="wizard-body">
        <el-descriptions :column="1" border class="preview-box">
          <el-descriptions-item label="稿件类型">{{ typeText }}</el-descriptions-item>
          <el-descriptions-item label="用途">{{ form.usage === 'personal' ? '个人用途' : '商业用途' }}</el-descriptions-item>
          <el-descriptions-item label="截稿日期">{{ form.deadline || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预算">¥{{ form.budgetMin }} — ¥{{ form.budgetMax }} / 张</el-descriptions-item>
          <el-descriptions-item label="标题">{{ form.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需求描述">{{ form.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="题材">{{ form.subject.join(' / ') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="格式">{{ form.formats.join(' / ') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="合作方式">{{ form.mode === 1 ? '定向邀请' : '公开招募' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="wizard-actions">
        <el-button v-if="step > 0" round @click="step -= 1">上一步</el-button>
        <el-button v-if="step < 3" type="primary" round @click="next">下一步</el-button>
        <el-button v-else type="primary" round :loading="submitting" @click="submit">
          发布企划
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createProject } from '@/api/project'
import { uploadFile } from '@/api/upload'
import { SUBJECT_OPTIONS } from '@/constants/subjects'

const router = useRouter()
const step = ref(0)
const submitting = ref(false)

const types = [
  { value: 'oc', label: '自设/OC', icon: '🧑‍🎨' },
  { value: 'avatar', label: '头像', icon: '🪞' },
  { value: 'illustration', label: '插图', icon: '🖼️' },
  { value: 'live2d', label: 'Live2d', icon: '💃' },
  { value: 'character', label: '立绘', icon: '🧍' },
  { value: 'design', label: '平面设计', icon: '🎨' },
  { value: 'other', label: '其他', icon: '✨' },
]

const tagOptions = ['国潮', '插画', '立绘', '场景', '原画', '同人', 'Q版', '古风']
const subjectOptions = ['Q版', '女性', '男性', '古风', '现代', '科幻', '奇幻', '兽人']
const formatOptions = ['JPG', 'PNG', 'PSD', 'GIF', 'WEBP']

const form = reactive({
  manuscriptType: 'illustration',
  usage: 'personal',
  deadline: '',
  budgetMin: 300,
  budgetMax: 500,
  directed: false,
  title: '',
  description: '',
  tags: [],
  subjects: [],
  subject: [],
  formats: [],
  mode: 0,
  invitePainterId: null,
  images: [],
})

// 参考图：文件列表（UI）与 URL 列表（提交）
const imageFileList = ref([])

async function doUpload({ file, onSuccess, onError }) {
  try {
    const url = await uploadFile(file)
    form.images.push(url)
    onSuccess(url)
  } catch (e) {
    ElMessage.error('图片上传失败')
    onError(e)
  }
}

function onImageRemove(file) {
  const url = file.response || file.url
  const idx = form.images.indexOf(url)
  if (idx >= 0) form.images.splice(idx, 1)
}

function onImageExceed() {
  ElMessage.warning('最多上传 6 张参考图')
}

const typeText = computed(() => types.find((t) => t.value === form.manuscriptType)?.label || '-')

/** 截稿日期至少为发布后 7 天 */
function disabledDate(date) {
  const min = new Date()
  min.setDate(min.getDate() + 7)
  min.setHours(0, 0, 0, 0)
  return date.getTime() < min.getTime()
}

function next() {
  if (step.value === 0) {
    if (!form.deadline) {
      ElMessage.warning('请选择截稿日期')
      return
    }
    if (!form.budgetMax || form.budgetMax <= 0) {
      ElMessage.warning('请设置预算区间')
      return
    }
  }
  if (step.value === 1 && !form.title.trim()) {
    ElMessage.warning('请填写企划标题')
    return
  }
  step.value += 1
}

async function submit() {
  submitting.value = true
  try {
    // 高级选项（题材 / 格式）并入需求描述，避免信息丢失
    const extras = []
    if (form.subject.length) extras.push('题材：' + form.subject.join(' / '))
    if (form.formats.length) extras.push('交付格式：' + form.formats.join(' / '))
    const description = extras.length
      ? `${form.description}\n\n[高级选项] ${extras.join('；')}`
      : form.description

    await createProject({
      title: form.title,
      description,
      budget: form.budgetMax,
      deadline: form.deadline,
      tags: form.tags,
      images: form.images.length ? form.images.join(',') : null,
      manuscriptType: form.manuscriptType,
      subject: form.subjects,
      mode: form.mode,
      invitePainterId: form.mode === 1 ? form.invitePainterId : null,
    })
    ElMessage.success('企划发布成功！等待画师应征')
    router.push({ name: 'DashboardProjects' })
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.wb-card {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 28px;
}

.page-title {
  margin: 0 0 24px;
  font-size: 18px;
  color: var(--huiyue-text);
}

.wizard-steps {
  margin-bottom: 32px;
}

.wizard-body h4 {
  margin: 24px 0 14px;
  font-size: 15px;
  color: var(--huiyue-text);
}

.choice-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}

.choice-card {
  border: 1px solid var(--huiyue-border);
  border-radius: 8px;
  padding: 16px 6px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.choice-card:hover {
  border-color: var(--huiyue-primary);
}

.choice-card.is-active {
  border-color: var(--huiyue-primary);
  background: var(--huiyue-primary-light);
}

.choice-emoji {
  font-size: 26px;
}

.choice-card strong {
  font-size: 13px;
  color: var(--huiyue-text);
}

.usage-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.usage-card {
  border: 1px solid var(--huiyue-border);
  border-radius: 8px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.usage-card:hover {
  border-color: var(--huiyue-primary);
}

.usage-card.is-active {
  border-color: var(--huiyue-primary);
  background: var(--huiyue-primary-light);
}

.usage-card strong {
  font-size: 15px;
}

.usage-card p {
  color: var(--huiyue-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.form-row {
  display: flex;
  gap: 32px;
  margin-top: 24px;
}

.inline-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.range-sep {
  margin: 0 8px;
  color: var(--huiyue-text-secondary);
}

.preview-box {
  max-width: 640px;
  margin: 0 auto;
}

.wizard-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 28px;
}

@media (max-width: 900px) {
  .choice-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .usage-group,
  .form-row {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
