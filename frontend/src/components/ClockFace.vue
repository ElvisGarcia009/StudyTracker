<script setup>
import { computed } from 'vue'

/**
 * Esfera del timer: un anillo fino que se completa con el avance y la hora en el centro.
 */
const props = defineProps({
  /** Texto del reloj, p. ej. "24:59" o "1:02:05" */
  time: { type: String, required: true },
  /** Avance de 0 a 1 */
  progress: { type: Number, default: 0 },
  /** Color del anillo de progreso */
  color: { type: String, default: 'var(--primary)' },
  /** 'on' encendida, 'dim' en pausa, 'off' apagada (reposo) */
  light: { type: String, default: 'on' },
})

const R = 140
const CIRC = 2 * Math.PI * R
const offset = computed(() => CIRC * (1 - Math.min(1, Math.max(0, props.progress))))
// Marcas cada 5 minutos; las de los cuartos, más largas
const ticks = Array.from({ length: 12 }, (_, i) => ({ i, major: i % 3 === 0 }))
</script>

<template>
  <div class="face" :class="[`light-${light}`, { long: time.length > 5 }]" :style="{ '--lit': color }">
    <svg viewBox="0 0 300 300" class="dial" aria-hidden="true">
      <circle cx="150" cy="150" :r="R" class="track" />
      <circle
        v-if="light !== 'off' && progress > 0"
        cx="150"
        cy="150"
        :r="R"
        class="arc"
        :stroke-dasharray="CIRC"
        :stroke-dashoffset="offset"
        transform="rotate(-90 150 150)"
      />
      <line
        v-for="t in ticks"
        :key="t.i"
        x1="150"
        :y1="t.major ? 24 : 27"
        x2="150"
        y2="33"
        class="tick"
        :transform="`rotate(${t.i * 30} 150 150)`"
      />
    </svg>
    <div class="center">
      <div class="digits num" role="timer">{{ time }}</div>
      <div class="caption"><slot /></div>
    </div>
  </div>
</template>

<style scoped>
.face {
  position: relative;
  width: min(320px, 76vw);
  aspect-ratio: 1;
  container-type: inline-size;
}

.dial {
  width: 100%;
  height: 100%;
}

.track {
  fill: none;
  stroke: var(--border);
  stroke-width: 3;
}

.arc {
  fill: none;
  stroke: var(--lit);
  stroke-width: 3;
  stroke-linecap: round;
  transition:
    stroke-dashoffset 0.6s var(--ease),
    stroke 0.4s var(--ease),
    opacity 0.4s var(--ease);
}

.light-dim .arc {
  opacity: 0.45;
}

.tick {
  stroke: var(--border-strong);
  stroke-width: 2;
  stroke-linecap: round;
}

.center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.digits {
  /* Geist con cifras tabulares: el reloj no "baila" al cambiar de número */
  font-size: 20cqi;
  font-weight: 500;
  line-height: 1;
  letter-spacing: -0.035em;
  color: var(--text);
  transition: color 0.4s var(--ease);
}

.long .digits {
  font-size: 14.5cqi;
}

.light-dim .digits {
  color: var(--text-muted);
}

.light-off .digits {
  color: var(--text-secondary);
}

.caption {
  color: var(--text-muted);
  font-size: 0.8125rem;
  font-variant-numeric: tabular-nums;
  text-align: center;
  max-width: 78%;
}
</style>
