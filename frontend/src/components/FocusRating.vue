<script setup>
/** Calificación de concentración de 1 a 5. Si readonly es false, se puede hacer clic. */
const model = defineModel({ type: Number, default: null })
defineProps({ readonly: { type: Boolean, default: false } })

const labels = ['Muy distraído', 'Distraído', 'Normal', 'Concentrado', 'Súper enfocado']
</script>

<template>
  <span class="rating" :class="{ readonly }">
    <button
      v-for="n in 5"
      :key="n"
      type="button"
      class="star"
      :class="{ on: model && n <= model }"
      :disabled="readonly"
      :title="labels[n - 1]"
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
  font-size: 1.5rem;
  line-height: 1;
  color: var(--border);
  cursor: pointer;
  transition: transform 0.1s;
}

.star:hover:not(:disabled) {
  transform: scale(1.15);
}

.star.on {
  color: #f5b301;
}

.readonly .star {
  font-size: 0.95rem;
  cursor: default;
}

.label {
  margin-left: 8px;
  color: var(--text-muted);
  font-size: 0.88rem;
}
</style>
