<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import BaseModal from '@/components/BaseModal.vue'
import { scheduleApi } from '@/api'
import { useSubjectsStore } from '@/stores/subjects'
import { useToastStore } from '@/stores/toast'
import { confirmDialog } from '@/utils/confirm'
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
  const name = subjects.byId[form.subjectId]?.name || 'este bloque'
  const ok = await confirmDialog({
    title: '¿Eliminar este bloque?',
    message: `${name}, ${DAY_LABELS[form.dayOfWeek].toLowerCase()} (${formatMinutes(form.plannedMinutes)}). Tus sesiones ya registradas no se tocan.`,
    confirmLabel: 'Eliminar bloque',
    danger: true,
  })
  if (!ok) return
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
      <span class="empty-icon"><AppIcon name="schedule" :size="18" /></span>
      <h2>Primero crea una materia</h2>
      <p>El plan se arma con bloques de tus materias.</p>
      <RouterLink to="/subjects" class="btn btn-primary">Ir a materias</RouterLink>
    </div>

    <div v-else class="week">
      <section v-for="day in DAYS" :key="day" class="day" :class="{ today: day === today }" :aria-label="DAY_LABELS[day]">
        <header class="day-head">
          <h2>{{ DAY_LABELS[day] }}</h2>
          <span v-if="day === today" class="tag tag-accent">Hoy</span>
          <span class="day-total num">{{ dayTotal(day) ? formatMinutes(dayTotal(day)) : 'Libre' }}</span>
        </header>
        <div class="blocks">
          <button
            v-for="b in byDay[day]"
            :key="b.id"
            type="button"
            class="block"
            :style="{ '--c': subjects.byId[b.subjectId]?.color }"
            :aria-label="`Editar bloque: ${subjects.byId[b.subjectId]?.name}, ${b.startTime ? `a las ${b.startTime}, ` : ''}${formatMinutes(b.plannedMinutes)}`"
            @click="open(day, b)"
          >
            <span class="block-name">{{ subjects.byId[b.subjectId]?.name }}</span>
            <span class="block-meta num">
              <span v-if="b.startTime" class="mono">{{ b.startTime }}</span>
              {{ formatMinutes(b.plannedMinutes) }}
            </span>
          </button>
          <button class="add" type="button" :aria-label="`Agregar bloque el ${DAY_LABELS[day].toLowerCase()}`" @click="open(day)">
            <AppIcon name="plus" :size="14" /> <span>Agregar</span>
          </button>
        </div>
      </section>
    </div>

    <BaseModal v-if="editing" :title="form.id ? 'Editar bloque' : 'Nuevo bloque'" @close="editing = false">
      <form id="block-form" class="form" @submit.prevent="save">
        <div class="field">
          <label for="b-subject">Materia</label>
          <select id="b-subject" v-model="form.subjectId" name="subject" class="input" required>
            <option v-for="s in subjects.active" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="field-row">
          <div class="field">
            <label for="b-day">Día</label>
            <select id="b-day" v-model="form.dayOfWeek" name="day" class="input">
              <option v-for="d in DAYS" :key="d" :value="d">{{ DAY_LABELS[d] }}</option>
            </select>
          </div>
          <div class="field">
            <label for="b-time">Hora (opcional)</label>
            <input id="b-time" v-model="form.startTime" name="start-time" type="time" autocomplete="off" class="input num" />
          </div>
        </div>
        <div class="field">
          <label for="b-min">Duración (minutos)</label>
          <input id="b-min" v-model.number="form.plannedMinutes" name="minutes" type="number" inputmode="numeric" autocomplete="off" min="5" max="1440" step="5" class="input num" required />
          <div class="presets" role="group" aria-label="Duraciones rápidas">
            <button
              v-for="m in [30, 45, 60, 90, 120]"
              :key="m"
              type="button"
              class="preset"
              :class="{ chosen: form.plannedMinutes === m }"
              :aria-pressed="form.plannedMinutes === m"
              @click="form.plannedMinutes = m"
            >
              {{ formatMinutes(m) }}
            </button>
          </div>
        </div>
        <p v-if="error" class="error-text" role="alert">{{ error }}</p>
      </form>
      <template #footer>
        <button v-if="form.id" class="btn btn-danger" type="button" @click="remove"><AppIcon name="trash" :size="14" /> Eliminar</button>
        <span class="spacer" />
        <button class="btn" type="button" @click="editing = false">Cancelar</button>
        <button class="btn btn-primary" type="submit" form="block-form">Guardar bloque</button>
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
  font-size: 1.75rem;
  font-weight: 600;
  letter-spacing: -0.6px;
  line-height: 1.15;
}

.week {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
}

.day {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 240px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: inset 0 1px 0 var(--highlight);
}

.day.today {
  border-color: color-mix(in srgb, var(--primary) 55%, var(--border));
}

.day-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
  padding: 12px 12px 10px;
  border-bottom: 1px solid var(--border);
}

.day-head h2 {
  font-size: 0.875rem;
}

/* La etiqueta "Hoy" no debe hacer más alta esta cabecera que las demás */
.day-head .tag {
  padding-block: 0;
  line-height: 1.5;
}

.day-total {
  flex-basis: 100%;
  color: var(--text-muted);
  font-size: 0.75rem;
}

.blocks {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
}

.block {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  padding: 7px 9px;
  border: 1px solid var(--border);
  border-left: 3px solid var(--c);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  color: var(--text);
  font: inherit;
  text-align: left;
  cursor: pointer;
  transition:
    background-color 0.15s var(--ease),
    border-color 0.15s var(--ease);
}

.block:hover {
  background: var(--surface-3);
  border-color: var(--border-strong);
  border-left-color: var(--c);
}

.block-name {
  font-weight: 500;
  font-size: 0.8125rem;
  overflow-wrap: anywhere;
}

.block-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0 6px;
  font-size: 0.75rem;
  color: var(--text-muted);
}

.add {
  margin-top: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 30px;
  padding: 5px;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-muted);
  font: inherit;
  font-size: 0.8125rem;
  cursor: pointer;
  transition:
    border-color 0.15s var(--ease),
    color 0.15s var(--ease),
    background-color 0.15s var(--ease);
}

.add:hover {
  border-color: var(--border-tertiary);
  border-style: solid;
  color: var(--text);
  background: var(--surface-2);
}

.presets {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
}

.preset {
  min-height: 28px;
  padding: 3px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-muted);
  font: inherit;
  font-size: 0.8125rem;
  font-variant-numeric: tabular-nums;
  cursor: pointer;
  transition:
    background-color 0.15s var(--ease),
    color 0.15s var(--ease),
    border-color 0.15s var(--ease);
}

.preset:hover {
  color: var(--text);
  border-color: var(--border-strong);
}

.preset.chosen {
  background: var(--surface-4);
  border-color: var(--border-tertiary);
  color: var(--text);
}

/* Tabletas: los días en dos columnas, sin altura mínima */
@media (max-width: 1100px) {
  .week {
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  }

  .day {
    min-height: 0;
  }
}

/* Móvil: cada día es una fila compacta, con el botón de agregar a la derecha */
@media (max-width: 640px) {
  .week-total {
    align-items: flex-start;
  }

  .week {
    grid-template-columns: minmax(0, 1fr);
    gap: 0;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--radius-lg);
    overflow: hidden;
  }

  .day {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    border: none;
    border-bottom: 1px solid var(--border);
    border-radius: 0;
    box-shadow: none;
  }

  .day:last-child {
    border-bottom: none;
  }

  .day.today {
    box-shadow: inset 3px 0 0 var(--primary);
  }

  .day-head {
    border-bottom: none;
    padding: 12px 16px;
  }

  .day-total {
    flex-basis: auto;
    margin-left: auto;
  }

  .blocks {
    display: contents;
  }

  .block {
    grid-column: 1 / -1;
    margin: 0 12px 8px 16px;
  }

  .add {
    grid-row: 1;
    grid-column: 2;
    align-self: center;
    margin: 0 12px 0 0;
    border-style: solid;
    border-color: var(--border);
    min-width: 40px;
    min-height: 40px;
  }

  .add span {
    display: none;
  }
}
</style>
