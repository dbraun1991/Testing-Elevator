# Testing-Elevator — Implementierungsplan

## Idee & Narrativer Bogen

Konferenzvortrag (60 min, CCC-Publikum) mit drei Live-Demos auf verschiedenen Testebenen.
Ein einziger Spring Boot Service zieht sich durch alle Stages — das Publikum lernt keinen neuen
Kontext, nur die Perspektive wechselt.

```
"Unsere Validierung ist grün. Alles gut?"
    → Nein. Pitest zeigt: die Tests sind blind.

"Wir fixen es. Aber spricht der Service wirklich mit der DB?"
    → Docker Compose sieht gut aus — aber niemand prüft den Inhalt.

"Testcontainers prüft es. Alles grün — und trotzdem bricht der Service ein."
    → Grafana zeigt den Einbruch live.
```

---

## Der Service — Drei Endpunkte

| Endpunkt             | Verhalten                                                                 |
|----------------------|---------------------------------------------------------------------------|
| `GET /echo?msg=`     | Gibt `msg` unverändert zurück                                             |
| `GET /uuid`          | Generiert UUIDv4, persistiert in PostgreSQL, gibt UUID zurück             |
| `GET /key?size=`     | Validiert size (512/1024/2048/4096), generiert RSA-Key, gibt zurück:      |
|                      | `{ "shortSha": "a3f9c1d2", "durationMs": 4821 }`                         |

Ungültige `size`-Werte → HTTP 400.
`/uuid` ohne PostgreSQL → HTTP 500 (bewusst, für Container-Demo).

---

## Stages

### Stage 1 — Basis: Spring Boot Service
Fundament. Alle weiteren Stages bauen darauf auf.

### Stage 2 — P1: Unittest + Mutation Testing (Pitest)
Scope: `KeySizeValidator`.
Dramaturgie: Schwache Tests → Pitest zeigt Lücken → Live-Fix.

### Stage 3 — P2: Container-Demo (Docker Compose)
Scope: Service + PostgreSQL als Docker Compose Stack.
Manuelle HTTP-Requests via `.http`-File.
Persistenz läuft — wird aber nicht verifiziert.

### Stage 4 — P3: Integrationstest (Testcontainers)
Scope: `/uuid` + PostgreSQL-Persistenz programmatisch verifiziert.
Pointe: Gleicher Aufbau wie Stage 3 — aber jetzt wird der DB-Inhalt geprüft.

### Stage 5 — P4: Lasttest (k6 + Prometheus + Grafana)
Scope: Alle drei Endpunkte unter Last. RSA-4096 als DDoS-Demonstration.
Grafana zeigt den Einbruch live.

---

## File-Tree

```
testing-elevator/
│
├── plan.md
│
├── keyservice/                          # Block 1 — Spring Boot Service
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/keyservice/
│       │   │   ├── KeyserviceApplication.java
│       │   │   ├── controller/
│       │   │   │   └── KeyController.java
│       │   │   ├── service/
│       │   │   │   ├── EchoService.java
│       │   │   │   ├── UuidService.java
│       │   │   │   └── KeyService.java
│       │   │   ├── validator/
│       │   │   │   └── KeySizeValidator.java        # Pitest-Target
│       │   │   ├── model/
│       │   │   │   └── UuidEntry.java               # JPA Entity
│       │   │   └── repository/
│       │   │       └── UuidRepository.java
│       │   └── resources/
│       │       └── application.properties
│       └── test/
│           ├── java/com/keyservice/
│           │   ├── validator/
│           │   │   ├── KeySizeValidatorWeakTest.java   # P1 — schwache Tests
│           │   │   └── KeySizeValidatorStrongTest.java # P1 — starke Tests (Live-Fix)
│           │   └── integration/
│           │       └── UuidIntegrationTest.java        # P3 — Testcontainers
│           └── resources/
│               └── application-test.properties
│
├── docker/                              # Block 3 — Container-Demo
│   ├── docker-compose.yml               # Service + PostgreSQL
│   └── http/
│       ├── keyservice-api.http
│       └── http-client.env.json
│
└── loadtest/                            # Block 5 — Lasttest
    ├── docker-stack.yml                 # Service + Prometheus + Grafana (kein PostgreSQL)
    ├── k6/
    │   └── script.js
    ├── prometheus/
    │   └── prometheus.yml
    └── grafana/
        ├── config.monitoring
        └── provisioning/
            ├── datasources/
            │   └── datasource.yml
            └── dashboards/
                ├── dashboard.yml
                └── keyservice-dashboard.json
```

---

## Detaillierte Umsetzungsschritte

---

### Block 1 — Spring Boot Service

**Schritt 1.1 — `pom.xml`**
- Spring Boot Parent: `4.0.3`
- Java: `25`
- Dependencies:
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-actuator`
  - `micrometer-registry-prometheus`
  - `postgresql` (runtime)
  - `spring-boot-starter-test` (test)
  - `org.testcontainers:postgresql` (test)
  - `org.testcontainers:junit-jupiter` (test)
- Plugins:
  - `pitest-maven` mit `pitest-junit5-plugin`

**Schritt 1.2 — `KeySizeValidator.java`**
- Methode: `boolean isValid(int size)`
- Erlaubte Werte: `512, 1024, 2048, 4096` — als `Set<Integer>`
- Bewusst einfach gehalten: eine Zeile Logik, maximale Mutations-Angriffsfläche

**Schritt 1.3 — `UuidEntry.java` (JPA Entity)**
- Felder: `id` (Long, auto), `uuid` (String), `createdAt` (LocalDateTime)
- `@Entity`, `@Table(name = "uuid_log")`

**Schritt 1.4 — `UuidRepository.java`**
- `extends JpaRepository<UuidEntry, Long>`

**Schritt 1.5 — Services**
- `EchoService`: gibt `msg` zurück
- `UuidService`: generiert UUID, persistiert via Repository, gibt UUID-String zurück
- `KeyService`:
  - Ruft `KeySizeValidator.isValid()` auf — wirft `IllegalArgumentException` bei ungültigem Wert
  - Generiert RSA-KeyPair mit `KeyPairGenerator`
  - Misst Generierungsdauer in ms
  - Gibt `shortSha` (erste 8 Zeichen des Base64-Public-Key) + `durationMs` zurück

**Schritt 1.6 — `KeyController.java`**
- `GET /echo?msg=` → `EchoService`
- `GET /uuid` → `UuidService`
- `GET /key?size=` → `KeyService`; `IllegalArgumentException` → HTTP 400

**Schritt 1.7 — `application.properties`**
- Port: `8081`
- DataSource: konfigurierbar via Umgebungsvariablen (`SPRING_DATASOURCE_URL` etc.)
- Actuator: `management.endpoints.web.exposure.include=health,info,prometheus`
- Prometheus-Endpoint: `management.endpoint.prometheus.enabled=true`

**Schritt 1.8 — `Dockerfile`**
- Base: `eclipse-temurin:25-jdk-alpine` (Build-Stage) + `eclipse-temurin:25-jre-alpine` (Runtime)
- Multi-stage Build
- Exponiert Port `8081`

---

### Block 2 — P1: JUnit + Pitest

**Schritt 2.1 — `KeySizeValidatorWeakTest.java`** *(schwache Tests — Demo-Start)*
- Testet nur: `isValid(512)` → true, `isValid(4096)` → true
- Kein Test für ungültige Werte
- Kein Boundary-Test
- Pitest Mutation Score: ~30–40% (Lücken sichtbar)

**Schritt 2.2 — Pitest-Konfiguration in `pom.xml`**
- `targetClasses`: `com.keyservice.validator.*`
- `targetTests`: `com.keyservice.validator.*`
- `mutators`: `DEFAULTS`
- `outputFormats`: `HTML`
- Ausführung: `mvn test-compile org.pitest:pitest-maven:mutationCoverage`

**Schritt 2.3 — `KeySizeValidatorStrongTest.java`** *(Live-Fix auf zweitem Rechner)*
- Alle vier erlaubten Werte: `isValid(512/1024/2048/4096)` → true
- Typische ungültige Werte: `0`, `511`, `513`, `2047`, `2049`, `-1`, `Integer.MAX_VALUE` → false
- Grenzwert benachbart zu erlaubten Werten (Boundary-Tests)
- Pitest Mutation Score: ~95–100%

---

### Block 3 — P2: Container-Demo (Docker Compose)

**Schritt 3.1 — `docker-compose.yml`**
- Service `keyservice`: Image `keyservice:latest`, Port `8081:8081`
  - Umgebungsvariablen für DataSource zeigen auf `postgres`-Service
- Service `postgres`: `postgres:17-alpine`
  - DB: `keyservice`, User: `keyservice`, Password: `keyservice`
- Healthcheck für PostgreSQL
- `keyservice` depends_on postgres mit condition `service_healthy`

**Schritt 3.2 — `keyservice-api.http`**
- Request 1: `GET /echo?msg=hello`
- Request 2: `GET /uuid`
- Request 3: `GET /key?size=512`
- Request 4: `GET /key?size=2048`
- Request 5: `GET /key?size=9999` (→ 400, für Demo)

**Schritt 3.3 — `http-client.env.json`**
- Environment `local`: `server = http://localhost:8081`
- Environment `docker`: `server = http://localhost:8081` (identisch, für Klarheit)

---

### Block 4 — P3: Testcontainers

**Schritt 4.1 — `application-test.properties`**
- DataSource auf Testcontainers-Platzhalter (oder leer lassen, da Testcontainers dynamic port übernimmt)
- `spring.jpa.hibernate.ddl-auto=create-drop`

**Schritt 4.2 — `UuidIntegrationTest.java`**
- Annotationen: `@SpringBootTest`, `@Testcontainers`, `@ActiveProfiles("test")`
- `@Container`: `PostgreSQLContainer` (`postgres:17-alpine`)
- `@DynamicPropertySource`: DataSource-URL, User, Password aus Container
- Test `uuidIsPersisted`:
  1. HTTP GET `/uuid` via `TestRestTemplate`
  2. UUID aus Response extrahieren
  3. Via `UuidRepository.findAll()` prüfen ob Eintrag mit dieser UUID in DB existiert
  4. AssertJ: `assertThat(entries).anyMatch(e -> e.getUuid().equals(responseUuid))`
- Teardown: automatisch durch Testcontainers

---

### Block 5 — P4: Lasttest

**Schritt 5.1 — `docker-stack.yml`**
- Service `keyservice`: Image `keyservice:latest`, Port `8081:8081`
  - **Kein PostgreSQL** — `/uuid` gibt 500 zurück (bewusst, nicht Gegenstand des Lasttests)
  - Umgebungsvariable `JAVA_OPTS`: `-Xmx512m` (Ressourcen begrenzen für sichtbaren Effekt)
- Service `prometheus`: `prom/prometheus:v3.x`, Port `9090:9090`
- Service `grafana`: `grafana/grafana:11.x`, Port `3000:3000`
  - Provisioning via Volumes eingehängt

**Schritt 5.2 — `prometheus.yml`**
- Scrape-Interval: `5s`
- Target: `keyservice:8081/actuator/prometheus`

**Schritt 5.3 — `script.js` (k6)**
- Stages (gestuft):
  1. `duration: '30s', target: 5` — Warmup: nur `/echo` + `/uuid`
  2. `duration: '30s', target: 10` — Anstieg: `/key?size=512`
  3. `duration: '60s', target: 20` — Peak: `/key?size=4096` — **DDoS-Moment**
  4. `duration: '30s', target: 0` — Abfall
- Default-Funktion:
  - Warmup-Phase: `/echo` + `/uuid`
  - Ab Stage 2: `/key?size=512`
  - Ab Stage 3: `/key?size=4096`
- Thresholds: `http_req_duration: ['p(95)<2000']` — wird in Stage 3 reißen

**Schritt 5.4 — Grafana Dashboard (`keyservice-dashboard.json`)**
- Provisioned JSON (kein manuelles Klicken)
- Panels:
  - `process_cpu_usage` — CPU-Last (Hauptpanel, zeigt den Einbruch)
  - `http_server_requests_seconds_count` — Request-Rate
  - `http_server_requests_seconds_max` — maximale Response-Zeit
- Refresh: `5s`
- Zeitfenster: `last 15 minutes`

**Schritt 5.5 — `datasource.yml` + `dashboard.yml`**
- Grafana Datasource: Prometheus via `http://prometheus:9090`
- Dashboard-Pfad: `/etc/grafana/provisioning/dashboards/`

---

## Technologie-Versionen (final)

| Komponente     | Version           |
|----------------|-------------------|
| Java           | Oracle JDK 25.0.2 |
| Spring Boot    | 4.0.3             |
| PostgreSQL     | 17 (alpine)       |
| Testcontainers | 1.20.x            |
| JUnit 5        | via Spring Boot   |
| Pitest         | 1.17.x            |
| k6             | latest binary     |
| Prometheus     | v3.x              |
| Grafana        | 11.x              |
| Docker Compose | v2                |