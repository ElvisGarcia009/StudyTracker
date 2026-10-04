<script setup>
/** Una cifra del resumen. Vive dentro de un panel compartido, no en una tarjeta propia. */
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  hint: String,
  /** Avance de 0 a 100; si se pasa, se dibuja una barra. */
  progress: { type: Number, default: null },
})
</script>

<template>
  <div class="stat">
    <span class="stat-label">{{ label }}</span>
    <span class="stat-value">{{ value }}</span>
    <div
      v-if="progress !== null"
      class="bar"
      role="progressbar"
      :aria-label="`${label}: avance`"
      :aria-valuenow="Math.round(Math.min(100, progress))"
      aria-valuemin="0"
      aria-valuemax="100"
    >
      <div class="bar-fill" :style="{ width: Math.min(100, progress) + '%' }" />
    </div>
    <span v-if="hint" class="stat-hint">{{ hint }}</span>
  </div>
</template>

<style scoped>
.stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.stat-label {
  color: var(--text-muted);
  font-size: 0.8125rem;
  font-weight: 500;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 600;
  line-height: 1.15;
  letter-spacing: -0.6px;
  font-variant-numeric: tabular-nums;
}

.stat-hint {
  color: var(--text-muted);
  font-size: 0.8125rem;
}

.bar {
  height: 4px;
  margin: 2px 0;
  background: var(--surface-3);
  border-radius: var(--radius-pill);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: var(--primary);
  border-radius: var(--radius-pill);
  transition: width 0.5s var(--ease);
}
</style>
