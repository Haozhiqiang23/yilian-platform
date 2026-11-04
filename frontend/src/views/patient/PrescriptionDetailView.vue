<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPrescription } from '@/api/patient'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMsg = ref('')
const prescription = ref(null)
const prescriptionId = computed(() => route.params.id)
const medicines = computed(() => {
  const detailItems = prescription.value?.medicines || prescription.value?.items || prescription.value?.drugList
  if (Array.isArray(detailItems) && detailItems.length > 0) return detailItems

  // 当前后端以文本字段返回药品与用法；转换成单条展示数据，兼容以后升级为药品明细数组。
  if (prescription.value?.medicineInfo) {
    return [{
      name: prescription.value.medicineInfo,
      usage: prescription.value.usageInfo,
      amount: prescription.value.amount,
    }]
  }
  return []
})

const doctorAdvice = computed(() =>
  prescription.value?.advice
  || prescription.value?.remark
  || prescription.value?.doctorAdvice
  || prescription.value?.usageInfo,
)

async function loadPrescription() {
  loading.value = true
  errorMsg.value = ''
  try {
    prescription.value = await getPrescription(prescriptionId.value)
  } catch (e) {
    errorMsg.value = e.message || '处方详情加载失败，请稍后重试'
    prescription.value = null
  } finally {
    loading.value = false
  }
}

function textOf(value, fallback = '暂无信息') {
  return value || value === 0 ? value : fallback
}

onMounted(loadPrescription)
</script>

<template>
  <div class="prescription-detail-view">
    <section class="page-hero" aria-labelledby="prescription-title">
      <button class="back-btn" type="button" aria-label="返回诊疗记录" @click="router.push('/patient/records')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M19 12H5" />
          <path d="m12 19-7-7 7-7" />
        </svg>
      </button>
      <div>
        <p class="eyebrow">Prescription</p>
        <h1 id="prescription-title">处方详情</h1>
        <p>读取处方药品、用法用量和医嘱信息，仅展示后端正式接口返回的数据。</p>
      </div>
      <button class="refresh-btn" :disabled="loading" type="button" aria-label="刷新处方详情" @click="loadPrescription">
        <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 12a9 9 0 0 1-15.5 6.2" />
          <path d="M3 12A9 9 0 0 1 18.5 5.8" />
          <path d="M18 2v4h-4" />
          <path d="M6 22v-4h4" />
        </svg>
      </button>
    </section>

    <p v-if="errorMsg" class="notice danger" role="alert">{{ errorMsg }}</p>
    <section v-if="loading" class="state-card">正在请求处方详情接口...</section>

    <template v-else>
      <section class="summary-card" aria-label="处方概要">
        <span class="status-tag">{{ textOf(prescription?.status, '处方状态待确认') }}</span>
        <dl>
          <div>
            <dt>处方编号</dt>
            <dd>{{ prescriptionId }}</dd>
          </div>
          <div>
            <dt>诊疗记录</dt>
            <dd>{{ textOf(prescription?.medicalRecordId || prescription?.recordId) }}</dd>
          </div>
          <div>
            <dt>开方医生</dt>
            <dd>{{ textOf(prescription?.doctorName || prescription?.doctor?.name) }}</dd>
          </div>
          <div>
            <dt>开方时间</dt>
            <dd>{{ textOf(prescription?.createTime || prescription?.prescriptionTime) }}</dd>
          </div>
        </dl>
      </section>

      <section class="medicine-card" aria-label="药品清单">
        <h2>药品清单</h2>
        <div v-if="medicines.length === 0" class="empty-state">暂无药品明细。</div>
        <article v-for="(item, index) in medicines" v-else :key="item.id || item.medicineId || index" class="medicine-item">
          <strong>{{ textOf(item.name || item.medicineName, '药品名称未填写') }}</strong>
          <span v-if="item.spec || item.specification">规格：{{ item.spec || item.specification }}</span>
          <span v-if="item.amount !== undefined && item.amount !== null">处方金额：¥{{ item.amount }}</span>
          <p>{{ textOf(item.usage || item.dosage || item.frequency, '用法用量未填写') }}</p>
        </article>
      </section>

      <section class="advice-card" aria-label="医嘱">
        <h2>医嘱</h2>
        <p>{{ textOf(doctorAdvice) }}</p>
      </section>

      <section class="settlement-notice" aria-label="费用提示">
        <h2>取药提示</h2>
        <p>请携带处方到药房取药，相关费用请在线下结算。</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.prescription-detail-view {
  min-height: 100%;
  padding: 14px 16px calc(var(--bottom-nav-h) + 24px);
}

.page-hero,
.summary-card,
.medicine-card,
.advice-card,
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
.medicine-card,
.advice-card {
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
  background: var(--color-accent-soft);
  color: var(--color-accent);
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

.medicine-card h2,
.advice-card h2 {
  color: var(--color-ink);
  font-size: 17px;
  font-weight: 800;
}

.medicine-item {
  display: grid;
  gap: 5px;
  margin-top: 10px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--color-bg);
}

.medicine-item strong {
  color: var(--color-ink);
  font-size: 14px;
}

.medicine-item span,
.medicine-item p,
.advice-card p {
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
