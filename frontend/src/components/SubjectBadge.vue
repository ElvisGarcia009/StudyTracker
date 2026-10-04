<script setup>
defineProps({
  subject: { type: Object, default: null },
})
</script>

<template>
  <span class="badge" :style="{ '--c': subject?.color || 'var(--text-faint)' }">
    <span class="swatch" aria-hidden="true" />
    <span class="name">{{ subject?.name || 'Materia eliminada' }}</span>
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  min-width: 0;
}

.swatch {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: var(--c);
  flex-shrink: 0;
}

.name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
