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
    <div class="fill" :style="{ width: Math.min(100, Math.max(0, value)) + '%', '--c': color }" />
  </div>
</template>

<style scoped>
.track {
  background: var(--recess);
  box-shadow: inset 0 0 0 1px var(--border);
  border-radius: 99px;
  overflow: hidden;
  width: 100%;
}

/* La barra se ilumina hacia su extremo, como un filamento */
.fill {
  height: 100%;
  border-radius: 99px;
  background: linear-gradient(90deg, color-mix(in srgb, var(--c) 55%, transparent), var(--c));
  box-shadow: 0 0 12px -2px var(--c);
  transition: width 0.5s ease;
}
</style>
