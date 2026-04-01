# Testing-Elevator — Kurzreferenz

## Vorbereitung

```bash
cd keyservice
mvn package -DskipTests
docker build -t keyservice:latest .
```

---

## Einstieg

```bash
cd keyservice
mvn spring-boot:run
```

```
GET http://localhost:8081/echo?msg=hello
GET http://localhost:8081/key?size=512
GET http://localhost:8081/key?size=9999
```

---

## P1 — Unittest + Mutation Testing

```bash
mvn test -Dtest=KeySizeValidatorWeakTest
open target/site/jacoco/index.html
mvn test-compile org.pitest:pitest-maven:mutationCoverage
open target/pit-reports/index.html
```

`@Disabled` in `KeySizeValidatorStrongTest.java` entfernen.

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

---

## P2 — Docker Compose

```bash
cd keyservice
mvn package -DskipTests
docker build -t keyservice:latest .
cd ../p2-docker-containerization
docker compose up
```

```
GET /echo?msg=hello-testing-elevator
GET /uuid
GET /key?size=512
GET /key?size=2048
GET /key?size=9999
```

```bash
docker compose down
```

---

## P3 — Testcontainers

```bash
cd keyservice
mvn test -Dtest=UuidIntegrationTest
```

---

## P4 — Lasttest

```bash
cd p4-loadtest
docker compose -f docker-stack.yml up
```

```
http://localhost:3000   (admin / admin)
http://localhost:9090
http://localhost:8081/actuator/prometheus
```

```bash
cd k6
k6 run script.js
```

```bash
cd p4-loadtest
docker compose -f docker-stack.yml down -v
```
