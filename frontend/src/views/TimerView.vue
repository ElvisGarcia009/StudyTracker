<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import FocusRating from '@/components/FocusRating.vue'
import SubjectBadge from '@/components/SubjectBadge.vue'
import { scheduleApi } from '@/api'
import { useSubjectsStore } from '@/stores/subjects'
import { useTimerStore } from '@/stores/timer'
import { useToastStore } from '@/stores/toast'
import { formatClock, formatMinutes, todayKey } from '@/utils/format'
import { notificationsSupported, requestNotificationPermission } from '@/utils/alerts'

const subjects = useSubjectsStore()
const timer = useTimerStore()
const toast = useToastStore()

// ---------- Formulario para empezar ----------
const POMODORO_KEY = 'st-pomodoro'
function loadPomodoroSettings() {
  try {
    return { workMinutes: 25, breakMinutes: 5, longBreakMinutes: 15, ...JSON.parse(localStorage.getItem(POMODORO_KEY) || '{}') }
  } catch {
    return { workMinutes: 25, breakMinutes: 5, longBreakMinutes: 15 }
  }
}

const form = reactive({ subjectId: '', mode: 'POMODORO', ...loadPomodoroSettings() })

watch(
  () => [form.workMinutes, form.breakMinutes, form.longBreakMinutes],
  ([workMinutes, breakMinutes, longBreakMinutes]) => {
    try {
      localStorage.setItem(POMODORO_KEY, JSON.stringify({ workMinutes, breakMinutes, longBreakMinutes }))
    } catch {
      // sin almacenamiento: se usarán los valores por defecto la próxima vez
    }
  },
)

// Preselecciona la primera materia activa
watch(
  () => subjects.active,
  (list) => {
    if (!form.subjectId && list.length) form.subjectId = list[0].id
  },
  { immediate: true },
)

const todayBlocks = ref([])
onMounted(async () => {
  try {
    const blocks = await scheduleApi.list()
    todayBlocks.value = blocks.filter((b) => b.dayOfWeek === todayKey())
  } catch {
    // el plan del día es opcional en esta vista
  }
})

async function start(subjectId = form.subjectId) {
  if (!subjectId) return toast.error('Elige una materia')
  try {
    await timer.start({
      subjectId,
      mode: form.mode,
      workMinutes: form.workMinutes,
      breakMinutes: form.breakMinutes,
      longBreakMinutes: form.longBreakMinutes,
    })
  } catch (e) {
    toast.error('No se pudo iniciar', e.message)
  }
}

async function act(action) {
  try {
    await action()
  } catch (e) {
    toast.error('Algo salió mal', e.message)
    await timer.load()
  }
}

// ---------- Terminar sesión ----------
const finishing = ref(false)
const finishForm = reactive({ notes: '', focusRating: null })
let pausedToFinish = false

async function openFinish() {
  pausedToFinish = timer.isRunning
  if (pausedToFinish) await act(timer.pause)
  finishForm.notes = ''
  finishForm.focusRating = null
  finishing.value = true
}

async function saveFinish() {
  try {
    const result = await timer.stop(finishForm.notes, finishForm.focusRating)
    finishing.value = false
    toast.success('Sesión guardada', `Estudiaste ${formatMinutes(result.session.elapsedSeconds / 60)}`)
    toast.achievements(result.newAchievements)
  } catch (e) {
    toast.error('No se pudo guardar', e.message)
  }
}

/** Cerrar el modal sin guardar: si el timer corría, lo reanudamos. */
async function cancelFinish() {
  finishing.value = false
  if (pausedToFinish) await act(timer.resume)
}

async function discard() {
  if (!confirm('¿Descartar esta sesión? El tiempo no se guardará.')) return
  finishing.value = false
  await act(timer.discard)
}

// ---------- Anillo de progreso ----------
const RADIUS = 120
const CIRCUMFERENCE = 2 * Math.PI * RADIUS
const dashOffset = computed(() => CIRCUMFERENCE * (1 - timer.progress))

const activeSubject = computed(() => subjects.byId[timer.session?.subjectId])
const ringColor = computed(() => (timer.phase === 'break' ? 'var(--success)' : activeSubject.value?.color || 'var(--primary)'))

const phaseLabel = computed(() => {
  if (timer.phase === 'break') return timer.breakRemaining > 0 ? '☕ Descanso' : '⏰ ¡Se acabó el descanso!'
  if (timer.phase === 'paused') return '⏸ En pausa'
  return timer.isPomodoro ? '🎯 Enfoque' : '📖 Estudiando'
})

// ---------- Notificaciones ----------
const notifPermission = ref(notificationsSupported() ? Notification.permission : 'unsupported')
async function enableNotifications() {
  notifPermission.value = await requestNotificationPermission()
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Timer</h1>
        <p>Cronometra tu estudio en modo libre o con la técnica Pomodoro.</p>
      </div>
      <button v-if="notifPermission === 'default'" class="btn btn-sm" @click="enableNotifications">
        🔔 Activar notificaciones
      </button>
    </div>

    <!-- Sin materias -->
    <div v-if="subjects.loaded && !subjects.active.length && !timer.isActive" class="card empty">
      <span class="emoji">📘</span>
      <h2>Necesitas al menos una materia</h2>
      <p>Crea una materia para poder cronometrar tu estudio.</p>
      <RouterLink to="/subjects" class="btn btn-primary">Crear materia</RouterLink>
    </div>

    <!-- Sesión en curso -->
    <div v-else-if="timer.isActive" class="card timer-card" :class="timer.phase">
      <SubjectBadge :subject="activeSubject" />
      <div class="phase">{{ phaseLabel }}</div>

      <div class="ring-wrap">
        <svg viewBox="0 0 280 280" class="ring">
          <circle cx="140" cy="140" :r="RADIUS" class="ring-track" />
          <circle
            cx="140"
            cy="140"
            :r="RADIUS"
            class="ring-progress"
            :stroke="ringColor"
            :stroke-dasharray="CIRCUMFERENCE"
            :stroke-dashoffset="dashOffset"
          />
        </svg>
        <div class="ring-center">
          <div class="clock">{{ formatClock(timer.displaySeconds) }}</div>
          <div class="muted small">
            <template v-if="timer.isPomodoro">Total: {{ formatClock(timer.elapsed) }}</template>
            <template v-else>tiempo estudiado</template>
          </div>
        </div>
      </div>

      <div v-if="timer.isPomodoro" class="tomatoes" :title="`${timer.session.pomodorosCompleted} pomodoros completados`">
        <span v-for="n in Math.max(4, timer.session.pomodorosCompleted)" :key="n" :class="{ done: n <= timer.session.pomodorosCompleted }">🍅</span>
      </div>

      <div class="controls">
        <button v-if="timer.phase === 'focus'" class="btn btn-lg" :disabled="timer.busy" @click="act(timer.pause)">⏸ Pausar</button>
        <button v-else-if="timer.phase === 'paused'" class="btn btn-lg btn-primary" :disabled="timer.busy" @click="act(timer.resume)">
          ▶ Reanudar
        </button>
        <button v-else class="btn btn-lg btn-primary" :disabled="timer.busy" @click="act(timer.resume)">
          {{ timer.breakRemaining > 0 ? '⏭ Saltar descanso' : '▶ Seguir estudiando' }}
        </button>
        <button class="btn btn-lg" :disabled="timer.busy" @click="openFinish">✓ Terminar</button>
      </div>
      <button class="btn btn-ghost btn-sm btn-danger" :disabled="timer.busy" @click="discard">Descartar sesión</button>
    </div>

    <!-- Empezar una sesión -->
    <div v-else-if="subjects.loaded" class="grid start-grid">
      <div class="card">
        <div class="card-header"><h2>Nueva sesión</h2></div>
        <form class="form" @submit.prevent="start()">
          <div class="field">
            <label for="subject">Materia</label>
            <select id="subject" v-model="form.subjectId" class="input">
              <option v-for="s in subjects.active" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>

          <div class="field">
            <label>Modo</label>
            <div class="segmented">
              <button type="button" :class="{ active: form.mode === 'POMODORO' }" @click="form.mode = 'POMODORO'">🍅 Pomodoro</button>
              <button type="button" :class="{ active: form.mode === 'FREE' }" @click="form.mode = 'FREE'">⏱️ Libre</button>
            </div>
          </div>

          <div v-if="form.mode === 'POMODORO'" class="field-row">
            <div class="field">
              <label for="work">Enfoque (min)</label>
              <input id="work" v-model.number="form.workMinutes" type="number" min="1" max="180" class="input" />
            </div>
            <div class="field">
              <label for="break">Descanso (min)</label>
              <input id="break" v-model.number="form.breakMinutes" type="number" min="1" max="60" class="input" />
            </div>
            <div class="field">
              <label for="long">Descanso largo (min)</label>
              <input id="long" v-model.number="form.longBreakMinutes" type="number" min="1" max="120" class="input" />
            </div>
          </div>
          <p v-if="form.mode === 'POMODORO'" class="muted small hint">
            Cada 4 pomodoros toca un descanso largo. Al terminar cada fase sonará un aviso.
          </p>
          <p v-else class="muted small hint">El cronómetro corre hasta que lo pauses o lo termines.</p>

          <button class="btn btn-primary btn-lg" type="submit" :disabled="timer.busy || !form.subjectId">▶ Empezar</button>
        </form>
      </div>

      <div class="card">
        <div class="card-header">
          <h2>Plan de hoy</h2>
          <RouterLink to="/schedule" class="btn btn-ghost btn-sm">Editar plan</RouterLink>
        </div>
        <div v-if="todayBlocks.length" class="today-list">
          <div v-for="b in todayBlocks" :key="b.id" class="today-item">
            <SubjectBadge :subject="subjects.byId[b.subjectId]" />
            <span class="muted small">{{ b.startTime ? b.startTime + ' · ' : '' }}{{ formatMinutes(b.plannedMinutes) }}</span>
            <span class="spacer" />
            <button class="btn btn-sm" :disabled="timer.busy" @click="start(b.subjectId)">▶</button>
          </div>
        </div>
        <div v-else class="empty"><span class="emoji">🗓️</span>No hay bloques planificados para hoy.</div>
      </div>
    </div>

    <BaseModal v-if="finishing" title="¿Cómo te fue?" @close="cancelFinish">
      <form id="finish-form" class="form" @submit.prevent="saveFinish">
        <p class="muted">
          Estudiaste <strong>{{ formatMinutes(timer.elapsed / 60) }}</strong> de
          <strong>{{ activeSubject?.name }}</strong>
          <template v-if="timer.isPomodoro"> · {{ timer.session?.pomodorosCompleted }} 🍅</template>
        </p>
        <div class="field">
          <label>Concentración</label>
          <FocusRating v-model="finishForm.focusRating" />
        </div>
        <div class="field">
          <label for="notes">Notas (opcional)</label>
          <textarea id="notes" v-model="finishForm.notes" class="input" maxlength="2000" placeholder="¿Qué estudiaste? ¿Qué te quedó pendiente?" />
        </div>
      </form>
      <template #footer>
        <button class="btn" type="button" @click="cancelFinish">Seguir estudiando</button>
        <button class="btn btn-primary" type="submit" form="finish-form" :disabled="timer.busy">Guardar sesión</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.timer-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 32px 18px;
}

.phase {
  font-weight: 600;
  font-size: 1.05rem;
}

.ring-wrap {
  position: relative;
  width: min(300px, 78vw);
  aspect-ratio: 1;
}

.ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ring-track {
  fill: none;
  stroke: var(--surface-2);
  stroke-width: 14;
}

.ring-progress {
  fill: none;
  stroke-width: 14;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.5s linear, stroke 0.3s;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.clock {
  font-family: var(--mono);
  font-size: clamp(2.6rem, 11vw, 3.6rem);
  font-weight: 700;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
}

.paused .clock {
  opacity: 0.6;
}

.tomatoes {
  display: flex;
  gap: 6px;
  font-size: 1.3rem;
  flex-wrap: wrap;
  justify-content: center;
}

.tomatoes span {
  filter: grayscale(1);
  opacity: 0.3;
}

.tomatoes span.done {
  filter: none;
  opacity: 1;
}

.controls {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

.start-grid {
  grid-template-columns: 1.3fr 1fr;
  align-items: start;
}

.hint {
  margin: -4px 0 0;
}

.today-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.today-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 4px;
  border-bottom: 1px solid var(--border);
}

.today-item:last-child {
  border-bottom: none;
}

@media (max-width: 860px) {
  .start-grid {
    grid-template-columns: 1fr;
  }
}
</style>
