<script setup>
import { computed, ref } from 'vue'
import { navItems } from '@/router'
import { useTimerStore } from '@/stores/timer'
import { useSubjectsStore } from '@/stores/subjects'
import { formatClock } from '@/utils/format'

const timer = useTimerStore()
const subjects = useSubjectsStore()

const theme = ref(document.documentElement.dataset.theme || 'light')

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
</script>

<template>
  <aside class="sidebar">
    <div class="brand">
      <img src="/favicon.svg" alt="" width="30" height="30" />
      <span>StudyTracker</span>
    </div>

    <RouterLink v-if="timer.isActive" to="/timer" class="live-timer" :class="timer.phase">
      <span class="dot" />
      <span class="live-name">{{ timer.phase === 'break' ? 'Descanso' : activeSubject?.name || 'Estudiando' }}</span>
      <span class="live-clock">{{ formatClock(timer.displaySeconds) }}</span>
    </RouterLink>

    <nav class="nav">
      <RouterLink v-for="item in navItems" :key="item.path" :to="item.path" class="nav-link">
        <span class="nav-icon">{{ item.icon }}</span>
        <span class="nav-label">{{ item.label }}</span>
      </RouterLink>
    </nav>

    <button class="theme-btn" type="button" @click="toggleTheme">
      {{ theme === 'dark' ? '☀️ Tema claro' : '🌙 Tema oscuro' }}
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
  gap: 18px;
  padding: 22px 14px;
  background: var(--surface);
  border-right: 1px solid var(--border);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 1.1rem;
  padding: 0 8px;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  color: var(--text-muted);
  text-decoration: none;
  font-weight: 500;
  white-space: nowrap;
}

.nav-link:hover {
  background: var(--surface-2);
  color: var(--text);
}

.nav-link.router-link-exact-active {
  background: var(--primary-soft);
  color: var(--primary);
}

.nav-icon {
  font-size: 1.1rem;
  width: 22px;
  text-align: center;
}

.live-timer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--primary-soft);
  color: var(--primary);
  text-decoration: none;
  font-weight: 600;
  font-size: 0.9rem;
}

.live-timer.break {
  background: var(--success-soft);
  color: var(--success);
}

.live-timer.paused {
  background: var(--warning-soft);
  color: var(--warning);
}

.live-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.live-clock {
  font-family: var(--mono);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 1.4s ease-in-out infinite;
}

.paused .dot {
  animation: none;
}

@keyframes pulse {
  50% {
    opacity: 0.25;
  }
}

.theme-btn {
  margin-top: auto;
  border: 1px solid var(--border);
  background: transparent;
  color: var(--text-muted);
  padding: 9px 12px;
  border-radius: 10px;
  font: inherit;
  cursor: pointer;
  text-align: left;
}

.theme-btn:hover {
  background: var(--surface-2);
  color: var(--text);
}

/* En pantallas chicas la barra lateral pasa a ser una barra superior */
@media (max-width: 860px) {
  .sidebar {
    position: sticky;
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
  }

  .brand {
    padding: 0;
    flex: 1;
  }

  .theme-btn {
    margin: 0;
    padding: 6px 10px;
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
  }

  .nav-link {
    padding: 8px 10px;
    gap: 6px;
    font-size: 0.88rem;
  }
}
</style>
