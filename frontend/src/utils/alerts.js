// Sonidos y notificaciones del timer. Se generan con Web Audio, sin archivos de audio.

let audioContext

function tone(frequency, start, duration) {
  const ctx = audioContext
  const osc = ctx.createOscillator()
  const gain = ctx.createGain()
  osc.type = 'sine'
  osc.frequency.value = frequency
  gain.gain.setValueAtTime(0.0001, ctx.currentTime + start)
  gain.gain.exponentialRampToValueAtTime(0.25, ctx.currentTime + start + 0.02)
  gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + start + duration)
  osc.connect(gain).connect(ctx.destination)
  osc.start(ctx.currentTime + start)
  osc.stop(ctx.currentTime + start + duration + 0.05)
}

/** Tres notas ascendentes. */
export function playChime() {
  try {
    audioContext ??= new (window.AudioContext || window.webkitAudioContext)()
    if (audioContext.state === 'suspended') audioContext.resume()
    tone(660, 0, 0.25)
    tone(880, 0.22, 0.25)
    tone(1175, 0.44, 0.45)
  } catch {
    // El navegador puede bloquear el audio si no hubo interacción previa
  }
}

export function notificationsSupported() {
  return typeof window !== 'undefined' && 'Notification' in window
}

export async function requestNotificationPermission() {
  if (!notificationsSupported()) return 'denied'
  return Notification.requestPermission()
}

export function notify(title, body) {
  if (notificationsSupported() && Notification.permission === 'granted' && document.hidden) {
    new Notification(title, { body, icon: '/favicon.svg' })
  }
}
