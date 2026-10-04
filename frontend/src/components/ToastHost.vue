<script setup>
import AppIcon from '@/components/AppIcon.vue'
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()

// Iconos de línea por tipo (el store trae glifos y emojis; aquí no se usan)
const ICONS = { success: 'check-circle', error: 'alert', achievement: 'goals', info: 'bell' }
</script>

<template>
  <div class="toast-host">
    <TransitionGroup name="toast">
      <div v-for="t in toast.toasts" :key="t.id" class="toast" :class="t.type" :role="t.type === 'error' ? 'alert' : 'status'">
        <span class="toast-icon">
          <AppIcon :name="ICONS[t.type] || 'bell'" :size="16" />
        </span>
        <div class="toast-text">
          <strong>{{ t.title }}</strong>
          <p v-if="t.message">{{ t.message }}</p>
        </div>
        <button class="icon-btn close" type="button" aria-label="Cerrar aviso" @click="toast.dismiss(t.id)">
          <AppIcon name="close" :size="14" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  right: 16px;
  bottom: max(16px, env(safe-area-inset-bottom));
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: min(360px, calc(100vw - 32px));
}

.toast {
  --c: var(--text-muted);
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 12px 8px 12px 14px;
  background: var(--surface-2);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-lg);
  box-shadow: inset 0 1px 0 var(--highlight), var(--shadow-pop);
}

.toast.success {
  --c: var(--success);
}

.toast.error {
  --c: var(--danger);
}

.toast.achievement {
  --c: var(--primary-text);
}

.toast-icon {
  display: grid;
  place-items: center;
  padding-top: 1px;
  color: var(--c);
  flex-shrink: 0;
}

.toast-text {
  flex: 1;
  min-width: 0;
  overflow-wrap: anywhere;
}

.toast-text strong {
  font-weight: 500;
}

.toast p {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 0.8125rem;
}

.close {
  width: 26px;
  height: 26px;
  margin-top: -3px;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.2s var(--ease),
    transform 0.2s var(--ease);
}
</style>
