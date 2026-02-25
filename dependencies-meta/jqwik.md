# jqwik

## Metadata

| Field        | Value |
|--------------|-------|
| GAV          | `net.jqwik:jqwik:1.8.4` |
| purl         | `pkg:maven/net.jqwik/jqwik@1.8.4` |
| Maven Central | https://mvnrepository.com/artifact/net.jqwik/jqwik/1.8.4 |
| License      | MIT / Eclipse Public License 2.0 |
| Source       | https://github.com/jqwik-team/jqwik |

## Justification

jqwik is a property-based testing library (QuickCheck style) built specifically for JUnit 5.
Used in `CompilerPropertiesTest` to verify that predicate names and data values from
MeDaLog inputs appear (perhaps modified) in the Souffle output.

**Why jqwik:**
- MIT / EPL 2.0 dual license (both permissive)
- Native JUnit 5 integration via `@Property` and `@Provide` – no separate runner needed
- Rich set of built-in arbitraries and combinators
- Active development, multiple committers, strong community
- Ranked highest among JUnit-5-compatible QuickCheck libraries on Maven Central
