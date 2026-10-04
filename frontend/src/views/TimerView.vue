<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import BaseModal from '@/components/BaseModal.vue'
import ClockFace from '@/components/ClockFace.vue'
import FocusRating from '@/components/FocusRating.vue'
import SubjectBadge from '@/components/SubjectBadge.vue'
import { scheduleApi } from '@/api'
import { useSubjectsStore } from '@/stores/subjects'
import { useTimerStore } from '@/stores/timer'
import { useToastStore } from '@/stores/toast'
import { confirmDialog } from '@/utils/confirm'
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
  const ok = await confirmDialog({
    title: '¿Descartar esta sesión?',
    message: 'El tiempo que llevas no se guardará en tu historial.',
    confirmLabel: 'Descartar sesión',
    danger: true,
  })
  if (!ok) return
  finishing.value = false
  await act(timer.discard)
}

// ---------- Esfera del reloj ----------
const activeSubject = computed(() => subjects.byId[timer.session?.subjectId])
// El anillo usa el acento; en el descanso, el verde de éxito
const ringColor = computed(() => (timer.phase === 'break' ? 'var(--success)' : 'var(--primary)'))

const phaseLabel = computed(() => {
  if (timer.phase === 'break') return timer.breakRemaining > 0 ? 'Descanso' : 'Se acabó el descanso'
  if (timer.phase === 'paused') return 'En pausa'
  return timer.isPomodoro ? 'Enfoque' : 'Estudiando'
})

// En reposo, la esfera apagada muestra con qué tiempo arrancarías
const previewTime = computed(() =>
  form.mode === 'POMODORO' ? formatClock(Math.max(1, form.workMinutes || 0) * 60) : formatClock(0),
)
const previewCaption = computed(() =>
  form.mode === 'POMODORO'
    ? `${form.workMinutes || 0} min de enfoque, ${form.breakMinutes || 0} de descanso`
    : 'Cronómetro libre',
)

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
      <button v-if="notifPermission === 'default'" class="btn btn-sm" type="button" @click="enableNotifications">
        <AppIcon name="bell" :size="14" /> Activar notificaciones
      </button>
    </div>

    <!-- Sin materias -->
    <div v-if="subjects.loaded && !subjects.active.length && !timer.isActive" class="card empty">
      <span class="empty-icon"><AppIcon name="subjects" :size="18" /></span>
      <h2>Necesitas al menos una materia</h2>
      <p>Crea una materia para poder cronometrar tu estudio.</p>
      <RouterLink to="/subjects" class="btn btn-primary">Crear materia</RouterLink>
    </div>

    <!-- Sesión en curso -->
    <section v-else-if="timer.isActive" class="card stage" :class="timer.phase" aria-label="Sesión en curso">
      <div class="stage-head">
        <span class="phase" :class="timer.phase">
          <span class="phase-dot" aria-hidden="true" />{{ phaseLabel }}
        </span>
        <SubjectBadge :subject="activeSubject" />
      </div>

      <ClockFace
        :time="formatClock(timer.displaySeconds)"
        :progress="timer.progress"
        :color="ringColor"
        :light="timer.phase === 'paused' ? 'dim' : 'on'"
      >
        <template v-if="timer.isPomodoro">Total {{ formatClock(timer.elapsed) }}</template>
        <template v-else>tiempo estudiado</template>
      </ClockFace>

      <div v-if="timer.isPomodoro" class="pomodoros">
        <span class="pips" aria-hidden="true">
          <i v-for="n in Math.max(4, timer.session.pomodorosCompleted)" :key="n" :class="{ done: n <= timer.session.pomodorosCompleted }" />
        </span>
        <span class="muted small num">
          {{ timer.session.pomodorosCompleted }} {{ timer.session.pomodorosCompleted === 1 ? 'pomodoro completado' : 'pomodoros completados' }}
        </span>
      </div>

      <div class="controls">
        <button v-if="timer.phase === 'focus'" class="btn btn-lg" type="button" :disabled="timer.busy" @click="act(timer.pause)">
          <AppIcon name="pause" /> Pausar
        </button>
        <button v-else-if="timer.phase === 'paused'" class="btn btn-lg btn-primary" type="button" :disabled="timer.busy" @click="act(timer.resume)">
          <AppIcon name="play" :size="14" /> Reanudar
        </button>
        <button v-else class="btn btn-lg btn-primary" type="button" :disabled="timer.busy" @click="act(timer.resume)">
          <AppIcon :name="timer.breakRemaining > 0 ? 'skip' : 'play'" :size="14" />
          {{ timer.breakRemaining > 0 ? 'Saltar descanso' : 'Seguir estudiando' }}
        </button>
        <button class="btn btn-lg" type="button" :disabled="timer.busy" @click="openFinish"><AppIcon name="check" /> Terminar</button>
      </div>
      <button class="btn btn-ghost btn-sm discard" type="button" :disabled="timer.busy" @click="discard">Descartar sesión</button>
    </section>

    <!-- Empezar una sesión -->
    <template v-else-if="subjects.loaded">
      <div class="start">
        <div class="card idle-clock">
          <ClockFace :time="previewTime" light="off">{{ previewCaption }}</ClockFace>
        </div>

        <div class="card start-form">
          <div class="card-header"><h2>Nueva sesión</h2></div>
          <form class="form" @submit.prevent="start()">
            <div class="field">
              <label for="subject">Materia</label>
              <select id="subject" v-model="form.subjectId" name="subject" class="input">
                <option v-for="s in subjects.active" :key="s.id" :value="s.id">{{ s.name }}</option>
              </select>
            </div>

            <div class="field">
              <span id="mode-label" class="label">Modo</span>
              <div class="segmented" role="group" aria-labelledby="mode-label">
                <button type="button" :class="{ active: form.mode === 'POMODORO' }" :aria-pressed="form.mode === 'POMODORO'" @click="form.mode = 'POMODORO'">
                  Pomodoro
                </button>
                <button type="button" :class="{ active: form.mode === 'FREE' }" :aria-pressed="form.mode === 'FREE'" @click="form.mode = 'FREE'">
                  Libre
                </button>
              </div>
            </div>

            <div v-if="form.mode === 'POMODORO'" class="field-row">
              <div class="field">
                <label for="work">Enfoque (min)</label>
                <input id="work" v-model.number="form.workMinutes" name="work" type="number" inputmode="numeric" autocomplete="off" min="1" max="180" class="input num" />
              </div>
              <div class="field">
                <label for="break">Descanso (min)</label>
                <input id="break" v-model.number="form.breakMinutes" name="break" type="number" inputmode="numeric" autocomplete="off" min="1" max="60" class="input num" />
              </div>
              <div class="field">
                <label for="long">Descanso largo (min)</label>
                <input id="long" v-model.number="form.longBreakMinutes" name="long-break" type="number" inputmode="numeric" autocomplete="off" min="1" max="120" class="input num" />
              </div>
            </div>
            <p v-if="form.mode === 'POMODORO'" class="muted small">
              Cada 4 pomodoros toca un descanso largo. Al terminar cada fase sonará un aviso.
            </p>
            <p v-else class="muted small">El cronómetro corre hasta que lo pauses o lo termines.</p>

            <button class="btn btn-primary btn-lg submit" type="submit" :disabled="timer.busy || !form.subjectId">
              <AppIcon name="play" :size="14" /> Empezar sesión
            </button>
          </form>
        </div>
      </div>

      <section class="card">
        <div class="card-header">
          <h2>Plan de hoy</h2>
          <RouterLink to="/schedule" class="btn btn-ghost btn-sm">Editar plan</RouterLink>
        </div>
        <ul v-if="todayBlocks.length" class="today-list">
          <li v-for="b in todayBlocks" :key="b.id" class="today-item">
            <span class="when mono">{{ b.startTime || '—' }}</span>
            <SubjectBadge :subject="subjects.byId[b.subjectId]" />
            <span class="muted small num duration">{{ formatMinutes(b.plannedMinutes) }}</span>
            <span class="spacer" />
            <button
              class="btn btn-sm"
              type="button"
              :disabled="timer.busy"
              :aria-label="`Empezar ${subjects.byId[b.subjectId]?.name || 'materia'}`"
              @click="start(b.subjectId)"
            >
              <AppIcon name="play" :size="12" /> <span class="btn-text">Empezar</span>
            </button>
          </li>
        </ul>
        <div v-else class="empty compact">
          <p>No hay bloques planificados para hoy.</p>
          <RouterLink to="/schedule" class="btn btn-sm">Planificar la semana</RouterLink>
        </div>
      </section>
    </template>

    <BaseModal v-if="finishing" title="¿Cómo te fue?" @close="cancelFinish">
      <form id="finish-form" class="form" @submit.prevent="saveFinish">
        <p class="summary">
          Estudiaste <strong>{{ formatMinutes(timer.elapsed / 60) }}</strong> de
          <strong>{{ activeSubject?.name }}</strong>
          <template v-if="timer.isPomodoro">
            en {{ timer.session?.pomodorosCompleted }} {{ timer.session?.pomodorosCompleted === 1 ? 'pomodoro' : 'pomodoros' }}
          </template>
        </p>
        <div class="field">
          <span class="label">Concentración</span>
          <FocusRating v-model="finishForm.focusRating" />
        </div>
        <div class="field">
          <label for="notes">Notas (opcional)</label>
          <textarea id="notes" v-model="finishForm.notes" name="notes" class="input" maxlength="2000" placeholder="Qué estudiaste, qué quedó pendiente…" />
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
/* ---------- Sesión en curso: la esfera es la protagonista ---------- */
.stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 28px 24px 20px;
  border-radius: var(--radius-xl);
}

.stage-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  max-width: 100%;
}

.phase {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 10px;
  border-radius: var(--radius-pill);
  background: var(--primary-soft);
  color: var(--primary-text);
  font-size: 0.8125rem;
  font-weight: 500;
}

.phase-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.phase.break {
  background: var(--success-soft);
  color: var(--success);
}

.phase.paused {
  background: var(--surface-3);
  color: var(--text-muted);
}

.pomodoros {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.pips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  justify-content: center;
}

.pips i {
  width: 18px;
  height: 4px;
  border-radius: var(--radius-pill);
  background: var(--border-strong);
}

.pips i.done {
  background: var(--primary);
}

.controls {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.discard {
  color: var(--text-muted);
}

.discard:hover:not(:disabled) {
  color: var(--danger);
  background: var(--danger-soft);
}

/* ---------- Reposo: esfera apagada + formulario ---------- */
.start {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.15fr);
  gap: 16px;
}

.idle-clock {
  display: grid;
  place-items: center;
  padding: 24px;
  border-radius: var(--radius-xl);
}

.submit {
  align-self: flex-start;
}

.today-list {
  list-style: none;
  margin: 0 -24px -24px;
  padding: 0;
  border-top: 1px solid var(--border);
}

.today-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 24px;
  border-bottom: 1px solid var(--border);
}

.today-item:last-child {
  border-bottom: none;
}

.today-item :deep(.badge) {
  min-width: 0;
}

.today-item > .btn,
.duration {
  flex-shrink: 0;
  white-space: nowrap;
}

.when {
  flex-shrink: 0;
  min-width: 3.2em;
  color: var(--text-muted);
  font-size: 0.8125rem;
}

.empty.compact {
  padding: 16px 12px;
}

.summary {
  color: var(--text-secondary);
}

.summary strong {
  color: var(--text);
  font-weight: 500;
}

@media (max-width: 860px) {
  .start {
    grid-template-columns: minmax(0, 1fr);
  }

  .idle-clock :deep(.face) {
    width: min(240px, 64vw);
  }
}

@media (max-width: 640px) {
  .today-list {
    margin: 0 -16px -16px;
  }

  .today-item {
    padding: 10px 16px;
    gap: 10px;
  }

  .submit {
    align-self: stretch;
  }
}

@media (max-width: 480px) {
  .today-item .btn-text {
    display: none;
  }
}
</style>
