<script setup>
defineProps({
  /** 0 a 100 */
  value: { type: Number, required: true },
  color: { type: String, default: 'var(--primary)' },
  height: { type: Number, default: 6 },
  /** Nombre accesible de la barra */
  label: { type: String, default: 'Avance' },
})
</script>

<template>
  <div
    class="track"
    :style="{ height: height + 'px' }"
    role="progressbar"
    :aria-label="label"
    :aria-valuenow="Math.round(Math.min(100, Math.max(0, value)))"
    aria-valuemin="0"
    aria-valuemax="100"
  >
    <div class="fill" :style="{ width: Math.min(100, Math.max(0, value)) + '%', background: color }" />
  </div>
</template>

<style scoped>
.track {
  width: 100%;
  background: var(--surface-3);
  border-radius: var(--radius-pill);
  overflow: hidden;
}

.fill {
  height: 100%;
  border-radius: var(--radius-pill);
  transition: width 0.5s var(--ease);
}
</style>
