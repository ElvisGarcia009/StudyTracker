<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import BaseModal from '@/components/BaseModal.vue'
import ProgressBar from '@/components/ProgressBar.vue'
import { achievementsApi, goalsApi } from '@/api'
import { useSubjectsStore } from '@/stores/subjects'
import { useToastStore } from '@/stores/toast'
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
  if (!confirm(`¿Eliminar la meta "${goal.title}"?`)) return
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
      <button class="btn btn-primary" @click="open()"><AppIcon name="plus" :size="16" /> Nueva meta</button>
    </div>

    <section class="section">
      <h2 class="section-heading">Mis metas</h2>
      <div v-if="!goals.length" class="card empty">
        <p>Una meta es un número de horas para un periodo, por ejemplo: 20 horas de Java este mes.</p>
        <button class="btn btn-primary" @click="open()">Crear una meta</button>
      </div>
      <div class="goals">
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
          <ProgressBar :value="g.percent" :color="goalColor(g)" :height="6" />
          <div class="goal-actions">
            <button class="icon-btn" title="Editar" :aria-label="`Editar ${g.title}`" @click="open(g)"><AppIcon name="edit" /></button>
            <button class="icon-btn danger" title="Eliminar" :aria-label="`Eliminar ${g.title}`" @click="remove(g)"><AppIcon name="trash" /></button>
          </div>
        </article>
      </div>
    </section>

    <section class="section">
      <div class="section-title">
        <h2 class="section-heading">Logros</h2>
        <span class="muted num">{{ unlockedCount }} de {{ achievements.length }} desbloqueados</span>
      </div>
      <div class="achievements">
        <article v-for="a in achievements" :key="a.code" class="achievement" :class="{ locked: !a.unlocked }">
          <div class="medal" aria-hidden="true">{{ a.icon }}</div>
          <h3>{{ a.name }}</h3>
          <p class="muted small">{{ a.description }}</p>
          <span v-if="a.unlocked" class="tag tag-success">
            {{ formatDate(a.unlockedAt, { day: 'numeric', month: 'short', year: 'numeric' }) }}
          </span>
          <span v-else class="tag"><AppIcon name="lock" :size="12" /> Bloqueado</span>
        </article>
      </div>
    </section>

    <BaseModal v-if="editing" :title="form.id ? 'Editar meta' : 'Nueva meta'" @close="editing = false">
      <form id="goal-form" class="form" @submit.prevent="save">
        <div class="field">
          <label for="g-title">Título</label>
          <input id="g-title" v-model="form.title" class="input" maxlength="100" required placeholder="Ej. Terminar el curso de Quarkus" />
        </div>
        <div class="field-row">
          <div class="field">
            <label for="g-subject">Materia</label>
            <select id="g-subject" v-model="form.subjectId" class="input">
              <option value="">Todas</option>
              <option v-for="s in subjects.active" :key="s.id" :value="s.id">{{ s.name }}</option>
            </select>
          </div>
          <div class="field">
            <label for="g-hours">Horas</label>
            <input id="g-hours" v-model.number="form.targetHours" type="number" min="0.5" step="0.5" class="input" required />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label for="g-period">Periodo</label>
            <select id="g-period" v-model="form.period" class="input">
              <option v-for="(label, key) in PERIOD_LABELS" :key="key" :value="key">{{ label }}</option>
            </select>
          </div>
          <div v-if="form.period === 'UNTIL_DATE'" class="field">
            <label for="g-deadline">Fecha límite</label>
            <input id="g-deadline" v-model="form.deadline" type="date" class="input" required />
          </div>
        </div>
        <p v-if="error" class="error-text">{{ error }}</p>
      </form>
      <template #footer>
        <button class="btn" type="button" @click="editing = false">Cancelar</button>
        <button class="btn btn-primary" type="submit" form="goal-form">Guardar</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-heading {
  font-size: 1.55rem;
}

.goals {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(290px, 1fr));
  gap: 16px;
}

.goal {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 12px;
}

.goal.done {
  border-color: color-mix(in srgb, var(--success) 40%, transparent);
}

.goal-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.goal-title {
  flex: 1;
  min-width: 0;
  font-size: 1.2rem;
  overflow-wrap: anywhere;
}

.goal-progress {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4px;
}

.percent {
  font-family: var(--serif);
  font-size: 1.9rem;
  line-height: 1;
}

.done .percent {
  color: var(--success);
}

.goal-actions {
  display: flex;
  justify-content: flex-end;
  margin-right: -8px;
}

/* Logros: medallas que se encienden al desbloquearse */
.achievements {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.achievement {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;
  padding: 20px 14px 16px;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: var(--glass);
  -webkit-backdrop-filter: blur(16px);
  backdrop-filter: blur(16px);
}

.achievement h3 {
  font-size: 1.02rem;
}

.achievement p {
  flex: 1;
}

.medal {
  font-size: 2.1rem;
  width: 66px;
  height: 66px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  margin-bottom: 6px;
  background: radial-gradient(circle at 50% 40%, var(--primary-soft), transparent 75%);
  box-shadow:
    inset 0 0 0 1.5px color-mix(in srgb, var(--primary) 55%, transparent),
    0 0 26px -6px var(--primary-glow);
}

.locked {
  background: transparent;
  border-style: dashed;
}

.locked .medal {
  filter: grayscale(1);
  opacity: 0.4;
  background: var(--surface-2);
  box-shadow: inset 0 0 0 1px var(--border);
}

.locked h3 {
  color: var(--text-muted);
}
</style>
