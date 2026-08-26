<template>
  <main class="certification-guide page-container">
    <article class="guide-article">
      <header class="guide-heading">
        <span>PAINTER GUIDE</span>
        <h1>画师认证指南</h1>
        <p>准备好公开作品与可验证的个人创作主页，完成认证后即可在艺绘应征企划、处理邀请并发布橱窗服务。</p>
      </header>

      <section class="guide-section">
        <h2>申请前需要完成什么</h2>
        <p>
          画师认证用于确认申请人与公开作品之间的对应关系。提交申请前，请先在账户设置中完善画师个人简介，并在作品集中公开至少
          <strong>4 件由您创作的完整作品</strong>。
        </p>
        <p>
          满足申请条件后，进入“账户设置 → 身份认证”，填写对外展示的画师笔名和常用创作主页地址。申请进入审核后不能重复提交，审核结果会通过系统消息通知您。
        </p>
      </section>

      <section class="guide-section">
        <h2>如何准备公开作品</h2>

        <div class="guide-topic">
          <h3>优先展示能够代表当前水平的作品</h3>
          <p>
            四件公开作品是提交认证的基本条件。审核时会结合您当前公开的作品判断完成度、创作能力和展示内容是否可信，因此作品质量比单纯增加数量更重要。
          </p>
          <p>
            建议选择完成度较高、画面信息清晰，并且能够体现常用风格或擅长题材的作品。草稿、局部练习、过程截图或尚未完成的线稿不适合作为主要认证材料。
          </p>
        </div>

        <div class="guide-topic">
          <h3>确保作品来源清楚</h3>
          <p>
            请只上传本人原创，或您能够说明参与范围和使用权限的作品。不要使用他人作品、来源不明的图片或容易让审核人员误判作者身份的素材。
          </p>
          <p>
            如果作品属于多人合作，可以在作品说明中写明自己负责的部分，让展示内容与申请身份保持一致。
          </p>
        </div>
      </section>

      <section class="guide-section">
        <h2>如何填写常用创作主页</h2>
        <p>
          常用站应填写能够看到您本人创作记录的个人主页、作者主页或作品集页面，不能只填写平台首页。当前支持 Pixiv、Lofter、微博、小红书、哔哩哔哩、DeviantArt、站酷和涂鸦王国。
        </p>
        <p>
          该地址主要用于辅助核对身份与作品发布记录。审核关注的是主页内容能否与申请资料相互印证，不以关注人数或粉丝数量作为认证门槛。提交前请确认链接可以正常访问，并确实指向您的个人页面。
        </p>
      </section>

      <section class="guide-section">
        <h2>提交以后会发生什么</h2>
        <p>
          申请提交后，认证状态会变为“审核中”。管理员完成审核后，您会在消息中心收到通过或驳回通知。审核期间仍可继续完善作品，但不能重复发起同一份认证申请。
        </p>
        <p>
          通过认证后，画师接稿能力会正式开放；如果申请被驳回，请根据通知中的原因调整资料或作品，再重新提交。认证通过后修改笔名或常用站，也需要经过新的资料审核，原有资料会在新申请通过前继续生效。
        </p>
      </section>

      <footer class="guide-actions">
        <div>
          <h2>准备开始认证了吗？</h2>
          <p>先确认简介和公开作品完整，再进入身份认证提交申请。</p>
        </div>
        <div class="guide-buttons">
          <el-button round @click="router.push({ name: 'PainterWorks' })">管理我的作品</el-button>
          <el-button type="primary" round @click="router.push({ name: 'AccountSettings', query: { section: 'identity' } })">前往画师认证</el-button>
        </div>
      </footer>
    </article>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.certification-guide {
  padding-top: 34px;
  padding-bottom: 72px;
}

.guide-article {
  max-width: 920px;
  box-sizing: border-box;
  margin: 0 auto;
  border: 1px solid var(--artiv-border);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 8px 28px rgb(42 67 84 / 5%);
  padding: 46px 64px 52px;
}

.guide-heading {
  padding-bottom: 30px;
  border-bottom: 1px solid var(--artiv-border);
}

.guide-heading > span {
  display: block;
  margin-bottom: 10px;
  color: var(--artiv-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 1.4px;
}

.guide-heading h1 {
  margin: 0;
  color: var(--artiv-text);
  font-size: 32px;
  line-height: 1.35;
}

.guide-heading p {
  max-width: 720px;
  margin: 14px 0 0;
  color: var(--artiv-text-secondary);
  font-size: 15px;
  line-height: 1.9;
}

.guide-section {
  padding: 30px 0;
  border-bottom: 1px solid var(--artiv-border);
}

.guide-section h2,
.guide-actions h2 {
  margin: 0 0 18px;
  color: var(--artiv-text);
  font-size: 22px;
  line-height: 1.5;
}

.guide-section p {
  margin: 0 0 16px;
  color: #4e5c66;
  font-size: 15px;
  line-height: 2;
}

.guide-section p:last-child {
  margin-bottom: 0;
}

.guide-section strong {
  color: var(--artiv-primary);
  font-weight: 700;
}

.guide-topic + .guide-topic {
  margin-top: 26px;
}

.guide-topic h3 {
  margin: 0 0 12px;
  color: #263743;
  font-size: 17px;
  line-height: 1.6;
}

.guide-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding-top: 32px;
}

.guide-actions h2 {
  margin-bottom: 6px;
  font-size: 19px;
}

.guide-actions p {
  margin: 0;
  color: var(--artiv-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.guide-buttons {
  display: flex;
  flex-shrink: 0;
  gap: 10px;
}

@media (max-width: 760px) {
  .certification-guide {
    padding: 18px 14px 40px;
  }

  .guide-article {
    padding: 30px 22px 34px;
  }

  .guide-heading h1 {
    font-size: 27px;
  }

  .guide-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .guide-buttons {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
