<script setup>
import { computed, onMounted, ref } from 'vue'
import { Bar, Doughnut } from 'vue-chartjs'
import AppIcon from '@/components/AppIcon.vue'
import StatCard from '@/components/StatCard.vue'
import HeatMap from '@/components/HeatMap.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import { statsApi } from '@/api'
import { useSubjectsStore } from '@/stores/subjects'
import { useToastStore } from '@/stores/toast'
import { baseOptions, cssVar, useThemeVersion } from '@/utils/chart'
import { formatDate, formatHours, formatMinutes, parseLocalDate } from '@/utils/format'

const subjects = useSubjectsStore()
const toast = useToastStore()
const themeVersion = useThemeVersion()

const stats = ref(null)
const heatmap = ref([])
const loading = ref(true)

onMounted(async () => {
  try {
    ;[stats.value, heatmap.value] = await Promise.all([statsApi.dashboard(), statsApi.heatmap(365)])
  } catch (e) {
    toast.error('No se pudieron cargar las estadísticas', e.message)
  } finally {
    loading.value = false
  }
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return 'Buenos días'
  if (h < 19) return 'Buenas tardes'
  return 'Buenas noches'
})

const pct = (value, total) => (total > 0 ? (value / total) * 100 : null)

// La meta de la semana es la suma de metas por materia; si no hay, usamos lo planificado
const weekTarget = computed(() => stats.value?.weekGoalMinutes || stats.value?.weekPlannedMinutes || 0)

const weekChart = computed(() => {
  themeVersion.value // dependencia para redibujar al cambiar el tema
  const days = stats.value?.last7Days ?? []
  return {
    data: {
      labels: days.map((d) => formatDate(d.date, { weekday: 'short' }).replace('.', '')),
      datasets: [
        {
          label: 'Estudiado',
          data: days.map((d) => +(d.minutes / 60).toFixed(2)),
          backgroundColor: cssVar('--primary'),
          borderRadius: 6,
          maxBarThickness: 34,
        },
        {
          label: 'Planificado',
          data: days.map((d) => +(d.plannedMinutes / 60).toFixed(2)),
          backgroundColor: cssVar('--heat-0'),
          borderRadius: 6,
          maxBarThickness: 34,
        },
      ],
    },
    options: {
      ...baseOptions(),
      plugins: {
        ...baseOptions().plugins,
        tooltip: { callbacks: { label: (c) => `${c.dataset.label}: ${formatMinutes(c.raw * 60)}` } },
      },
    },
  }
})

// Dona por materia: esta semana; si aún no hay nada, el histórico
const subjectSource = computed(() => {
  const week = (stats.value?.weekBySubject ?? []).filter((s) => s.minutes > 0)
  return week.length ? { title: 'Esta semana', list: week } : { title: 'Histórico', list: stats.value?.allTimeBySubject ?? [] }
})

const subjectChart = computed(() => {
  themeVersion.value
  const list = subjectSource.value.list
  return {
    data: {
      labels: list.map((s) => s.name),
      datasets: [
        {
          data: list.map((s) => s.minutes),
          backgroundColor: list.map((s) => s.color),
          borderColor: cssVar('--surface'),
          borderWidth: 2,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '70%',
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8, color: cssVar('--text-muted') } },
        tooltip: { callbacks: { label: (c) => ` ${c.label}: ${formatMinutes(c.raw)}` } },
      },
    },
  }
})

const subjectsWithGoal = computed(() => (stats.value?.weekBySubject ?? []).filter((s) => s.goalMinutes > 0))

const plural = (n, one, many) => `${n} ${n === 1 ? one : many}`

// Una frase que resume el día, debajo del saludo
const lede = computed(() => {
  const s = stats.value
  if (!s) return ''
  const today = s.todayMinutes > 0 ? `Hoy llevas ${formatMinutes(s.todayMinutes)} de estudio.` : 'Hoy todavía no has estudiado.'
  const streak = s.currentStreak > 0 ? ` Vas en una racha de ${plural(s.currentStreak, 'día', 'días')}.` : ''
  return today + streak
})
</script>

<template>
  <div class="page">
    <header class="hero">
      <div class="hero-text">
        <h1>{{ greeting }}</h1>
        <p class="date">{{ formatDate(new Date()) }}</p>
        <p v-if="lede" class="lede">{{ lede }}</p>
      </div>
      <RouterLink to="/timer" class="btn btn-primary btn-lg"><AppIcon name="play" /> Empezar a estudiar</RouterLink>
    </header>

    <div v-if="subjects.loaded && subjects.subjects.length === 0" class="card empty">
      <h2>Todavía no tienes materias</h2>
      <p>Crea tu primera materia para empezar a planificar y medir tu estudio.</p>
      <RouterLink to="/subjects" class="btn btn-primary">Crear materia</RouterLink>
    </div>

    <template v-if="stats">
      <section class="card ledger" aria-label="Resumen">
        <StatCard
          label="Hoy"
          :value="formatMinutes(stats.todayMinutes)"
          :progress="pct(stats.todayMinutes, stats.todayPlannedMinutes)"
          :hint="stats.todayPlannedMinutes ? `de ${formatMinutes(stats.todayPlannedMinutes)} planificados` : 'Nada planificado para hoy'"
        />
        <StatCard
          label="Esta semana"
          :value="formatHours(stats.weekMinutes)"
          :progress="pct(stats.weekMinutes, weekTarget)"
          :hint="weekTarget ? `Meta: ${formatHours(weekTarget)}` : 'Define una meta semanal en tus materias'"
        />
        <StatCard
          label="Racha actual"
          :value="plural(stats.currentStreak, 'día', 'días')"
          :hint="`Mejor racha: ${plural(stats.bestStreak, 'día', 'días')}`"
        />
        <StatCard
          label="Total acumulado"
          :value="formatHours(stats.totalMinutes)"
          :hint="`${plural(stats.totalSessions, 'sesión', 'sesiones')}${stats.averageFocus ? `, concentración ${stats.averageFocus}/5` : ''}`"
        />
      </section>

      <div class="charts">
        <section class="card">
          <div class="card-header">
            <h2>Últimos 7 días</h2>
            <span class="muted small">en horas</span>
          </div>
          <div class="chart-box">
            <Bar :key="themeVersion" :data="weekChart.data" :options="weekChart.options" />
          </div>
        </section>
        <section class="card">
          <div class="card-header">
            <h2>Por materia</h2>
            <span class="muted small">{{ subjectSource.title }}</span>
          </div>
          <div v-if="subjectSource.list.length" class="chart-box">
            <Doughnut :key="themeVersion" :data="subjectChart.data" :options="subjectChart.options" />
          </div>
          <div v-else class="empty">
            <p>Aún no hay sesiones registradas.</p>
            <RouterLink to="/timer" class="btn btn-sm">Empezar una sesión</RouterLink>
          </div>
        </section>
      </div>

      <section v-if="subjectsWithGoal.length" class="card">
        <div class="card-header">
          <h2>Meta semanal por materia</h2>
        </div>
        <div class="goal-list">
          <div v-for="s in subjectsWithGoal" :key="s.subjectId" class="goal-row">
            <div class="row">
              <span class="spine" :style="{ background: s.color }" />
              <strong>{{ s.name }}</strong>
              <span class="spacer" />
              <span class="muted small num">{{ formatMinutes(s.minutes) }} de {{ formatMinutes(s.goalMinutes) }}</span>
              <span v-if="s.minutes >= s.goalMinutes" class="tag tag-success">Cumplida</span>
            </div>
            <ProgressBar :value="(s.minutes / s.goalMinutes) * 100" :color="s.color" :height="6" />
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card-header">
          <h2>Actividad del último año</h2>
        </div>
        <HeatMap :days="heatmap" />
      </section>
    </template>

    <div v-else-if="loading" class="card empty">Cargando estadísticas…</div>
  </div>
</template>

<style scoped>
.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 20px;
  padding: 6px 0 8px;
}

.hero h1 {
  font-size: clamp(2.3rem, 5vw, 3.3rem);
  letter-spacing: -0.02em;
  line-height: 1.05;
}

.date {
  margin-top: 8px;
  color: var(--text-muted);
}

.lede {
  margin-top: 14px;
  font-size: 1.12rem;
  max-width: 46ch;
}

/* Cuatro cifras en una sola ficha, separadas por filetes */
.ledger {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  padding: 22px 6px;
}

.ledger > * {
  padding: 2px 22px;
  border-left: 1px solid var(--border);
}

.ledger > :first-child {
  border-left: none;
}

.charts {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: 16px;
}

.chart-box {
  position: relative;
  height: 260px;
}

.goal-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.goal-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.spine {
  width: 5px;
  height: 16px;
  border-radius: 2px;
}

@media (max-width: 1000px) {
  .ledger {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: 18px;
  }

  .ledger > :nth-child(odd) {
    border-left: none;
  }

  .ledger > :nth-child(n + 3) {
    border-top: 1px solid var(--border);
    padding-top: 18px;
  }
}

@media (max-width: 900px) {
  .charts {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .hero .btn-lg {
    width: 100%;
  }

  .ledger {
    padding: 16px 0;
  }

  .ledger > * {
    padding: 2px 16px;
  }

  .ledger > :nth-child(n + 3) {
    padding-top: 16px;
  }
}
</style>
