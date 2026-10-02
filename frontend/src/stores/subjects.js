import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { subjectsApi } from '@/api'

/** Materias compartidas por todas las vistas. */
export const useSubjectsStore = defineStore('subjects', () => {
  const subjects = ref([])
  const loaded = ref(false)

  const active = computed(() => subjects.value.filter((s) => !s.archived))
  const byId = computed(() => Object.fromEntries(subjects.value.map((s) => [s.id, s])))

  async function load() {
    subjects.value = await subjectsApi.list()
    loaded.value = true
  }

  async function ensureLoaded() {
    if (!loaded.value) await load()
  }

  async function save(subject) {
    const body = {
      name: subject.name,
      color: subject.color,
      weeklyGoalMinutes: subject.weeklyGoalMinutes,
      archived: subject.archived ?? false,
    }
    if (subject.id) await subjectsApi.update(subject.id, body)
    else await subjectsApi.create(body)
    await load()
  }

  async function remove(id) {
    await subjectsApi.remove(id)
    await load()
  }

  return { subjects, loaded, active, byId, load, ensureLoaded, save, remove }
})
