# MeDaLog

## About

This is an informal specification that aims to be precise enough to be used as an input for AI code generators like claude code. Generated code (grammar and parser) needs to be tested and manually checked. 

## MeDaLog Grammar

*MeDaLog* is a conservative exension of the [souffle dialect](https://souffle-lang.github.io/) of datalog, adding two new features: *modules*  and *annotations*.


## Definitions

An *identifier* is a string consisting of alphanumeric characters and underscores, starting with a character or underscore. An identifier must not be souffle keyword. 

A *rule* is a datalog rule. Here the notion of rule also includes facts which can be considered as rule without a body (i.e. without premisses). 


## Modules

Rules (and facts) can be placed in modules. Modules are declared using the keyword module followed by a unique *id* of type identifier as defined below. 

```
module family
{
	grandparent(x,y) :- parent(x,y), parent(y,z).
	grandchild(x,y) :- grandparent(y,x).
}
```

The content of a module is one of the following:

- valid souffle datalog (predicate delarations and rules)
- other modules (i.e. modules can be nested), referred to as *inner modules*
- annotations on inner modules and rules as specified below


## Annotations

There are two types of annotations: *rule annotations* and *module annotations*.


#### Rule Annotations

Rule annotations precede rules, and have the following structure: 

`@<key>:<value>` 

where:

- `value` is of any of the following types: string, boolean, numerical, a json object or array, or a timestamp in ISO8601 format.
- `key` is an identifier as defined above. 

Example:

```
module family {
	@author: "John Doe"
	@created: 2025-02-16T14:30:00Z
	@last-modified: 2025-02-16T14:30:00Z
	@verified: {name:"Veritas Truthful",email:"veritas@true.com"}
	@id="rule42"
	grandparent(x,y) :- parent(x,y), parent(y,z).
}
```

#### Module Annotations

Module annotations precede modules, and have the following structure: 

```
@author: "John Doe"
@created: 2025-02-16T14:30:00Z
@last-modified: 2025-02-16T14:30:00Z
@verified: "Veritas Truthful"
module family {
	grandparent(x,y) :- parent(x,y), parent(y,z).
}
```

The module name (`family` in the example) is semantically equivalent to a module annotation `@id="family"`.

#### Annotation Inheritance

Annotations are inferred from annotations of the embracing module. This is referred to as *annotation inheritance*. 


If a rule or module and its embracing module have annotations with the same key, the rule or inner module annotation is used. This is referred to as *annotation overriding*. 

The `@id` annotation cannot be inherited from surrounding modules. 

We refer to annotations directly defined for a rule or module as an *asserted annotations*, annotations inferred through inheritance as *inferred annotations*.


## Compiling MeDaLog

A MeDaLog compiles into a souffle datalog program. The following modifications are being made.
s

### Predicate Declarations

Each predicate declared in a medalog program is extended by an additional first slot holding a unique fact or rule id.

Example:

```
.decl grandparent(gp symbol,gc symbol)
```

is compiled to: 

```
.decl grandparent(id symbol,gp symbol,gc symbol)
```

### Facts 

If a fact has an @id annotation, then the respective value is used. 

Example:

```
@id: "tim-fact-1"
parent("Tim","Tom").
```

is compiled to: 

```
parent("tim-fact-1","Tim","Tom").
```


If the @id annotation is missing and cannot be inherited from surrounding modules, then the compiler generates and uses a unique fact id.

Example:

```
parent("Tim","Tom").
```

is compiled to: 

```
parent("F42","Tim","Tom").
```

Where `"F42"` is a compiler-generated fact id. 


### Rules

If a fact has an @id annotation, then the respective value is used. 

Example:

```
@id: "grandparentrule"
grandparent(x,y) :- parent(x,y), parent(y,z).
```

is compiled to: 

```
grandparent(<aggreation>(grandpaternrule,id1,id2),x,y) :- parent(id1,x,y), parent(id2,y,z).
```

Where `<aggregation>` is an aggregation function that takes the rule id (either specified or inferred by the compiler like for facts if the @id annotation is missing). 

The compiler must have a strategy to specify (plug in) such an aggregation function, the function used by default is Souflee's `cat` function, with arguments separated by `","` and embraced by `[`,`]`. 

Look at the following repository folder how to do this: 
https://github.com/binaryeq/daleq/tree/main/src/main/resources/rules .

Using the default aggregation function will create ids in derived facts that comply to this grammar: 
https://github.com/binaryeq/daleq/blob/main/src/main/antlr4/io/github/bineq/daleq/souffle/provenance/Proof.g4 . 


### Module

A `_module` predicate is introduced, declared as follows:

```
.decl _module(id symbol,moduleId symbol)
```

The compiler creates facts for each rule and module that is within another module. Those facts define module membership.

Example (from other examples above):

```
_module("rule42","family").
```

### Annotations


An `_annotation` predicate is introduced, declared as follows:

```
.decl _annotation(id symbol,annotation_name symbol,annotation_value symbol)
```

Facts instantiating `_annotation` are created for each asserted annotation that is not `@id`.

Example (from other examples above):

```
_annotation("rule42","author","John Doe").
_annotation("rule42","created","2025-02-16T14:30:00Z").
```

Note that annotation values need to be converted to strings. 

For inferred annotations, datalog rules are generated to infer those facts. 
Those rules use the `_annotation` predicate in body and head, and the `_module` predicate in the body. 


### Compiler Errors

The compiler should create an error if any of the following is true: 

- two or more `@id` annotations use the same value

### Compiler Warnings

The compiler should emit a warning if any of the following is true: 

- an `@Id` or `@ID` annotation is encountered
- different annotations are used, but the keys are the same when capitalised 














