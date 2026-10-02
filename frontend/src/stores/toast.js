import { defineStore } from 'pinia'
import { ref } from 'vue'

let nextId = 1

/** Mensajes flotantes: éxito, error y logros desbloqueados. */
export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])

  function push({ type = 'info', title, message = '', icon = '', duration = 3500 }) {
    const id = nextId++
    toasts.value.push({ id, type, title, message, icon })
    setTimeout(() => dismiss(id), duration)
  }

  function dismiss(id) {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }

  const success = (title, message) => push({ type: 'success', title, message, icon: '✓' })
  const error = (title, message) => push({ type: 'error', title, message, icon: '!', duration: 5000 })

  function achievements(list = []) {
    list.forEach((a) =>
      push({ type: 'achievement', title: `¡Logro desbloqueado! ${a.name}`, message: a.description, icon: a.icon, duration: 7000 }),
    )
  }

  return { toasts, push, dismiss, success, error, achievements }
})
