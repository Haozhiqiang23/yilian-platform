<script setup>
// 通用占位页：骨架阶段展示"页面待开发"信息
defineProps({
  title: { type: String, default: '页面' },
  desc: { type: String, default: '该页面将在后续迭代中实现，功能点可参考《医联智诊平台前端页面设计方案》。' },
  icon: { type: String, default: 'construction' },
})
</script>

<template>
  <div class="placeholder-page">
    <div class="placeholder-card">
      <div class="ph-icon">
        <svg v-if="icon === 'construction'" width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 6l4 4-4 1-3-3 1-4 2 2z" />
          <path d="M20 8l2 2-8 8-6 2-2-6 6-6 8 6z" />
          <path d="M2 22h6M9 17l-4 4" />
        </svg>
        <svg v-else width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
          <path d="M14 2v6h6" />
        </svg>
      </div>
      <h2 class="ph-title">{{ title }}</h2>
      <p class="ph-desc">{{ desc }}</p>
    </div>
  </div>
</template>

<style scoped>
.placeholder-page {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.placeholder-card {
  text-align: center;
  max-width: 380px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 48px 36px;
  box-shadow: var(--shadow-sm);
}
.ph-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  border-radius: 20px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
}
.ph-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-ink);
  margin-bottom: 10px;
}
.ph-desc {
  font-size: 13px;
  color: var(--color-ink-3);
  line-height: 1.8;
}
</style>