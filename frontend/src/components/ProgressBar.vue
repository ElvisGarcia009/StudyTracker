<script setup>
defineProps({
  /** 0 a 100 */
  value: { type: Number, required: true },
  color: { type: String, default: 'var(--primary)' },
  height: { type: Number, default: 8 },
})
</script>

<template>
  <div class="track" :style="{ height: height + 'px' }" role="progressbar" :aria-valuenow="Math.round(value)" aria-valuemin="0" aria-valuemax="100">
    <div class="fill" :style="{ width: Math.min(100, Math.max(0, value)) + '%', background: color }" />
  </div>
</template>

<style scoped>
.track {
  background: var(--surface-2);
  border-radius: 99px;
  overflow: hidden;
  width: 100%;
}

.fill {
  height: 100%;
  border-radius: 99px;
  transition: width 0.4s ease;
}
</style>
