<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
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
      <button class="btn btn-primary" @click="open()">＋ Nueva meta</button>
    </div>

    <section class="page">
      <h2>🎯 Mis metas</h2>
      <div v-if="!goals.length" class="card empty">
        <span class="emoji">🎯</span>
        Crea una meta, por ejemplo: "20 horas de Java este mes".
      </div>
      <div class="grid goals">
        <div v-for="g in goals" :key="g.id" class="card goal">
          <div class="row">
            <h2 class="goal-title">{{ g.title }}</h2>
            <span class="spacer" />
            <span v-if="g.completed" class="tag tag-success">✓ Cumplida</span>
            <span v-else-if="g.expired" class="tag tag-danger">Venció</span>
          </div>
          <div class="muted small">
            {{ periodText(g) }} · {{ g.subjectId ? subjects.byId[g.subjectId]?.name : 'Todas las materias' }}
          </div>
          <ProgressBar :value="g.percent" :color="goalColor(g)" :height="10" />
          <div class="row small">
            <strong>{{ formatMinutes(g.progressMinutes) }}</strong>
            <span class="muted">de {{ formatHours(g.targetMinutes) }}</span>
            <span class="spacer" />
            <span class="muted">{{ g.percent }}%</span>
          </div>
          <div class="row goal-actions">
            <button class="icon-btn" title="Editar" aria-label="Editar" @click="open(g)">✏️</button>
            <button class="icon-btn" title="Eliminar" aria-label="Eliminar" @click="remove(g)">🗑️</button>
          </div>
        </div>
      </div>
    </section>

    <section class="page">
      <div class="row">
        <h2>🏆 Logros</h2>
        <span class="tag">{{ unlockedCount }} / {{ achievements.length }}</span>
      </div>
      <div class="grid achievements">
        <div v-for="a in achievements" :key="a.code" class="card achievement" :class="{ locked: !a.unlocked }">
          <div class="medal">{{ a.icon }}</div>
          <strong>{{ a.name }}</strong>
          <span class="muted small">{{ a.description }}</span>
          <span v-if="a.unlocked" class="tag tag-success">
            {{ formatDate(a.unlockedAt, { day: 'numeric', month: 'short', year: 'numeric' }) }}
          </span>
          <span v-else class="tag">🔒 Bloqueado</span>
        </div>
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
.goals {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}

.goal {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.goal-title {
  min-width: 0;
  overflow-wrap: anywhere;
}

.goal-actions {
  justify-content: flex-end;
  margin: -6px -6px -8px 0;
}

.achievements {
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
}

.achievement {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;
}

.medal {
  font-size: 2.3rem;
  width: 64px;
  height: 64px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--warning-soft);
  margin-bottom: 4px;
}

.locked .medal {
  filter: grayscale(1);
  opacity: 0.4;
  background: var(--surface-2);
}

.locked strong {
  color: var(--text-muted);
}
</style>
