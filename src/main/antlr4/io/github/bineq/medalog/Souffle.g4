/**
 * Grammar for the Souffle Datalog language, including its native
 * outer-annotation syntax @[key = value].
 *
 * Used by MeDaLog annotation processors to parse standard Souffle
 * programs and apply identity / metadata transformations.
 */
grammar Souffle;

// ============================================================
// PARSER RULES
// ============================================================

program
    : item* EOF
    ;

// An item is either an annotated directive / rule / fact, or a bare directive.
item
    : annotation* directive
    | annotation* rule_
    | annotation* fact
    ;

// -------------------------------------------------------
// Outer annotation:  @[key = value, key2: value2]
// -------------------------------------------------------
annotation
    : AT LBRACKET annotationPair (COMMA annotationPair)* RBRACKET
    ;

annotationPair
    : annotationKey (EQ | COLON) annotationValue
    ;

// Keys may be hyphenated: last-modified
annotationKey
    : IDENTIFIER (MINUS IDENTIFIER)*
    ;

annotationValue
    : STRING
    | MINUS? NUMBER
    | BOOLEAN
    ;

// -------------------------------------------------------
// Directives
// -------------------------------------------------------
directive
    : declDirective
    | typeDirective
    | inputDirective
    | outputDirective
    | printSizeDirective
    | limitSizeDirective
    | functorDirective
    | pragmaDirective
    | initDirective
    | overrideDirective
    | numberTypeDirective
    | symbolTypeDirective
    | compDirective
    ;

// .decl pred(slot: type, ...) [qualifiers]
declDirective
    : DECL qualifiedName LPAREN slotList? RPAREN declQualifier*
    ;

qualifiedName
    : IDENTIFIER (DOT IDENTIFIER)*
    ;

slotList
    : slot (COMMA slot)*
    ;

slot
    : IDENTIFIER COLON typeExpr
    ;

typeExpr
    : IDENTIFIER                                      // symbol, number, float, unsigned or user-type
    | LBRACKET slotList RBRACKET                      // inline record type: [f: t, ...]
    ;

declQualifier
    : IDENTIFIER+                                     // e.g. eqrel, btree, brie, inline, magic
    ;

typeDirective
    : TYPE IDENTIFIER (EQ typeUnion | LBRACKET slotList RBRACKET | LT IDENTIFIER)
    ;

typeUnion
    : typeExpr (PIPE typeExpr)*
    ;

inputDirective
    : INPUT qualifiedName (LPAREN ioArgList? RPAREN)?
    ;

outputDirective
    : OUTPUT qualifiedName (LPAREN ioArgList? RPAREN)?
    ;

printSizeDirective
    : PRINTSIZE qualifiedName
    ;

limitSizeDirective
    : LIMITSIZE qualifiedName LPAREN ioArgList RPAREN
    ;

functorDirective
    : FUNCTOR IDENTIFIER LPAREN typeList? RPAREN COLON typeExpr STATEFUL?
    ;

pragmaDirective
    : PRAGMA STRING STRING?
    ;

initDirective
    : INIT IDENTIFIER EQ qualifiedName
    ;

overrideDirective
    : OVERRIDE IDENTIFIER
    ;

numberTypeDirective
    : NUMBER_TYPE IDENTIFIER
    ;

symbolTypeDirective
    : SYMBOL_TYPE IDENTIFIER
    ;

// .comp Name [<T,...>] [: Parent [<T,...>]] { items }
compDirective
    : COMP IDENTIFIER (LT identList GT)? (COLON IDENTIFIER (LT typeList GT)?)? LBRACE item* RBRACE
    ;

identList
    : IDENTIFIER (COMMA IDENTIFIER)*
    ;

typeList
    : typeExpr (COMMA typeExpr)*
    ;

ioArgList
    : ioArg (COMMA ioArg)*
    ;

ioArg
    : IDENTIFIER EQ (STRING | IDENTIFIER | NUMBER)
    ;

// -------------------------------------------------------
// Facts and rules
// -------------------------------------------------------

// A fact: atom.
fact
    : atom DOT
    ;

// A rule: head :- body.
rule_
    : head IMPL body DOT
    ;

// Head: one atom or a semicolon-separated list (disjunctive head)
head
    : atom (SEMICOLON atom)*
    ;

// Body: disjunction of conjunctions
body
    : conjunction (SEMICOLON conjunction)*
    ;

// Conjunction: comma-separated body literals
conjunction
    : bodyLiteral (COMMA bodyLiteral)*
    ;

// A body literal is one of:
//   - negated atom:          !pred(args)
//   - negated group:         !(body)
//   - positive atom:         pred(args)
//   - constraint:            term op term
//   - parenthesised group:   (body)
bodyLiteral
    : NEG atom                          # NegAtom
    | NEG LPAREN body RPAREN            # NegGroup
    | atom                              # PosAtom
    | term compOp term                  # Constraint
    | LPAREN body RPAREN                # GroupedBody
    ;

compOp
    : EQ | NEQ | LT | GT | LEQ | GEQ
    ;

// -------------------------------------------------------
// Atoms and terms
// -------------------------------------------------------

atom
    : qualifiedName LPAREN argList? RPAREN
    ;

argList
    : arg (COMMA arg)*
    ;

arg
    : UNDERSCORE
    | term
    ;

// Left-recursive term rule — ANTLR4 handles this automatically.
term
    : MINUS term                                            # UnaryMinus
    | BNOT term                                             # BitwiseNot
    | LNOT term                                             # LogicalNot
    | LPAREN term RPAREN                                    # Paren
    | AS LPAREN term COMMA typeExpr RPAREN                  # TypeCast
    | aggregate                                             # AggTerm
    | qualifiedName LPAREN argList? RPAREN                  # FunCall
    | DOLLAR qualifiedName LPAREN argList? RPAREN           # AdtBranch
    | LBRACKET argList? RBRACKET                            # AdtRecord
    | qualifiedName                                         # VarOrConst
    | NUMBER                                                # Num
    | STRING                                                # Str
    | BOOLEAN                                               # Bool
    | UNDERSCORE                                            # Wildcard
    | NIL                                                   # NilLit
    | term (STAR | SLASH | PERCENT) term                    # MulDiv
    | term (PLUS | MINUS) term                              # AddSub
    | term CARET term                                       # Exponent
    | term (BAND | BOR | BXOR | BSHL | BSHR | BSHRU) term  # Bitwise
    | term (LAND | LOR | LXOR) term                         # Logical
    ;

// Aggregates: count:{body}  /  sum x:{body}  /  min x:{body}  etc.
aggregate
    : (SUM | MIN | MAX | MEAN) term COLON LBRACE body RBRACE  # AggWithTarget
    | COUNT COLON LBRACE body RBRACE                           # AggCount
    ;


// ============================================================
// LEXER RULES
// ============================================================

// ---- Directives (start with '.'; must precede DOT and IDENTIFIER) ----
DECL        : '.decl' ;
TYPE        : '.type' ;
INPUT       : '.input' ;
OUTPUT      : '.output' ;
PRINTSIZE   : '.printsize' ;
LIMITSIZE   : '.limitsize' ;
FUNCTOR     : '.functor' ;
PRAGMA      : '.pragma' ;
INIT        : '.init' ;
OVERRIDE    : '.override' ;
COMP        : '.comp' ;
NUMBER_TYPE : '.number_type' ;
SYMBOL_TYPE : '.symbol_type' ;

// ---- Aggregate keywords (before IDENTIFIER) ----
COUNT   : 'count' ;
SUM     : 'sum' ;
MIN     : 'min' ;
MAX     : 'max' ;
MEAN    : 'mean' ;

// ---- Other reserved words ----
STATEFUL : 'stateful' ;
AS       : 'as' ;
NIL      : 'nil' ;

BOOLEAN  : 'true' | 'false' ;

// ---- Bitwise / logical operators (before IDENTIFIER) ----
BAND  : 'band' ;
BOR   : 'bor' ;
BXOR  : 'bxor' ;
BSHL  : 'bshl' ;
BSHR  : 'bshr' ;
BSHRU : 'bshru' ;
BNOT  : 'bnot' ;
LAND  : 'land' ;
LOR   : 'lor' ;
LXOR  : 'lxor' ;
LNOT  : 'lnot' ;

// ---- Operators (multi-char before single-char) ----
IMPL    : ':-' ;
NEQ     : '!=' ;
LEQ     : '<=' ;
GEQ     : '>=' ;

// Single-char operators
NEG     : '!' ;
EQ      : '=' ;
LT      : '<' ;
GT      : '>' ;
PLUS    : '+' ;
MINUS   : '-' ;
STAR    : '*' ;
SLASH   : '/' ;
PERCENT : '%' ;
CARET   : '^' ;
DOLLAR  : '$' ;

// ---- Punctuation ----
AT        : '@' ;
COLON     : ':' ;
PIPE      : '|' ;
COMMA     : ',' ;
DOT       : '.' ;
SEMICOLON : ';' ;
LBRACE    : '{' ;
RBRACE    : '}' ;
LPAREN    : '(' ;
RPAREN    : ')' ;
LBRACKET  : '[' ;
RBRACKET  : ']' ;

// ---- UNDERSCORE — must come before IDENTIFIER ----
// Bare '_' → UNDERSCORE; '_foo' → IDENTIFIER (maximal munch)
UNDERSCORE : '_' ;

// ---- Number literals ----
// Float before integer so 3.14 is a single token
NUMBER
    : [0-9]+ '.' [0-9]+                 // float: 3.14
    | '0x' [0-9a-fA-F]+                 // hex: 0xFF
    | [0-9]+                            // decimal integer
    ;

// ---- String literal ----
STRING
    : '"' (ESC | ~["\\\n\r])* '"'
    ;

fragment ESC : '\\' . ;

// ---- Identifier ----
// '_foo' (underscore followed by ≥1 alnum) matches as IDENTIFIER (maximal munch beats UNDERSCORE)
// Bare '_' matches UNDERSCORE (same length, UNDERSCORE listed first — first-match wins)
IDENTIFIER
    : [a-zA-Z] [a-zA-Z0-9_]*
    | '_' [a-zA-Z0-9_]+
    ;

// ---- Whitespace and comments — hidden channel preserves formatting in rewriter ----
WS            : [ \t\r\n]+ -> channel(HIDDEN) ;
LINE_COMMENT  : '//' ~[\r\n]* -> channel(HIDDEN) ;
BLOCK_COMMENT : '/*' .*? '*/' -> channel(HIDDEN) ;
