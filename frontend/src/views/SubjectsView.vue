<script setup>
import { computed, reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import BaseModal from '@/components/BaseModal.vue'
import { useSubjectsStore } from '@/stores/subjects'
import { useToastStore } from '@/stores/toast'
import { confirmDialog } from '@/utils/confirm'
import { formatHours } from '@/utils/format'

const store = useSubjectsStore()
const toast = useToastStore()

// Paleta de etiquetas: legible sobre el lienzo oscuro y sobre el claro
const PALETTE = [
  { value: '#5e6ad2', name: 'Lavanda' },
  { value: '#4ea7fc', name: 'Azul' },
  { value: '#26b5ce', name: 'Turquesa' },
  { value: '#4cb782', name: 'Verde' },
  { value: '#a3b745', name: 'Oliva' },
  { value: '#f2c94c', name: 'Amarillo' },
  { value: '#f2994a', name: 'Naranja' },
  { value: '#eb5757', name: 'Rojo' },
  { value: '#e86fb0', name: 'Rosa' },
  { value: '#95a2b3', name: 'Gris' },
]
const COLORS = PALETTE.map((c) => c.value)

const showArchived = ref(false)
const visible = computed(() => store.subjects.filter((s) => showArchived.value || !s.archived))
const archivedCount = computed(() => store.subjects.filter((s) => s.archived).length)

// ---------- Formulario ----------
const editing = ref(null) // null = cerrado
const form = reactive({ id: null, name: '', color: COLORS[0], weeklyGoalHours: 5, archived: false })
const error = ref('')
const saving = ref(false)

function open(subject = null) {
  error.value = ''
  Object.assign(form, subject
    ? { id: subject.id, name: subject.name, color: subject.color, weeklyGoalHours: subject.weeklyGoalMinutes / 60, archived: subject.archived }
    : { id: null, name: '', color: COLORS[store.subjects.length % COLORS.length], weeklyGoalHours: 5, archived: false })
  editing.value = true
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    await store.save({ ...form, weeklyGoalMinutes: Math.round((form.weeklyGoalHours || 0) * 60) })
    toast.success(form.id ? 'Materia actualizada' : 'Materia creada')
    editing.value = null
  } catch (e) {
    error.value = e.message
  } finally {
    saving.value = false
  }
}

async function toggleArchive(subject) {
  try {
    await store.save({ ...subject, archived: !subject.archived })
    toast.success(subject.archived ? 'Materia restaurada' : 'Materia archivada')
  } catch (e) {
    toast.error('No se pudo actualizar', e.message)
  }
}

async function remove(subject) {
  const ok = await confirmDialog({
    title: `¿Eliminar “${subject.name}”?`,
    message: 'Se eliminarán también sus sesiones, su plan y sus metas. Si solo quieres ocultarla, archívala.',
    confirmLabel: 'Eliminar materia',
    danger: true,
  })
  if (!ok) return
  try {
    await store.remove(subject.id)
    toast.success('Materia eliminada')
  } catch (e) {
    toast.error('No se pudo eliminar', e.message)
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Materias</h1>
        <p>Lo que estás estudiando y cuántas horas quieres dedicarle por semana.</p>
      </div>
      <div class="row header-actions">
        <label v-if="archivedCount" class="check small">
          <input v-model="showArchived" type="checkbox" name="show-archived" /> Ver archivadas ({{ archivedCount }})
        </label>
        <button class="btn btn-primary" type="button" @click="open()"><AppIcon name="plus" :size="14" /> Nueva materia</button>
      </div>
    </div>

    <div v-if="store.loaded && !visible.length" class="card empty">
      <span class="empty-icon"><AppIcon name="subjects" :size="18" /></span>
      <h2>Aún no hay materias</h2>
      <p>Una materia es cualquier cosa que quieras estudiar: Java, Inglés, Cálculo…</p>
      <button class="btn btn-primary" type="button" @click="open()">Crear la primera materia</button>
    </div>

    <section v-else-if="visible.length" class="card list" aria-label="Lista de materias">
      <div class="list-head" aria-hidden="true">
        <span>Materia</span>
        <span>Meta semanal</span>
        <span />
      </div>
      <ul>
        <li v-for="s in visible" :key="s.id" class="subject" :class="{ archived: s.archived }">
          <div class="subject-name">
            <span class="swatch" :style="{ background: s.color }" aria-hidden="true" />
            <span class="name">{{ s.name }}</span>
            <span v-if="s.archived" class="tag">Archivada</span>
          </div>
          <span class="goal num">
            <span class="goal-label">Meta: </span>{{ s.weeklyGoalMinutes ? `${formatHours(s.weeklyGoalMinutes)} por semana` : 'Sin meta' }}
          </span>
          <div class="actions">
            <button class="btn btn-ghost btn-sm" type="button" :aria-label="`Editar ${s.name}`" @click="open(s)">
              <AppIcon name="edit" :size="14" /> Editar
            </button>
            <button class="btn btn-ghost btn-sm" type="button" :aria-label="`${s.archived ? 'Restaurar' : 'Archivar'} ${s.name}`" @click="toggleArchive(s)">
              <AppIcon :name="s.archived ? 'restore' : 'archive'" :size="14" /> {{ s.archived ? 'Restaurar' : 'Archivar' }}
            </button>
            <button class="icon-btn danger" type="button" title="Eliminar" :aria-label="`Eliminar ${s.name}`" @click="remove(s)">
              <AppIcon name="trash" />
            </button>
          </div>
        </li>
      </ul>
    </section>

    <BaseModal v-if="editing" :title="form.id ? 'Editar materia' : 'Nueva materia'" @close="editing = null">
      <form id="subject-form" class="form" @submit.prevent="save">
        <div class="field">
          <label for="name">Nombre</label>
          <input id="name" v-model="form.name" name="subject-name" autocomplete="off" class="input" maxlength="60" required placeholder="Ej. Java Quarkus…" />
        </div>
        <div class="field">
          <span id="color-label" class="label">Color</span>
          <div class="colors" role="group" aria-labelledby="color-label">
            <button
              v-for="c in PALETTE"
              :key="c.value"
              type="button"
              class="color"
              :class="{ selected: form.color === c.value }"
              :style="{ '--c': c.value }"
              :title="c.name"
              :aria-label="c.name"
              :aria-pressed="form.color === c.value"
              @click="form.color = c.value"
            />
            <label class="color-picker" title="Otro color">
              <input v-model="form.color" type="color" name="custom-color" aria-label="Elegir otro color" />
            </label>
          </div>
        </div>
        <div class="field">
          <label for="goal">Meta semanal (horas)</label>
          <input id="goal" v-model.number="form.weeklyGoalHours" name="weekly-goal" type="number" inputmode="decimal" autocomplete="off" min="0" max="168" step="0.5" class="input num" />
          <span class="muted small">Déjala en 0 si no quieres meta para esta materia.</span>
        </div>
        <p v-if="error" class="error-text" role="alert">{{ error }}</p>
      </form>
      <template #footer>
        <button class="btn" type="button" @click="editing = null">Cancelar</button>
        <button class="btn btn-primary" type="submit" form="subject-form" :disabled="saving">{{ saving ? 'Guardando…' : 'Guardar materia' }}</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.header-actions {
  gap: 14px;
}

.list {
  padding: 0;
  overflow: hidden;
}

.list ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.list-head,
.subject {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr) 15rem;
  align-items: center;
  gap: 16px;
  padding: 0 12px 0 20px;
}

.list-head {
  min-height: 36px;
  border-bottom: 1px solid var(--border);
  color: var(--text-muted);
  font-size: 0.75rem;
  font-weight: 500;
  letter-spacing: 0.4px;
}

.subject {
  min-height: 52px;
  border-bottom: 1px solid var(--border);
  transition: background-color 0.15s var(--ease);
}

.subject:last-child {
  border-bottom: none;
}

.subject:hover {
  background: var(--surface-2);
}

.subject.archived .name,
.subject.archived .goal {
  color: var(--text-muted);
}

.subject-name {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.swatch {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.name {
  font-weight: 500;
  font-size: 0.9375rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goal {
  color: var(--text-secondary);
}

.goal-label {
  display: none;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
}

.colors {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.color {
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  border: none;
  background: var(--c);
  cursor: pointer;
  transition: box-shadow 0.15s var(--ease);
}

.color.selected {
  box-shadow: 0 0 0 2px var(--surface-2), 0 0 0 4px var(--c);
}

.color-picker {
  position: relative;
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  border: 1px dashed var(--border-tertiary);
  background: conic-gradient(#eb5757, #f2c94c, #4cb782, #4ea7fc, #5e6ad2, #e86fb0, #eb5757);
  cursor: pointer;
  overflow: hidden;
}

.color-picker:focus-within {
  outline: 2px solid var(--focus-ring);
  outline-offset: 2px;
}

.color-picker input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

@media (max-width: 640px) {
  .list-head {
    display: none;
  }

  .subject {
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-areas:
      'name actions'
      'goal actions';
    row-gap: 2px;
    padding: 10px 8px 10px 16px;
  }

  .subject-name {
    grid-area: name;
  }

  .goal {
    grid-area: goal;
    padding-left: 20px;
    font-size: 0.8125rem;
    color: var(--text-muted);
  }

  .goal-label {
    display: inline;
  }

  .actions {
    grid-area: actions;
  }

  /* En móvil, las acciones de texto se quedan solo con su icono */
  .actions .btn {
    font-size: 0;
    gap: 0;
    width: 40px;
    padding: 0;
  }
}
</style>
