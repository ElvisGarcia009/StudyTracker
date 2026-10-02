export const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

export const DAY_LABELS = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
}

export const PERIOD_LABELS = {
  WEEKLY: 'Cada semana',
  MONTHLY: 'Cada mes',
  UNTIL_DATE: 'Hasta una fecha',
}

/** Día de la semana de hoy en el formato del backend ("MONDAY"…). */
export function todayKey() {
  return DAYS[(new Date().getDay() + 6) % 7]
}

/** 95 → "1 h 35 min", 45 → "45 min", 120 → "2 h" */
export function formatMinutes(minutes) {
  const m = Math.max(0, Math.round(minutes || 0))
  const h = Math.floor(m / 60)
  const rest = m % 60
  if (h === 0) return `${rest} min`
  return rest === 0 ? `${h} h` : `${h} h ${rest} min`
}

/** Horas con un decimal: 95 → "1.6 h" */
export function formatHours(minutes) {
  const h = (minutes || 0) / 60
  return `${h % 1 === 0 ? h : h.toFixed(1)} h`
}

/** Segundos a reloj: 65 → "01:05", 3725 → "1:02:05" */
export function formatClock(totalSeconds) {
  const s = Math.max(0, Math.floor(totalSeconds))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const pad = (n) => String(n).padStart(2, '0')
  return h > 0 ? `${h}:${pad(m)}:${pad(sec)}` : `${pad(m)}:${pad(sec)}`
}

export function formatTime(iso) {
  return new Date(iso).toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit' })
}

export function formatDate(isoOrDate, options = { weekday: 'long', day: 'numeric', month: 'long' }) {
  const date = typeof isoOrDate === 'string' && isoOrDate.length === 10 ? parseLocalDate(isoOrDate) : new Date(isoOrDate)
  const text = date.toLocaleDateString('es', options)
  return text.charAt(0).toUpperCase() + text.slice(1)
}

/** "2026-10-02" → Date en hora local (sin el desfase de UTC). */
export function parseLocalDate(value) {
  const [y, m, d] = value.split('-').map(Number)
  return new Date(y, m - 1, d)
}

/** Date → "2026-10-02" en hora local. */
export function toDateInput(date = new Date()) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** Date → "2026-10-02T14:30" para inputs datetime-local. */
export function toDateTimeInput(date = new Date()) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${toDateInput(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}
