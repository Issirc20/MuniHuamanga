import http from 'k6/http';
import { check, sleep } from 'k6';

// Configuración de prueba de carga con 150 usuarios virtuales concurrentes (US-15, RNF-01, RNF-02)
export const options = {
  stages: [
    { duration: '5s', target: 50 },   // Calentamiento a 50 usuarios
    { duration: '10s', target: 150 }, // Carga sostenida a 150 usuarios concurrentes
    { duration: '5s', target: 0 },    // Enfriamiento
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'], // 95% de peticiones deben responder en < 3000 ms (RNF-02)
    http_req_failed: ['rate<0.01'],    // Tasa de error menor al 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export default function () {
  // 1. Verificación pública de licencia por código QR
  const resVerificacion = http.get(`${BASE_URL}/api/public/licencias/LIC-2026-00000002`);
  check(resVerificacion, {
    'Verificación QR status es 200': (r) => r.status === 200,
    'Licencia está vigente y autorizada': (r) => r.body.includes('VIGENTE / AUTORIZADA'),
  });

  // 2. Consulta de seguimiento ciudadano de expediente
  const resExpediente = http.get(`${BASE_URL}/api/expedientes/tramite/EXP-2026-00001`);
  check(resExpediente, {
    'Consulta expediente status es 200': (r) => r.status === 200,
  });

  sleep(0.5);
}
