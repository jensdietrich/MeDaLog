# ANTLR4

## Metadata

| Field        | Value |
|--------------|-------|
| GAV          | `org.antlr:antlr4-runtime:4.13.2` (runtime) / `org.antlr:antlr4-maven-plugin:4.13.2` (build) |
| purl         | `pkg:maven/org.antlr/antlr4-runtime@4.13.2` |
| Maven Central | https://mvnrepository.com/artifact/org.antlr/antlr4-runtime/4.13.2 |
| License      | BSD 3-Clause |
| Source       | https://github.com/antlr/antlr4 |

## Justification

ANTLR4 (ANother Tool for Language Recognition) is the industry-standard parser generator for JVM
languages. It is used to generate the MeDaLog lexer and parser from the grammar file
`MeDaLog.g4`.

**Why ANTLR4:**
- Mature project maintained by the original ANTLR author (Terence Parr) with wide adoption
- BSD 3-Clause license is permissive
- Adaptive LL(\*) parsing handles ambiguous grammars gracefully
- Generates both listeners and visitors, enabling clean separation of parse and code-generation passes
- Ranked \#1 parser generator on Maven Central with millions of downloads per month
- Active development with regular releases; issues managed on GitHub
- Minimal transitive dependencies (only the runtime JAR is needed at run time)
