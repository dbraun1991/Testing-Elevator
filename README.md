# Testing-Elevator
Testing auf verschiedenen Abstraktions- bzw. Testebenen.

Ein Spring Boot Service als roter Faden durch drei Teststufen —
von der Unit-Test-Validierung bis zum Lasttest mit Grafana.

## Der Service

Drei Endpunkte, ein Service:

| Endpunkt          | Was er tut                                              |
|-------------------|---------------------------------------------------------|
| `GET /echo?msg=`  | Gibt `msg` zurück                                       |
| `GET /uuid`       | Generiert UUID, persistiert in PostgreSQL               |
| `GET /key?size=`  | Generiert RSA-Key (512/1024/2048/4096 Bit), gibt `shortSha` + `durationMs` zurück |

---

## Stages

### P1 — Unittest + Mutation Testing
```
cd keyservice
mvn test -Dtest=KeySizeValidatorWeakTest
mvn test-compile org.pitest:pitest-maven:mutationCoverage
# → Report: target/pit-reports/index.html
```

Danach den Fix einspielen:
```
mvn test -Dtest=KeySizeValidatorStrongTest
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

### P2 — Container-Demo (Docker Compose)
```
# Image bauen
cd keyservice
mvn package -DskipTests
docker build -t keyservice:latest .

# Stack starten
cd ../p2-docker-containerization
docker compose up
```
Dann `http/keyservice-api.http` in IntelliJ / VS Code öffnen, Environment `docker` wählen.

### P3 — Integrationstest (Testcontainers)
```
cd keyservice
mvn test -Dtest=UuidIntegrationTest
```
Docker muss laufen. PostgreSQL-Container startet und stoppt automatisch.

### P4 — Lasttest (k6 + Prometheus + Grafana)
```
# Stack starten
cd p4-loadtest
docker compose -f docker-stack.yml up

# Grafana öffnen: http://localhost:3000  (admin / admin)
# Dashboard "Keyservice — Load Test" ist automatisch geladen

# k6 starten (separates Terminal)
cd k6
k6 run script.js
```

---

## Versionen

| Komponente     | Version           |
|----------------|-------------------|
| Java           | Oracle JDK 25.0.2 |
| Spring Boot    | 4.0.3             |
| PostgreSQL     | 17 (alpine)       |
| Testcontainers | 1.20.x            |
| Pitest         | 1.17.x            |
| Prometheus     | v3.2.1            |
| Grafana        | 11.5.0            |
| k6             | latest binary     |
| Docker Compose | v2                |
