import http from 'k6/http';
import { sleep, check } from 'k6';

/**
 * P4 — Lasttest
 *
 * Drei Szenarien laufen zeitversetzt nacheinander:
 *   warmup  → /echo + /uuid                  (0s  – 40s)
 *   medium  → /echo + /uuid + /key?size=512   (30s – 80s)
 *   peak    → /echo + /uuid + /key?size=4096  (60s – 150s) ← DDoS-Moment
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
      executor:  'ramping-vus',
      startTime: '0s',
      stages: [
        { duration: '10s', target: 5 },
        { duration: '20s', target: 5 },
        { duration: '10s', target: 0 },
      ],
      exec: 'scenarioWarmup',
    },
    medium: {
      executor:  'ramping-vus',
      startTime: '30s',
      stages: [
        { duration: '10s', target: 10 },
        { duration: '20s', target: 10 },
        { duration: '10s', target: 0  },
      ],
      exec: 'scenarioMedium',
    },
    peak: {
      executor:  'ramping-vus',
      startTime: '60s',
      stages: [
        { duration: '15s', target: 20 },
        { duration: '60s', target: 20 },
        { duration: '15s', target: 0  },
      ],
      exec: 'scenarioPeak',
    },
  },
  thresholds: {
    // Wird im Peak-Szenario reißen — das ist beabsichtigt
    'http_req_duration': ['p(95)<2000'],
  },
};

// --- Szenario-Funktionen ---

export function scenarioWarmup() {
  echo();
  uuid();
  sleep(1);
}

export function scenarioMedium() {
  echo();
  uuid();
  keyGen(512);
  sleep(1);
}

export function scenarioPeak() {
  echo();
  uuid();
  keyGen(4096);
  sleep(1);
}

// --- Endpunkt-Funktionen ---

function echo() {
  const res = http.get(`${BASE_URL}/echo?msg=k6-load-test`);
  check(res, { 'echo 200': (r) => r.status === 200 });
}

function uuid() {
  const res = http.get(`${BASE_URL}/uuid`);
  // 500 erwartet wenn kein PostgreSQL im Stack — bewusst nicht als Fehler gewertet
  check(res, { 'uuid responded': (r) => r.status === 200 || r.status === 500 });
}

function keyGen(size) {
  const res = http.get(`${BASE_URL}/key?size=${size}`);
  check(res, { [`key ${size} 200`]: (r) => r.status === 200 });
}
