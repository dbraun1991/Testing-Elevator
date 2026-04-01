import http from 'k6/http';
import { sleep, check } from 'k6';

/**
 * P4 — Lasttest
 *
 * Vier Szenarien laufen nacheinander — jeder Endpunkt spiked einzeln:
 *   warmup      → alle Endpunkte  (0s  – 5s)    1 VU, Linien sichtbar machen
 *   echoSpike   → /echo           (10s – 50s)   schnell, flach
 *   uuidSpike   → /uuid           (60s – 100s)  mit DB, flach
 *   key512Spike → /key?size=512   (110s – 150s) kleine Keys, kein Problem
 *   key4096Peak → /key?size=4096  (160s – 240s) große Keys — Einbruch
 *
 * Starten:
 *   k6 run script.js
 *
 * Grafana:    http://localhost:3000  (admin / admin)
 * Prometheus: http://localhost:9090
 */

const BASE_URL = 'http://localhost:8081';

export const options = {
  scenarios: {
    warmup: {
      executor:  'shared-iterations',
      startTime: '0s',
      vus:        1,
      iterations: 1,
      maxDuration: '10s',
      exec: 'scenarioWarmup',
    },
    echoSpike: {
      executor:  'ramping-vus',
      startTime: '10s',
      stages: [
        { duration: '10s', target: 20 },
        { duration: '20s', target: 20 },
        { duration: '10s', target: 0  },
      ],
      exec: 'scenarioEcho',
    },
    uuidSpike: {
      executor:  'ramping-vus',
      startTime: '60s',
      stages: [
        { duration: '10s', target: 20 },
        { duration: '20s', target: 20 },
        { duration: '10s', target: 0  },
      ],
      exec: 'scenarioUuid',
    },
    key512Spike: {
      executor:  'ramping-vus',
      startTime: '110s',
      stages: [
        { duration: '10s', target: 20 },
        { duration: '20s', target: 20 },
        { duration: '10s', target: 0  },
      ],
      exec: 'scenarioKey512',
    },
    key4096Peak: {
      executor:  'ramping-vus',
      startTime: '160s',
      stages: [
        { duration: '15s', target: 20 },
        { duration: '50s', target: 20 },
        { duration: '15s', target: 0  },
      ],
      exec: 'scenarioKey4096',
    },
  },
  thresholds: {
    // Wird im key4096-Peak reißen — beabsichtigt
    'http_req_duration': ['p(95)<2000'],
  },
};

// --- Szenario-Funktionen ---

export function scenarioWarmup() {
  http.get(`${BASE_URL}/echo?msg=warmup`);
  http.get(`${BASE_URL}/uuid`);
  http.get(`${BASE_URL}/key?size=512`);
  http.get(`${BASE_URL}/key?size=4096`);
}

export function scenarioEcho() {
  const res = http.get(`${BASE_URL}/echo?msg=k6-load-test`);
  check(res, { 'echo 200': (r) => r.status === 200 });
  sleep(1);
}

export function scenarioUuid() {
  const res = http.get(`${BASE_URL}/uuid`);
  check(res, { 'uuid 200': (r) => r.status === 200 });
  sleep(1);
}

export function scenarioKey512() {
  const res = http.get(`${BASE_URL}/key?size=512`);
  check(res, { 'key512 200': (r) => r.status === 200 });
  sleep(1);
}

export function scenarioKey4096() {
  const res = http.get(`${BASE_URL}/key?size=4096`);
  check(res, { 'key4096 200': (r) => r.status === 200 });
  sleep(1);
}
