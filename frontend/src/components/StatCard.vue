<script setup>
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
  <div class="card stat">
    <div class="stat-top">
      <span class="stat-label">{{ label }}</span>
      <span class="stat-icon">{{ icon }}</span>
    </div>
    <div class="stat-value">{{ value }}</div>
    <div v-if="progress !== null" class="bar">
      <div class="bar-fill" :style="{ width: Math.min(100, progress) + '%' }" />
    </div>
    <div v-if="hint" class="stat-hint">{{ hint }}</div>
  </div>
</template>

<style scoped>
.stat {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  color: var(--text-muted);
  font-size: 0.88rem;
  font-weight: 500;
}

.stat-icon {
  font-size: 1.3rem;
}

.stat-value {
  font-size: 1.7rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.stat-hint {
  color: var(--text-muted);
  font-size: 0.84rem;
}

.bar {
  height: 6px;
  background: var(--surface-2);
  border-radius: 99px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: var(--primary);
  border-radius: 99px;
  transition: width 0.4s ease;
}
</style>
