import http from './http'

const data = (promise) => promise.then((r) => r.data)

export const subjectsApi = {
  list: () => data(http.get('/subjects')),
  create: (body) => data(http.post('/subjects', body)),
  update: (id, body) => data(http.put(`/subjects/${id}`, body)),
  remove: (id) => http.delete(`/subjects/${id}`),
}

export const scheduleApi = {
  list: () => data(http.get('/schedule')),
  create: (body) => data(http.post('/schedule', body)),
  update: (id, body) => data(http.put(`/schedule/${id}`, body)),
  remove: (id) => http.delete(`/schedule/${id}`),
}

export const sessionsApi = {
  list: (params) => data(http.get('/sessions', { params })),
  // 204 (sin sesión activa) llega como cadena vacía: lo normalizamos a null
  active: () => data(http.get('/sessions/active')).then((s) => s || null),
  start: (body) => data(http.post('/sessions/start', body)),
  pause: (id) => data(http.post(`/sessions/${id}/pause`)),
  resume: (id) => data(http.post(`/sessions/${id}/resume`)),
  completePomodoro: (id) => data(http.post(`/sessions/${id}/pomodoro`)),
  stop: (id, body) => data(http.post(`/sessions/${id}/stop`, body)),
  createManual: (body) => data(http.post('/sessions', body)),
  update: (id, body) => data(http.put(`/sessions/${id}`, body)),
  remove: (id) => http.delete(`/sessions/${id}`),
}

export const goalsApi = {
  list: () => data(http.get('/goals')),
  create: (body) => data(http.post('/goals', body)),
  update: (id, body) => data(http.put(`/goals/${id}`, body)),
  remove: (id) => http.delete(`/goals/${id}`),
}

export const achievementsApi = {
  list: () => data(http.get('/achievements')),
}

export const statsApi = {
  dashboard: () => data(http.get('/stats/dashboard')),
  heatmap: (days = 365) => data(http.get('/stats/heatmap', { params: { days } })),
}
