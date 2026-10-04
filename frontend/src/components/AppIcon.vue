<script setup>
/** Iconos de línea a 1.6 px. Heredan el color del texto (currentColor). */
const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 16 },
  /** Rellena la forma (estrellas encendidas, play) */
  filled: { type: Boolean, default: false },
})

const PATHS = {
  // Navegación
  dashboard: 'M4 20V11M10 20V5M16 20v-8M21 20H3',
  timer: 'M12 21a8 8 0 1 0 0-16 8 8 0 0 0 0 16ZM12 9v4l2.5 2M9.5 2.5h5',
  subjects: 'M5 4h4v16H5zM10 4h4v16h-4zM15.6 5.3l3.8-1 3.8 14.6-3.8 1z',
  schedule: 'M4 6.5A1.5 1.5 0 0 1 5.5 5h13A1.5 1.5 0 0 1 20 6.5v12a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18.5zM4 10h16M8.5 3v4M15.5 3v4',
  history: 'M3.5 12a8.5 8.5 0 1 0 2.6-6.1L3.5 8.5M3.5 4v4.5H8M12 7.5V12l3 2',
  goals: 'M8 4h8v5.5a4 4 0 0 1-8 0zM8 6H5.5a2.5 2.5 0 0 0 2.6 4M16 6h2.5a2.5 2.5 0 0 1-2.6 4M12 13.5V17M8.5 20.5h7M10 17h4v3.5h-4z',
  // Interfaz
  sun: 'M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM12 2.5v2M12 19.5v2M4.6 4.6 6 6M18 18l1.4 1.4M2.5 12h2M19.5 12h2M4.6 19.4 6 18M18 6l1.4-1.4',
  moon: 'M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5Z',
  play: 'M7 4.5v15l12-7.5z',
  pause: 'M8 5v14M16 5v14',
  check: 'm5 12.5 4.5 4.5L19 7.5',
  skip: 'M5 5v14l9-7zM18 5v14',
  plus: 'M12 5v14M5 12h14',
  edit: 'M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16zM13.5 6.5l4 4',
  trash: 'M4.5 7h15M10 11v6M14 11v6M6 7l1 12.5A1.5 1.5 0 0 0 8.5 21h7a1.5 1.5 0 0 0 1.5-1.5L18 7M9 7V4.5h6V7',
  archive: 'M3.5 4h17v4h-17zM5 8v11.5h14V8M10 12h4',
  restore: 'M4 12a8 8 0 1 0 2.5-5.8L4 8.5M4 4v4.5h4.5',
  bell: 'M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15zM10 21h4',
  close: 'M6 6l12 12M18 6 6 18',
  lock: 'M6 11h12v9H6zM8.5 11V8a3.5 3.5 0 0 1 7 0v3',
  menu: 'M4 7h16M4 12h16M4 17h16',
  alert: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18ZM12 7.5V13M12 16.5h.01',
  star: 'M12 3.5l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.8l-5.2 2.8 1-5.8-4.3-4.1 5.9-.9z',
  chart: 'M4 4v16h16M8 16v-4M12 16V8M16 16v-6',
  // Logros
  target: 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18ZM12 16.5a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9ZM12 12h.01',
  flame: 'M12 21c3.9 0 6.5-2.6 6.5-6.3 0-3.6-2.5-6-4.3-8.7-.4 2.2-1.6 3.6-3 4.2C11.6 7.6 10.5 5 8.7 3c.2 3.4-3.2 5.6-3.2 10.2C5.5 18 8.2 21 12 21Z',
  bolt: 'M13 3 5 13.5h6L10.5 21 19 10.5h-6z',
  crown: 'M4 19.5h16M4.5 15.5 3.5 7l5 4L12 5l3.5 6 5-4-1 8.5z',
  'check-circle': 'M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18ZM8 12.3l2.7 2.7L16 9.5',
  hourglass: 'M6.5 3h11M6.5 21h11M7.5 3c0 4 2.5 6 4.5 9-2 3-4.5 5-4.5 9M16.5 3c0 4-2.5 6-4.5 9 2 3 4.5 5 4.5 9',
  sunrise: 'M3 18h18M6.5 18a5.5 5.5 0 0 1 11 0M12 4v3.5M5 10.2l1.6 1.4M19 10.2l-1.6 1.4M8.5 21h7',
}

const fill = () => (props.filled || props.name === 'play' || props.name === 'skip' ? 'currentColor' : 'none')
</script>

<template>
  <svg
    class="icon"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.6"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <path :d="PATHS[name]" :fill="fill()" />
  </svg>
</template>
