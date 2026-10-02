import { onBeforeUnmount, onMounted, ref } from 'vue'
import { ArcElement, BarElement, CategoryScale, Chart, Legend, LinearScale, Tooltip } from 'chart.js'

Chart.register(ArcElement, BarElement, CategoryScale, LinearScale, Tooltip, Legend)

export function cssVar(name) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

/** Cambia cada vez que se alterna el tema, para volver a dibujar las gráficas con los nuevos colores. */
export function useThemeVersion() {
  const version = ref(0)
  const bump = () => version.value++
  onMounted(() => window.addEventListener('themechange', bump))
  onBeforeUnmount(() => window.removeEventListener('themechange', bump))
  return version
}

export function baseOptions() {
  const text = cssVar('--text-muted')
  const grid = cssVar('--border')
  Chart.defaults.font.family = cssVar('--font')
  Chart.defaults.color = text
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { labels: { usePointStyle: true, boxWidth: 8, color: text } },
      tooltip: { padding: 10, cornerRadius: 8 },
    },
    scales: {
      x: { grid: { display: false }, ticks: { color: text } },
      y: { beginAtZero: true, grid: { color: grid }, border: { display: false }, ticks: { color: text } },
    },
  }
}
