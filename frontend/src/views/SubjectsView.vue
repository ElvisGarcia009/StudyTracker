<script setup>
import { computed, reactive, ref } from 'vue'
import BaseModal from '@/components/BaseModal.vue'
import { useSubjectsStore } from '@/stores/subjects'
import { useToastStore } from '@/stores/toast'
import { formatHours } from '@/utils/format'

const store = useSubjectsStore()
const toast = useToastStore()

const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#06b6d4', '#8b5cf6', '#ec4899', '#84cc16', '#f97316', '#14b8a6']

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
  const ok = confirm(
    `¿Borrar "${subject.name}"?\n\nSe eliminarán también sus sesiones, su plan y sus metas. ` +
      'Si solo quieres ocultarla, mejor archívala.',
  )
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
      <div class="row">
        <label v-if="archivedCount" class="check small">
          <input v-model="showArchived" type="checkbox" /> Ver archivadas ({{ archivedCount }})
        </label>
        <button class="btn btn-primary" @click="open()">＋ Nueva materia</button>
      </div>
    </div>

    <div v-if="store.loaded && !visible.length" class="card empty">
      <span class="emoji">📘</span>
      <h2>Aún no hay materias</h2>
      <p>Por ejemplo: "Java", "Inglés", "Cálculo"…</p>
      <button class="btn btn-primary" @click="open()">Crear la primera</button>
    </div>

    <div class="grid subjects">
      <div v-for="s in visible" :key="s.id" class="card subject" :class="{ archived: s.archived }" :style="{ '--c': s.color }">
        <div class="subject-top">
          <span class="avatar">{{ s.name.charAt(0).toUpperCase() }}</span>
          <div class="subject-info">
            <h2>{{ s.name }}</h2>
            <span class="muted small">
              {{ s.weeklyGoalMinutes ? `Meta: ${formatHours(s.weeklyGoalMinutes)} por semana` : 'Sin meta semanal' }}
            </span>
          </div>
          <span v-if="s.archived" class="tag">Archivada</span>
        </div>
        <div class="actions">
          <button class="btn btn-sm" @click="open(s)">✏️ Editar</button>
          <button class="btn btn-sm" @click="toggleArchive(s)">{{ s.archived ? '↩️ Restaurar' : '🗄️ Archivar' }}</button>
          <span class="spacer" />
          <button class="icon-btn" title="Eliminar" aria-label="Eliminar" @click="remove(s)">🗑️</button>
        </div>
      </div>
    </div>

    <BaseModal v-if="editing" :title="form.id ? 'Editar materia' : 'Nueva materia'" @close="editing = null">
      <form id="subject-form" class="form" @submit.prevent="save">
        <div class="field">
          <label for="name">Nombre</label>
          <input id="name" v-model="form.name" class="input" maxlength="60" required placeholder="Ej. Java Quarkus" autofocus />
        </div>
        <div class="field">
          <label>Color</label>
          <div class="colors">
            <button
              v-for="c in COLORS"
              :key="c"
              type="button"
              class="color"
              :class="{ selected: form.color === c }"
              :style="{ background: c }"
              :aria-label="`Color ${c}`"
              @click="form.color = c"
            />
            <input v-model="form.color" type="color" class="color-picker" title="Otro color" />
          </div>
        </div>
        <div class="field">
          <label for="goal">Meta semanal (horas)</label>
          <input id="goal" v-model.number="form.weeklyGoalHours" type="number" min="0" max="168" step="0.5" class="input" />
          <span class="muted small">Déjala en 0 si no quieres meta para esta materia.</span>
        </div>
        <p v-if="error" class="error-text">{{ error }}</p>
      </form>
      <template #footer>
        <button class="btn" type="button" @click="editing = null">Cancelar</button>
        <button class="btn btn-primary" type="submit" form="subject-form" :disabled="saving">Guardar</button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.subjects {
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
}

.subject {
  display: flex;
  flex-direction: column;
  gap: 16px;
  border-top: 4px solid var(--c);
}

.subject.archived {
  opacity: 0.6;
}

.subject-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.subject-info {
  flex: 1;
  min-width: 0;
}

.subject-info h2 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: 1.2rem;
  color: #fff;
  background: var(--c);
  flex-shrink: 0;
}

.actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.check {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  cursor: pointer;
}

.colors {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.color {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 3px solid transparent;
  cursor: pointer;
  box-shadow: 0 0 0 1px var(--border);
}

.color.selected {
  border-color: var(--surface);
  box-shadow: 0 0 0 2px var(--text);
}

.color-picker {
  width: 34px;
  height: 30px;
  border: none;
  background: none;
  cursor: pointer;
  padding: 0;
}
</style>
