<script setup>
import BaseModal from '@/components/BaseModal.vue'
import { confirmState, settleConfirm } from '@/utils/confirm'
</script>

<template>
  <BaseModal v-if="confirmState.open" :title="confirmState.title" width="420px" role="alertdialog" @close="settleConfirm(false)">
    <p v-if="confirmState.message" class="message">{{ confirmState.message }}</p>
    <template #footer>
      <!-- El foco empieza en Cancelar: la opción segura -->
      <button class="btn" type="button" data-autofocus @click="settleConfirm(false)">{{ confirmState.cancelLabel }}</button>
      <button class="btn" :class="confirmState.danger ? 'btn-danger-solid' : 'btn-primary'" type="button" @click="settleConfirm(true)">
        {{ confirmState.confirmLabel }}
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.message {
  color: var(--text-secondary);
  white-space: pre-line;
}
</style>
