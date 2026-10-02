<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import FocusRating from '@/components/FocusRating.vue'
import SubjectBadge from '@/components/SubjectBadge.vue'
import { sessionsApi } from '@/api'
import { useSubjectsStore } from '@/stores/subjects'
import { useToastStore } from '@/stores/toast'
import { formatDate, formatMinutes, formatTime, toDateInput, toDateTimeInput } from '@/utils/format'

const subjects = useSubjectsStore()
const toast = useToastStore()

// ---------- Filtros (por defecto, últimos 30 días) ----------
const filters = reactive({
  from: toDateInput(new Date(Date.now() - 29 * 86400000)),
  to: toDateInput(),
  subjectId: '',
})

const sessions = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    sessions.value = await sessionsApi.list({
      from: filters.from || undefined,
      to: filters.to || undefined,
      subjectId: filters.subjectId || undefined,
    })
  } catch (e) {
    toast.error('No se pudo cargar el historial', e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)
watch(filters, load)

// Agrupadas por día local
const groups = computed(() => {
  const map = new Map()
  for (const s of sessions.value) {
    const day = toDateInput(new Date(s.startedAt))
    if (!map.has(day)) map.set(day, [])
    map.get(day).push(s)
  }
  return [...map.entries()].map(([day, list]) => ({
    day,
    list,
    minutes: list.reduce((sum, s) => sum + s.elapsedSeconds / 60, 0),
  }))
})

const totalMinutes = computed(() => sessions.value.reduce((sum, s) => sum + s.elapsedSeconds / 60, 0))

// ---------- Crear / editar ----------
const editing = ref(false)
const form = reactive({})
const error = ref('')

function open(session = null) {
  error.value = ''
  Object.assign(
    form,
    session
      ? {
          id: session.id,
          subjectId: session.subjectId,
          startedAt: toDateTimeInput(new Date(session.startedAt)),
          durationMinutes: Math.max(1, Math.round(session.elapsedSeconds / 60)),
          mode: session.mode,
          pomodorosCompleted: session.pomodorosCompleted,
          notes: session.notes || '',
          focusRating: session.focusRating,
        }
      : {
          id: null,
          subjectId: subjects.active[0]?.id || '',
          startedAt: toDateTimeInput(new Date(Date.now() - 60 * 60000)),
          durationMinutes: 60,
          mode: 'FREE',
          pomodorosCompleted: 0,
          notes: '',
          focusRating: null,
        },
  )
  editing.value = true
}

async function save() {
  error.value = ''
  const body = {
    subjectId: form.subjectId,
    startedAt: new Date(form.startedAt).toISOString(),
    durationMinutes: form.durationMinutes,
    mode: form.mode,
    pomodorosCompleted: form.mode === 'POMODORO' ? form.pomodorosCompleted || 0 : 0,
    notes: form.notes,
    focusRating: form.focusRating,
  }
  try {
    const result = form.id ? await sessionsApi.update(form.id, body) : await sessionsApi.createManual(body)
    editing.value = false
    toast.success(form.id ? 'Sesión actualizada' : 'Sesión registrada')
    toast.achievements(result.newAchievements)
    await load()
  } catch (e) {
    error.value = e.message
  }
}

async function remove(session) {
  if (!confirm('¿Eliminar esta sesión del historial?')) return
  try {
    await sessionsApi.remove(session.id)
    toast.success('Sesión eliminada')
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
        <h1>Historial</h1>
        <p>{{ sessions.length }} sesiones · {{ formatMinutes(totalMinutes) }} en el periodo</p>
      </div>
      <button class="btn btn-primary" :disabled="!subjects.active.length" @click="open()">＋ Registrar sesión</button>
    </div>

    <div class="card filters">
      <div class="field">
        <label for="f-from">Desde</label>
        <input id="f-from" v-model="filters.from" type="date" class="input" />
      </div>
      <div class="field">
        <label for="f-to">Hasta</label>
        <input id="f-to" v-model="filters.to" type="date" class="input" />
      </div>
      <div class="field">
        <label for="f-subject">Materia</label>
        <select id="f-subject" v-model="filters.subjectId" class="input">
          <option value="">Todas</option>
          <option v-for="s in subjects.subjects" :key="s.id" :value="s.id">{{ s.name }}</option>
        </select>
      </div>
    </div>

    <div v-if="!loading && !sessions.length" class="card empty">
      <span class="emoji">🕘</span>
      No hay sesiones en este periodo.
    </div>

    <section v-for="g in groups" :key="g.day" class="day-group">
      <div class="day-title">
        <h2>{{ formatDate(g.day) }}</h2>
        <span class="tag">{{ formatMinutes(g.minutes) }}</span>
      </div>
      <div class="card session-list">
        <article v-for="s in g.list" :key="s.id" class="session">
          <div class="session-main">
            <SubjectBadge :subject="subjects.byId[s.subjectId]" />
            <span class="muted small">{{ formatTime(s.startedAt) }} – {{ formatTime(s.endedAt) }}</span>
            <span class="duration">{{ formatMinutes(s.elapsedSeconds / 60) }}</span>
            <span v-if="s.mode === 'POMODORO'" class="tag" :title="`${s.pomodorosCompleted} pomodoros`">🍅 {{ s.pomodorosCompleted }}</span>
            <FocusRating v-if="s.focusRating" :model-value="s.focusRating" readonly />
            <span class="spacer" />
            <button class="icon-btn" title="Editar" aria-label="Editar" @click="open(s)">✏️</button>
            <button class="icon-btn" title="Eliminar" aria-label="Eliminar" @click="remove(s)">🗑️</button>
          </div>
          <p v-if="s.notes" class="notes">{{ s.notes }}</p>
        </article>
      </div>
    </section>

    <BaseModal v-if="editing" :title="form.id ? 'Editar sesión' : 'Registrar sesión'" @close="editing = false">
      <form id="session-form" class="form" @submit.prevent="save">
        <div class="field">
          <label for="s-subject">Materia</label>
          <select id="s-subject" v-model="form.subjectId" class="input" required>
            <option v-for="s in subjects.subjects" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="field-row">
          <div class="field">
            <label for="s-start">Inicio</label>
            <input id="s-start" v-model="form.startedAt" type="datetime-local" class="input" required />
          </div>
          <div class="field">
            <label for="s-dur">Duración (min)</label>
            <input id="s-dur" v-model.number="form.durationMinutes" type="number" min="1" max="1440" class="input" required />
          </div>
        </div>
        <div class="field-row">
          <div class="field">
            <label>Modo</label>
            <div class="segmented">
              <button type="button" :class="{ active: form.mode === 'FREE' }" @click="form.mode = 'FREE'">Libre</button>
              <button type="button" :class="{ active: form.mode === 'POMODORO' }" @click="form.mode = 'POMODORO'">🍅 Pomodoro</button>
            </div>
          </div>
          <div v-if="form.mode === 'POMODORO'" class="field">
            <label for="s-pomo">Pomodoros</label>
            <input id="s-pomo" v-model.number="form.pomodorosCompleted" type="number" min="0" max="100" class="input" />
          </div>
        </div>
        <div class="field">
          <label>Concentración</label>
          <FocusRating v-model="form.focusRating" />
        </div>
        <div class="field">
          <label for="s-notes">Notas</label>
          <textarea id="s-notes" v-model="form.notes" class="input" maxlength="2000" />
        </div>
        <p v-if="error" class="error-text">{{ error }}</p>
      </form>
      <template #footer>
        <button class="btn" type="button" @click="editing = false">Cancelar</button>
        <button class="btn btn-primary" type="submit" form="session-form">Guardar</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.day-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.day-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.session-list {
  padding: 4px 16px;
}

.session {
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
}

.session:last-child {
  border-bottom: none;
}

.session-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.duration {
  font-weight: 600;
  font-family: var(--mono);
  font-size: 0.9rem;
}

.notes {
  margin: 8px 0 0;
  padding: 8px 12px;
  background: var(--surface-2);
  border-radius: 8px;
  white-space: pre-wrap;
  font-size: 0.9rem;
  color: var(--text-muted);
}
</style>
