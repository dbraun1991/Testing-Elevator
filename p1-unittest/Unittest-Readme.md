# p1-unittest

**TODO:** Pitest-Konfiguration und Testergebnisse hier ablegen.

Relevante Dateien befinden sich unter:
```
keyservice/src/test/java/com/keyservice/validator/
├── KeySizeValidatorWeakTest.java
└── KeySizeValidatorStrongTest.java
```

Pitest-Report nach Ausführung unter:
```
keyservice/target/pit-reports/index.html
```

Ausführung:
```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```
