<script setup>
import { computed, onMounted, ref } from 'vue'
import { Bar, Doughnut } from 'vue-chartjs'
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
          borderWidth: 3,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      cutout: '62%',
      plugins: {
        legend: { position: 'bottom', labels: { usePointStyle: true, boxWidth: 8, color: cssVar('--text-muted') } },
        tooltip: { callbacks: { label: (c) => ` ${c.label}: ${formatMinutes(c.raw)}` } },
      },
    },
  }
})

const subjectsWithGoal = computed(() => (stats.value?.weekBySubject ?? []).filter((s) => s.goalMinutes > 0))
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>{{ greeting }} 👋</h1>
        <p>{{ formatDate(new Date()) }}</p>
      </div>
      <RouterLink to="/timer" class="btn btn-primary">⏱️ Empezar a estudiar</RouterLink>
    </div>

    <div v-if="subjects.loaded && subjects.subjects.length === 0" class="card empty">
      <span class="emoji">📘</span>
      <h2>Todavía no tienes materias</h2>
      <p>Crea tu primera materia para empezar a planificar y medir tu estudio.</p>
      <RouterLink to="/subjects" class="btn btn-primary">Crear materia</RouterLink>
    </div>

    <template v-if="stats">
      <div class="grid grid-4">
        <StatCard
          icon="☀️"
          label="Hoy"
          :value="formatMinutes(stats.todayMinutes)"
          :progress="pct(stats.todayMinutes, stats.todayPlannedMinutes)"
          :hint="stats.todayPlannedMinutes ? `de ${formatMinutes(stats.todayPlannedMinutes)} planificados` : 'Nada planificado para hoy'"
        />
        <StatCard
          icon="📆"
          label="Esta semana"
          :value="formatHours(stats.weekMinutes)"
          :progress="pct(stats.weekMinutes, weekTarget)"
          :hint="weekTarget ? `Meta: ${formatHours(weekTarget)}` : 'Define una meta semanal en tus materias'"
        />
        <StatCard
          icon="🔥"
          label="Racha actual"
          :value="`${stats.currentStreak} ${stats.currentStreak === 1 ? 'día' : 'días'}`"
          :hint="`Mejor racha: ${stats.bestStreak} ${stats.bestStreak === 1 ? 'día' : 'días'}`"
        />
        <StatCard
          icon="⏳"
          label="Total acumulado"
          :value="formatHours(stats.totalMinutes)"
          :hint="`${stats.totalSessions} sesiones${stats.averageFocus ? ` · concentración ${stats.averageFocus}/5` : ''}`"
        />
      </div>

      <div class="grid charts">
        <div class="card">
          <div class="card-header">
            <h2>Últimos 7 días</h2>
            <span class="muted small">horas</span>
          </div>
          <div class="chart-box">
            <Bar :key="themeVersion" :data="weekChart.data" :options="weekChart.options" />
          </div>
        </div>
        <div class="card">
          <div class="card-header">
            <h2>Por materia</h2>
            <span class="muted small">{{ subjectSource.title }}</span>
          </div>
          <div v-if="subjectSource.list.length" class="chart-box">
            <Doughnut :key="themeVersion" :data="subjectChart.data" :options="subjectChart.options" />
          </div>
          <div v-else class="empty"><span class="emoji">🍩</span>Aún no hay sesiones registradas</div>
        </div>
      </div>

      <div v-if="subjectsWithGoal.length" class="card">
        <div class="card-header">
          <h2>Meta semanal por materia</h2>
        </div>
        <div class="goal-list">
          <div v-for="s in subjectsWithGoal" :key="s.subjectId" class="goal-row">
            <div class="row">
              <span class="swatch" :style="{ background: s.color }" />
              <strong>{{ s.name }}</strong>
              <span class="spacer" />
              <span class="muted small">{{ formatMinutes(s.minutes) }} / {{ formatMinutes(s.goalMinutes) }}</span>
              <span v-if="s.minutes >= s.goalMinutes" class="tag tag-success">✓ Cumplida</span>
            </div>
            <ProgressBar :value="(s.minutes / s.goalMinutes) * 100" :color="s.color" />
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <h2>Actividad del último año</h2>
        </div>
        <HeatMap :days="heatmap" />
      </div>
    </template>

    <div v-else-if="loading" class="card empty">Cargando estadísticas…</div>
  </div>
</template>

<style scoped>
.charts {
  grid-template-columns: 1.6fr 1fr;
}

.chart-box {
  position: relative;
  height: 260px;
}

.goal-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.goal-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.swatch {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}

@media (max-width: 900px) {
  .charts {
    grid-template-columns: 1fr;
  }
}
</style>
