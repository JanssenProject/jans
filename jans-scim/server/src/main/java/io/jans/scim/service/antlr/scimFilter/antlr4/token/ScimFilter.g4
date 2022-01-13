/*
 * ScimFilter.g4
 * Created by jgomer on 2017-12-19
 *
 * Issue the following command to (re)generate Java classes in package org.gluu.oxtrust.service.antlr.scimFilter.antlr4
 * java -cp antlr-4.5.3-complete.jar org.antlr.v4.Tool -visitor -package org.gluu.oxtrust.service.antlr.scimFilter.antlr4 ScimFilter.g4
 */

grammar ScimFilter;

/*
 * Parser Rules. Edit only if you really know what you are doing 8-|
 */

attrpath : ATTRNAME SUBATTR? ;

compareop : 'eq' | 'ne' | 'co' | 'sw' | 'ew' | 'gt' | 'lt' | 'ge' | 'le' ;

compvalue : BOOLEAN | NUMBER | STRING | NULL ;
       
attrexp : attrpath 'pr'
        | attrpath compareop compvalue ;

filter : 'not'? '(' filter ')'	#negatedFilter
       | filter 'and' filter 	#andFilter
       | filter 'or' filter	#orFilter
       | attrexp 		#simpleExpr
       ;

 
/*
 * Lexer Rules. Edit only if you really know what you are doing 8-|
 */
fragment DIGIT : [0-9] ;
fragment LOWERCASE : [a-z] ;
fragment UPPERCASE : [A-Z] ;
fragment HEXDIG : [a-fA-F0-9] ;
fragment NOQUOTEORBKSLSH : [\u0020-\u0021\u0023-\u005B\u005D-\uFFFF] ;
fragment BKSLSH: '\u005C' ;
		
WHITESPACE : [\t ]+ -> skip ;

ALPHA : LOWERCASE | UPPERCASE ;

NUMBER : '-'? DIGIT+ ([\.] DIGIT+)? ;

BOOLEAN : 'false' | 'true' ;

NULL : 'null' ;

NAMECHAR : '-' | '_' | DIGIT | ALPHA ;

URI : ALPHA (NAMECHAR | ':' | '.')* ALPHA ':' ;

ATTRNAME : URI? ALPHA NAMECHAR* ;

SUBATTR : '.' ATTRNAME ;

CHAR : NOQUOTEORBKSLSH | (BKSLSH 'u' HEXDIG HEXDIG HEXDIG HEXDIG) | (BKSLSH [\u0022\u005C\u002F\u0062\u0066\u006E\u0072\u0074]) ;

STRING : '"' CHAR* '"' ;
