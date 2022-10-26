// Whenever this file is edited, Class io.jans.agama.dsl.Transpiler and its dependants MUST BE reviewed
grammar AuthnFlow;

/*
 * Fix for Python-like indentation tokens. See https://github.com/yshavit/antlr-denter
 */

tokens { INDENT, DEDENT }

@lexer::header {
  import com.yuvalshavit.antlr4.DenterHelper;
}

@lexer::members {
  private final DenterHelper denter = DenterHelper.builder()
    .nl(NL)
    .indent(AuthnFlowParser.INDENT)
    .dedent(AuthnFlowParser.DEDENT)
    .pullToken(AuthnFlowLexer.super::nextToken);

  @Override
  public Token nextToken() {
    return denter.nextToken();
  }
}

NL: '\r'? '\n' [\t ]* ;

/*
 * Parser Rules
 */

flow: header statement+ ;

header: FLOWSTART WS qname WS? INDENT base timeout? configs? inputs? DEDENT NL* ;
// header always ends in a NL

qname: ALPHANUM | QNAME ;   //if flow name is a single word, it is not identified as QNAME but ALPHANUM by parser

base: BASE WS STRING WS? NL;

timeout: TIMEOUT WS UINT WS SECS WS? NL ;

configs: CONFIGS WS short_var WS? NL ;
 
inputs: FLOWINPUTS (WS short_var)+ WS? NL ;

short_var: ALPHANUM ;   //created for convenience for code generator

statement: flow_call | action_call | rrf_call | assignment | log | rfac | finish | ifelse | choice | loop | loopy ;

preassign: variable WS? EQ WS? ;

preassign_catch: variable? WS? '|' WS? short_var WS? EQ WS? ;

variable: short_var | QNAME | DOTEXPR | DOTIDXEXPR ;

flow_call: preassign? FLOWCALL WS ('$' variable | qname) argument* WS? (overrides | NL) ;

overrides: INDENT OVERRIDE (WS STRING WS STRING)* (WS? NL STRING WS STRING)* WS? NL DEDENT ;
//I don't get why the NL is needed above

action_call: (preassign | preassign_catch)? ACTIONCALL WS (static_call | oo_call) WS? NL ;

rrf_call: preassign? RRFCALL WS STRING (WS variable)? (WS BOOL)? WS? NL ;

log: LOG argument+ WS? NL ;

static_call: qname '#' ALPHANUM argument* ;

oo_call: variable WS ALPHANUM argument* ;

argument: WS simple_expr ;

simple_expr: literal | variable | (MINUS variable) ;

literal: BOOL | STRING | UINT | SINT | DECIMAL | NUL;

expression: object_expr | array_expr | simple_expr ;

array_expr: '[' WS? expression* (SPCOMMA expression)* WS? ']' ;

object_expr: '{' WS? keypair* (SPCOMMA keypair)* WS? '}' ;

assignment: preassign expression WS? NL ;

keypair: ALPHANUM WS? ':' WS? expression ;

rfac: preassign? RFAC WS (STRING | variable) NL;

finish: FINISH WS (BOOL | STRING | variable) WS? NL ;

choice: MATCH WS simple_expr WS TO WS? INDENT option+ DEDENT elseblock? ;

option: simple_expr WS? INDENT statement+ DEDENT ;

ifelse: caseof INDENT statement+ DEDENT elseblock? ;

caseof: WHEN WS boolean_expr boolean_op_expr* ;

boolean_op_expr: NL* (AND | OR) WS? NL* boolean_expr ;

boolean_expr: simple_expr WS IS WS (NOT WS)? simple_expr WS? ;

elseblock: OTHERWISE WS? INDENT statement+ DEDENT ;

loop: preassign? ITERATE WS variable WS USE WS short_var INDENT statement+ quit_stmt? DEDENT ;

loopy: preassign? REPEAT WS (variable | UINT) WS MAXTIMES WS? INDENT statement+ quit_stmt? DEDENT ;

quit_stmt: QUIT WS caseof NL statement* ;

/*
 * Lexer Rules
 */

fragment DIGIT : [0-9] ;
fragment CH : [a-zA-Z] ;
fragment ALNUM : CH ('_' | CH | DIGIT)* ;
fragment SPACES: [\t ]+ ;
fragment COMMA: ',' ;

COMMENT: '//' ~[\r\n]* -> skip ;

FLOWSTART: 'Flow' ;

BASE: 'Basepath' ;

TIMEOUT: 'Timeout' ;

CONFIGS: 'Configs' ;

FLOWINPUTS: 'Inputs' ;

LOG: 'Log' ;

FLOWCALL: 'Trigger' ;

ACTIONCALL: 'Call' ;

RRFCALL: 'RRF' ;

OVERRIDE: 'Override templates' ;

WHEN: 'When' ;

OTHERWISE: 'Otherwise' ;

REPEAT: 'Repeat' ;

ITERATE: 'Iterate over' ;

MATCH: 'Match' ;

QUIT: 'Quit' ;

FINISH: 'Finish' ;

RFAC: 'RFAC' ;

IS: 'is' ;

NOT: 'not' ;

AND: 'and' ;

OR: 'or' ;

SECS: 'seconds' ;

TO: 'to' ;

MAXTIMES: 'times max' ;

USE: 'using' ;

EQ: '=' ;

MINUS: '-' ;

NUL: 'null' ;

BOOL: 'false' | 'true' ;

STRING: '"' [\u0009\u0020\u0021\u0023-\u007E\u0080-\u008C\u00A0-\uFFFF]* '"' ;
//horizontal tab, space, exclamation mark, ASCII chars from hash(#) to tilde(~), plus other printable unicode chars 

UINT : DIGIT+ ;

SINT : MINUS UINT ;

DECIMAL: (SINT | UINT) '.' UINT ;

ALPHANUM: ALNUM ;

QNAME: ALNUM ('.' ALNUM)* ;

EVALNUM: '.' ( STRING | ('$'? ALNUM) ) ;

DOTEXPR: ALNUM EVALNUM* ;

DOTIDXEXPR: DOTEXPR ('[' SPACES? (UINT | ALNUM) SPACES? ']' EVALNUM*)+ ;

SPCOMMA: SPACES? NL* COMMA SPACES? NL* ;

WS: SPACES ;	//Entails "spaces" (plural)
