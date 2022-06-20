parser grammar NewickParser;

options { tokenVocab=NewickLexer; }

tree: node ';'? EOF;

node: ('(' node (COMMA node)* ')')? post ;

post: label? nodeMeta=meta? (':' lengthMeta=meta? length=number)? ;

label: number | STRING ;

meta: '[&' attrib (ACOMMA attrib)* ']' ;

attrib: attribKey=ASTRING '=' attribValue ;

attribValue: attribNumber | ASTRING | vector;

number: INT | FLOAT | FLOAT_SCI;
attribNumber: AINT | AFLOAT | AFLOAT_SCI;

vector: '{' attribValue (ACOMMA attribValue)* '}' ;

