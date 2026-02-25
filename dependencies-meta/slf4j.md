# SLF4J

## Metadata

| Field        | Value |
|--------------|-------|
| GAV          | `org.slf4j:slf4j-api:2.0.12` / `org.slf4j:slf4j-simple:2.0.12` |
| purl         | `pkg:maven/org.slf4j/slf4j-api@2.0.12` |
| Maven Central | https://mvnrepository.com/artifact/org.slf4j/slf4j-api/2.0.12 |
| License      | MIT |
| Source       | https://github.com/qos-ch/slf4j |

## Justification

SLF4J (Simple Logging Facade for Java) is the de-facto standard logging API for Java libraries.
The `slf4j-api` module provides a stable API; `slf4j-simple` is used as the runtime provider.

**Why SLF4J:**
- MIT license (permissive)
- Decouples the compiler library from any specific logging framework; consumers can swap in
  Logback, Log4j2, etc. at runtime
- `slf4j-simple` has no additional transitive dependencies
- Maintained by the QOS.ch team with decades of stability and active community support
- Ranked among the most downloaded JVM libraries on Maven Central
