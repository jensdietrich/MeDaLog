# JaCoCo and PITest

## JaCoCo

| Field        | Value |
|--------------|-------|
| GAV          | `org.jacoco:jacoco-maven-plugin:0.8.11` |
| purl         | `pkg:maven/org.jacoco/jacoco-maven-plugin@0.8.11` |
| Maven Central | https://mvnrepository.com/artifact/org.jacoco/jacoco-maven-plugin/0.8.11 |
| License      | Eclipse Public License 2.0 |
| Source       | https://github.com/jacoco/jacoco |

JaCoCo (Java Code Coverage) generates statement, branch, and line coverage reports during
`mvn test`. Reports are written to `target/site/jacoco/`.

**Why JaCoCo:** EPL 2.0, de-facto standard Maven coverage plugin, integrates with all major
CI systems, zero additional dependencies at compile/test time.

---

## PITest

| Field        | Value |
|--------------|-------|
| GAV          | `org.pitest:pitest-maven:1.15.3` + `org.pitest:pitest-junit5-plugin:1.2.1` |
| purl         | `pkg:maven/org.pitest/pitest-maven@1.15.3` |
| Maven Central | https://mvnrepository.com/artifact/org.pitest/pitest-maven/1.15.3 |
| License      | Apache License 2.0 |
| Source       | https://github.com/hcoles/pitest |

PITest performs mutation testing by systematically modifying the compiled bytecode and running
the test suite to verify that mutations are detected ("killed"). Run with `mvn pitest:mutationCoverage`.
Reports are written to `target/pit-reports/`.

**Why PITest:** Apache 2.0, leading mutation testing tool for Java, actively maintained with
JUnit 5 support via `pitest-junit5-plugin`.
