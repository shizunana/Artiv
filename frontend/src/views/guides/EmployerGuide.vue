<template>
  <main class="employer-guide page-container">
    <div class="guide-layout">
      <aside class="guide-directory" aria-label="企划方指南目录">
        <span>EMPLOYER GUIDE</span>
        <h2>企划方指南</h2>
        <nav>
          <a v-for="item in directory" :key="item.id" :href="`#${item.id}`">
            <em>{{ item.number }}</em>
            <span>{{ item.label }}</span>
          </a>
        </nav>
      </aside>

      <article class="guide-article">
        <header class="guide-heading">
          <span>ARTIV EMPLOYER GUIDE</span>
          <h1>从发布企划到完成合作</h1>
          <p>了解艺绘的企划、应征、邀请和订单流程，做好第一次约稿前的准备，让需求、价格和交付安排从一开始就足够清楚。</p>
          <div class="guide-flow" aria-label="企划方合作流程">
            <span>发布企划</span><i>→</i><span>选择画师</span><i>→</i><span>托管下单</span><i>→</i><span>阶段验收</span><i>→</i><span>完成评价</span>
          </div>
        </header>

        <section id="publish" class="guide-section">
          <div class="section-index">01</div>
          <div class="section-content">
            <h2>先发布一份容易理解的企划</h2>
            <p>公开企划会进入企划大厅，由认证画师主动应征；定向邀请只发送给创建时选择的一名画师，不在大厅公开。两种方式都从“发布企划”开始，但适合的使用场景不同。</p>

            <div class="guide-grid">
              <div class="guide-panel">
                <h3>公开企划</h3>
                <p>适合希望比较多位画师的作品、报价和应征说明。发布后还可以从画师主页发出邀请报价或带价邀请。</p>
              </div>
              <div class="guide-panel">
                <h3>定向邀请</h3>
                <p>适合已有明确合作对象的情况。画师接受后会生成有效应征并建立会话，之后仍由企划方发送正式订单。</p>
              </div>
            </div>

            <h3>预算要与工作量对应</h3>
            <p>预算区间应让画师能够判断项目规模。范围过大会让实际预期变得模糊；如果人物数量、用途或细节要求较多，应在需求中说明，并为相应工作量预留合理预算。</p>

            <h3>截稿日期控制招募时间</h3>
            <p>发布时选择的截稿日期至少为当前日期两天后。所选日期当天仍可产生新应征，次日企划才会从大厅排除。它控制的是企划招募，不是订单必须交付的日期；已经产生的应征、会话和订单不会因企划过期而中断。</p>

            <div class="guide-tip">
              <strong>填写建议</strong>
              <p>写清画面内容、人物数量、题材风格、使用范围、尺寸规格、颜色模式和稿件格式，并上传最接近期望效果的参考图。第一张参考图也会成为企划列表封面。</p>
            </div>

            <p>创作阶段默认为“初稿 → 修改 → 终稿”，可在发布时增删、重命名和排序。阶段会在下单时保存为订单快照，之后修改企划不会改变已经建立的订单。</p>
          </div>
        </section>

        <section id="recruit" class="guide-section">
          <div class="section-index">02</div>
          <div class="section-content">
            <h2>让合适的画师看到企划</h2>
            <p>公开企划发布后会在招募有效期内进入大厅。若应征较少，可以先检查标题、参考图、需求说明、预算和截稿日期，再通过“我的企划”编辑内容或延长已经过期的截稿日期。</p>

            <h3>从画师主页主动邀请</h3>
            <ul>
              <li><strong>邀请报价：</strong>提醒画师查看公开企划，由画师根据需求决定是否应征及如何报价。</li>
              <li><strong>带价邀请：</strong>企划方先给出邀请价格；画师接受后按该价格生成应征记录。</li>
              <li><strong>定向邀请：</strong>属于定向企划的原有流程，不与公开企划邀请混用。</li>
            </ul>
            <p>邀请被接受并不等于直接生成订单。只有形成有效应征后，企划方才能继续沟通并发送正式订单。同一个公开企划可以邀请多位不同画师，但已经应征的画师无需再次邀请。</p>
          </div>
        </section>

        <section id="select" class="guide-section">
          <div class="section-index">03</div>
          <div class="section-content">
            <h2>比较应征并确认合作细节</h2>
            <p>在企划详情页的应征画师区域，可以查看报价、个人简介、题材标签和作品预览。建议再进入画师公开主页，结合相关题材作品、整体完成度和历史合作评价进行判断。</p>
            <p>准备合作时，先通过消息中心确认画面范围、最终价格、各阶段要交付的内容和大致时间。公开企划的普通应征需要企划方主动发起沟通；画师接受邀请时，系统会自动建立或复用双方会话。</p>

            <div class="guide-checklist">
              <h3>发送订单前建议确认</h3>
              <span>需求范围是否一致</span>
              <span>报价是否包含全部内容</span>
              <span>参考图与用途是否清楚</span>
              <span>创作阶段是否适合本次合作</span>
            </div>
          </div>
        </section>

        <section id="order" class="guide-section">
          <div class="section-index">04</div>
          <div class="section-content">
            <h2>发送订单并进入正式合作</h2>
            <p>企划方从有效应征向画师发送企划订单，订单金额使用当前应征报价并进入平台托管。画师确认接单后，订单从“待接单”进入第一个创作阶段。</p>
            <p>正式合作以后，以订单详情中的状态和创作阶段为准。企划的截稿日期不会阻止画师接单、提交稿件、企划方验收、订单结算或后续评价。</p>

            <div class="guide-tip guide-tip--plain">
              <strong>重要约定请留在站内</strong>
              <p>需求变更、修改意见和阶段确认尽量通过艺绘会话沟通，方便双方在后续验收或争议处理时查看完整记录。</p>
            </div>
          </div>
        </section>

        <section id="delivery" class="guide-section">
          <div class="section-index">05</div>
          <div class="section-content">
            <h2>按阶段验收画师提交的稿件</h2>
            <p>画师会按照订单的真实阶段提交文件。交易完成前，企划方查看的是系统生成的低分辨率水印预览，原始稿件不会提前开放。</p>
            <ul>
              <li><strong>通过当前阶段：</strong>确认本阶段成果，订单进入下一阶段。</li>
              <li><strong>要求修改：</strong>填写具体意见，画师重新提交当前阶段。</li>
              <li><strong>提前确认完成：</strong>当前成果已经满足需求时，可跳过后续阶段直接完成交易。</li>
            </ul>
            <p>最后阶段通过或提前确认完成后，托管款项结算给画师，企划方获得高清原图。每次确认前都应认真检查，修改意见尽量说明具体位置、问题和预期效果。</p>
          </div>
        </section>

        <section id="after-sale" class="guide-section">
          <div class="section-index">06</div>
          <div class="section-content">
            <h2>完成合作、评价与处理分歧</h2>
            <p>合作中出现理解偏差时，应先在双方会话中协商。若确实无法继续，可按订单当前状态使用取消、退款或争议入口，并填写原因及必要的图片证据。</p>
            <p>争议提交后，订单会暂停普通流转并等待管理员仲裁。当前仲裁结果为全额退款企划方或全额结算画师，不支持部分退款，因此应将仲裁作为协商无法解决后的最后手段。</p>
            <p>订单正常完成后，双方可在规定期限内分别提交星级、文字和图片评价。评价会进入对应身份的公开合作记录；如果评价存在违规内容，可通过评价申诉交由管理员审核。</p>
          </div>
        </section>

        <footer class="guide-actions">
          <div>
            <h2>准备发布第一份企划了吗？</h2>
            <p>先准备清晰的需求和参考图，再进入发布向导完成预算、日期和创作阶段设置。</p>
          </div>
          <div class="guide-buttons">
            <el-button round @click="router.push({ name: 'DashboardHome' })">返回工作台</el-button>
            <el-button type="primary" round @click="router.push({ name: 'ProjectNew' })">立即发布企划</el-button>
          </div>
        </footer>
      </article>
    </div>
  </main>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
const directory = [
  { id: 'publish', number: '01', label: '发布清晰企划' },
  { id: 'recruit', number: '02', label: '获得画师应征' },
  { id: 'select', number: '03', label: '选择合适画师' },
  { id: 'order', number: '04', label: '发送正式订单' },
  { id: 'delivery', number: '05', label: '阶段提交与验收' },
  { id: 'after-sale', number: '06', label: '完成与处理分歧' },
]
</script>

<style scoped>
.employer-guide { padding-top: 34px; padding-bottom: 72px; }
.guide-layout { display: grid; max-width: 1160px; grid-template-columns: 220px minmax(0, 1fr); align-items: start; margin: 0 auto; gap: 20px; }
.guide-directory { position: sticky; top: 88px; box-sizing: border-box; border: 1px solid var(--artiv-border); border-radius: 12px; background: #fff; box-shadow: 0 8px 28px rgb(42 67 84 / 5%); padding: 24px 18px; }
.guide-directory > span { color: var(--artiv-primary); font-size: 10px; font-weight: 700; letter-spacing: 1.2px; }
.guide-directory h2 { margin: 7px 0 19px; color: var(--artiv-text); font-size: 20px; }
.guide-directory nav { display: grid; gap: 5px; }
.guide-directory a { display: flex; align-items: center; gap: 10px; border-radius: 7px; padding: 9px 10px; color: #566570; font-size: 13px; text-decoration: none; transition: color .2s, background .2s; }
.guide-directory a:hover { background: #eef9fe; color: var(--artiv-primary); }
.guide-directory em { color: #a1afb8; font-size: 10px; font-style: normal; font-weight: 700; }
.guide-article { min-width: 0; box-sizing: border-box; border: 1px solid var(--artiv-border); border-radius: 12px; background: #fff; box-shadow: 0 8px 28px rgb(42 67 84 / 5%); padding: 46px 64px 52px; }
.guide-heading { padding-bottom: 34px; border-bottom: 1px solid var(--artiv-border); }
.guide-heading > span { display: block; margin-bottom: 10px; color: var(--artiv-primary); font-size: 12px; font-weight: 700; letter-spacing: 1.4px; }
.guide-heading h1 { margin: 0; color: var(--artiv-text); font-size: 34px; line-height: 1.35; }
.guide-heading p { max-width: 740px; margin: 14px 0 0; color: var(--artiv-text-secondary); font-size: 15px; line-height: 1.9; }
.guide-flow { display: flex; flex-wrap: wrap; align-items: center; margin-top: 24px; gap: 8px; }
.guide-flow span { border-radius: 999px; background: #edf8fd; padding: 7px 11px; color: #28708f; font-size: 12px; }
.guide-flow i { color: #b0bec6; font-size: 12px; font-style: normal; }
.guide-section { display: grid; grid-template-columns: 46px minmax(0, 1fr); padding: 35px 0; border-bottom: 1px solid var(--artiv-border); gap: 8px; scroll-margin-top: 82px; }
.section-index { padding-top: 4px; color: #b7c6ce; font-size: 12px; font-weight: 700; }
.section-content h2,.guide-actions h2 { margin: 0 0 18px; color: var(--artiv-text); font-size: 23px; line-height: 1.5; }
.section-content h3 { margin: 25px 0 10px; color: #263743; font-size: 17px; line-height: 1.6; }
.section-content p,.section-content li { color: #4e5c66; font-size: 15px; line-height: 2; }
.section-content p { margin: 0 0 16px; }
.section-content p:last-child { margin-bottom: 0; }
.section-content ul { margin: 0 0 17px; padding-left: 21px; }
.section-content li + li { margin-top: 5px; }
.section-content strong { color: var(--artiv-primary); font-weight: 700; }
.guide-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin: 20px 0 8px; gap: 12px; }
.guide-panel { border: 1px solid #dcebf2; border-radius: 10px; background: #f8fcfe; padding: 18px 20px; }
.guide-panel h3 { margin: 0 0 7px; font-size: 15px; }
.guide-panel p { margin: 0; font-size: 13px; line-height: 1.8; }
.guide-tip { margin: 22px 0; border-left: 3px solid var(--artiv-primary); border-radius: 0 8px 8px 0; background: #f1faff; padding: 16px 19px; }
.guide-tip strong { display: block; margin-bottom: 5px; font-size: 14px; }
.guide-tip p { margin: 0; font-size: 13px; line-height: 1.8; }
.guide-tip--plain { background: #f7fafb; }
.guide-checklist { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 22px; gap: 10px; }
.guide-checklist h3 { grid-column: 1 / -1; margin: 0 0 4px; }
.guide-checklist span { position: relative; border: 1px solid var(--artiv-border); border-radius: 8px; padding: 11px 12px 11px 31px; color: #4e5c66; font-size: 13px; }
.guide-checklist span::before { position: absolute; top: 13px; left: 12px; width: 8px; height: 8px; border-radius: 50%; background: var(--artiv-primary); content: ''; }
.guide-actions { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding-top: 34px; }
.guide-actions h2 { margin-bottom: 6px; font-size: 19px; }
.guide-actions p { margin: 0; color: var(--artiv-text-secondary); font-size: 13px; line-height: 1.7; }
.guide-buttons { display: flex; flex-shrink: 0; gap: 10px; }
@media (max-width: 900px) {
  .guide-layout { grid-template-columns: 1fr; }
  .guide-directory { position: static; }
  .guide-directory nav { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 700px) {
  .employer-guide { padding: 18px 14px 40px; }
  .guide-article { padding: 30px 22px 34px; }
  .guide-heading h1 { font-size: 28px; }
  .guide-directory nav,.guide-grid,.guide-checklist { grid-template-columns: 1fr; }
  .guide-section { grid-template-columns: 1fr; }
  .section-index { padding: 0; }
  .guide-actions { align-items: flex-start; flex-direction: column; }
  .guide-buttons { width: 100%; flex-wrap: wrap; }
}
</style>
