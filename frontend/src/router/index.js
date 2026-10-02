import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'

export const navItems = [
  { path: '/', name: 'dashboard', label: 'Dashboard', icon: '📊' },
  { path: '/timer', name: 'timer', label: 'Timer', icon: '⏱️' },
  { path: '/subjects', name: 'subjects', label: 'Materias', icon: '📘' },
  { path: '/schedule', name: 'schedule', label: 'Plan semanal', icon: '🗓️' },
  { path: '/history', name: 'history', label: 'Historial', icon: '🕘' },
  { path: '/goals', name: 'goals', label: 'Metas y logros', icon: '🏆' },
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/timer', name: 'timer', component: () => import('@/views/TimerView.vue') },
    { path: '/subjects', name: 'subjects', component: () => import('@/views/SubjectsView.vue') },
    { path: '/schedule', name: 'schedule', component: () => import('@/views/ScheduleView.vue') },
    { path: '/history', name: 'history', component: () => import('@/views/HistoryView.vue') },
    { path: '/goals', name: 'goals', component: () => import('@/views/GoalsView.vue') },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

export default router
