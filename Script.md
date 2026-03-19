# Testing-Elevator — Präsentationsskript

---

## Vorbereitung (vor dem Vortrag)

- [ ] Docker läuft
- [ ] IntelliJ / IDE geöffnet mit `keyservice/` als Projekt
- [ ] Terminal bereit in `keyservice/`
- [ ] Browser bereit (für JaCoCo-Report, Pitest-Report, Grafana)
- [ ] Zweiter Rechner / zweites Fenster mit `KeySizeValidatorStrongTest.java` geöffnet
- [ ] Service einmalig bauen und Docker-Image erstellen:

```bash
cd keyservice
mvn package -DskipTests
docker build -t keyservice:latest .
```

---

## Einstieg — Der Service

Kurz vorstellen was der Service tut und die drei Endpunkte zeigen.

```bash
# Service lokal starten (ohne Docker, für den Einstieg)
cd keyservice
mvn spring-boot:run
```

Im Browser / Postman / Terminal kurz zeigen:
```
GET http://localhost:8081/echo?msg=hello
GET http://localhost:8081/key?size=512
GET http://localhost:8081/key?size=9999   ← 400
```

Service stoppen.

---

## P1 — Unittest + Mutation Testing

### 1.1 — Code zeigen

`KeySizeValidator.java` öffnen. Eine Methode, eine Zeile Logik.
Publikum sieht: das ist überschaubar, das kann man testen.

### 1.2 — Schwache Tests zeigen

`KeySizeValidatorWeakTest.java` öffnen.
Zwei Tests. Sehen okay aus.

### 1.3 — Tests ausführen

```bash
mvn test -Dtest=KeySizeValidatorWeakTest
```

Alles grün. ✓

### 1.4 — JaCoCo Coverage zeigen

```bash
# Coverage wurde automatisch mit mvn test erzeugt
# Report öffnen:
open target/site/jacoco/index.html
```

Coverage ist hoch. Der Code wird durchlaufen. Alles gut?

**Pause. Frage ans Publikum.**

### 1.5 — Pitest starten

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

```bash
# Report öffnen:
open target/pit-reports/index.html
```

Viele Mutationen überleben. Die Tests sind blind.
Beispiel zeigen: `contains` → `!contains` — kein Test schlägt an.

### 1.6 — Live-Fix (zweiter Rechner / zweites Fenster)

`KeySizeValidatorStrongTest.java` öffnen.
`@Disabled` entfernen.

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Report neu laden. Score ~95–100%.

**Erkenntnis:** Coverage sagt nicht, ob Tests etwas prüfen. Pitest schon.

---

## P2 — Container-Demo (Docker Compose)

### 2.1 — Überleitung

*"Die Validierung ist jetzt sauber. Aber spricht der Service wirklich korrekt mit der Datenbank?"*

### 2.2 — Stack starten

```bash
# Image bauen (muss lokal vorhanden sein — wird nicht aus Registry gepullt)
cd keyservice
mvn package -DskipTests
docker build -t keyservice:latest .

# Stack starten
cd ../p2-docker-containerization
docker compose up
```

Zwei Container starten: `keyservice` + `postgres`.
Logs zeigen: Service ist bereit.

### 2.3 — HTTP-Requests feuern

`http/keyservice-api.http` in IDE öffnen. Environment: `docker`.

Requests der Reihe nach ausführen:
```
GET /echo?msg=hello-testing-elevator     ← sofortige Antwort
GET /uuid                                ← UUID zurück, DB persistiert still
GET /key?size=512                        ← schnell
GET /key?size=2048                       ← merklich langsamer
GET /key?size=9999                       ← 400
```

Konsolen-Log zeigt `[UUID] generated=...` und `[UUID] persisted=...`.

### 2.4 — Die Frage stellen

*"Ist die UUID wirklich in der Datenbank? Wir sehen es im Log — aber prüft das jemand?"*

```bash
# Stack stoppen
docker compose down
```

---

## P3 — Integrationstest (Testcontainers)

### 3.1 — Überleitung

*"Testcontainers prüft es. Gleicher Aufbau — aber programmatisch verifiziert."*

`UuidIntegrationTest.java` öffnen. Kurz erklären:
- `@Testcontainers` + `@Container` → PostgreSQL startet aus dem Test heraus
- `@DynamicPropertySource` → DataSource wird automatisch verdrahtet
- Assertion: UUID aus Response == UUID in DB

### 3.2 — Test ausführen

```bash
cd keyservice
mvn test -Dtest=UuidIntegrationTest
```

Im Terminal sichtbar:
- Container-Start
- Schema-Erstellung (`CREATE TABLE uuid_log`)
- Test läuft
- Teardown — automatisch

Alles grün. ✓

**Erkenntnis:** Docker Compose zeigt, dass es läuft. Testcontainers beweist, dass es korrekt ist.

---

## P4 — Lasttest (k6 + Prometheus + Grafana)

### 4.1 — Überleitung

*"Alles grün. Unittest grün, Integrationstest grün — und trotzdem bricht der Service ein."*

### 4.2 — Stack starten

```bash
cd p4-loadtest
docker compose -f docker-stack.yml up
```

Drei Container: `keyservice`, `prometheus`, `grafana`.

### 4.3 — Grafana öffnen

```
http://localhost:3000
Login: admin / admin
```

Dashboard **"Keyservice — Load Test"** ist automatisch geladen.
Drei Panels: CPU, Request Rate, Max Response Time.
Alles flach. Noch.

### 4.4 — k6 starten

```bash
cd k6
k6 run script.js
```

**Szenario Warmup (0–40s):**
`/echo` + `/uuid` — CPU bleibt flach, Grafana zeigt kaum Aktivität.

**Szenario Medium (30–80s):**
`/key?size=512` kommt dazu — leichter Anstieg sichtbar.

**Szenario Peak (60–150s):**
`/key?size=4096` unter 20 VUs — **Grafana zeigt den Einbruch.**
CPU steigt, Max Response Time explodiert.
Im Service-Log: `[KEY] generating RSA-4096 ...` häuft sich.
`/echo`-Requests kommen weiter schnell durch — Thread-Pool-Effekt sichtbar.

k6-Terminal: Threshold `p(95)<2000ms` reißt. Rot.

### 4.5 — Abfall

Last geht zurück. Grafana erholt sich. CPU sinkt.

**Erkenntnis:** Kein Unittest, kein Integrationstest hätte das gezeigt.
Nur Last deckt auf, wie sich ein System unter Druck verhält.

---

## Abschluss

| Stage | Tool | Frage | Antwort |
|---|---|---|---|
| P1 | JaCoCo + Pitest | Sind meine Tests gut? | Coverage lügt. Mutation Testing nicht. |
| P2 | Docker Compose | Läuft der Service mit der DB? | Ja — aber niemand prüft den Inhalt. |
| P3 | Testcontainers | Ist die Persistenz korrekt? | Ja — programmatisch bewiesen. |
| P4 | k6 + Grafana | Hält der Service Last stand? | Nein — und nur Last zeigt es. |
