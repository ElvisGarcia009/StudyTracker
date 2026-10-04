<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import AppIcon from '@/components/AppIcon.vue'

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
          <button class="icon-btn" type="button" aria-label="Cerrar" @click="emit('close')">
            <AppIcon name="close" :size="18" />
          </button>
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
  background: color-mix(in srgb, var(--scene-bottom) 60%, transparent);
  -webkit-backdrop-filter: blur(6px);
  backdrop-filter: blur(6px);
  display: grid;
  place-items: center;
  padding: 16px;
  z-index: 50;
  animation: fade 0.18s ease;
}

.modal {
  width: 100%;
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%, var(--primary-soft), transparent 70%),
    var(--glass-strong);
  -webkit-backdrop-filter: blur(24px) saturate(140%);
  backdrop-filter: blur(24px) saturate(140%);
  border: 1px solid var(--border-strong);
  border-radius: 24px;
  box-shadow: var(--shadow-lg);
  animation: rise 0.22s cubic-bezier(0.2, 0.8, 0.3, 1);
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 22px 22px 0 24px;
}

header h2 {
  font-size: 1.4rem;
}

.body {
  padding: 20px 24px;
}

footer {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 24px 22px;
}

@keyframes fade {
  from {
    opacity: 0;
  }
}

@keyframes rise {
  from {
    transform: translateY(14px) scale(0.98);
    opacity: 0;
  }
}
</style>
