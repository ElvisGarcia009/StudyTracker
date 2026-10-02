import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { sessionsApi } from '@/api'
import { formatClock } from '@/utils/format'
import { notify, playChime } from '@/utils/alerts'

/**
 * Estado del timer. La sesión vive en el backend; aquí solo guardamos la
 * última respuesta y calculamos el tiempo que pasó desde entonces.
 */
export const useTimerStore = defineStore('timer', () => {
  const session = ref(null)
  const fetchedAt = ref(Date.now()) // cuándo llegó la última respuesta (reloj local)
  const serverOffset = ref(0) // diferencia entre el reloj del servidor y el local
  const now = ref(Date.now())
  const busy = ref(false)
  const breakAlerted = ref(false)
  let interval = null

  // ---------- Estado derivado ----------
  const isActive = computed(() => session.value !== null)
  const isRunning = computed(() => session.value?.status === 'RUNNING')
  const isPomodoro = computed(() => session.value?.mode === 'POMODORO')
  const onBreak = computed(() => session.value?.status === 'PAUSED' && !!session.value?.breakEndsAt)

  const elapsed = computed(() => {
    if (!session.value) return 0
    const base = session.value.elapsedSeconds
    return isRunning.value ? base + (now.value - fetchedAt.value) / 1000 : base
  })

  const workTotal = computed(() => (session.value?.workMinutes || 25) * 60)
  const workRemaining = computed(() => workTotal.value - (elapsed.value - (session.value?.lastPomodoroAtSeconds || 0)))

  const breakTotal = computed(() => {
    const s = session.value
    if (!s) return 0
    const long = s.pomodorosCompleted > 0 && s.pomodorosCompleted % 4 === 0
    return (long ? s.longBreakMinutes : s.breakMinutes) * 60
  })
  const breakRemaining = computed(() => {
    if (!onBreak.value) return 0
    return Math.max(0, (Date.parse(session.value.breakEndsAt) - (now.value + serverOffset.value)) / 1000)
  })

  /** idle | focus | paused | break */
  const phase = computed(() => {
    if (!session.value) return 'idle'
    if (onBreak.value) return 'break'
    return isRunning.value ? 'focus' : 'paused'
  })

  /** Lo que se muestra en el reloj grande. */
  const displaySeconds = computed(() => {
    if (phase.value === 'break') return breakRemaining.value
    if (isPomodoro.value) return Math.max(0, workRemaining.value)
    return elapsed.value
  })

  /** Avance del anillo, de 0 a 1. */
  const progress = computed(() => {
    if (phase.value === 'break') return breakTotal.value ? 1 - breakRemaining.value / breakTotal.value : 0
    if (isPomodoro.value) return Math.min(1, 1 - workRemaining.value / workTotal.value)
    return (elapsed.value % 3600) / 3600 // en modo libre da una vuelta por hora
  })

  // ---------- Acciones ----------
  function setSession(data) {
    session.value = data
    fetchedAt.value = Date.now()
    now.value = fetchedAt.value
    if (data?.serverTime) serverOffset.value = Date.parse(data.serverTime) - fetchedAt.value
    if (!data?.breakEndsAt) breakAlerted.value = false
  }

  async function run(action) {
    busy.value = true
    try {
      return await action()
    } finally {
      busy.value = false
    }
  }

  const load = () => run(async () => setSession(await sessionsApi.active()))

  const start = (body) => run(async () => setSession(await sessionsApi.start(body)))
  const pause = () => run(async () => setSession(await sessionsApi.pause(session.value.id)))
  const resume = () => run(async () => setSession(await sessionsApi.resume(session.value.id)))

  /** Termina la sesión y devuelve los logros recién desbloqueados. */
  const stop = (notes, focusRating) =>
    run(async () => {
      const result = await sessionsApi.stop(session.value.id, { notes, focusRating })
      setSession(null)
      return result
    })

  const discard = () =>
    run(async () => {
      await sessionsApi.remove(session.value.id)
      setSession(null)
    })

  async function completePomodoro() {
    await run(async () => setSession(await sessionsApi.completePomodoro(session.value.id)))
    playChime()
    notify('🍅 ¡Pomodoro completado!', 'Tómate un descanso, te lo ganaste.')
  }

  // ---------- Reloj ----------
  function tick() {
    now.value = Date.now()

    if (isPomodoro.value && isRunning.value && workRemaining.value <= 0 && !busy.value) {
      completePomodoro().catch(() => {})
    }
    if (phase.value === 'break' && breakRemaining.value <= 0 && !breakAlerted.value) {
      breakAlerted.value = true
      playChime()
      notify('⏰ Descanso terminado', 'Hora de volver a estudiar.')
    }

    document.title = session.value
      ? `${phase.value === 'break' ? '☕' : isRunning.value ? '▶' : '⏸'} ${formatClock(displaySeconds.value)} · StudyTracker`
      : 'StudyTracker'
  }

  function startTicking() {
    if (!interval) interval = setInterval(tick, 500)
  }

  return {
    session, busy, now,
    isActive, isRunning, isPomodoro, onBreak, phase,
    elapsed, workTotal, workRemaining, breakTotal, breakRemaining, displaySeconds, progress,
    load, start, pause, resume, stop, discard, completePomodoro, startTicking,
  }
})
