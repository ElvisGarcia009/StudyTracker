import axios from 'axios'

// Todas las llamadas van a /api. En Docker, nginx las pasa al backend;
// en desarrollo lo hace el proxy de Vite (ver vite.config.js).
const http = axios.create({ baseURL: '/api' })

// Convierte los errores del backend ({ status, message }) en un Error legible
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message =
      error.response?.data?.message ||
      (error.response ? `Error ${error.response.status}` : 'No se pudo conectar con el servidor')
    return Promise.reject(new Error(message))
  },
)

export default http
