<script setup>
import { useToastStore } from '@/stores/toast'

const toast = useToastStore()
</script>

<template>
  <div class="toast-host" aria-live="polite">
    <TransitionGroup name="toast">
      <div v-for="t in toast.toasts" :key="t.id" class="toast" :class="t.type" @click="toast.dismiss(t.id)">
        <span class="toast-icon">{{ t.icon }}</span>
        <div class="toast-text">
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
  --c: var(--primary);
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 13px 16px 13px 14px;
  background: var(--glass-strong);
  -webkit-backdrop-filter: blur(20px);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-strong);
  border-radius: 16px;
  box-shadow: var(--shadow-lg);
  cursor: pointer;
}

.toast.success {
  --c: var(--success);
}

.toast.error {
  --c: var(--danger);
}

.toast-icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: 50%;
  background: color-mix(in srgb, var(--c) 16%, transparent);
  color: var(--c);
  font-weight: 700;
  font-size: 0.95rem;
}

/* Un logro llega con la luz de la lámpara */
.toast.achievement {
  border-color: color-mix(in srgb, var(--primary) 45%, transparent);
  box-shadow: var(--shadow-lg), 0 0 40px -12px var(--primary-glow);
}

.toast.achievement .toast-icon {
  width: 36px;
  height: 36px;
  font-size: 1.3rem;
}

.toast-text {
  min-width: 0;
}

.toast p {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 0.88rem;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
</style>
