<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
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
        <p>Qué vas a estudiar cada día y por cuánto tiempo.</p>
      </div>
      <div class="week-total">
        <span class="muted small">Total planificado</span>
        <strong class="num">{{ formatHours(weekTotal) }}</strong>
      </div>
    </div>

    <div v-if="subjects.loaded && !subjects.active.length" class="card empty">
      <h2>Primero crea una materia</h2>
      <p>El plan se arma con bloques de tus materias.</p>
      <RouterLink to="/subjects" class="btn btn-primary">Ir a materias</RouterLink>
    </div>

    <div v-else class="week">
      <section v-for="day in DAYS" :key="day" class="day" :class="{ today: day === today }" :aria-label="DAY_LABELS[day]">
        <header class="day-head">
          <h2>{{ DAY_LABELS[day] }}</h2>
          <span class="muted small num">{{ dayTotal(day) ? formatMinutes(dayTotal(day)) : 'Libre' }}</span>
          <span v-if="day === today" class="today-tag">Hoy</span>
        </header>
        <button
          v-for="b in byDay[day]"
          :key="b.id"
          class="block"
          :style="{ '--c': subjects.byId[b.subjectId]?.color }"
          @click="open(day, b)"
        >
          <span class="block-name">{{ subjects.byId[b.subjectId]?.name }}</span>
          <span class="block-meta num">
            <span v-if="b.startTime" class="block-time">{{ b.startTime }}</span>
            {{ formatMinutes(b.plannedMinutes) }}
          </span>
        </button>
        <button class="add" :aria-label="`Agregar bloque el ${DAY_LABELS[day]}`" @click="open(day)">
          <AppIcon name="plus" :size="15" /> Agregar
        </button>
      </section>
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
          <div class="presets">
            <button
              v-for="m in [30, 45, 60, 90, 120]"
              :key="m"
              type="button"
              class="btn btn-sm"
              :class="{ chosen: form.plannedMinutes === m }"
              :aria-pressed="form.plannedMinutes === m"
              @click="form.plannedMinutes = m"
            >
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
.week-total {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.week-total strong {
  font-family: var(--serif);
  font-weight: 400;
  font-size: 2rem;
  line-height: 1.1;
}

.week {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
}

/* Cada día es un estante de vidrio */
.day {
  position: relative;
  background: var(--glass);
  -webkit-backdrop-filter: blur(16px) saturate(140%);
  backdrop-filter: blur(16px) saturate(140%);
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow);
  padding: 14px 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 260px;
  min-width: 0;
}

/* Hoy, la lámpara alumbra este estante */
.day.today {
  border-color: color-mix(in srgb, var(--primary) 50%, transparent);
  background:
    radial-gradient(ellipse 120% 60% at 50% 0%, var(--primary-soft), transparent 75%),
    var(--glass);
  box-shadow: var(--shadow), 0 0 40px -16px var(--primary-glow);
}

.day-head {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 0 4px 10px;
  border-bottom: 1px solid var(--border);
}

.day-head h2 {
  font-size: 1.08rem;
}

.today .day-head h2 {
  color: var(--primary);
}

.today-tag {
  position: absolute;
  top: 2px;
  right: 2px;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--primary);
}

.block {
  text-align: left;
  border: 1px solid color-mix(in srgb, var(--c) 30%, transparent);
  border-left: 4px solid var(--c);
  background: color-mix(in srgb, var(--c) 14%, transparent);
  color: var(--text);
  border-radius: 10px;
  padding: 8px 10px;
  cursor: pointer;
  font: inherit;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  transition: background 0.15s, box-shadow 0.2s;
}

.block:hover {
  background: color-mix(in srgb, var(--c) 24%, transparent);
  box-shadow: 0 0 18px -8px var(--c);
}

.block-name {
  font-weight: 600;
  font-size: 0.89rem;
  overflow-wrap: anywhere;
}

.block-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0 8px;
  font-size: 0.79rem;
  color: var(--text-muted);
}

.block-time {
  color: var(--text);
}

.add {
  margin-top: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed var(--border-strong);
  background: transparent;
  color: var(--text-muted);
  border-radius: 10px;
  padding: 7px;
  cursor: pointer;
  font: inherit;
  font-size: 0.85rem;
  transition: border-color 0.15s, color 0.15s, background 0.15s;
}

.add:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-soft);
}

.presets {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.presets .chosen {
  background: var(--primary-soft);
  border-color: color-mix(in srgb, var(--primary) 50%, transparent);
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

@media (max-width: 640px) {
  .week-total {
    align-items: flex-start;
  }
}
</style>
