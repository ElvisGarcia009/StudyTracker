<script setup>
defineProps({
  subject: { type: Object, default: null },
})
</script>

<template>
  <span class="badge" :style="{ '--c': subject?.color || '#9aa0b3' }">
    <span class="swatch" />
    <span class="name">{{ subject?.name || 'Materia eliminada' }}</span>
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 0.92rem;
  min-width: 0;
}

/* Un pequeño lomo de libro con el color de la materia */
.swatch {
  width: 5px;
  height: 15px;
  border-radius: 2px;
  background: var(--c);
  box-shadow: 0 0 8px -1px var(--c);
  flex-shrink: 0;
}

.name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
