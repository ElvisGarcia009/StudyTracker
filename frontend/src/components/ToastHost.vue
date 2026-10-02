<script setup>
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
</script>

<template>
  <div class="toast-host" aria-live="polite">
    <TransitionGroup name="toast">
      <div v-for="t in toast.toasts" :key="t.id" class="toast" :class="t.type" @click="toast.dismiss(t.id)">
        <span class="toast-icon">{{ t.icon }}</span>
        <div>
          <strong>{{ t.title }}</strong>
          <p v-if="t.message">{{ t.message }}</p>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  right: 16px;
  bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 100;
  max-width: min(380px, calc(100vw - 32px));
}

.toast {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px 16px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-left: 4px solid var(--primary);
  border-radius: 12px;
  box-shadow: var(--shadow-lg);
  cursor: pointer;
}

.toast.success {
  border-left-color: var(--success);
}

.toast.error {
  border-left-color: var(--danger);
}

.toast.achievement {
  border-left-color: var(--warning);
}

.toast-icon {
  font-size: 1.3rem;
  line-height: 1.2;
}

.toast.success .toast-icon {
  color: var(--success);
}

.toast.error .toast-icon {
  color: var(--danger);
  font-weight: 700;
}

.toast p {
  margin: 2px 0 0;
  color: var(--text-muted);
  font-size: 0.88rem;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
</style>
