<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { navItems } from '@/router'
import { useTimerStore } from '@/stores/timer'
import { useSubjectsStore } from '@/stores/subjects'
import { formatClock } from '@/utils/format'

const timer = useTimerStore()
const subjects = useSubjectsStore()
const route = useRoute()

const THEME_COLORS = { dark: '#010102', light: '#f4f5f5' }
const theme = ref(document.documentElement.dataset.theme || 'dark')

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  document.documentElement.dataset.theme = theme.value
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', THEME_COLORS[theme.value])
  try {
    localStorage.setItem('st-theme', theme.value)
  } catch {
    // almacenamiento no disponible: el tema solo dura esta visita
  }
  window.dispatchEvent(new Event('themechange'))
}

const activeSubject = computed(() => subjects.byId[timer.session?.subjectId])
const liveLabel = computed(() => (timer.phase === 'break' ? 'Descanso' : activeSubject.value?.name || 'Estudiando'))

// Menú desplegable en pantallas chicas: se cierra al navegar
const menuOpen = ref(false)
watch(() => route.fullPath, () => (menuOpen.value = false))
const currentLabel = computed(() => navItems.find((i) => i.path === route.path)?.label ?? 'Menú')
</script>

<template>
  <aside class="sidebar" :class="{ open: menuOpen }" @keydown.esc="menuOpen = false">
    <div class="top">
      <RouterLink to="/" class="brand" aria-label="StudyTracker, ir al dashboard">
        <img src="/favicon.svg" alt="" width="22" height="22" />
        <span translate="no">StudyTracker</span>
      </RouterLink>
      <button
        class="icon-btn menu-btn"
        type="button"
        :aria-expanded="menuOpen"
        aria-controls="main-nav"
        :aria-label="menuOpen ? 'Cerrar menú' : `Abrir menú (${currentLabel})`"
        @click="menuOpen = !menuOpen"
      >
        <AppIcon :name="menuOpen ? 'close' : 'menu'" :size="18" />
      </button>
    </div>

    <RouterLink
      v-if="timer.isActive"
      to="/timer"
      class="live-timer"
      :class="timer.phase"
      :title="timer.phase === 'paused' ? 'Sesión en pausa' : 'Sesión en curso'"
    >
      <AppIcon v-if="timer.phase === 'paused'" name="pause" :size="12" />
      <span v-else class="dot" aria-hidden="true" />
      <span v-if="timer.phase === 'paused'" class="sr-only">En pausa:</span>
      <span class="live-name">{{ liveLabel }}</span>
      <span class="live-clock mono">{{ formatClock(timer.displaySeconds) }}</span>
    </RouterLink>

    <nav id="main-nav" class="nav" aria-label="Secciones">
      <RouterLink v-for="item in navItems" :key="item.path" :to="item.path" class="nav-link">
        <AppIcon :name="item.name" :size="16" />
        <span>{{ item.label }}</span>
      </RouterLink>
    </nav>

    <button class="theme-btn" type="button" @click="toggleTheme">
      <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" :size="16" />
      <span>{{ theme === 'dark' ? 'Tema claro' : 'Tema oscuro' }}</span>
    </button>
  </aside>
</template>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  height: 100dvh;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px 12px 14px;
  border-right: 1px solid var(--border);
  background: var(--bg);
}

.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  color: var(--text);
  text-decoration: none;
  font-size: 0.9375rem;
  font-weight: 600;
  letter-spacing: -0.2px;
}

.brand img {
  border-radius: 6px;
}

.menu-btn {
  display: none;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 32px;
  padding: 6px 10px;
  border-radius: var(--radius-md);
  color: var(--text-muted);
  text-decoration: none;
  font-weight: 500;
  white-space: nowrap;
  transition:
    background-color 0.15s var(--ease),
    color 0.15s var(--ease);
}

.nav-link:hover {
  background: var(--surface-2);
  color: var(--text);
}

.nav-link.router-link-exact-active {
  background: var(--surface-3);
  color: var(--text);
}

.nav-link.router-link-exact-active .icon {
  color: var(--primary-text);
}

.live-timer {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 7px 10px;
  border-radius: var(--radius-md);
  background: var(--surface);
  border: 1px solid var(--border);
  color: var(--text);
  text-decoration: none;
  font-weight: 500;
  font-size: 0.8125rem;
  transition: border-color 0.15s var(--ease);
}

.live-timer:hover {
  border-color: var(--border-strong);
}

.live-timer.paused {
  color: var(--text-muted);
}

.live-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.live-clock {
  font-size: 0.8125rem;
  color: var(--text-secondary);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--primary);
  flex-shrink: 0;
  animation: pulse 2s ease-in-out infinite;
}

.break .dot {
  background: var(--success);
}

@keyframes pulse {
  50% {
    opacity: 0.35;
  }
}

.theme-btn {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 32px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  padding: 6px 10px;
  border-radius: var(--radius-md);
  font: inherit;
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.15s var(--ease),
    color 0.15s var(--ease);
}

.theme-btn:hover {
  background: var(--surface-2);
  color: var(--text);
}

/* En pantallas chicas: barra superior fija con menú desplegable */
@media (max-width: 860px) {
  .sidebar {
    z-index: 20;
    height: auto;
    min-width: 0;
    gap: 8px;
    padding: 10px 12px;
    padding-top: max(10px, env(safe-area-inset-top));
    border-right: none;
    border-bottom: 1px solid var(--border);
    background: color-mix(in srgb, var(--bg) 92%, transparent);
    -webkit-backdrop-filter: blur(12px);
    backdrop-filter: blur(12px);
  }

  .menu-btn {
    display: inline-grid;
  }

  .nav,
  .theme-btn {
    display: none;
  }

  .open .nav,
  .open .theme-btn {
    display: flex;
  }

  .open .nav {
    padding-top: 4px;
    border-top: 1px solid var(--border);
  }

  .nav-link,
  .theme-btn {
    min-height: 44px;
  }

  .open .theme-btn {
    margin-top: 0;
    border-top: 1px solid var(--border);
    border-radius: 0;
    padding-top: 10px;
  }
}
</style>
