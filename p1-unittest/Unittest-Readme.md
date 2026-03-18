# p1-unittest

## Ziel dieser Stage

Zeigen, dass hohe Code-Coverage nicht bedeutet, dass Tests gut sind.

Der Ablauf in zwei Schritten:

---

## Schritt 1 — Code Coverage mit JaCoCo

**Was wird gezeigt:** Die schwachen Tests laufen durch. Coverage sieht gut aus.
**Pointe:** 100% Coverage sagt nichts darüber aus, ob die Tests tatsächlich etwas prüfen.

```bash
cd keyservice
mvn test -Dtest=KeySizeValidatorWeakTest
```

Report öffnen:
```
target/site/jacoco/index.html
```

Coverage ist hoch — der Code wird durchlaufen. Alles grün. Alles gut?

---

## Schritt 2 — Mutation Testing mit Pitest

**Was wird gezeigt:** Pitest verändert den Code minimal (Mutationen) und prüft,
ob ein Test diese Veränderung bemerkt. Schwache Tests bemerken es nicht.

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Report öffnen:
```
target/pit-reports/index.html
```

Viele Mutationen überleben — die Tests sind blind.

---

## Schritt 3 — Live-Fix

Die starken Tests einspielen und Pitest erneut ausführen:

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage -Dtest=KeySizeValidatorStrongTest
```

Mutation Score steigt auf ~95–100%.

---

## Relevante Testklassen

```
keyservice/src/test/java/com/keyservice/validator/
├── KeySizeValidatorWeakTest.java    ← Demo-Start (schwach)
└── KeySizeValidatorStrongTest.java  ← Live-Fix   (stark)
```
