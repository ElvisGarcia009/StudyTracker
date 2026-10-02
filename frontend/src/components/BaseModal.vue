<script setup>
import { onBeforeUnmount, onMounted } from 'vue'

defineProps({
  title: { type: String, required: true },
  width: { type: String, default: '480px' },
})
const emit = defineEmits(['close'])

function onKey(e) {
  if (e.key === 'Escape') emit('close')
}
onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div class="backdrop" @mousedown.self="emit('close')">
      <div class="modal" :style="{ maxWidth: width }" role="dialog" aria-modal="true" :aria-label="title">
        <header>
          <h2>{{ title }}</h2>
          <button class="icon-btn" type="button" aria-label="Cerrar" @click="emit('close')">✕</button>
        </header>
        <div class="body">
          <slot />
        </div>
        <footer v-if="$slots.footer">
          <slot name="footer" />
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background: rgba(10, 12, 20, 0.5);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 50;
  animation: fade 0.15s ease;
}

.modal {
  width: 100%;
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  animation: rise 0.18s ease;
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 0;
}

.body {
  padding: 18px 20px;
}

footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 0 20px 18px;
}

@keyframes fade {
  from {
    opacity: 0;
  }
}

@keyframes rise {
  from {
    transform: translateY(12px);
    opacity: 0;
  }
}
</style>
