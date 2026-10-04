<script setup>
/** Calificación de concentración de 1 a 5. Si readonly es false, se puede hacer clic. */
const model = defineModel({ type: Number, default: null })
defineProps({ readonly: { type: Boolean, default: false } })

const labels = ['Muy distraído', 'Distraído', 'Normal', 'Concentrado', 'Súper enfocado']
</script>

<template>
  <span class="rating" :class="{ readonly }" :title="readonly && model ? `Concentración: ${labels[model - 1]}` : undefined">
    <button
      v-for="n in 5"
      :key="n"
      type="button"
      class="star"
      :class="{ on: model && n <= model }"
      :disabled="readonly"
      :title="readonly ? undefined : labels[n - 1]"
      :aria-label="`${n} de 5: ${labels[n - 1]}`"
      @click="model = model === n ? null : n"
    >
      ★
    </button>
    <span v-if="!readonly && model" class="label">{{ labels[model - 1] }}</span>
  </span>
</template>

<style scoped>
.rating {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.star {
  border: none;
  background: none;
  padding: 0 1px;
  font-size: 1.55rem;
  line-height: 1;
  color: var(--tick-off);
  cursor: pointer;
  transition: transform 0.12s, color 0.15s, text-shadow 0.2s;
}

.star:hover:not(:disabled) {
  transform: scale(1.15);
}

.star.on {
  color: var(--primary);
  text-shadow: 0 0 12px var(--primary-glow);
}

.readonly .star {
  font-size: 0.92rem;
  cursor: default;
  text-shadow: none;
}

.label {
  margin-left: 10px;
  color: var(--text-muted);
  font-size: 0.88rem;
}
</style>
