import { reactive } from 'vue'

/** Estado del diálogo de confirmación compartido (lo pinta ConfirmHost). */
export const confirmState = reactive({
  open: false,
  title: '',
  message: '',
  confirmLabel: 'Confirmar',
  cancelLabel: 'Cancelar',
  danger: false,
  resolve: null,
})

/**
 * Sustituto de window.confirm() con el estilo de la app.
 * Devuelve una promesa que se resuelve en true (confirmar) o false (cancelar).
 */
export function confirmDialog(options) {
  return new Promise((resolve) => {
    // Si había otro diálogo abierto, cuenta como cancelado
    confirmState.resolve?.(false)
    Object.assign(confirmState, {
      title: '¿Seguro?',
      message: '',
      confirmLabel: 'Confirmar',
      cancelLabel: 'Cancelar',
      danger: false,
      ...options,
      open: true,
      resolve,
    })
  })
}

export function settleConfirm(value) {
  const resolve = confirmState.resolve
  confirmState.open = false
  confirmState.resolve = null
  resolve?.(value)
}
