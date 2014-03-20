grammar Expression;

//start: expression;

// Parser rules:

expression :
        expression op=('+'|'-') factor  # AddSub
    |   factor                          # ELSEWHERE1
    ;

factor :
        factor op=('*'|'/') atom   # MulDiv
    |   atom                       # ELSEWHERE2
    ;

atom :
        '(' expression ')'                  # Bracketed
    |   op=('exp'|'log') '(' expression ')' # UnaryOp
    |   VARNAME  ('[' i=NNINT ']')?         # Variable
    |   val=(NNFLOAT | NNINT)               # Number
    |   '-' atom       			    # Negation
    ;

// Lexer rules:

ADD : '+' ;
SUB : '-' ;
MUL : '*' ;
DIV : '/' ;
EXP : 'exp' ;
LOG : 'log' ;

NNINT : '0' | NZD D* ;
NNFLOAT : NNINT ('.' D*) ([eE] '-'? D+)? ;
fragment D : [0-9] ;
fragment NZD : [1-9] ;

VARNAME : [a-zA-Z_][a-zA-Z_\-0-9]* ;

WHITESPACE : [ \t\r\n]+ -> skip ;
