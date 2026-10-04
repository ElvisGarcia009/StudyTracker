<script setup>
import { onMounted } from 'vue'
import AppSidebar from '@/components/AppSidebar.vue'
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
  <div class="layout">
    <AppSidebar />
    <main class="content">
      <RouterView />
    </main>
    <ToastHost />
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr);
  min-height: 100vh;
}

.content {
  padding: 40px clamp(16px, 3.5vw, 64px) 64px;
  /* Ocupa todo el ancho disponible; solo en monitores ultra anchos se centra */
  max-width: 2000px;
  margin-inline: auto;
  width: 100%;
  min-width: 0;
  /* el halo de la lámpara puede derramarse, pero nunca crear scroll horizontal */
  overflow-x: clip;
}

@media (max-width: 860px) {
  .layout {
    /* minmax(0, …) evita que la fila de navegación ensanche la página */
    grid-template-columns: minmax(0, 1fr);
  }

  .content {
    padding: 22px 16px 48px;
  }
}
</style>
