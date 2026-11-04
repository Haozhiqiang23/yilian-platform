<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getReport } from '@/api/patient'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMsg = ref('')
const report = ref(null)
const reportId = computed(() => route.params.id)
const items = computed(() => {
  const detailItems = report.value?.items || report.value?.indicators || report.value?.results
  if (Array.isArray(detailItems) && detailItems.length > 0) return detailItems

  // 当前后端以 result 文本返回综合检查结果；包装成单项，兼容以后返回指标数组。
  if (report.value?.result) {
    return [{
      name: report.value.itemName || report.value.examName || '综合检查结果',
      value: report.value.result,
    }]
  }
  return []
})

const reportConclusion = computed(() =>
  report.value?.remark
  || report.value?.conclusion
  || report.value?.summary
  || report.value?.diagnosis,
)

async function loadReport() {
  loading.value = true
  errorMsg.value = ''
  try {
    report.value = await getReport(reportId.value)
  } catch (e) {
    errorMsg.value = e.message || '检查报告加载失败，请稍后重试'
    report.value = null
  } finally {
    loading.value = false
  }
}

function textOf(value, fallback = '暂无信息') {
  return value || value === 0 ? value : fallback
}

onMounted(loadReport)
</script>

<template>
  <div class="report-detail-view">
    <section class="page-hero" aria-labelledby="report-title">
      <button class="back-btn" type="button" aria-label="返回诊疗记录" @click="router.push('/patient/records')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 12H5" />
          <path d="m12 19-7-7 7-7" />
        </svg>
      </button>
      <div>
        <p class="eyebrow">Examination Report</p>
        <h1 id="report-title">检查报告</h1>
        <p>展示检查项目、结果指标和报告结论，仅使用后端检查报告接口返回数据。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" aria-label="刷新检查报告" @click="loadReport">
        <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 0 1-15.5 6.2" />
          <path d="M3 12A9 9 0 0 1 18.5 5.8" />
          <path d="M18 2v4h-4" />
          <path d="M6 22v-4h4" />
        </svg>
      </button>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <section v-if="loading" class="state-card">正在请求检查报告接口...</section>

    <template v-else>
      <section class="summary-card" aria-label="报告概要">
        <span class="status-tag">{{ textOf(report?.status, '报告状态待确认') }}</span>
        <dl>
          <div>
            <dt>报告编号</dt>
            <dd>{{ reportId }}</dd>
          </div>
          <div>
            <dt>检查名称</dt>
            <dd>{{ textOf(report?.examName || report?.examinationName || report?.name) }}</dd>
          </div>
          <div>
            <dt>检查时间</dt>
            <dd>{{ textOf(report?.examTime || report?.checkTime || report?.createTime) }}</dd>
          </div>
          <div>
            <dt>报告医生</dt>
            <dd>{{ textOf(report?.doctorName || report?.reportDoctor) }}</dd>
          </div>
        </dl>
      </section>

      <section class="result-card" aria-label="检查结果">
        <h2>检查结果</h2>
        <div v-if="items.length === 0" class="empty-state">检查结果尚未生成。</div>
        <article v-for="(item, index) in items" v-else :key="item.id || item.code || index" class="result-item">
          <strong>{{ textOf(item.name || item.itemName, '检查指标未填写') }}</strong>
          <span>{{ textOf(item.value || item.result, '检查结果尚未生成') }}</span>
          <p v-if="item.reference || item.referenceRange">参考范围：{{ item.reference || item.referenceRange }}</p>
        </article>
      </section>

      <section class="conclusion-card" aria-label="报告结论">
        <h2>报告结论</h2>
        <p>{{ textOf(reportConclusion) }}</p>
      </section>

      <section class="settlement-notice" aria-label="费用提示">
        <h2>领取提示</h2>
        <p>领取报告时请到医院窗口办理，相关费用请在线下结算。</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.report-detail-view {
  min-height: 100%;
  padding: 14px 16px calc(var(--bottom-nav-h) + 24px);
}

.page-hero,
.summary-card,
.result-card,
.conclusion-card,
.settlement-notice,
.state-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);
}

.settlement-notice {
  margin-top: 12px;
  padding: 16px;
  border-left: 4px solid var(--color-primary);
}

.settlement-notice h2 {
  margin: 0 0 6px;
  font-size: 17px;
}

.settlement-notice p {
  margin: 0;
  color: var(--color-ink-3);
  line-height: 1.7;
}

.page-hero {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.back-btn,
.refresh-btn {
  display: grid;
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  place-items: center;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--color-primary-deep);
}

.refresh-btn {
  margin-left: auto;
}

.eyebrow {
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 800;
}

.page-hero h1 {
  margin-top: 3px;
  color: var(--color-ink);
  font-size: 22px;
  font-weight: 800;
}

.page-hero p {
  margin-top: 5px;
  color: var(--color-ink-3);
  font-size: 12.5px;
}

.notice {
  margin-top: 12px;
  border-radius: var(--radius-sm);
  padding: 11px 13px;
  background: var(--color-danger-soft);
  color: var(--color-danger);
  font-size: 13px;
  font-weight: 700;
}

.state-card,
.summary-card,
.result-card,
.conclusion-card {
  margin-top: 12px;
  padding: 16px;
}

.state-card,
.empty-state {
  color: var(--color-ink-3);
  text-align: center;
  font-size: 13px;
}

.status-tag {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 10px;
  background: var(--color-info-soft);
  color: var(--color-info);
  font-size: 12px;
  font-weight: 800;
}

dl {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

dl div {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 10px;
}

dl div:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

dt {
  flex-shrink: 0;
  color: var(--color-ink-3);
  font-size: 12.5px;
  font-weight: 700;
}

dd {
  min-width: 0;
  color: var(--color-ink);
  text-align: right;
  font-size: 13px;
  font-weight: 800;
  overflow-wrap: anywhere;
}

.result-card h2,
.conclusion-card h2 {
  color: var(--color-ink);
  font-size: 17px;
  font-weight: 800;
}

.result-item {
  display: grid;
  gap: 5px;
  margin-top: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-bg);
}

.result-item strong {
  color: var(--color-ink);
  font-size: 14px;
}

.result-item span,
.result-item p,
.conclusion-card p {
  color: var(--color-ink-3);
  font-size: 12.5px;
  line-height: 1.7;
}

button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (max-width: 520px) {
  .page-hero,
  dl div {
    align-items: flex-start;
  }

  dl div {
    flex-direction: column;
  }

  dd {
    text-align: left;
  }
}
</style>
