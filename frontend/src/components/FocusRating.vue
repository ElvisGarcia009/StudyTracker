<script setup>
import AppIcon from '@/components/AppIcon.vue'

/** Calificación de concentración de 1 a 5. Si readonly es false, se puede hacer clic. */
const model = defineModel({ type: Number, default: null })
defineProps({ readonly: { type: Boolean, default: false } })

const labels = ['Muy distraído', 'Distraído', 'Normal', 'Concentrado', 'Súper enfocado']
</script>

<template>
  <!-- Solo lectura: un único elemento con su descripción, no cinco botones deshabilitados -->
  <span
    v-if="readonly"
    class="rating readonly"
    role="img"
    :aria-label="model ? `Concentración: ${model} de 5, ${labels[model - 1].toLowerCase()}` : 'Sin calificar'"
    :title="model ? `Concentración: ${labels[model - 1]}` : undefined"
  >
    <AppIcon v-for="n in 5" :key="n" name="star" :size="12" :filled="!!model && n <= model" :class="{ on: model && n <= model }" />
  </span>

  <span v-else class="rating" role="group" aria-label="Concentración">
    <button
      v-for="n in 5"
      :key="n"
      type="button"
      class="star"
      :class="{ on: model && n <= model }"
      :title="labels[n - 1]"
      :aria-label="`${n} de 5: ${labels[n - 1]}`"
      :aria-pressed="model === n"
      @click="model = model === n ? null : n"
    >
      <AppIcon name="star" :size="22" :filled="!!model && n <= model" />
    </button>
    <span class="label" aria-live="polite">{{ model ? labels[model - 1] : '' }}</span>
  </span>
</template>

<style scoped>
.rating {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.star {
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-md);
  background: none;
  color: var(--border-tertiary);
  cursor: pointer;
  transition:
    background-color 0.15s var(--ease),
    color 0.15s var(--ease);
}

.star:hover {
  background: var(--surface-3);
  color: var(--text-muted);
}

.star.on {
  color: var(--primary-text);
}

.readonly {
  gap: 1px;
  color: var(--border-tertiary);
}

.readonly .on {
  color: var(--text-muted);
}

.label {
  margin-left: 8px;
  color: var(--text-muted);
  font-size: 0.8125rem;
}
</style>
