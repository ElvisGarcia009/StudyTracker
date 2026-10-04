<script setup>
import { computed, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import { navItems } from '@/router'
import { useTimerStore } from '@/stores/timer'
import { useSubjectsStore } from '@/stores/subjects'
import { formatClock } from '@/utils/format'

const timer = useTimerStore()
const subjects = useSubjectsStore()

const theme = ref(document.documentElement.dataset.theme || 'dark')

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  document.documentElement.dataset.theme = theme.value
  try {
    localStorage.setItem('st-theme', theme.value)
  } catch {
    // almacenamiento no disponible: el tema solo dura esta visita
  }
  window.dispatchEvent(new Event('themechange'))
}

const activeSubject = computed(() => subjects.byId[timer.session?.subjectId])
const liveLabel = computed(() => (timer.phase === 'break' ? 'Descanso' : activeSubject.value?.name || 'Estudiando'))
</script>

<template>
  <aside class="sidebar">
    <RouterLink to="/" class="brand" aria-label="StudyTracker, ir al dashboard">
      <img src="/favicon.svg" alt="" width="34" height="34" />
      <span>StudyTracker</span>
    </RouterLink>

    <RouterLink
      v-if="timer.isActive"
      to="/timer"
      class="live-timer"
      :class="timer.phase"
      :title="timer.phase === 'paused' ? 'Sesión en pausa' : 'Sesión en curso'"
    >
      <AppIcon v-if="timer.phase === 'paused'" name="pause" :size="13" />
      <span v-else class="dot" />
      <span v-if="timer.phase === 'paused'" class="sr-only">En pausa:</span>
      <span class="live-name">{{ liveLabel }}</span>
      <span class="live-clock">{{ formatClock(timer.displaySeconds) }}</span>
    </RouterLink>

    <nav class="nav" aria-label="Secciones">
      <RouterLink v-for="item in navItems" :key="item.path" :to="item.path" class="nav-link">
        <AppIcon :name="item.name" :size="19" />
        <span class="nav-label">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <button class="theme-btn" type="button" @click="toggleTheme">
      <AppIcon :name="theme === 'dark' ? 'sun' : 'moon'" :size="17" />
      <span>{{ theme === 'dark' ? 'Tema claro' : 'Tema oscuro' }}</span>
    </button>
  </aside>
</template>

<style scoped>
.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 22px;
  padding: 26px 16px 20px;
  background: color-mix(in srgb, var(--glass) 70%, transparent);
  -webkit-backdrop-filter: blur(20px) saturate(140%);
  backdrop-filter: blur(20px) saturate(140%);
  border-right: 1px solid var(--border);
}

.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 8px;
  color: var(--text);
  text-decoration: none;
  font-family: var(--serif);
  font-size: 1.22rem;
}

.brand img {
  border-radius: 10px;
  box-shadow: 0 6px 22px -6px var(--primary-glow);
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.nav-link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  color: var(--text-muted);
  text-decoration: none;
  font-weight: 500;
  white-space: nowrap;
  transition: background 0.18s, color 0.18s;
}

.nav-link:hover {
  background: var(--surface-2);
  color: var(--text);
}

/* La sección actual queda "bajo la lámpara" */
.nav-link.router-link-exact-active {
  background: linear-gradient(90deg, var(--primary-soft), transparent 140%);
  color: var(--primary);
  font-weight: 600;
}

.nav-link.router-link-exact-active .icon {
  filter: drop-shadow(0 0 6px var(--primary-glow));
}

.live-timer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 13px;
  border-radius: 14px;
  background: var(--primary-soft);
  border: 1px solid color-mix(in srgb, var(--primary) 30%, transparent);
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
  font-size: 0.9rem;
  box-shadow: 0 8px 24px -14px var(--primary-glow);
}

.live-timer.break {
  background: var(--success-soft);
  border-color: color-mix(in srgb, var(--success) 30%, transparent);
  color: var(--success);
}

.live-timer.paused {
  background: var(--surface-2);
  border-color: var(--border);
  color: var(--text-muted);
  box-shadow: none;
}

.live-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.live-clock {
  font-family: var(--serif);
  font-size: 1.02rem;
  font-variant-numeric: tabular-nums;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 10px currentColor;
  animation: pulse 1.6s ease-in-out infinite;
  flex-shrink: 0;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

@keyframes pulse {
  50% {
    opacity: 0.3;
  }
}

.theme-btn {
  margin-top: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-muted);
  padding: 10px 13px;
  border-radius: 12px;
  font: inherit;
  font-weight: 500;
  cursor: pointer;
  text-align: left;
  transition: background 0.18s, color 0.18s;
}

.theme-btn:hover {
  background: var(--surface-2);
  color: var(--text);
}

/* En pantallas chicas la barra lateral pasa a ser una barra superior */
@media (max-width: 860px) {
  .sidebar {
    z-index: 20;
    min-width: 0;
    height: auto;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
    padding: 12px 16px 8px;
    border-right: none;
    border-bottom: 1px solid var(--border);
    background: var(--glass-strong);
  }

  .brand {
    padding: 0;
    flex: 1;
    font-size: 1.12rem;
  }

  .brand img {
    width: 30px;
    height: 30px;
  }

  .theme-btn {
    margin: 0;
    padding: 7px 11px;
    font-size: 0.85rem;
  }

  .live-timer {
    order: 3;
    width: 100%;
  }

  .nav {
    order: 4;
    width: 100%;
    min-width: 0;
    flex-direction: row;
    overflow-x: auto;
    scrollbar-width: none;
    gap: 2px;
  }

  .nav-link {
    padding: 8px 11px;
    gap: 7px;
    font-size: 0.88rem;
  }

  .nav-link.router-link-exact-active {
    background: var(--primary-soft);
  }
}
</style>
