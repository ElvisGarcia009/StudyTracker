<script setup>
defineProps({
  subject: { type: Object, default: null },
})
</script>

<template>
  <span class="badge" :style="{ '--c': subject?.color || '#9aa0b3' }">
    <span class="swatch" />
    {{ subject?.name || 'Materia eliminada' }}
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-weight: 600;
  font-size: 0.9rem;
  min-width: 0;
}

.swatch {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  background: var(--c);
  flex-shrink: 0;
}
</style>
