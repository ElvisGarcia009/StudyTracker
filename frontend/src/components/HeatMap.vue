<script setup>
import { computed, nextTick, ref, watch } from 'vue'
import { formatDate, formatMinutes, parseLocalDate } from '@/utils/format'

/** Mapa de calor estilo GitHub: una columna por semana, una fila por día (lunes arriba). */
const props = defineProps({
  days: { type: Array, required: true }, // [{ date: '2026-10-02', minutes: 45 }]
})

// Nombres cortos de mes y día desde Intl, sin el punto final ("oct." → "Oct")
const short = (opts, date) => {
  const text = new Intl.DateTimeFormat('es', opts).format(date).replace('.', '')
  return text.charAt(0).toUpperCase() + text.slice(1)
}
const MONTHS = Array.from({ length: 12 }, (_, m) => short({ month: 'short' }, new Date(2026, m, 1)).slice(0, 3))
// 5 de enero de 2026 fue lunes
const WEEKDAYS = Array.from({ length: 7 }, (_, d) => short({ weekday: 'short' }, new Date(2026, 0, 5 + d)))

function level(minutes) {
  if (!minutes) return 0
  if (minutes < 30) return 1
  if (minutes < 60) return 2
  if (minutes < 120) return 3
  return 4
}

// Celdas vacías al inicio para que el primer día caiga en su fila de la semana
const cells = computed(() => {
  if (!props.days.length) return []
  const first = parseLocalDate(props.days[0].date)
  const padding = (first.getDay() + 6) % 7
  return [
    ...Array.from({ length: padding }, (_, i) => ({ key: `pad-${i}`, empty: true })),
    ...props.days.map((d) => ({ key: d.date, ...d, level: level(d.minutes) })),
  ]
})

const weekCount = computed(() => Math.ceil(cells.value.length / 7))

// Etiqueta de mes sobre la primera semana de cada mes
const monthLabels = computed(() => {
  const labels = []
  let lastMonth = -1
  for (let w = 0; w < weekCount.value; w++) {
    const cell = cells.value.slice(w * 7, w * 7 + 7).find((c) => !c.empty)
    if (!cell) continue
    const month = parseLocalDate(cell.date).getMonth()
    if (month !== lastMonth) {
      labels.push({ week: w, text: MONTHS[month] })
      lastMonth = month
    }
  }
  return labels
})

const totalDays = computed(() => props.days.filter((d) => d.minutes > 0).length)
const totalMinutes = computed(() => props.days.reduce((sum, d) => sum + (d.minutes || 0), 0))
const summary = computed(
  () =>
    `Actividad del último año: ${totalDays.value} ${totalDays.value === 1 ? 'día' : 'días'} con estudio, ${formatMinutes(totalMinutes.value)} en total`,
)

// Al cargar, desplaza el scroll al final para ver las semanas más recientes
const scroller = ref(null)
watch(
  () => props.days.length,
  async () => {
    await nextTick()
    if (scroller.value) scroller.value.scrollLeft = scroller.value.scrollWidth
  },
  { immediate: true },
)
</script>

<template>
  <div class="heatmap">
    <div ref="scroller" class="scroller" tabindex="0" aria-label="Mapa de actividad, desplazable">
      <div class="inner" role="img" :aria-label="summary" :style="{ '--weeks': weekCount }">
        <div class="months" aria-hidden="true">
          <span v-for="m in monthLabels" :key="m.week" :style="{ gridColumn: m.week + 1 }">{{ m.text }}</span>
        </div>
        <div class="weekdays" aria-hidden="true">
          <span>{{ WEEKDAYS[0] }}</span><span /><span>{{ WEEKDAYS[2] }}</span><span /><span>{{ WEEKDAYS[4] }}</span><span /><span />
        </div>
        <div class="grid" aria-hidden="true">
          <div
            v-for="c in cells"
            :key="c.key"
            class="cell"
            :class="c.empty ? 'empty' : `l${c.level}`"
            :title="c.empty ? '' : `${formatDate(c.date, { weekday: 'short', day: 'numeric', month: 'short' })}: ${c.minutes ? formatMinutes(c.minutes) : 'sin estudio'}`"
          />
        </div>
      </div>
    </div>
    <div class="footer">
      <span class="muted small num">{{ totalDays }} {{ totalDays === 1 ? 'día' : 'días' }} con estudio en el último año</span>
      <span class="legend small muted" aria-hidden="true">
        Menos
        <i class="cell l0" /><i class="cell l1" /><i class="cell l2" /><i class="cell l3" /><i class="cell l4" />
        Más
      </span>
    </div>
  </div>
</template>

<style scoped>
.heatmap {
  --gap: 3px;
  container-type: inline-size;
}

.scroller {
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 4px;
  border-radius: var(--radius-xs);
}

.inner {
  /* Las celdas crecen para llenar el ancho del panel (entre 10 y 18 px) */
  --cell: clamp(10px, calc((100cqi - 30px - (var(--weeks) - 1) * var(--gap)) / var(--weeks)), 18px);
  display: grid;
  grid-template-columns: 30px auto;
  grid-template-rows: auto auto;
  width: max-content;
  margin-left: auto;
}

.months {
  grid-column: 2;
  display: grid;
  grid-template-columns: repeat(var(--weeks), var(--cell));
  column-gap: var(--gap);
  font-size: 0.75rem;
  color: var(--text-muted);
  height: 20px;
}

.months span {
  white-space: nowrap;
}

.weekdays {
  grid-row: 2;
  display: grid;
  grid-template-rows: repeat(7, var(--cell));
  row-gap: var(--gap);
  font-size: 0.6875rem;
  color: var(--text-muted);
  line-height: var(--cell);
}

.grid {
  grid-row: 2;
  grid-column: 2;
  display: grid;
  grid-template-rows: repeat(7, var(--cell));
  grid-auto-flow: column;
  grid-auto-columns: var(--cell);
  gap: var(--gap);
}

.cell {
  width: var(--cell);
  height: var(--cell);
  border-radius: 3px;
  display: inline-block;
}

.cell.empty {
  visibility: hidden;
}

.l0 {
  background: var(--heat-0);
}

.l1 {
  background: color-mix(in srgb, var(--primary) 30%, var(--heat-0));
}

.l2 {
  background: color-mix(in srgb, var(--primary) 55%, var(--heat-0));
}

.l3 {
  background: color-mix(in srgb, var(--primary) 78%, var(--heat-0));
}

.l4 {
  background: var(--primary-hover);
}

[data-theme='light'] .l4 {
  background: var(--primary);
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.legend {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.legend .cell {
  width: 10px;
  height: 10px;
}
</style>
