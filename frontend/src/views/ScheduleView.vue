<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import { scheduleApi } from '@/api'
import { useSubjectsStore } from '@/stores/subjects'
import { useToastStore } from '@/stores/toast'
import { DAYS, DAY_LABELS, formatHours, formatMinutes, todayKey } from '@/utils/format'

const subjects = useSubjectsStore()
const toast = useToastStore()

const blocks = ref([])
const loading = ref(true)

async function load() {
  try {
    blocks.value = await scheduleApi.list()
  } catch (e) {
    toast.error('No se pudo cargar el plan', e.message)
  } finally {
    loading.value = false
  }
}
onMounted(load)

// Bloques agrupados por día (solo de materias activas)
const byDay = computed(() => {
  const result = Object.fromEntries(DAYS.map((d) => [d, []]))
  for (const b of blocks.value) {
    const subject = subjects.byId[b.subjectId]
    if (subject && !subject.archived) result[b.dayOfWeek].push(b)
  }
  return result
})

const dayTotal = (day) => byDay.value[day].reduce((sum, b) => sum + b.plannedMinutes, 0)
const weekTotal = computed(() => DAYS.reduce((sum, d) => sum + dayTotal(d), 0))
const today = todayKey()

// ---------- Formulario ----------
const editing = ref(false)
const form = reactive({ id: null, subjectId: '', dayOfWeek: 'MONDAY', startTime: '', plannedMinutes: 60 })
const error = ref('')

function open(day, block = null) {
  error.value = ''
  Object.assign(
    form,
    block
      ? { ...block, startTime: block.startTime || '' }
      : { id: null, subjectId: subjects.active[0]?.id || '', dayOfWeek: day, startTime: '', plannedMinutes: 60 },
  )
  editing.value = true
}

async function save() {
  error.value = ''
  const body = {
    subjectId: form.subjectId,
    dayOfWeek: form.dayOfWeek,
    startTime: form.startTime || null,
    plannedMinutes: form.plannedMinutes,
  }
  try {
    if (form.id) await scheduleApi.update(form.id, body)
    else await scheduleApi.create(body)
    editing.value = false
    await load()
  } catch (e) {
    error.value = e.message
  }
}

async function remove() {
  try {
    await scheduleApi.remove(form.id)
    editing.value = false
    await load()
  } catch (e) {
    error.value = e.message
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Plan semanal</h1>
        <p>Qué vas a estudiar cada día y por cuánto tiempo. Total planificado: <strong>{{ formatHours(weekTotal) }}</strong></p>
      </div>
    </div>

    <div v-if="subjects.loaded && !subjects.active.length" class="card empty">
      <span class="emoji">📘</span>
      <h2>Primero crea una materia</h2>
      <RouterLink to="/subjects" class="btn btn-primary">Ir a materias</RouterLink>
    </div>

    <div v-else class="week">
      <div v-for="day in DAYS" :key="day" class="day" :class="{ today: day === today }">
        <div class="day-head">
          <strong>{{ DAY_LABELS[day] }}</strong>
          <span class="muted small">{{ dayTotal(day) ? formatMinutes(dayTotal(day)) : '—' }}</span>
        </div>
        <button
          v-for="b in byDay[day]"
          :key="b.id"
          class="block"
          :style="{ '--c': subjects.byId[b.subjectId]?.color }"
          @click="open(day, b)"
        >
          <span class="block-name">{{ subjects.byId[b.subjectId]?.name }}</span>
          <span class="block-meta">{{ b.startTime ? b.startTime + ' · ' : '' }}{{ formatMinutes(b.plannedMinutes) }}</span>
        </button>
        <button class="add" @click="open(day)">＋ Agregar</button>
      </div>
    </div>

    <BaseModal v-if="editing" :title="form.id ? 'Editar bloque' : 'Nuevo bloque'" @close="editing = false">
      <form id="block-form" class="form" @submit.prevent="save">
        <div class="field">
          <label for="b-subject">Materia</label>
          <select id="b-subject" v-model="form.subjectId" class="input" required>
            <option v-for="s in subjects.active" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="field-row">
          <div class="field">
            <label for="b-day">Día</label>
            <select id="b-day" v-model="form.dayOfWeek" class="input">
              <option v-for="d in DAYS" :key="d" :value="d">{{ DAY_LABELS[d] }}</option>
            </select>
          </div>
          <div class="field">
            <label for="b-time">Hora (opcional)</label>
            <input id="b-time" v-model="form.startTime" type="time" class="input" />
          </div>
        </div>
        <div class="field">
          <label for="b-min">Duración (minutos)</label>
          <input id="b-min" v-model.number="form.plannedMinutes" type="number" min="5" max="1440" step="5" class="input" required />
          <div class="row">
            <button v-for="m in [30, 45, 60, 90, 120]" :key="m" type="button" class="btn btn-sm" @click="form.plannedMinutes = m">
              {{ formatMinutes(m) }}
            </button>
          </div>
        </div>
        <p v-if="error" class="error-text">{{ error }}</p>
      </form>
      <template #footer>
        <button v-if="form.id" class="btn btn-danger" type="button" @click="remove">Eliminar</button>
        <span class="spacer" />
        <button class="btn" type="button" @click="editing = false">Cancelar</button>
        <button class="btn btn-primary" type="submit" form="block-form">Guardar</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.week {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
}

.day {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 220px;
  min-width: 0;
}

.day.today {
  border-color: var(--primary);
  box-shadow: 0 0 0 1px var(--primary);
}

.day-head {
  display: flex;
  flex-direction: column;
  padding: 0 2px 6px;
  border-bottom: 1px solid var(--border);
}

.block {
  text-align: left;
  border: none;
  border-left: 4px solid var(--c);
  background: color-mix(in srgb, var(--c) 13%, var(--surface));
  color: var(--text);
  border-radius: 8px;
  padding: 8px 9px;
  cursor: pointer;
  font: inherit;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.block:hover {
  background: color-mix(in srgb, var(--c) 22%, var(--surface));
}

.block-name {
  font-weight: 600;
  font-size: 0.88rem;
  overflow-wrap: anywhere;
}

.block-meta {
  font-size: 0.78rem;
  color: var(--text-muted);
}

.add {
  margin-top: auto;
  border: 1px dashed var(--border);
  background: transparent;
  color: var(--text-muted);
  border-radius: 8px;
  padding: 7px;
  cursor: pointer;
  font: inherit;
  font-size: 0.85rem;
}

.add:hover {
  border-color: var(--primary);
  color: var(--primary);
}

@media (max-width: 1000px) {
  .week {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }

  .day {
    min-height: 0;
  }
}
</style>
