grammar Newick;

// Parser rules:

tree: node ';'? EOF;

node: ('(' node (',' node)* ')')? post ;

post: label? meta? (':' length=number)? ;

label: number | STRING ;

meta: '[&' attrib (',' attrib)* ']' ;

attrib: attribKey=STRING '=' attribValue ;

attribValue: number | STRING | vector;

number: INT | FLOAT ;

vector: '{' attribValue (',' attribValue)* '}' ;


// Lexer rules:

FLOAT : '-'? NNINT ('.' D*) ([eE] '-'? D+)? ;
INT : '-'? NNINT;
fragment NNINT : '0' | NZD D* ;
fragment NZD : [1-9] ;
fragment D : [0-9] ;

STRING :
    [a-zA-Z0-9|#*%/.\-+_&]+  // these chars don't need quotes
    | '"' .*? '"'
    | '\'' .*? '\''
    ;

WHITESPACE : [ \t\r\n]+ -> skip ;