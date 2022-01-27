/*
[The "BSD licence"]
Copyright (c) 2013 Terence Parr, Sam Harwell
Copyright (c) 2017 Ivan Kochurkin
Copyright (c) 2017 Alexander Weigl; the JML Extension
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
lexer grammar JavaJMLLexer;

@header {
import lombok.Getter;
import lombok.Setter;
}


@members {
   @Getter @Setter private boolean key = true;
	 @Getter boolean _slJml = false;

   /**
    *
    */
   @Getter @Setter private int parenthesisLevel = 0;
   private void incrParen() { parenthesisLevel++;}// System.err.println("LVL U: "+parenthesisLevel);}
   private void decrParen() { parenthesisLevel--;}// System.err.println("LVL D: "+parenthesisLevel);}

   /**
    *
    */
   @Getter @Setter private int bracesLevel = 0;
   private void incrBrace() { bracesLevel++;}// System.err.println("LVL U: "+parenthesisLevel);}
   private void decrBrace() { bracesLevel--;}// System.err.println("LVL D: "+parenthesisLevel);}


   /**
    *
    */
   @Getter @Setter private int bracketLevel = 0;
   private void incrBracket() { bracketLevel++;}
   private void decrBracket() { bracketLevel--;}

   private boolean semicolonOnToplevel() { return bracketLevel==0 && bracesLevel == 0 && parenthesisLevel==0; }
}

// Keywords
ABSTRACT:           'abstract';
ASSERT:             'assert';
BOOLEAN:            'boolean';
BREAK:              'break';
BYTE:               'byte';
CASE:               'case';
CATCH:              'catch';
CHAR:               'char';
CLASS:              'class';
CONST:              'const';
CONTINUE:           'continue';
DEFAULT:            'default';
DO:                 'do';
DOUBLE:             'double';
ELSE:               'else';
ENUM:               'enum';
EXTENDS:            'extends';
FINAL:              'final';
FINALLY:            'finally';
FLOAT:              'float';
FOR:                'for';
IF:                 'if';
GOTO:               'goto';
IMPLEMENTS:         'implements';
IMPORT:             'import';
INSTANCEOF:         'instanceof';
INT:                'int';
INTERFACE:          'interface';
LONG:               'long';
NATIVE:             'native';
NEW:                'new';
PACKAGE:            'package';
PRIVATE:            'private';
PROTECTED:          'protected';
PUBLIC:             'public';
RETURN:             'return';
SHORT:              'short';
STATIC:             'static';
STRICTFP:           'strictfp';
SUPER:              'super';
SWITCH:             'switch';
SYNCHRONIZED:       'synchronized';
THIS:               'this';
THROW:              'throw';
THROWS:             'throws';
TRANSIENT:          'transient';
TRY:                'try';
VOID:               'void';
VOLATILE:           'volatile';
WHILE:              'while';

// Literals

DECIMAL_LITERAL:    ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]?;
HEX_LITERAL:        '0' [xX] [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])? [lL]?;
OCT_LITERAL:        '0' '_'* [0-7] ([0-7_]* [0-7])? [lL]?;
BINARY_LITERAL:     '0' [bB] [01] ([01_]* [01])? [lL]?;

FLOAT_LITERAL:      (Digits '.' Digits? | '.' Digits) ExponentPart? [fFdD]?
             |       Digits (ExponentPart [fFdD]? | [fFdD])
             ;

HEX_FLOAT_LITERAL:  '0' [xX] (HexDigits '.'? | HexDigits? '.' HexDigits) [pP] [+-]? Digits [fFdD]?;

BOOL_LITERAL:       'true' | 'false';

CHAR_LITERAL:       '\'' (~['\\\r\n] | EscapeSequence) '\'';

STRING_LITERAL:     '"' (~["\\\r\n] | EscapeSequence)* '"';

NULL_LITERAL:       'null';

// Separators

LPAREN:             '(';
RPAREN:             ')';
LBRACE:             '{';
RBRACE:             '}';
LBRACK:             '[';
RBRACK:             ']';
SEMI:               ';';
COMMA:              ',';
DOT:                '.';

// Operators

ASSIGN:             '=';
GT:                 '>';
LT:                 '<';
BANG:               '!';
TILDE:              '~';
QUESTION:           '?';
COLON:              ':';
EQUAL:              '==';
LE:                 '<=';
GE:                 '>=';
NOTEQUAL:           '!=';
AND:                '&&';
OR:                 '||';
INC:                '++';
DEC:                '--';
ADD:                '+';
SUB:                '-';
MUL:                '*';
DIV:                '/';
BITAND:             '&';
BITOR:              '|';
CARET:              '^';
MOD:                '%';

ADD_ASSIGN:         '+=';
SUB_ASSIGN:         '-=';
MUL_ASSIGN:         '*=';
DIV_ASSIGN:         '/=';
AND_ASSIGN:         '&=';
OR_ASSIGN:          '|=';
XOR_ASSIGN:         '^=';
MOD_ASSIGN:         '%=';
LSHIFT_ASSIGN:      '<<=';
RSHIFT_ASSIGN:      '>>=';
URSHIFT_ASSIGN:     '>>>=';

// Java 8 tokens

ARROW:              '->';
COLONCOLON:         '::';

// Additional symbols not defined in the lexical specification

AT:                 '@';
ELLIPSIS:           '...';

// Whitespace and comments

WS:                 [ \t\r\n\u000C]+ -> channel(HIDDEN);
//Extension for JML
JML_START:       '/*@' {_slJml=false;} -> pushMode(jmlContract);
JML_SINGLELINE:  '//@' {_slJml=true;}  -> type(JML_START), pushMode(jmlContract);

COMMENT_START:    '/*' ~[@]        -> channel(HIDDEN), pushMode(comment);
COMMENT_END:      '*/'; // should never be hit by this lexer mode, catched by modes expr, jmlContract, comment.
LINE_COMMENT:     '//' ~[@] ~[\r\n]*    -> channel(HIDDEN);

// Identifiers
IDENTIFIER:         JavaLetter JavaLetterOrDigit*;

// Fragments rules

fragment
ExponentPart
    : [eE] [+-]? Digits
    ;

// Escape Sequences for Character and String Literals

fragment
EscapeSequence
    : '\\' [btnfr"'\\]
    | OctalEscape
    | UnicodeEscape
    ;

fragment
OctalEscape: '\\' ([0-3]? [0-7])? [0-7];

fragment
UnicodeEscape
    : '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

fragment
HexDigits
    : HexDigit ((HexDigit | '_') * HexDigit)?
    ;

fragment
HexDigit
    : [0-9a-fA-F]
    ;

fragment
Digits
    : [0-9] ([0-9_]* [0-9])?
    ;

fragment
JavaLetter
    : [a-zA-Z$_] // these are the "java letters" below 0x7F
    | // covers all characters above 0x7F which are not a surrogate
        ~[\u0000-\u007F\uD800-\uDBFF]
    | // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;

fragment
JavaLetterOrDigit
    : [a-zA-Z0-9$_] // these are the "java letters or digits" below 0x7F
    | // covers all characters above 0x7F which are not a surrogate
        ~[\u0000-\u007F\uD800-\uDBFF]
    | // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;

ERROR_CHAR: .;//catch errors

/** Lexer for JML contracts.
******************************************************************************/

mode jmlContract;
/* JML and JML* keywords */

//Behaviors
BEHAVIOR:               'behavior' | 'behaviour';
NORMAL_BEHAVIOR:        'normal_' BEHAVIOR;
EXCEPTIONAL_BEHAVIOR:   'exceptional_' BEHAVIOR;
MODEL_BEHAVIOR:         'model_' BEHAVIOR;
BREAK_BEHAVIOR:         'break_' BEHAVIOR;
CONTINUE_BEHAVIOR:      'continue_' BEHAVIOR;
RETURN_BEHAVIOR:        'return_' BEHAVIOR;

//Visibility
JC_PACKAGE:             'spec_'? 'package'      -> type(PACKAGE);
JC_PRIVATE:             'spec_'? 'private'      -> type(PRIVATE);
JC_PROTECTED:           'spec_'? 'protected'    -> type(PROTECTED);
JC_PUBLIC:              'spec_'? 'public'       -> type(PUBLIC);

//
JC_STATIC:              'static' -> type(STATIC);
ALSO:                   'also';

// Keywords that introduce expression
JC_ASSERT:              'assert'        -> pushMode(jmlExpr), type(ASSERT);
MODEL:                  'model'         -> pushMode(jmlExpr); //TODO this is not always the case
INVARIANT:              'invariant'     -> pushMode(jmlExpr);
CONSTRAINT:             'constraint'    -> pushMode(jmlExpr);
INITIALLY:              'initially'     -> pushMode(jmlExpr);
AXIOM:                  'axiom'         -> pushMode(jmlExpr);
ACCESSIBLE:             'accessible'    -> pushMode(jmlExpr);
ASSIGNABLE:             'assignable'    -> pushMode(jmlExpr);
BREAKS:                 'breaks'        -> pushMode(jmlExpr);
CONTINUES:              'continues'     -> pushMode(jmlExpr);
DECREASES:              ('decreasing'
                        | 'decreases')  -> pushMode(jmlExpr); // internal translation for 'measured_by'
DEPENDS:                'depends'       -> pushMode(jmlExpr); // internal translation for 'accessible' on model fields
DETERMINES:             'determines'    -> pushMode(jmlExpr); //KeY extension, not official JML
ENSURES:                ('ensures'
                        | 'post'
                        ) '_free'? '_redundantly'? -> pushMode(jmlExpr);
LOOP_DETERMINES:        'loop_determines'         -> pushMode(jmlExpr);  // internal translation for 'determines' in loop invariants
LOOP_SEPARATES:         'loop_separates'          -> pushMode(jmlExpr);  //KeY extension, deprecated
MODEL_METHOD_AXIOM:     'model_method_axiom'      -> pushMode(jmlExpr);  //KeY extension, not official JML
MERGE_PARAMS:           'merge_params'            -> pushMode(jmlExpr);  //KeY extension, not official JML
REPRESENTS:             'represents'              -> pushMode(jmlExpr);
REQUIRES:               ('requires' | 'post')
                        '_free'? '_redundantly'?  -> pushMode(jmlExpr);
RETURNS:                'returns'                 -> pushMode(jmlExpr);  //KeY extension, not official JML
SEPARATES:              'separates'               -> pushMode(jmlExpr);  //KeY extension, not official JML
SIGNALS:                'signals'                 -> pushMode(jmlExpr);
SIGNALS_ONLY:           'signals_only'            -> pushMode(jmlExpr);
DIVERGES:               'diverges'                -> pushMode(jmlExpr);
SET:                    'set'                     -> pushMode(jmlExpr);
LOOP_INVARIANT:         ('maintaining' | 'loop_invariant')
                        '_free'?                  -> pushMode(jmlExpr);
GHOST:                  'ghost'                   -> pushMode(jmlExpr);
MODIFIABLE:             'modifiable'              -> pushMode(jmlExpr);
MODIFIES:               'modifies'                -> pushMode(jmlExpr);
MEASURED_BY:            '\\'? 'measured_by'       -> pushMode(jmlExpr);


// Modifiers
NON_NULL:               'non_null';
NULLABLE:               'nullable';
UNREACHABLE:            'unreachable';
PURE: 		              'pure';
STRICTLY_PURE:          'strictly_pure';
HELPER:                 'helper';
NULLABLE_BY_DEFAULT:    'nullable_by_default';
INSTANCE:               'instance';
TWO_STATE:              'two_state';
NO_STATE:               'no_state';

JML_END:                {!_slJml}? '*/'       -> popMode;
WS_CONTRACT_QUIT:       {_slJml}?  [\r\n\f]   -> type(JML_END), popMode;
WS_CONTRACT_IGNORE:     {!_slJml}?  [@\r\n\u000C]+ -> channel(HIDDEN);
WS_CONTRACT:            [ \t]+   -> channel(HIDDEN);

JC_COMMENT:            {!_slJml}? '{*' -> channel(HIDDEN), type(COMMENT_START), pushMode(jmlComment);
LINE_COMMENT_CONTRACT:  '//' ~[\r\n]*       -> channel(HIDDEN);

JC_NESTED_CONTRACT_START:   {!_slJml}? '{|';
JC_NESTED_CONTRACT_END:     {!_slJml}? '|}';

JC_COMMA:           ',' -> type(COMMA);

JC_BOOLEAN:         'boolean'   -> type(BOOLEAN);
JC_INT:             'int'       -> type(INT);
JC_LONG:            'long'      -> type(LONG);
JC_FLOAT:           'float'     -> type(FLOAT);
JC_DOUBLE:          'double'    -> type(DOUBLE);

JC_ERROR_CHAR: . -> type(ERROR_CHAR);

mode jmlExpr;

DOTDOT:             '..';
EQUIVALENCE:        '<==>';
ANTIVALENCE:        '<=!=>';
IMPLIES:            '==>';
IMPLIESBACKWARD :   '<==';
LOCKSET_LEQ:        '<#=';
LOCKSET_LT:         '<#';
ST:                 '<:';

/** Copied from JavaLexer, import not working in multi-mode
Additions:

* '@' is a whitespace for JML
* add jml operators
* SEMI (';') decides between toplevel and nested
  Toplevel jumps back to `contract` mode.
*/


/* Keywords without prefix »\«
    These keywords are critical as they pollute the

*/
JE_ABSTRACT:           'abstract'   -> type(ABSTRACT);
JE_TWO_STATE:          'two_state'  -> type(TWO_STATE);
JE_NO_STATE:           'no_state'   -> type(NO_STATE);
JE_INSTANCE:           'instance'   -> type(INSTANCE);

//Prefixed keywords
JE_MEASURED_BY:     '\\measured_by' -> type(MEASURED_BY);
SUCH_THAT:          '\\such_that';
LBLPOS:             '\\lblpos';
LBLNEG:             '\\lblneg';
FORALL:             '\\forall';
EXISTS:             '\\exists';
BY:                 '\\by';
DECLASSIFIES:       '\\declassifies';
ERASES:             '\\erases';
NEW_OBJECTS:        '\\new_objects';

JE_BOOLEAN:            'boolean'    -> type(BOOLEAN);
JE_BREAK:              'break'      -> type(BREAK);
JE_BYTE:               'byte'       -> type(BYTE);
JE_CASE:               'case'       -> type(CASE);
JE_CATCH:              'catch'      -> type(CATCH);
JE_CHAR:               'char'       -> type(CHAR);
JE_CLASS:              'class'      -> type(CLASS);
JE_CONST:              'const'      -> type(CONST);
JE_CONTINUE:           'continue'   -> type(CONTINUE);
JE_DEFAULT:            'default'    -> type(DEFAULT);
JE_DO:                 'do'         -> type(DO);
JE_DOUBLE:             'double'     -> type(DOUBLE);
JE_ELSE:               'else'       -> type(ELSE);
JE_ENUM:               'enum'       -> type(ENUM);
JE_EXTENDS:            'extends'    -> type(EXTENDS);
JE_FINAL:              'final'      -> type(FINAL);
JE_FINALLY:            'finally'    -> type(FINALLY);
JE_FLOAT:              'float'      -> type(FLOAT);
JE_FOR:                'for'        -> type(FOR);
JE_IF:                 'if'         -> type(IF);
JE_GOTO:               'goto'       -> type(GOTO);
JE_IMPLEMENTS:         'implements' -> type(IMPLEMENTS);
JE_IMPORT:             'import'     -> type(IMPORT);
JE_INSTANCEOF:         'instanceof' -> type(INSTANCEOF);
JE_INT:                'int'        -> type(INT);
JE_INTERFACE:          'interface'  -> type(INTERFACE);
JE_LONG:               'long'       -> type(LONG);
JE_NATIVE:             'native'     -> type(NATIVE);
JE_NEW:                'new'        -> type(NEW);
JE_RETURN:             'return'     -> type(RETURN);
JE_SHORT:              'short'      -> type(SHORT);
JE_STATIC:             'static'     -> type(STATIC);
JE_STRICTFP:           'strictfp'   -> type(STRICTFP);
JE_SUPER:              'super'      -> type(SUPER);
JE_SWITCH:             'switch'     -> type(SWITCH);
JE_SYNCHRONIZED:       'synchronized' -> type(SYNCHRONIZED);
JE_THIS:               'this' -> type(THIS);
JE_THROW:              'throw' -> type(THROW);
JE_THROWS:             'throws' -> type(THROWS);
JE_TRANSIENT:          'transient' -> type(TRANSIENT);
JE_TRY:                'try' -> type(TRY);
JE_VOID:               'void' -> type(VOID);
JE_VOLATILE:           'volatile' -> type(VOLATILE);
JE_WHILE:              'while' -> type(WHILE);

// Literals
JE_DECIMAL_LITERAL:    ('0' | [1-9] (Digits? | '_'+ Digits)) [lL]? -> type(DECIMAL_LITERAL);
JE_HEX_LITERAL:        '0' [xX] [0-9a-fA-F] ([0-9a-fA-F_]* [0-9a-fA-F])? [lL]? -> type(HEX_LITERAL);
JE_OCT_LITERAL:        '0' '_'* [0-7] ([0-7_]* [0-7])? [lL]? -> type(OCT_LITERAL);
JE_BINARY_LITERAL:     '0' [bB] [01] ([01_]* [01])? [lL]? -> type(BINARY_LITERAL);

JE_FLOAT_LITERAL:   (   (Digits '.' Digits? | '.' Digits) ExponentPart? [fFdD]?
                    |          Digits (ExponentPart [fFdD]? | [fFdD])
                    ) -> type(FLOAT_LITERAL) ;

JE_HEX_FLOAT_LITERAL:  '0' [xX] (HexDigits '.'? | HexDigits? '.' HexDigits) [pP] [+-]? Digits [fFdD]? -> type(HEX_FLOAT_LITERAL);

JE_BOOL_LITERAL:       ('true' | 'false') -> type(BOOL_LITERAL);

JE_CHAR_LITERAL:       '\'' (~['\\\r\n] | EscapeSequence) '\'' -> type(CHAR_LITERAL);

JE_STRING_LITERAL:     '"' (~["\\\r\n] | EscapeSequence)* '"' -> type(STRING_LITERAL);

JE_NULL_LITERAL:       'null' -> type(NULL_LITERAL);

// Separators
JE_LPAREN:             '(' {incrParen();}   -> type(LPAREN);
JE_RPAREN:             ')' {decrParen();}   -> type(RPAREN);
JE_LBRACE:             '{' {incrBrace();}   -> type(LBRACE);
JE_RBRACE:             '}' {decrBrace();}   -> type(RBRACE);
JE_LBRACK:             '[' {incrBracket();} -> type(LBRACK);
JE_RBRACK:             ']' {decrBracket();} -> type(RBRACK);
JE_SEMI:               ';' { ! semicolonOnToplevel()}? -> type(SEMI);
SEMI_TOPLEVEL:         ';' {   semicolonOnToplevel()}? -> popMode; //jump back to contract mode
JE_COMMA:              ',' -> type(COMMA);
JE_DOT:                '.' -> type(DOT);

// Operators
JE_ASSIGN:             '=' -> type(ASSIGN);
JE_GT:                 '>' -> type(GT);
JE_LT:                 '<'-> type(LT);
JE_BANG:               '!'-> type(BANG);
JE_TILDE:              '~'-> type(TILDE);
JE_QUESTION:           '?'-> type(QUESTION);
JE_COLON:              ':'-> type(COLON);
JE_EQUAL:              '==' -> type(EQUAL);
JE_LE:                 '<=' -> type(LE);
JE_GE:                 '>=' -> type(GE);
JE_NOTEQUAL:           '!=' -> type(NOTEQUAL);
JE_AND:                '&&' -> type(AND);
JE_OR:                 '||' -> type(OR);
JE_INC:                '++' -> type(INC);
JE_DEC:                '--' -> type(DEC);
JE_ADD:                '+' -> type(ADD);
JE_SUB:                '-' -> type(SUB);
JE_MUL:                '*' -> type(MUL);
JE_DIV:                '/' -> type(DIV);
JE_BITAND:             '&' -> type(BITAND);
JE_BITOR:              '|' -> type(BITOR) ;
JE_CARET:              '^' -> type(CARET);
JE_MOD:                '%' -> type(MOD);


JE_ADD_ASSIGN:         '+=' -> type(ADD_ASSIGN);
JE_SUB_ASSIGN:         '-=' -> type(SUB_ASSIGN);
JE_MUL_ASSIGN:         '*=' -> type(MUL_ASSIGN);
JE_DIV_ASSIGN:         '/=' -> type(DIV_ASSIGN);
JE_AND_ASSIGN:         '&=' -> type(AND_ASSIGN);
JE_OR_ASSIGN:          '|=' -> type(OR_ASSIGN);
JE_XOR_ASSIGN:         '^=' -> type(XOR_ASSIGN);
JE_MOD_ASSIGN:         '%=' -> type(MOD_ASSIGN);
JE_LSHIFT_ASSIGN:      '<<=' -> type(LSHIFT_ASSIGN);
JE_RSHIFT_ASSIGN:      '>>=' -> type(RSHIFT_ASSIGN);
JE_URSHIFT_ASSIGN:     '>>>=' -> type(URSHIFT_ASSIGN);

// Java 8 tokens

JE_ARROW:              '->' -> type(ARROW);
JE_COLONCOLON:         '::' -> type(COLONCOLON);

// Additional symbols not defined in the lexical specification

//AT:                 '@'; // is now a white space
JE_ELLIPSIS:           '...' -> type(ELLIPSIS);

// Whitespace and comments

JE_JML_END:             {!_slJml}? '*/'       -> type(JML_END), popMode, popMode;
JE_WS_CONTRACT_QUIT:       {_slJml}?  [\r\n\f]   -> type(JML_END), popMode, popMode;
JE_WS_CONTRACT_IGNORE:     {!_slJml}?  [@\r\n\u000C]+ -> channel(HIDDEN), type(WS);
JE_WS_CONTRACT:            [ \t]+   -> channel(HIDDEN), type(WS);

//JML:                '/*@' .*? '*/'; // capture
JE_COMMENT:            {!_slJml}? '{*'              -> channel(HIDDEN), type(COMMENT_START), pushMode(jmlComment);
JE_LINE_COMMENT:       '//' ~[\r\n]*     -> channel(HIDDEN), type(LINE_COMMENT);
JE_END_COMMENT:        {!_slJml}? '*/' -> /*channel(HIDDEN),*/ popMode, popMode, type(JML_END);
// Identifiers

//JE_IDENTIFIER:         JavaLetter JavaLetterOrDigit* -> type(IDENTIFIER);

// Fragments rules

fragment
JE_ExponentPart
    : [eE] [+-]? Digits
    ;

// Escape Sequences for Character and String Literals

fragment
JE_EscapeSequence
    : '\\' [btnfr"'\\]
    | OctalEscape
    | UnicodeEscape
    ;

fragment
JE_OctalEscape: '\\' ([0-3]? [0-7])? [0-7];

fragment
JE_UnicodeEscape
    : '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;

fragment
JE_HexDigits
    : HexDigit ((HexDigit | '_') * HexDigit)?
    ;

fragment
JE_HexDigit
    : [0-9a-fA-F]
    ;

fragment
JE_Digits: [0-9] ([0-9_]* [0-9])?
    ;

fragment
JE_JavaLetter: [a-zA-Z$_] // these are the "java letters" below 0x7F
    | // covers all characters above 0x7F which are not a surrogate
        ~[\u0000-\u007F\uD800-\uDBFF]
    | // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;

fragment
JE_JavaLetterOrDigit
    : [a-zA-Z0-9$_] // these are the "java letters or digits" below 0x7F
    | // covers all characters above 0x7F which are not a surrogate
        ~[\u0000-\u007F\uD800-\uDBFF]
    | // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
        [\uD800-\uDBFF] [\uDC00-\uDFFF]
    ;
//END OF JavaLexer
//</editor-fold>

// all non-listed catched as JML_IDENTIFIER
JML_IDENTIFIER : '\\'? JavaLetter  ( JavaLetterOrDigit )*  -> type(IDENTIFIER);

JE_ERROR_CHAR: . -> type(ERROR_CHAR);

mode comment;

COMMENT_END_COMMENT: '*/'   -> channel(HIDDEN), popMode, type(COMMENT_END);
COMMENT_EVERY_CHAR: .       -> channel(HIDDEN);


mode jmlComment;

JML_COMMENT_END: '*}'         -> channel(HIDDEN), popMode;
JML_COMMENT_CONTRACT_END: '*/'-> popMode, popMode, type(COMMENT_END);
JML_COMMENT_EVERY_CHAR: .     -> channel(HIDDEN);
