<script>
// Pila de modales abiertos: solo el de arriba responde a Escape y atrapa el foco
const stack = []
</script>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'

defineProps({
  title: { type: String, required: true },
  width: { type: String, default: '480px' },
  role: { type: String, default: 'dialog' },
})
const emit = defineEmits(['close'])

const dialog = ref(null)
const titleId = `modal-title-${Math.random().toString(36).slice(2, 8)}`
const token = Symbol('modal')
let returnFocus = null

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

function focusables() {
  return [...(dialog.value?.querySelectorAll(FOCUSABLE) ?? [])].filter((el) => el.offsetParent !== null || el === document.activeElement)
}

function onKey(e) {
  if (stack[stack.length - 1] !== token) return
  if (e.key === 'Escape') {
    e.preventDefault()
    emit('close')
    return
  }
  if (e.key !== 'Tab') return
  const items = focusables()
  if (!items.length) return
  const first = items[0]
  const last = items[items.length - 1]
  if (e.shiftKey && document.activeElement === first) {
    e.preventDefault()
    last.focus()
  } else if (!e.shiftKey && document.activeElement === last) {
    e.preventDefault()
    first.focus()
  } else if (!dialog.value.contains(document.activeElement)) {
    e.preventDefault()
    first.focus()
  }
}

onMounted(async () => {
  returnFocus = document.activeElement
  stack.push(token)
  window.addEventListener('keydown', onKey)
  await nextTick()
  if (dialog.value.contains(document.activeElement)) return // p. ej. un campo con autofocus
  const preferred = dialog.value.querySelector('[data-autofocus], [autofocus]')
  const firstField = dialog.value.querySelector('.body :is(input, select, textarea):not([disabled])')
  ;(preferred || firstField || focusables()[0] || dialog.value).focus()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKey)
  const i = stack.indexOf(token)
  if (i !== -1) stack.splice(i, 1)
  if (returnFocus && document.contains(returnFocus)) returnFocus.focus()
})
</script>

<template>
  <Teleport to="body">
    <div class="backdrop" @mousedown.self="emit('close')">
      <div
        ref="dialog"
        class="modal"
        :style="{ maxWidth: width }"
        :role="role"
        aria-modal="true"
        :aria-labelledby="titleId"
        tabindex="-1"
      >
        <header>
          <h2 :id="titleId">{{ title }}</h2>
          <button class="icon-btn" type="button" aria-label="Cerrar" @click="emit('close')">
            <AppIcon name="close" :size="16" />
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
  z-index: 50;
  display: grid;
  place-items: center;
  padding: 16px;
  background: var(--overlay);
  animation: fade 0.15s var(--ease);
}

.modal {
  width: 100%;
  max-height: calc(100dvh - 32px);
  overflow-y: auto;
  overscroll-behavior: contain;
  background: var(--surface-2);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-lg);
  box-shadow: inset 0 1px 0 var(--highlight), var(--shadow-pop);
  animation: rise 0.18s var(--ease);
}

.modal:focus {
  outline: none;
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 16px 0 24px;
}

header h2 {
  font-size: 1.0625rem;
}

.body {
  padding: 16px 24px 20px;
}

footer {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  padding: 14px 24px;
  border-top: 1px solid var(--border);
}

@keyframes fade {
  from {
    opacity: 0;
  }
}

@keyframes rise {
  from {
    transform: translateY(8px) scale(0.985);
    opacity: 0;
  }
}
</style>
