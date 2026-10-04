<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import BaseModal from '@/components/BaseModal.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import { achievementsApi, goalsApi } from '@/api'
import { useSubjectsStore } from '@/stores/subjects'
import { useToastStore } from '@/stores/toast'
import { confirmDialog } from '@/utils/confirm'
import { PERIOD_LABELS, formatDate, formatHours, formatMinutes, toDateInput } from '@/utils/format'

const subjects = useSubjectsStore()
const toast = useToastStore()

const goals = ref([])
const achievements = ref([])

async function load() {
  try {
    ;[goals.value, achievements.value] = await Promise.all([goalsApi.list(), achievementsApi.list()])
  } catch (e) {
    toast.error('No se pudieron cargar las metas', e.message)
  }
}
onMounted(load)

const unlockedCount = computed(() => achievements.value.filter((a) => a.unlocked).length)

// Cada logro lleva un icono de línea según su código (el backend manda un emoji que aquí no usamos)
const ACHIEVEMENT_ICONS = {
  FIRST_SESSION: 'target',
  HOURS_10: 'flame',
  HOURS_50: 'subjects',
  HOURS_100: 'goals',
  STREAK_3: 'schedule',
  STREAK_7: 'bolt',
  STREAK_30: 'crown',
  POMODORO_4: 'timer',
  WEEKLY_GOAL: 'check-circle',
  MARATHON: 'hourglass',
  EARLY_BIRD: 'sunrise',
  NIGHT_OWL: 'moon',
}

function goalColor(goal) {
  if (goal.completed) return 'var(--success)'
  if (goal.expired) return 'var(--danger)'
  return subjects.byId[goal.subjectId]?.color || 'var(--primary)'
}

function periodText(goal) {
  if (goal.period === 'UNTIL_DATE') return `Hasta el ${formatDate(goal.deadline, { day: 'numeric', month: 'long', year: 'numeric' })}`
  return PERIOD_LABELS[goal.period]
}

// ---------- Formulario ----------
const editing = ref(false)
const form = reactive({})
const error = ref('')

function open(goal = null) {
  error.value = ''
  Object.assign(
    form,
    goal
      ? { id: goal.id, title: goal.title, subjectId: goal.subjectId || '', targetHours: goal.targetMinutes / 60, period: goal.period, deadline: goal.deadline || '' }
      : { id: null, title: '', subjectId: '', targetHours: 10, period: 'WEEKLY', deadline: toDateInput(new Date(Date.now() + 30 * 86400000)) },
  )
  editing.value = true
}

async function save() {
  error.value = ''
  const body = {
    title: form.title,
    subjectId: form.subjectId || null,
    targetMinutes: Math.round((form.targetHours || 0) * 60),
    period: form.period,
    deadline: form.period === 'UNTIL_DATE' ? form.deadline : null,
  }
  try {
    if (form.id) await goalsApi.update(form.id, body)
    else await goalsApi.create(body)
    editing.value = false
    await load()
  } catch (e) {
    error.value = e.message
  }
}

async function remove(goal) {
  const ok = await confirmDialog({
    title: `¿Eliminar la meta “${goal.title}”?`,
    message: 'Solo se borra la meta: tus sesiones de estudio se conservan.',
    confirmLabel: 'Eliminar meta',
    danger: true,
  })
  if (!ok) return
  try {
    await goalsApi.remove(goal.id)
    await load()
  } catch (e) {
    toast.error('No se pudo eliminar', e.message)
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Metas y logros</h1>
        <p>Ponte objetivos de horas y desbloquea logros mientras estudias.</p>
      </div>
      <button class="btn btn-primary" type="button" @click="open()"><AppIcon name="plus" :size="14" /> Nueva meta</button>
    </div>

    <section class="section" aria-labelledby="goals-heading">
      <h2 id="goals-heading" class="section-heading">Mis metas</h2>
      <div v-if="!goals.length" class="card empty">
        <span class="empty-icon"><AppIcon name="target" :size="18" /></span>
        <p>Una meta es un número de horas para un periodo, por ejemplo: 20 horas de Java este mes.</p>
        <button class="btn btn-primary" type="button" @click="open()">Crear una meta</button>
      </div>
      <div v-else class="goals">
        <article v-for="g in goals" :key="g.id" class="card goal" :class="{ done: g.completed, expired: g.expired && !g.completed }">
          <div class="goal-head">
            <h3 class="goal-title">{{ g.title }}</h3>
            <span v-if="g.completed" class="tag tag-success">Cumplida</span>
            <span v-else-if="g.expired" class="tag tag-danger">Venció</span>
          </div>
          <p class="muted small">
            {{ periodText(g) }}, {{ g.subjectId ? `en ${subjects.byId[g.subjectId]?.name || 'una materia eliminada'}` : 'en todas las materias' }}
          </p>
          <div class="goal-progress">
            <span class="percent num">{{ g.percent }}%</span>
            <span class="muted small num">{{ formatMinutes(g.progressMinutes) }} de {{ formatHours(g.targetMinutes) }}</span>
          </div>
          <ProgressBar :value="g.percent" :color="goalColor(g)" :height="4" :label="`Avance de ${g.title}`" />
          <div class="goal-actions">
            <button class="icon-btn" type="button" title="Editar" :aria-label="`Editar ${g.title}`" @click="open(g)"><AppIcon name="edit" /></button>
            <button class="icon-btn danger" type="button" title="Eliminar" :aria-label="`Eliminar ${g.title}`" @click="remove(g)"><AppIcon name="trash" /></button>
          </div>
        </article>
      </div>
    </section>

    <section class="section" aria-labelledby="achievements-heading">
      <div class="section-title">
        <h2 id="achievements-heading" class="section-heading">Logros</h2>
        <span class="tag num">{{ unlockedCount }} de {{ achievements.length }}</span>
      </div>
      <ul class="achievements">
        <li v-for="a in achievements" :key="a.code" class="achievement" :class="{ locked: !a.unlocked }">
          <span class="medal" aria-hidden="true">
            <AppIcon :name="ACHIEVEMENT_ICONS[a.code] || 'goals'" :size="18" />
          </span>
          <div class="achievement-text">
            <h3>{{ a.name }}</h3>
            <p>{{ a.description }}</p>
          </div>
          <span v-if="a.unlocked" class="tag tag-success num">
            <AppIcon name="check" :size="12" />
            {{ formatDate(a.unlockedAt, { day: 'numeric', month: 'short', year: 'numeric' }) }}
          </span>
          <span v-else class="tag locked-tag"><AppIcon name="lock" :size="12" /> Bloqueado</span>
        </li>
      </ul>
    </section>

    <BaseModal v-if="editing" :title="form.id ? 'Editar meta' : 'Nueva meta'" @close="editing = false">
      <form id="goal-form" class="form" @submit.prevent="save">
        <div class="field">
          <label for="g-title">Título</label>
          <input id="g-title" v-model="form.title" name="goal-title" autocomplete="off" class="input" maxlength="100" required placeholder="Ej. Terminar el curso de Quarkus…" />
        </div>
        <div class="field-row">
          <div class="field">
            <label for="g-subject">Materia</label>
            <select id="g-subject" v-model="form.subjectId" name="subject" class="input">
              <option value="">Todas</option>
              <option v-for="s in subjects.active" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="field">
            <label for="g-hours">Horas</label>
            <input id="g-hours" v-model.number="form.targetHours" name="hours" type="number" inputmode="decimal" autocomplete="off" min="0.5" step="0.5" class="input num" required />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label for="g-period">Periodo</label>
            <select id="g-period" v-model="form.period" name="period" class="input">
              <option v-for="(label, key) in PERIOD_LABELS" :key="key" :value="key">{{ label }}</option>
            </select>
          </div>
          <div v-if="form.period === 'UNTIL_DATE'" class="field">
            <label for="g-deadline">Fecha límite</label>
            <input id="g-deadline" v-model="form.deadline" name="deadline" type="date" autocomplete="off" class="input num" required />
          </div>
        </div>
        <p v-if="error" class="error-text" role="alert">{{ error }}</p>
      </form>
      <template #footer>
        <button class="btn" type="button" @click="editing = false">Cancelar</button>
        <button class="btn btn-primary" type="submit" form="goal-form">Guardar meta</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-heading {
  font-size: 1.25rem;
  letter-spacing: -0.3px;
}

.goals {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 12px;
}

.goal {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 20px 20px 12px;
}

.goal.done {
  border-color: color-mix(in srgb, var(--success) 35%, var(--border));
}

.goal-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.goal-title {
  flex: 1;
  min-width: 0;
  font-size: 1rem;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.goal-progress {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-top: 6px;
}

.percent {
  font-size: 1.75rem;
  font-weight: 600;
  letter-spacing: -0.6px;
  line-height: 1;
}

.done .percent {
  color: var(--success);
}

.goal-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 2px;
  margin-right: -8px;
}

/* Logros: rejilla de fichas; las bloqueadas se quedan en el lienzo */
.achievements {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.achievement {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  background: var(--surface);
  box-shadow: inset 0 1px 0 var(--highlight);
}

.achievement-text {
  flex: 1;
}

.achievement h3 {
  font-size: 0.9375rem;
  font-weight: 500;
}

.achievement p {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 0.8125rem;
}

.medal {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--primary-soft);
  border: 1px solid color-mix(in srgb, var(--primary) 35%, transparent);
  color: var(--primary-text);
}

.locked {
  background: transparent;
  box-shadow: none;
}

.locked .medal {
  background: var(--surface-2);
  border-color: var(--border);
  color: var(--text-faint);
}

.locked h3 {
  color: var(--text-secondary);
}

.locked-tag {
  background: transparent;
  box-shadow: inset 0 0 0 1px var(--border);
  color: var(--text-muted);
}
</style>
