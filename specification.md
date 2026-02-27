# MeDaLog

## About

This is an informal specification that aims to be precise enough to be used as an input for AI code generators like claude code. Generated code (grammar and parser) needs to be tested and manually checked. 

## MeDaLog Grammar

*MeDaLog* is a set of annotation processors for the *souffle* dialect of datalog.


## Definitions

An *identifier* is a string consisting of alphanumeric characters and underscores, starting with a character or underscore. An identifier must not be a souffle keyword. 

A *rule* is a souffle datalog rule. Here the notion of rule also includes facts which can be considered as rule without a body (i.e. without premisses). 

Two strings s1 and s2 are call *equivalent* if the strings resulting from removing leading and trailing whitespaces and then lowercasing the results are the same.


## The Identity Annotation Processor

The id annotation processor adds an additional `id` first slot predicates.

Input:  a souffle program
Output:  a transformed souffle program

### Predicate Declarations


Each predicate declared in the input program is extended by an additional first slot holding a unique fact or rule id.

Example:

```
.decl grandparent(gp symbol,gc symbol)
```

is extended to: 

```
.decl grandparent(id symbol,gp symbol,gc symbol)

```

If the original predicate already defines the first slot as `id symbol`, then this is used and no additional slot is created.  A compiler warning is created. 


### Facts

If a fact has an `@id` annotation, then the respective value is used. 

Example:

```
@[id = "tim-fact-1"]
parent("Tim","Tom").
```

is compiled to: 

```
parent("tim-fact-1","Tim","Tom").
```


If the `@id` annotation is missing, then the annotation processor generates and uses a unique fact id. By default, generated fact ids start with `F` followed by a number.

Example:

```
parent("Tim","Tom").
```

is compiled to: 

```
parent("F42","Tim","Tom").
```

Where `"F42"` is a compiler-generated unique fact id. 

### Rules

Rule ids are declared using `id` annotation. If the `@id` annotation is missing, then the annotation processor generates and uses a unique fact id. By default, generated fact ids start with `R` followed by a number.

Example:

```
@[id = "grandparentrule"]
grandparent(x,z) :- parent(x,y), parent(y,z).
```

is compiled to: 

```
grandparent(<aggregation>("grandparentrule",id1,id2),x,z) :- parent(id1,x,y), parent(id2,y,z).
```

Where `<aggregation>` is an aggregation function that takes the rule id and the ids of facts in the premisses as parameters. If negation us used in the rule body, then thus premise is represented by a `!` followed by the predicate name.

Example:

```
@[id = "grandparentrule"]
grandparent(x,z) :- parent(x,y), parent(y,z), !adopted(z).
```

is compiled to: 

```
grandparent(<aggregation>("grandparentrule",id1,id2,"!adopted"),x,z) :- parent(id1,x,y), parent(id2,y,z),!adopted(z).
```


The annotation processor must have a strategy to specify (plug in) such an aggregation function, the function used by default is souffle's `cat` function, with arguments separated by `","` and embraced by `[`,`]`. 

Look at the following repository folder how to do this: 
https://github.com/binaryeq/daleq/tree/main/src/main/resources/rules .

Using the default aggregation function will create ids in derived facts that comply to this grammar: 
https://github.com/binaryeq/daleq/blob/main/src/main/antlr4/io/github/bineq/daleq/souffle/provenance/Proof.g4 . 

The purpose of using such an aggregation is to provide custom provenance to record aspects of the annotation. The provenance method is eager, i.e. always computed during the evaluation of rules. The ids of inferred facts encode provenance information.


### Hints for Code Generation for the Identity Annotation Processor 

1. Generate the identity annotation processor in a class `io.github.bineq.medalog.id.IdentityAnnotationProcessor`
2. `IdentityAnnotationProcessor` should have static APIs methods named `process` taking streams, reader/writers and files as input and output, `process ` can be overloaded as needed
3. Also follow general code generation hints

## The Metadata Annotation Processor

Input: a souffle program and a list of annotation keys, such as `{"author","description","created","last-modified"}` , those are refereed as *meta-data annotations*

Output:  a transformed souffle  program


### Annotations

The standard outer annotation syntax of souffle are used for metadata annotations.
Both [components](https://souffle-lang.github.io/components) and rules can be annotated. 

Example:

```
@[author = "jens"]
@[description = "family rules"]
@[created: "2026-02-16T14:30:00Z"]
@[last-modified: "2026-03-16T14:30:00Z"]
.comp MyComponent {
	@[id = "grandparentrule"]
	@[last-modified: "2026-03-16T14:30:00Z"]
	@[description = "rule to describe grandparent relationships"]
	grandparent(x,z) :- parent(x,y), parent(y,z).
}
```


### Annotation Inheritance

Metadata annotations are inferred from metadata annotations of the embracing component. This is referred to as *annotation inheritance*.  For instance, in the example above, the metadata annotation `author` is inferred to also be an annotation of the rule with the id `grandparentrule`. 

If a rule or component and its embracing component have annotations with the same key, the rule or inner component annotation is used. This is referred to as *annotation overriding*. 

`id` is not used as a metadata annotation. An attempt to do so leads to an error during annotation processing. 

We refer to annotations directly defined for a rule or component as an *asserted annotations*, annotations inferred through inheritance as *inferred annotations*.

An `_annotation` predicate is introduced for meta data annotations, declared as follows:


```
.decl annotation(id symbol,annotation_name symbol,annotation_value symbol)
```

Facts instantiating `annotation` are created for each metadata annotation. The `annotation` predicate, facts and rules instantiating this are all to be located in a component `metadata`.

Example (from other examples above):

```
annotation("rule42","author","jens").
annotation("rule42","created","2025-02-16T14:30:00Z").
```


For inferred annotations, the compiler must generate datalog rules infer those facts. 
For this purpose, predicates, facts and rules must be created to represent the following: 

1. the component hierarchy
2. annotations on components
3. membership of rules and facts in components
4. inferred annotations

All those predicate and rules are defined in the component `metadata`.



### Hints for Code Generation for the Metadata Annotation Processor 

1. Generate the identity annotation processor in a class `io.github.bineq.medalog.meta.MetadataAnnotationProcessor`
2. `MetadataAnnotationProcessor ` should have static APIs methods named `process` taking streams, reader/writers and files as input and output, plus an additional *varchar* String argument containing metadata annotations. `process ` can be overloaded as needed
3. Also follow general code generation hints


## Hints for Code Generation

- apply existing rules
- create code in a Maven project, implement the compiler in Java
- the group id is `io.github.bineq`, the artifact id `medalog`
- create the souffle grammar using antlr4, the grammar definition should go in `src/main/antlr4/io/github/bineq/medalog/MeDaLog.g4`
- integrate logging to be used for compiler warnings
- signal compiler errors using a custom unchecked exception type `AnnotationProcessorException`
- compiler errors and warnings should include references to input line numbers
- those APIs are used for testing
- create a `main` method with two arguments for input and output 
- auxiliary classes can be generated in the same package
- create tests using JUnit5, including for corner cases like compiler warnings and errors
- the Maven pom.xml should include plugins for the following: 
  - test coverage
  - mutation coverage
  - quickcheck
- use quickcheck to test that the datalog in inputs appears (perhaps modified) embedded in outputs


### Errors

Annotation processing should create an error if any of the following is true: 

- two or more `@id` annotations use the same value
- two asserted annotations with the same key but different values are defined for the same rule or component

### Warnings

Annotation processing should emit a warning if any of the following is true: 

- an annotation key that is equivalent to `@id` is encountered
- different annotations are used with keys that are equivalent, but not equal 




















