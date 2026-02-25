grammar MeDaLog;

// ==============================
// PARSER RULES
// ==============================

/** Top-level program: a sequence of statements. */
program
    : statement* EOF
    ;

/** A statement is either a module declaration or an annotated Souffle item. */
statement
    : moduleDecl
    | annotatedStatement
    ;

/** Module declaration: optional annotations, keyword 'module', name, braced body. */
moduleDecl
    : annotation* MODULE IDENTIFIER LBRACE statement* RBRACE
    ;

/**
 * An annotated statement: zero or more annotations followed by a Souffle item.
 * Annotations and blank lines / comments may be interleaved (comments are skipped by lexer).
 */
annotatedStatement
    : annotation* souffleItem
    ;

// ==============================
// ANNOTATIONS
// ==============================

/** Annotation: @key: value  or  @key=value */
annotation
    : AT annotationKey (COLON | EQ) annotationValue
    ;

/**
 * Annotation key: identifier with optional hyphen-separated parts.
 * Example: last-modified, id, author
 */
annotationKey
    : IDENTIFIER (MINUS IDENTIFIER)*
    ;

/** Annotation value: any of the supported types. */
annotationValue
    : stringValue
    | numberValue
    | booleanValue
    | timestampValue
    | jsonObject
    | jsonArray
    ;

stringValue   : STRING ;
numberValue   : MINUS? (INTEGER | FLOAT_LIT) ;
booleanValue  : BOOLEAN ;
timestampValue: DATETIME | DATE_ONLY ;

/** JSON object: {"key": value, ...} */
jsonObject
    : LBRACE (jsonPair (COMMA jsonPair)*)? RBRACE
    ;

jsonPair
    : STRING COLON jsonValue
    ;

jsonValue
    : STRING
    | MINUS? INTEGER
    | MINUS? FLOAT_LIT
    | BOOLEAN
    | NULL_LIT
    | jsonObject
    | jsonArray
    ;

/** JSON array: [value, ...] */
jsonArray
    : LBRACKET (jsonValue (COMMA jsonValue)*)? RBRACKET
    ;

// ==============================
// SOUFFLE ITEMS
// ==============================

/** Any valid top-level Souffle item inside a module or at top level. */
souffleItem
    : predDeclaration
    | typeDeclaration
    | souffleDirective
    | rule_
    | fact
    ;

// ==============================
// PREDICATE DECLARATIONS
// ==============================

/**
 * Predicate declaration.
 * Supports new Souffle syntax (name: type) and old syntax (name type).
 * Examples:
 *   .decl parent(x: symbol, y: symbol)
 *   .decl grandparent(gp symbol, gc symbol)
 */
predDeclaration
    : DECL_KW IDENTIFIER LPAREN paramList? RPAREN declQualifier* PERIOD?
    ;

paramList
    : param (COMMA param)*
    ;

param
    : IDENTIFIER COLON souffleType   // new syntax: name: type
    | IDENTIFIER souffleType         // old syntax: name type
    ;

souffleType
    : IDENTIFIER                               // simple type: symbol, number, float, etc.
    | LBRACKET paramList RBRACKET              // record type: [field: type, ...]
    ;

/** Optional qualifiers after parameter list: eqrel, btree, brie, inline, overridable, etc. */
declQualifier
    : IDENTIFIER
    ;

// ==============================
// TYPE DECLARATIONS
// ==============================

typeDeclaration
    : TYPE_KW IDENTIFIER EQ typeExpr PERIOD?
    | TYPE_KW IDENTIFIER LBRACKET paramList RBRACKET PERIOD?
    ;

typeExpr
    : souffleType (PIPE souffleType)*
    ;

// ==============================
// SOUFFLE DIRECTIVES (.input, .output, etc.)
// ==============================

souffleDirective
    : INPUT_KW  IDENTIFIER directiveArgList? PERIOD?
    | OUTPUT_KW IDENTIFIER directiveArgList? PERIOD?
    | PRINTSIZE_KW IDENTIFIER PERIOD?
    | FUNCTOR_KW IDENTIFIER LPAREN typeList? RPAREN COLON souffleType (STATEFUL_KW)? PERIOD?
    | LIMITSIZE_KW IDENTIFIER (LPAREN directiveArgList RPAREN)? PERIOD?
    | PRAGMA_KW STRING (STRING)? PERIOD?
    | PLAN_KW INTEGER (COMMA INTEGER)* PERIOD?
    | OVERRIDE_KW IDENTIFIER PERIOD?
    | INIT_KW IDENTIFIER EQ IDENTIFIER PERIOD?
    | COMP_KW IDENTIFIER (IDENTIFIER)? LBRACE statement* RBRACE
    ;

directiveArgList
    : LPAREN directiveArg (COMMA directiveArg)* RPAREN
    ;

directiveArg
    : IDENTIFIER EQ (STRING | IDENTIFIER | INTEGER)
    ;

typeList
    : souffleType (COMMA souffleType)*
    ;

// ==============================
// RULES AND FACTS
// ==============================

/**
 * Souffle rule: head :- body.
 * Example: grandparent(x,y) :- parent(x,y), parent(y,z).
 */
rule_
    : atom IMPL body PERIOD
    ;

/**
 * Souffle fact: head.
 * Example: parent("Tim","Tom").
 */
fact
    : atom PERIOD
    ;

/**
 * An atom: predicate name followed by argument list.
 * Example: parent(x, y)
 */
atom
    : IDENTIFIER LPAREN argList? RPAREN
    ;

argList
    : arg (COMMA arg)*
    ;

arg
    : expr
    ;

// ==============================
// RULE BODY
// ==============================

body
    : bodyElement (COMMA bodyElement)*
    ;

/**
 * A body element can be:
 * - a comparison constraint (tried first to handle function-call-like left sides)
 * - a negated atom
 * - a positive atom
 * - a parenthesized sub-body
 * - an aggregate expression
 */
bodyElement
    : comparison                      // comparison: expr op expr
    | NEG atom                        // negated atom: !pred(args) or \+pred(args)
    | atom                            // positive atom: pred(args)
    | LPAREN body RPAREN              // parenthesized sub-body
    | aggregateExpr                   // aggregate: sum x : { body }
    ;

comparison
    : expr compOp expr
    ;

compOp
    : EQ | NEQ | LT | GT | LEQ | GEQ
    ;

/**
 * Souffle aggregate expression in body.
 * Examples:
 *   n = count : { parent(x, y) }
 *   x = sum y : { value(y) }
 */
aggregateExpr
    : IDENTIFIER EQ aggregateOp IDENTIFIER COLON LBRACE body RBRACE
    | aggregateOp IDENTIFIER COLON LBRACE body RBRACE
    ;

aggregateOp
    : IDENTIFIER   // sum, count, min, max, mean
    ;

// ==============================
// EXPRESSIONS
// ==============================

/**
 * Arithmetic/string expression.
 * Supports binary operators +, -, *, /, %, ^
 * and unary minus / bitwise/logical not.
 */
expr
    : term
    | expr binaryOp term
    | MINUS expr
    | BNOT expr
    ;

term
    : IDENTIFIER LPAREN argList? RPAREN   // function call or type constructor
    | IDENTIFIER                          // variable or symbolic constant
    | INTEGER
    | FLOAT_LIT
    | STRING
    | UNDERSCORE                          // anonymous wildcard
    | BOOLEAN                             // true / false (as Souffle value)
    | NULL_LIT                            // null
    | LPAREN expr RPAREN
    | AS LPAREN expr COMMA souffleType RPAREN   // type cast: as(expr, type)
    ;

binaryOp
    : PLUS | MINUS | STAR | SLASH | PERCENT | CARET
    | LAND | LOR | LXOR | BAND | BOR | BXOR | BSHR | BSHL | BSHRU
    ;

// ==============================
// LEXER RULES
// ==============================

// --- Keywords (must appear before IDENTIFIER) ---

MODULE      : 'module' ;
BOOLEAN     : 'true' | 'false' ;
NULL_LIT    : 'null' ;

// Souffle directives
DECL_KW     : '.decl' ;
TYPE_KW     : '.type' ;
INPUT_KW    : '.input' ;
OUTPUT_KW   : '.output' ;
PRINTSIZE_KW: '.printsize' ;
FUNCTOR_KW  : '.functor' ;
LIMITSIZE_KW: '.limitsize' ;
PRAGMA_KW   : '.pragma' ;
PLAN_KW     : '.plan' ;
OVERRIDE_KW : '.override' ;
INIT_KW     : '.init' ;
COMP_KW     : '.comp' ;
STATEFUL_KW : 'stateful' ;
AS          : 'as' ;
INLINE      : 'inline' ;

// Operators
IMPL    : ':-' ;
NEG     : '!' | '\\+' ;
EQ      : '=' ;
NEQ     : '!=' ;
LEQ     : '<=' ;
GEQ     : '>=' ;
LT      : '<' ;
GT      : '>' ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;
PERCENT : '%' ;
CARET   : '^' ;
LAND    : 'land' ;
LOR     : 'lor' ;
LXOR    : 'lxor' ;
BAND    : 'band' ;
BOR     : 'bor' ;
BXOR    : 'bxor' ;
BSHR    : 'bshr' ;
BSHL    : 'bshl' ;
BSHRU   : 'bshru' ;
BNOT    : 'bnot' ;

// Punctuation
AT       : '@' ;
COLON    : ':' ;
PIPE     : '|' ;
COMMA    : ',' ;
PERIOD   : '.' ;
SEMI     : ';' ;
LBRACE   : '{' ;
RBRACE   : '}' ;
LPAREN   : '(' ;
RPAREN   : ')' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
UNDERSCORE : '_' ;

// --- Literals (order matters: longer patterns first) ---

/**
 * ISO 8601 datetime with time component.
 * Example: 2025-02-16T14:30:00Z  or  2025-02-16T14:30:00+01:00
 */
DATETIME
    : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9]
      'T' [0-9][0-9] ':' [0-9][0-9] ':' [0-9][0-9]
      ('.' [0-9]+)?
      ('Z' | ('+' | '-') [0-9][0-9] ':' [0-9][0-9])
    ;

/**
 * ISO 8601 date only.
 * Example: 2025-02-16
 */
DATE_ONLY
    : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9]
    ;

/**
 * Floating point number.
 * Requires at least one digit after the decimal point to avoid consuming
 * the rule-terminating '.' in patterns like "x >= 18."
 */
FLOAT_LIT
    : [0-9]+ '.' [0-9]+
    | '.' [0-9]+
    ;

/** Integer (unsigned; sign handled at parser level). */
INTEGER
    : [0-9]+
    ;

/** Double-quoted string with escape sequences. */
STRING
    : '"' (ESC | ~["\\\n\r])* '"'
    ;

fragment ESC
    : '\\' .
    ;

/** Identifier: letter or underscore, followed by alphanumerics and underscores. */
IDENTIFIER
    : [a-zA-Z][a-zA-Z0-9_]*
    | '_' [a-zA-Z0-9_]+
    ;

// --- Whitespace and comments (skipped) ---

WS
    : [ \t\r\n]+ -> skip
    ;

LINE_COMMENT
    : '//' ~[\r\n]* -> skip
    ;

BLOCK_COMMENT
    : '/*' .*? '*/' -> skip
    ;
