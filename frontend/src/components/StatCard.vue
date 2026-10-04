<script setup>
/** Una cifra del resumen. Vive dentro de un panel compartido, no en una tarjeta propia. */
defineProps({
  icon: String,
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
    <div v-if="progress !== null" class="bar" role="progressbar" :aria-valuenow="Math.round(progress)" aria-valuemin="0" aria-valuemax="100">
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
  font-size: 0.9rem;
  font-weight: 500;
}

.stat-value {
  font-family: var(--serif);
  font-size: 2rem;
  line-height: 1.1;
  letter-spacing: -0.01em;
  font-variant-numeric: tabular-nums;
}

.stat-hint {
  color: var(--text-muted);
  font-size: 0.84rem;
}

.bar {
  height: 4px;
  margin: 4px 0 2px;
  background: var(--recess);
  border-radius: 99px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: var(--primary);
  border-radius: 99px;
  box-shadow: 0 0 10px var(--primary-glow);
  transition: width 0.5s ease;
}
</style>
