# p3-testcontainers

**TODO:** Integrationstest mit Testcontainers hier ablegen.

Zieldatei:
```
keyservice/src/test/java/com/keyservice/integration/UuidIntegrationTest.java
```

Testcontainers startet eine echte PostgreSQL-Instanz aus dem JUnit-Test heraus.
Teardown erfolgt automatisch — kein manuelles Aufräumen nötig.

Ausführung:
```bash
mvn test -Dtest=UuidIntegrationTest
```
