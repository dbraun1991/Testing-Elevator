# agents.md — Testing-Elevator: Kontext für schnellen Wiedereinstieg

## Projekt-Zweck

Konferenzvortrag (60 min, technisch affines Publikum, gemischte Test-Erfahrung).
Drei Live-Demos auf verschiedenen Testebenen, verbunden durch einen einzigen Spring Boot Service.
Der Vortrag läuft auf Mac oder Linux Mint.

Narrativer Bogen:
```
P1: "Unsere Validierung ist grün. Alles gut?"
    → Nein. Pitest zeigt: Tests sind blind. Live-Fix auf zweitem Rechner.

P2: "Aber spricht der Service wirklich korrekt mit der DB?"
    → Docker Compose zeigt: Service + DB laufen. Niemand prüft den Inhalt.

P3: "Testcontainers prüft es programmatisch. Alles grün —"
    → "— und trotzdem bricht der Service unter Last ein."

P4: Grafana zeigt den Einbruch live. RSA-4096 unter Last = DDoS-Effekt.
```

---

## Der Service — `keyservice / com.keyservice`

**Stack:** Spring Boot 4.0.3, Java 25 (Oracle JDK 25.0.2), Maven

**Drei Endpunkte:**

| Endpunkt          | Verhalten                                                        | Besonderheit                        |
|-------------------|------------------------------------------------------------------|-------------------------------------|
| `GET /echo?msg=`  | Gibt `msg` zurück                                                | Minimale Last, immer schnell        |
| `GET /uuid`       | Generiert UUID, persistiert in PostgreSQL, gibt UUID zurück      | Ohne DB → HTTP 500 (bewusst)        |
| `GET /key?size=`  | Validiert size, generiert RSA-Key, gibt shortSha + durationMs   | Hohe Last bei size=4096             |

**Validierungslogik (`KeySizeValidator`):**
- Erlaubte Werte: `512, 1024, 2048, 4096` — als `Set<Integer>`
- `isValid(int size)` → `ALLOWED_SIZES.contains(size)`
- Ungültige Werte → `IllegalArgumentException` → HTTP 400
- Diese Klasse ist das alleinige Target für Pitest

**Response `/key`:**
```json
{ "shortSha": "MIIBIjAN", "durationMs": 4821 }
```
`shortSha` = erste 8 Zeichen des Base64-codierten Public Keys.

**Persistenz:**
- `UuidEntry` (JPA Entity): `id`, `uuid`, `createdAt`
- `UuidRepository` extends `JpaRepository`
- Tabelle: `uuid_log`
- DataSource via Umgebungsvariablen konfigurierbar: `SPRING_DATASOURCE_URL`, `_USERNAME`, `_PASSWORD`

**Logging:** Alle drei Services loggen via SLF4J mit Präfixen `[ECHO]`, `[UUID]`, `[KEY]`.
Für die Demo gut sichtbar: bei RSA-4096 unter Last häufen sich `generating RSA-4096 ...`-Zeilen,
während `/echo`-Requests weiterhin schnell durchkommen (Thread-Pool-Effekt in Java).

**Actuator / Prometheus:**
- Endpunkt: `/actuator/prometheus`
- Scrape-Interval in Prometheus: `5s`

---

## Dateistruktur (Stand: final)

```
testing-elevator/
├── plan.md                              # Detaillierter Implementierungsplan
├── README.md                            # Kurzanleitung für Menschen
├── agents.md                            # Diese Datei
├── LICENSE
│
├── keyservice/                          # Spring Boot Service (Basis für alle Stages)
│   ├── dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/keyservice/
│       │   ├── KeyserviceApplication.java
│       │   ├── controller/KeyController.java
│       │   ├── service/
│       │   │   ├── EchoService.java
│       │   │   ├── UuidService.java
│       │   │   └── KeyService.java
│       │   ├── validator/KeySizeValidator.java    ← Pitest-Target
│       │   ├── model/UuidEntry.java               ← JPA Entity
│       │   └── repository/UuidRepository.java
│       ├── main/resources/application.properties
│       └── test/
│           ├── java/com/keyservice/
│           │   ├── validator/
│           │   │   ├── KeySizeValidatorWeakTest.java    ← Demo-Start (P1)
│           │   │   └── KeySizeValidatorStrongTest.java  ← Live-Fix (P1)
│           │   └── integration/
│           │       └── UuidIntegrationTest.java         ← Testcontainers (P3)
│           └── resources/application-test.properties
│
├── p1-unittest/
│   └── Unittest-Readme.md               # TODO-Reminder, kein Code
│
├── p2-docker-containerization/          # Container-Demo
│   ├── docker-compose.yml               # keyservice + postgres:17-alpine
│   └── http/
│       ├── keyservice-api.http          # Alle Endpunkte inkl. 400-Case
│       └── http-client.env.json         # Environments: local, docker
│
├── p3-testcontainers/
│   └── Testcontainers-Readme.md         # Testcontainers braucht keine docker-compose — das ist die Pointe
│
└── p4-loadtest/
    ├── docker-stack.yml                 # keyservice + prometheus + grafana (kein postgres)
    ├── k6/script.js                     # Gestufte Last: warmup → medium → peak
    ├── prometheus/prometheus.yml
    └── grafana/
        ├── config.monitoring            # admin / admin
        └── provisioning/
            ├── datasources/datasource.yml
            └── dashboards/
                ├── dashboard.yml
                └── keyservice-dashboard.json  # 3 Panels: CPU, req/s, max latency
```

---

## Wichtige Befehle

**Service bauen:**
```bash
cd keyservice
mvn package -DskipTests
docker build -t keyservice:latest .
```

**P1 — Pitest:**
```bash
# Schwache Tests → Lücken sehen
mvn test -Dtest=KeySizeValidatorWeakTest
mvn test-compile org.pitest:pitest-maven:mutationCoverage
# Report: target/pit-reports/index.html

# Starke Tests → Score steigt
mvn test -Dtest=KeySizeValidatorStrongTest
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

**P2 — Docker Compose:**
```bash
cd p2-docker-containerization
docker compose up
# http/keyservice-api.http in IDE öffnen, Environment: docker
```

**P3 — Testcontainers:**
```bash
cd keyservice
mvn test -Dtest=UuidIntegrationTest
# Docker muss laufen — Container startet/stoppt automatisch
```

**P4 — Lasttest:**
```bash
cd p4-loadtest
docker compose -f docker-stack.yml up

# Grafana: http://localhost:3000  (admin / admin)
# Prometheus: http://localhost:9090

cd k6
k6 run script.js
```

---

## k6-Szenario (Zeitplan)

| Zeitfenster | Szenario  | Endpunkte                          | Erwarteter Effekt in Grafana         |
|-------------|-----------|-------------------------------------|--------------------------------------|
| 0s – 40s    | warmup    | `/echo` + `/uuid`                   | CPU flach, Latenz niedrig            |
| 30s – 80s   | medium    | + `/key?size=512`                   | Leichter CPU-Anstieg                 |
| 60s – 150s  | peak      | + `/key?size=4096`                  | CPU-Einbruch, Latenz explodiert      |

Threshold `p(95)<2000ms` reißt im Peak — das ist beabsichtigt.

---

## Entscheidungen mit Begründung

| Entscheidung | Begründung |
|---|---|
| `KeySizeValidator` als eigene Klasse | Pitest braucht isolierten Scope — im Controller würde das Rauschen zu groß |
| `/uuid` ohne DB → 500 (bewusst) | Ehrlich für CCC-Publikum; macht Containerization-Konzept greifbar |
| Zwei Testklassen (Weak/Strong) | Dramaturgie: Pitest zeigt Lücken live, Fix auf zweitem Rechner sichtbar |
| Kein PostgreSQL im Lasttest-Stack | Bottleneck soll RSA sein, nicht DB; 500 bei /uuid wird in k6 toleriert |
| Grafana provisioned JSON | Kein manuelles Klicken während der Demo |
| `shortSha` statt vollem Public Key | Lesbar in Konsole und Response; durationMs ist das eigentliche Story-Element |
| k6 Scenarios mit `startTime` | Sauberer als manuelle elapsed-time-Berechnung; Szenarien klar trennbar |

---

## Offene Punkte / mögliche Anpassungen

- **Grafana-Schwellenwerte:** CPU-Thresholds (gelb bei 50%, rot bei 80%) sind auf eine
  Standard-Entwicklermaschine ausgelegt. Bei leistungsstärkerer Hardware ggf. anpassen.
- **k6 VU-Zahlen:** `target: 20` im Peak ist ein Startwert. Je nach Hardware kann dieser
  höher oder niedriger gesetzt werden, damit der Einbruch deutlich sichtbar ist.
- **`SPRING_AUTOCONFIGURE_EXCLUDE` im Lasttest:** Deaktiviert JPA/DataSource ohne PostgreSQL.
  Fallback: `application-loadtest.properties` mit leerem `spring.datasource.url` und Profil setzen.
- **Dockerfile mvnw:** Setzt voraus, dass `mvnw` im Projekt liegt (Spring Initializr generiert dies).
  Alternativ: JAR lokal bauen, Dockerfile kopiert nur das fertige JAR.