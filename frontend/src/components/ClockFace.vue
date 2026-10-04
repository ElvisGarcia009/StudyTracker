<script setup>
import { computed } from 'vue'

/**
 * Esfera de reloj: 60 marcas que se encienden según el avance y la hora en el centro,
 * con la luz de la lámpara detrás.
 */
const props = defineProps({
  /** Texto del reloj, p. ej. "24:59" o "1:02:05" */
  time: { type: String, required: true },
  /** Avance de 0 a 1 */
  progress: { type: Number, default: 0 },
  /** Color de las marcas encendidas */
  color: { type: String, default: 'var(--primary)' },
  /** Color de la luz que hay detrás */
  glow: { type: String, default: 'var(--primary-glow)' },
  /** 'on' encendida, 'dim' en pausa, 'off' apagada (reposo) */
  light: { type: String, default: 'on' },
  /** Los dos puntos parpadean mientras el reloj corre */
  ticking: { type: Boolean, default: false },
})

const ticks = Array.from({ length: 60 }, (_, i) => ({ i, major: i % 5 === 0 }))
const litCount = computed(() => Math.min(60, Math.floor(props.progress * 60 + 1e-6)))
const chars = computed(() => props.time.split(''))
</script>

<template>
  <div class="face" :class="[`light-${light}`, { long: time.length > 5 }]" :style="{ '--lit': color, '--glow': glow }">
    <div class="lamp" aria-hidden="true" />
    <svg viewBox="0 0 300 300" class="dial" aria-hidden="true">
      <circle cx="150" cy="150" r="118" class="rim" />
      <line
        v-for="t in ticks"
        :key="t.i"
        x1="150"
        :y1="t.major ? 10 : 14"
        x2="150"
        :y2="t.major ? 30 : 24"
        class="tick"
        :class="{ major: t.major, lit: t.i < litCount, lead: t.i === litCount - 1 }"
        :transform="`rotate(${t.i * 6} 150 150)`"
      />
    </svg>
    <div class="center">
      <div class="digits" :class="{ ticking }" role="timer" :aria-label="time">
        <span v-for="(c, i) in chars" :key="i" :class="c === ':' ? 'sep' : 'dg'">{{ c }}</span>
      </div>
      <div class="caption"><slot /></div>
    </div>
  </div>
</template>

<style scoped>
.face {
  position: relative;
  width: min(360px, 80vw);
  aspect-ratio: 1;
  container-type: inline-size;
}

/* La luz cálida que se derrama detrás de la esfera */
.lamp {
  position: absolute;
  inset: -22%;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 50%, var(--glow), transparent 62%);
  transition: opacity 0.8s ease, background 0.8s ease;
  pointer-events: none;
}

.light-dim .lamp {
  opacity: 0.35;
}

.light-off .lamp {
  opacity: 0.18;
}

.dial {
  position: relative;
  width: 100%;
  height: 100%;
}

.rim {
  fill: color-mix(in srgb, var(--glass) 70%, transparent);
  stroke: var(--border);
  stroke-width: 1;
}

.tick {
  stroke: var(--tick-off);
  stroke-width: 2;
  stroke-linecap: round;
  transition: stroke 0.5s ease;
}

.tick.major {
  stroke-width: 3.2;
}

.tick.lit {
  stroke: var(--lit);
}

.tick.lead {
  filter: drop-shadow(0 0 4px var(--lit));
}

.light-dim .tick.lit {
  stroke: color-mix(in srgb, var(--lit) 55%, transparent);
}

.center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.digits {
  display: flex;
  font-family: var(--serif);
  font-size: 21cqi;
  line-height: 1;
  color: var(--text);
  text-shadow: 0 0 28px var(--glow);
  transition: opacity 0.6s ease, text-shadow 0.6s ease;
}

.long .digits {
  font-size: 15.5cqi;
}

.light-dim .digits {
  opacity: 0.55;
  text-shadow: none;
}

.light-off .digits {
  opacity: 0.7;
  text-shadow: none;
}

/* Cada cifra en su celda fija: el reloj no "baila" al cambiar de número */
.dg {
  display: inline-block;
  width: 0.62em;
  text-align: center;
}

.sep {
  display: inline-block;
  width: 0.3em;
  text-align: center;
  transform: translateY(-0.06em);
}

.ticking .sep {
  animation: blink 1s steps(1) infinite;
}

@keyframes blink {
  50% {
    opacity: 0.25;
  }
}

.caption {
  color: var(--text-muted);
  font-size: 0.9rem;
  font-variant-numeric: tabular-nums;
}
</style>
