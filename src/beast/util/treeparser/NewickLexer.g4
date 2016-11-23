lexer grammar NewickLexer;

// Default mode rules

SEMI: ';' ;
COMMA: ',' ;
OPENP: '(' ;
CLOSEP: ')' ;
COLON: ':' ;

FLOAT_SCI: '-'? ((NNINT? ('.' D+)) | (NNINT ('.' D+)?)) ([eE] '-'? D+);
FLOAT : '-'? ((NNINT? ('.' D+)) | (NNINT ('.' D*)));
INT : '-'? NNINT;
fragment NNINT : '0' | NZD D* ;
fragment NZD : [1-9] ;
fragment D : [0-9] ;

OPENA: '[&' -> mode(ATTRIB_MODE);

STRING :
    [a-zA-Z0-9|#*%/.\-+_&]+  // these chars don't need quotes
    | '"' .*? '"'
    | '\'' .*? '\''
    ;

WHITESPACE : [ \t\r\n]+ -> skip ;

// Attrib mode rules

mode ATTRIB_MODE;

EQ: '=' ;
ACOMMA: ',' ;
OPENV: '{' ;
CLOSEV: '}' ;

AFLOAT_SCI: '-'? ((NNINT? ('.' D+)) | (NNINT ('.' D+)?)) ([eE] '-'? D+);
AFLOAT : '-'? ((NNINT? ('.' D+)) | (NNINT ('.' D*)));
AINT : '-'? NNINT;

ASTRING :
    [a-zA-Z0-9|#*%/.\-+_&:]+  // these chars don't need quotes
    | '"' .*? '"'
    | '\'' .*? '\''
    ;

CLOSEA: ']' -> mode(DEFAULT_MODE);

ATTRIBWS : [ \t\r\n]+ -> skip ;
