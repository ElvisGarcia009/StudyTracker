<script setup>
import { computed, reactive, ref } from 'vue'
import AppIcon from '@/components/AppIcon.vue'
import BaseModal from '@/components/BaseModal.vue'
import { useSubjectsStore } from '@/stores/subjects'
import { useToastStore } from '@/stores/toast'
import { formatHours } from '@/utils/format'

const store = useSubjectsStore()
const toast = useToastStore()

// Colores de tela de encuadernación: se leen bien de noche y de día
const COLORS = ['#e0a458', '#7fb08f', '#7f9fd6', '#c98597', '#a98fd1', '#d4b65c', '#5fa9a4', '#d98466', '#9cb35f', '#8e98ab']

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
      <div class="row header-actions">
        <label v-if="archivedCount" class="check small">
          <input v-model="showArchived" type="checkbox" /> Ver archivadas ({{ archivedCount }})
        </label>
        <button class="btn btn-primary" @click="open()"><AppIcon name="plus" :size="16" /> Nueva materia</button>
      </div>
    </div>

    <div v-if="store.loaded && !visible.length" class="card empty">
      <h2>Aún no hay materias</h2>
      <p>Una materia es cualquier cosa que quieras estudiar: Java, Inglés, Cálculo…</p>
      <button class="btn btn-primary" @click="open()">Crear la primera</button>
    </div>

    <div class="shelf">
      <article v-for="s in visible" :key="s.id" class="card subject" :class="{ archived: s.archived }" :style="{ '--c': s.color }">
        <div class="subject-top">
          <span class="avatar" aria-hidden="true">{{ s.name.charAt(0).toUpperCase() }}</span>
          <div class="subject-info">
            <h2>{{ s.name }}</h2>
            <span class="muted small">
              {{ s.weeklyGoalMinutes ? `Meta: ${formatHours(s.weeklyGoalMinutes)} por semana` : 'Sin meta semanal' }}
            </span>
          </div>
          <span v-if="s.archived" class="tag">Archivada</span>
        </div>
        <div class="actions">
          <button class="btn btn-sm" @click="open(s)"><AppIcon name="edit" :size="15" /> Editar</button>
          <button class="btn btn-sm" @click="toggleArchive(s)">
            <AppIcon :name="s.archived ? 'restore' : 'archive'" :size="15" /> {{ s.archived ? 'Restaurar' : 'Archivar' }}
          </button>
          <span class="spacer" />
          <button class="icon-btn danger" title="Eliminar" :aria-label="`Eliminar ${s.name}`" @click="remove(s)">
            <AppIcon name="trash" />
          </button>
        </div>
      </article>
    </div>

    <BaseModal v-if="editing" :title="form.id ? 'Editar materia' : 'Nueva materia'" @close="editing = null">
      <form id="subject-form" class="form" @submit.prevent="save">
        <div class="field">
          <label for="name">Nombre</label>
          <input id="name" v-model="form.name" class="input" maxlength="60" required placeholder="Ej. Java Quarkus" autofocus />
        </div>
        <div class="field">
          <span id="color-label" class="label">Color</span>
          <div class="colors" role="group" aria-labelledby="color-label">
            <button
              v-for="c in COLORS"
              :key="c"
              type="button"
              class="color"
              :class="{ selected: form.color === c }"
              :style="{ '--c': c }"
              :aria-label="`Color ${c}`"
              :aria-pressed="form.color === c"
              @click="form.color = c"
            />
            <label class="color-picker" title="Otro color">
              <input v-model="form.color" type="color" aria-label="Elegir otro color" />
            </label>
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
.header-actions {
  gap: 14px;
}

.shelf {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 16px;
}

/* Cada materia es un libro: el color va en el lomo */
.subject {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding-left: 30px;
  overflow: hidden;
}

.subject::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 9px;
  background: linear-gradient(90deg, color-mix(in srgb, var(--c) 70%, #000), var(--c) 60%, color-mix(in srgb, var(--c) 80%, #fff));
  box-shadow: 0 0 18px -2px color-mix(in srgb, var(--c) 60%, transparent);
}

.subject.archived {
  opacity: 0.55;
}

.subject-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.subject-info {
  flex: 1;
  min-width: 0;
}

.subject-info h2 {
  font-size: 1.3rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  font-family: var(--serif);
  font-size: 1.4rem;
  color: var(--text);
  background: color-mix(in srgb, var(--c) 22%, transparent);
  border: 1px solid color-mix(in srgb, var(--c) 45%, transparent);
  flex-shrink: 0;
}

.actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.colors {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.color {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  background: var(--c);
  cursor: pointer;
  box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.15);
  transition: box-shadow 0.18s, transform 0.12s;
}

.color:hover {
  transform: scale(1.08);
}

.color.selected {
  box-shadow: 0 0 0 3px var(--glass-strong), 0 0 0 5px var(--c), 0 0 16px var(--c);
}

.color-picker {
  position: relative;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 1px dashed var(--border-strong);
  background: conic-gradient(#e0a458, #7fb08f, #7f9fd6, #a98fd1, #c98597, #e0a458);
  cursor: pointer;
  overflow: hidden;
}

.color-picker:focus-within {
  outline: 2px solid var(--primary);
  outline-offset: 2px;
}

.color-picker input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
</style>
