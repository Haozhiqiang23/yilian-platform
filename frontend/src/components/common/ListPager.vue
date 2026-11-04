<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  total: { type: Number, default: 0 },
  disabled: Boolean,
})

const emit = defineEmits(['update:page'])
const pages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
</script>

<template>
  <nav v-if="total > 0" class="list-pager" aria-label="列表分页">
    <button :disabled="disabled || page <= 1" type="button" @click="emit('update:page', page - 1)">上一页</button>
    <span>第 {{ page }} / {{ pages }} 页 · 共 {{ total }} 条</span>
    <button :disabled="disabled || page >= pages" type="button" @click="emit('update:page', page + 1)">下一页</button>
  </nav>
</template>

<style scoped>
.list-pager { display: flex; align-items: center; justify-content: center; gap: 14px; margin-top: 16px; }
.list-pager button { min-width: 76px; min-height: 40px; border: 1.5px solid var(--color-border); border-radius: var(--radius-sm); background: #fff; color: var(--color-primary); font: inherit; font-size: 13px; font-weight: 800; }
.list-pager button:hover:not(:disabled) { border-color: var(--color-primary); background: var(--color-primary-soft); }
.list-pager button:disabled { cursor: not-allowed; opacity: .42; }
.list-pager span { color: var(--color-ink-3); font-size: 12px; font-variant-numeric: tabular-nums; }
@media (max-width: 480px) { .list-pager { gap: 8px; } .list-pager button { min-width: 68px; } }
</style>
