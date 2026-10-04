<script setup>
import { onMounted } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
import ConfirmHost from '@/components/ConfirmHost.vue'
import ToastHost from '@/components/ToastHost.vue'
import { useSubjectsStore } from '@/stores/subjects'
import { useTimerStore } from '@/stores/timer'
import { useToastStore } from '@/stores/toast'

const subjects = useSubjectsStore()
const timer = useTimerStore()
const toast = useToastStore()

onMounted(async () => {
  timer.startTicking()
  try {
    await Promise.all([subjects.load(), timer.load()])
  } catch (e) {
    toast.error('No se pudo cargar la información', e.message)
  }
})
</script>

<template>
  <a href="#main" class="skip-link">Saltar al contenido</a>
  <div class="layout">
    <AppSidebar />
    <main id="main" class="content" tabindex="-1">
      <RouterView />
    </main>
    <ToastHost />
    <ConfirmHost />
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  min-height: 100dvh;
}

.content {
  width: 100%;
  max-width: 1280px;
  margin-inline: auto;
  min-width: 0;
  padding: 36px clamp(16px, 3vw, 48px) 72px;
  padding-bottom: calc(72px + env(safe-area-inset-bottom));
}

.content:focus {
  outline: none;
}

@media (max-width: 860px) {
  .layout {
    /* minmax(0, …) evita que la barra superior ensanche la página */
    grid-template-columns: minmax(0, 1fr);
    /* La barra superior no debe estirarse cuando la página es corta */
    grid-template-rows: auto 1fr;
  }

  .content {
    padding: 20px 16px 48px;
    padding-inline: max(16px, env(safe-area-inset-left)) max(16px, env(safe-area-inset-right));
  }
}
</style>
