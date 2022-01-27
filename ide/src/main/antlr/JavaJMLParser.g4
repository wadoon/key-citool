/*
 [The "BSD licence"]
 Copyright (c) 2013 Terence Parr, Sam Harwell
 Copyright (c) 2017 Ivan Kochurkin
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

parser grammar JavaJMLParser;

options { tokenVocab=JavaJMLLexer; }

compilationUnit
    : packageDeclaration? importDeclaration* typeDeclaration* EOF
    ;

packageDeclaration
    : annotation* PACKAGE qualifiedName SEMI
    ;

importDeclaration
    : IMPORT STATIC? qualifiedName (DOT MUL)? SEMI
    ;

typeDeclaration
    : classOrInterfaceModifier*
      (classDeclaration | enumDeclaration | interfaceDeclaration | annotationTypeDeclaration)
    | SEMI
    ;

modifier
    : classOrInterfaceModifier
    | NATIVE
    | SYNCHRONIZED
    | TRANSIENT
    | VOLATILE
    ;

classOrInterfaceModifier
    : annotation
    | PUBLIC
    | PROTECTED
    | PRIVATE
    | STATIC
    | ABSTRACT
    | FINAL    // FINAL for class only -- does not apply to interfaces
    | STRICTFP
    | jmlModifier
    ;

variableModifier
    : FINAL
    | annotation
    ;

classDeclaration
    : CLASS IDENTIFIER typeParameters?
      (EXTENDS typeType)?
      (IMPLEMENTS typeList)?
      classBody
    ;

typeParameters
    : LT typeParameter (COMMA typeParameter)* GT
    ;

typeParameter
    : annotation* IDENTIFIER (EXTENDS typeBound)?
    ;

typeBound
    : typeType (BITAND typeType)*
    ;

enumDeclaration
    : ENUM IDENTIFIER (IMPLEMENTS typeList)? LBRACE enumConstants? COMMA? enumBodyDeclarations? RBRACE
    ;

enumConstants
    : enumConstant (COMMA enumConstant)*
    ;

enumConstant
    : annotation* IDENTIFIER arguments? classBody?
    ;

enumBodyDeclarations
    : SEMI classBodyDeclaration*
    ;

interfaceDeclaration
    : INTERFACE IDENTIFIER typeParameters? (EXTENDS typeList)? interfaceBody
    ;

classBody
    : LBRACE classBodyDeclaration* RBRACE
    ;

interfaceBody
    : LBRACE interfaceBodyDeclaration* RBRACE
    ;

classBodyDeclaration
    : SEMI
    | STATIC? block
    | jmlClassElem
    | (jmlContract)* modifier* memberDeclaration
    ;

memberDeclaration
    : methodDeclaration
    | genericMethodDeclaration
    | fieldDeclaration
    | constructorDeclaration
    | genericConstructorDeclaration
    | interfaceDeclaration
    | annotationTypeDeclaration
    | classDeclaration
    | enumDeclaration
    ;

/* We use rule this even for void methods which cannot have [] after parameters.
   This simplifies grammar and we can consider void to be a type, which
   renders the [] matching as a context-sensitive issue or a semantic check
   for invalid return type after parsing.
 */
methodDeclaration
    :
      typeTypeOrVoid jmlModifier? IDENTIFIER formalParameters (LBRACK RBRACK)*
      (THROWS qualifiedNameList)?
      methodBody
    ;

methodBody
    : block
    | SEMI
    ;

typeTypeOrVoid
    : typeType | VOID
    ;

genericMethodDeclaration
    : typeParameters methodDeclaration
    ;

genericConstructorDeclaration
    : typeParameters constructorDeclaration
    ;

constructorDeclaration
    : IDENTIFIER formalParameters (THROWS qualifiedNameList)? constructorBody=block
    ;

fieldDeclaration
    : typeType jmlModifier? variableDeclarators SEMI
    ;

interfaceBodyDeclaration
    : jmlContract* modifier* interfaceMemberDeclaration
    | jmlClassElem     //modifier* interfaceMemberDeclaration
    | SEMI
    ;

interfaceMemberDeclaration
    : constDeclaration
    | interfaceMethodDeclaration
    | genericInterfaceMethodDeclaration
    | interfaceDeclaration
    | annotationTypeDeclaration
    | classDeclaration
    | enumDeclaration
    ;

constDeclaration
    : typeType constantDeclarator (COMMA constantDeclarator)* SEMI
    ;

constantDeclarator
    : IDENTIFIER (LBRACK RBRACK)* ASSIGN variableInitializer
    ;

// see matching of [] comment in methodDeclaratorRest
// methodBody from Java 8
interfaceMethodDeclaration
    : interfaceMethodModifier* typeTypeOrVoid jmlModifier* IDENTIFIER formalParameters (LBRACK RBRACK)* (THROWS qualifiedNameList)? methodBody
    ;

// Java8
interfaceMethodModifier
    : annotation
    | PUBLIC
    | ABSTRACT
    | DEFAULT
    | STATIC
    | STRICTFP
    | jmlModifier
    ;

genericInterfaceMethodDeclaration
    : typeParameters interfaceMethodDeclaration
    ;

variableDeclarators
    : variableDeclarator (COMMA variableDeclarator)*
    ;

variableDeclarator
    : variableDeclaratorId (ASSIGN variableInitializer)?
    ;

variableDeclaratorId
    : IDENTIFIER (LBRACK RBRACK)*
    ;

variableInitializer
    : arrayInitializer
    | expression
    ;

arrayInitializer
    : LBRACE (variableInitializer (COMMA variableInitializer)* (COMMA)? )? RBRACE
    ;

classOrInterfaceType
    : IDENTIFIER typeArguments? (DOT IDENTIFIER typeArguments?)*
    ;

typeArgument
    : typeType
    | QUESTION ((EXTENDS | SUPER) typeType)?
    ;

qualifiedNameList
    : qualifiedName (COMMA qualifiedName)*
    ;

formalParameters
    : LPAREN formalParameterList? RPAREN
    ;

formalParameterList
    : formalParameter (COMMA formalParameter)* (COMMA lastFormalParameter)?
    | lastFormalParameter
    ;

formalParameter
    : (jmlModifier|variableModifier)*  typeType jmlModifier? variableDeclaratorId
    ;

lastFormalParameter
    : jmlModifier? variableModifier* jmlModifier? typeType ELLIPSIS variableDeclaratorId
    ;

qualifiedName
    : IDENTIFIER (DOT IDENTIFIER)*
    ;

literal
    : integerLiteral
    | FLOAT_LITERAL
    | CHAR_LITERAL
    | STRING_LITERAL
    | BOOL_LITERAL
    | NULL_LITERAL
    ;

integerLiteral
    : DECIMAL_LITERAL
    | HEX_LITERAL
    | OCT_LITERAL
    | BINARY_LITERAL
    ;

// ANNOTATIONS

annotation
    : AT qualifiedName (LPAREN ( elementValuePairs | elementValue )? RPAREN)?
    ;

elementValuePairs
    : elementValuePair (COMMA elementValuePair)*
    ;

elementValuePair
    : IDENTIFIER ASSIGN elementValue
    ;

elementValue
    : expression
    | annotation
    | elementValueArrayInitializer
    ;

elementValueArrayInitializer
    : LBRACE (elementValue (COMMA elementValue)*)? (COMMA)? RBRACE
    ;

annotationTypeDeclaration
    : AT INTERFACE IDENTIFIER annotationTypeBody
    ;

annotationTypeBody
    : LBRACE (annotationTypeElementDeclaration)* RBRACE
    ;

annotationTypeElementDeclaration
    : modifier* annotationTypeElementRest
    | SEMI // this is not allowed by the grammar, but apparently allowed by the actual compiler
    ;

annotationTypeElementRest
    : typeType annotationMethodOrConstantRest SEMI
    | classDeclaration SEMI?
    | interfaceDeclaration SEMI?
    | enumDeclaration SEMI?
    | annotationTypeDeclaration SEMI?
    ;

annotationMethodOrConstantRest
    : annotationMethodRest
    | annotationConstantRest
    ;

annotationMethodRest
    : IDENTIFIER LPAREN RPAREN defaultValue?
    ;

annotationConstantRest
    : variableDeclarators
    ;

defaultValue
    : DEFAULT elementValue
    ;

// STATEMENTS / BLOCKS

block
    : jmlBlockCntr? LBRACE blockStatement* RBRACE
    ;

blockStatement
    : localVariableDeclaration SEMI
    | statement
    | typeDeclaration
    ;

localVariableDeclaration
    : variableModifier* typeType variableDeclarators
    ;

statement
    : blockLabel=block
    | ASSERT expression (COLON expression)? SEMI
    | IF parExpression statement (ELSE statement)?
    | FOR LPAREN forControl RPAREN statement
    | WHILE parExpression statement
    | DO statement WHILE parExpression SEMI
    | TRY block (catchClause+ finallyBlock? | finallyBlock)
    | TRY resourceSpecification block catchClause* finallyBlock?
    | SWITCH parExpression LBRACE switchBlockStatementGroup* switchLabel* RBRACE
    | SYNCHRONIZED parExpression block
    | RETURN expression? SEMI
    | THROW expression SEMI
    | BREAK IDENTIFIER? SEMI
    | CONTINUE IDENTIFIER? SEMI
    | SEMI
    | statementExpression=expression SEMI
    | identifierLabel=IDENTIFIER COLON statement
    | jmlAnnotation
    ;

catchClause
    : CATCH LPAREN variableModifier* catchType IDENTIFIER RPAREN block
    ;

catchType
    : qualifiedName (BITOR qualifiedName)*
    ;

finallyBlock
    : FINALLY block
    ;

resourceSpecification
    : LPAREN resources SEMI? RPAREN
    ;

resources
    : resource (SEMI resource)*
    ;

resource
    : variableModifier* classOrInterfaceType variableDeclaratorId ASSIGN expression
    ;

/** Matches cases then statements, both of which are mandatory.
 *  To handle empty cases at the end, we add switchLabel* to statement.
 */
switchBlockStatementGroup
    : switchLabel+ blockStatement+
    ;

switchLabel
    : CASE (constantExpression=expression | enumConstantName=IDENTIFIER) COLON
    | DEFAULT COLON
    ;

forControl
    : enhancedForControl
    | forInit? SEMI expression? SEMI forUpdate=expressionList?
    ;

forInit
    : localVariableDeclaration
    | expressionList
    ;

enhancedForControl
    : variableModifier* typeType variableDeclaratorId COLON expression
    ;

// EXPRESSIONS

parExpression
    : LPAREN expression RPAREN
    ;

expressionList
    : expression (COMMA expression)*
    ;

expression
    : primary
    | expression bop=DOT
      (IDENTIFIER
      | THIS
      | NEW nonWildcardTypeArguments? innerCreator
      | SUPER superSuffix
      | explicitGenericInvocation
      )
    | expression LBRACK expression RBRACK
    | expression LPAREN expressionList? RPAREN
    | NEW creator
    | LPAREN typeType RPAREN expression
    | expression postfix=(INC | DEC)
    | prefix=(ADD|SUB|INC|DEC) expression
    | prefix=(TILDE|BANG) expression
    | expression bop=(MUL|DIV|MOD) expression
    | expression bop=(ADD|SUB) expression
    | expression (LT LT| GT GT GT | GT GT) expression
    | expression bop=(GE | LE | GT | LT ) expression
    | expression bop=INSTANCEOF typeType
    | expression bop=(EQUAL | NOTEQUAL) expression
    | expression bop=BITAND expression
    | expression bop=CARET expression
    | expression bop=BITOR expression
    | expression bop=AND expression
    | expression bop=OR expression
    | expression bop=QUESTION expression COLON expression
    | <assoc=right> expression
      bop=(   ASSIGN | ADD_ASSIGN | SUB_ASSIGN
            | MUL_ASSIGN | DIV_ASSIGN
            | AND_ASSIGN | OR_ASSIGN | XOR_ASSIGN | MOD_ASSIGN
            | LSHIFT_ASSIGN| RSHIFT_ASSIGN| URSHIFT_ASSIGN
            ) expression
    | lambdaExpression // Java8
    ;

// Java8
lambdaExpression
    : lambdaParameters ARROW lambdaBody
    ;

// Java8
lambdaParameters
    : IDENTIFIER
    | LPAREN formalParameterList? RPAREN
    | LPAREN IDENTIFIER (COMMA IDENTIFIER)* RPAREN
    ;

// Java8
lambdaBody
    : expression
    | block
    ;

primary
    : LPAREN expression RPAREN
    | THIS
    | SUPER
    | literal
    | IDENTIFIER
    | typeTypeOrVoid DOT CLASS
    | nonWildcardTypeArguments (explicitGenericInvocationSuffix | THIS arguments)
    | methodReference // Java 8
    ;

methodReference
    : (qualifiedName | typeType | (qualifiedName DOT)? SUPER ) COLONCOLON typeArguments? IDENTIFIER
    | classType COLONCOLON typeArguments? NEW
    | typeType COLONCOLON NEW
    ;

classType
    : (classOrInterfaceType DOT)? annotation* IDENTIFIER typeArguments?
    ;

creator
    : nonWildcardTypeArguments createdName classCreatorRest
    | createdName (arrayCreatorRest | classCreatorRest)
    ;

createdName
    : IDENTIFIER typeArgumentsOrDiamond? (DOT IDENTIFIER typeArgumentsOrDiamond?)*
    | primitiveType
    ;

innerCreator
    : IDENTIFIER nonWildcardTypeArgumentsOrDiamond? classCreatorRest
    ;

arrayCreatorRest
    : LBRACK (RBRACK (LBRACK RBRACK)* arrayInitializer | expression RBRACK (LBRACK expression RBRACK)* (LBRACK RBRACK)*)
    ;

classCreatorRest
    : arguments classBody?
    ;

explicitGenericInvocation
    : nonWildcardTypeArguments explicitGenericInvocationSuffix
    ;

typeArgumentsOrDiamond
    : LT GT
    | typeArguments
    ;

nonWildcardTypeArgumentsOrDiamond
    : LT GT
    | nonWildcardTypeArguments
    ;

nonWildcardTypeArguments
    : LT typeList GT
    ;

typeList
    : typeType (COMMA typeType)*
    ;

typeType
    : (classOrInterfaceType | primitiveType) (LBRACK RBRACK)*
    ;

primitiveType
    : BOOLEAN
    | CHAR
    | BYTE
    | SHORT
    | INT
    | LONG
    | FLOAT
    | DOUBLE
    ;

typeArguments
    : LT typeArgument (COMMA typeArgument)* GT
    ;

superSuffix
    : arguments
    | DOT IDENTIFIER arguments?
    ;

explicitGenericInvocationSuffix
    : SUPER superSuffix
    | IDENTIFIER arguments
    ;

arguments
    : LPAREN expressionList? RPAREN
    ;


/********************************************************
 JML -- Java Modelling Language

 Some parts from the 2nd KeY Book, some parts from http://www.eecs.ucf.edu/~leavens/JML/OldReleases/jmlrefman.pdf

 Convention all top-level rules are prefixed with »jml«.
 ********************************************************/

/**

*/
jmlContract     : JML_START methodContracts mod* JML_END
                ;

/**

*/
jmlClassElem    : JML_START classElem JML_END
                ;

/**

*/
jmlModifier     : JML_START  mod (COMMA? mod)* JML_END
                ;
/**

*/
jmlBlockCntr    : JML_START blockContracts JML_END
                ;

/**

*/
classElem       : classSpec+  |  modelMethod
                ;
/**

*/
classSpec   : visibility? (classInv | fieldDecl | represents | accessible ) SEMI_TOPLEVEL;

/**

*/
classInv    :   (STATIC | INSTANCE)?
                (INVARIANT | CONSTRAINT | INITIALLY | AXIOM)
                expr
            ;

/**

*/
fieldDecl   :  (INSTANCE|STATIC)? ( GHOST | MODEL ) (INSTANCE|STATIC)? typeType id (COMMA id)*
            ;

/**

*/
represents  :  REPRESENTS
                ( expr | id ASSIGN expr | id SUCH_THAT expr)
                (COMMA ( expr | id ASSIGN expr | id SUCH_THAT expr) )*
            ;

/**

*/
accessible  : ACCESSIBLE id COLON expr (COMMA expr)* mby?
            ;
/**

*/
modelMethod : (visibility? MODEL_BEHAVIOR methodContracts)?
              visibility? ( NO_STATE | TWO_STATE | HELPER | STATIC)*
               MODEL
              ( NO_STATE | TWO_STATE | HELPER | STATIC)*
              visibility? STATIC? typeType IDENTIFIER ('(' params? ')')?
              ( LBRACE RETURN expr SEMI RBRACE
              | SEMI_TOPLEVEL
              )
            ;

params      : jmlTypeType id ( COMMA jmlTypeType id )*
            ;
/**

*/
methodContracts : ALSO? methodContract ( ALSO methodContract )*
                ;

methodContract  : visibility?
                  behavior=( BEHAVIOR | NORMAL_BEHAVIOR | EXCEPTIONAL_BEHAVIOR)?
                  (clause |
                    JC_NESTED_CONTRACT_START
                    methodContracts
                    JC_NESTED_CONTRACT_END
                  )*
                ;

/**
*/
visibility  : PACKAGE | PRIVATE | PROTECTED | PUBLIC
            ;

/**
*/
clause      : (   requires
                | ensures
                | signals
                | signalsOnly
                | diverges
                | determs
                | assign
                | acc
                | mby )
	          SEMI_TOPLEVEL
            ;
/**


*/
requires    : REQUIRES heap* expr
            ;


/**


*/
ensures     : ENSURES heap* expr
            ;

/**


*/
signals     : SIGNALS LPAREN typeType id? RPAREN expr
            ;

/**
signals_only  \nothing;
singals_only  \everything;
singals_only  \everything;
*/
signalsOnly : SIGNALS_ONLY
                typeType (COMMA typeType )*
	        ;


diverges    : DIVERGES expr
            ;

determs     :   DETERMINES exprs
	            BY exprs
	            ( DECLASSIFIES exprs  | ERASES exprs )?
	            ( NEW_OBJECTS exprs )?
	        ;

/**
A heap specification is an identifier enclosed with angle brackets.

Examples:

* <javacard>
* <first>
*/
heap        : (LT IDENTIFIER GT)
            ;


/**

*/
assign      : (ASSIGNABLE |  MODIFIABLE | MODIFIES ) heap* expr (COMMA expr )*
            ;

acc         : ACCESSIBLE heap* expr (COMMA expr )* mby?
            ;

/**
Measures_by

*/
mby         : MEASURED_BY exprs
            ;

/**

*/
mod         : PURE | STRICTLY_PURE | MODEL | HELPER | NULLABLE_BY_DEFAULT
            | PUBLIC | PRIVATE | PROTECTED | PACKAGE | NON_NULL | NULLABLE
		    ;

/**
An annotation is a JML construct that can disappear
within a sequence of statements.

*/
annot       : setStm SEMI_TOPLEVEL
            | assert_ SEMI_TOPLEVEL
            | fieldDecl SEMI_TOPLEVEL
            | UNREACHABLE
            | loopInv
            | blockContracts
            ;
/**
Entry point for annotations.
*/
jmlAnnotation   : JML_START annot+ JML_END
                ;
/**

*/
loopInv     :   (
                    ( loopInvclause
                    | variantclause
                    | assign
                    | determs
                    )
                    SEMI_TOPLEVEL
                )+
            ;
/**
The loop_invariant clause.

Loop_invariants are allowed to be specified for a list of <heap>s
 and only takes one expression.

Examples:

* loop_invariant<h1> (\forall int i; i < j; f(i))
*/
loopInvclause   : LOOP_INVARIANT  heap* expr
                ;

/**

*/
variantclause   : DECREASES exprs
                ;
/**

*/
blockContracts  : ALSO? blockContract (ALSO blockContract)*
	            ;

blockContract   : visibility? bbehavior? bclause+
                ;

/**
Enumeration of behaviours for block contracts:

We support the british and american english keywords.

Example:
* behavior
* normal_behaviour
* exceptional_behaviour
* break_behavior
* continue_behavior
* return_behavior
*/
bbehavior       : BEHAVIOR
                | NORMAL_BEHAVIOR
                | EXCEPTIONAL_BEHAVIOR
                | BREAK_BEHAVIOR
                | CONTINUE_BEHAVIOR
                | RETURN_BEHAVIOR
                ;

/**
Clause in a block contract.

A block contract can either be a
* breaks
* continues
* returns
* or a clauses of a method contract (cf. <clause>).
*/
bclause         :
                ( breaks SEMI_TOPLEVEL
                | returns_ SEMI_TOPLEVEL
                | clause
                )
                ;

/**
A breaks non-terminal describes the irregular control within a loop.

Examples:

* breaks () true
* breaks (a) a==2
* continues (abc) a&2!=1
 */
breaks          : (BREAKS | CONTINUES)
                  LPAREN id? RPAREN expr
		        ;

returns_        : RETURNS expr
                ;

setStm          : SET location=expr ASSIGN value=expr
                ;

assert_         : ASSERT_ expr
                ;


/**

*/
exprs           :  (expr COMMA)* expr
                ;

/**
https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html

| Operators             | Precedence                    |
| postfix               | expr++ expr--                 |
| unary	                | ++expr --expr +expr -expr ~ ! |
| multiplicative        |  * / %                        |
| additive	            | + -                           |
| shift	                | << >> >>>                     |
| relational        	|  < > <= >= instanceof         |
| equality	            |  ==  !=                       |
| bitwise AND           | `&`                           |
| bitwise exclusive OR	|`^`                            |
| bitwise inclusive OR	|`|`                            |
| logical AND           |`&&`                           |
| logical OR            | `||`                          |
| ternary	            | ? :                           |
| assignment	        | = += -= *= /= %= &= ^= |= <<= >>= >>>= |

*/
expr  :
    // We start with the base cases, that have no degrees of freedom for selection to achieve a fast parser
      jmlPrimary                                                                                            #exprLiteral
    | quantifiedExpr                                                                                  #exprComprehension
    | LPAREN (LBLPOS | LBLNEG) IDENTIFIER expr RPAREN                                                       #labeledExpr
    | LPAREN expr RPAREN                                                                                     #exprParens
    //| (expr DOT ) id                                                                                  #exprFieldAccess
    |  expr LBRACK expr RBRACK                                                                          #exprArrayAccess
    |  expr LBRACK expr DOTDOT expr RBRACK                                                               #exprArryLocSet
    |  expr LBRACK MUL RBRACK                                                                           #exprArrayAccess
    //  cascade from the highest to the lowest precedence
    | LPAREN typeType RPAREN expr                                                                              #exprCast
    | SUB expr                                                                                           #exprUnaryMinus
    | BANG expr                                                                                       #exprLogicalNegate
    | TILDE expr                                                                                       #exprBinaryNegate
    | NEW (id DOT)* id                                                                                          #exprNew
    | expr bop=DOT id                                                                                            #access
    | expr bop=DOT MUL                                                                                           #locAll

    //arithmetic
    | expr MUL expr                                                                                  #exprMultiplication
    | <assoc=right> expr op=(MOD|DIV) expr                                                                #exprDivisions
    | expr op=(SUB|ADD) expr                                                                          #exprLineOperators

    | expr (LT LT | GT GT GT | GT GT) expr                                                                   #exprShifts

    | expr op=(LE|GE|GT|LT|INSTANCEOF|ST) expr                                                           #exprRelational

    // ST is for type comparison
    | expr op=(EQUAL|NOTEQUAL) expr                                                                      #exprEqualities

    | expr op=BITAND expr                                                                                 #exprBinaryAnd
    | expr op=CARET expr                                                                                  #exprBinaryXor
    | expr op=BITOR expr                                                                                   #exprBinaryOr
    | expr op=AND expr                                                                                   #exprLogicalAnd
    | expr op=OR expr                                                                                     #exprLogicalOr

	| expr LBRACK expr DOTDOT expr RBRACK                                                               #exprSubSequence
    | expr QUESTION expr COLON expr                                                                         #exprTernary
    // end of java hierarchy

    | expr op=IMPLIES expr                                                                         #exprImplicationRight
    | expr op=IMPLIESBACKWARD expr                                                                  #exprImplicationLeft
    | expr op=EQUIVALENCE expr                                                                          #exprEquivalence
    | expr op=ANTIVALENCE expr                                                                          #exprAntivalence
    | expr LPAREN exprs? RPAREN                                                                            #exprFunction
    | id LPAREN exprs? RPAREN                                                                              #exprFunction
    | THIS #exprThis
    | SUPER #exprSuper
    ;

jmlPrimary
    : LPAREN expr RPAREN
    | THIS
    | SUPER
    | literal
    | IDENTIFIER
    | typeTypeOrVoid DOT CLASS
    | nonWildcardTypeArguments (explicitGenericInvocationSuffix | THIS arguments)
    | methodReference // Java 8
    | jmlTypeType
    ;

/**
A comprehension is a variable binder with a list of expression

Examples:
* (\forall int x; x > 0 && x < a.length; p(x) )
* (\sum int x; x>= 0 && x < 10; x+1)
* (\product int x; x>= 0 && x < 10; y)
* (\max int x; x>= 0 && x < 10; f(x))

Currently following comprehension are defined:
    \sum, \product, \max, \min, \num_of, \exists, \foreach, \infinite_union

*/
quantifiedExpr
    :
        LPAREN
            op=(FORALL|EXISTS|IDENTIFIER)
            typeType id (COMMA id)* SEMI
            (expr SEMI)*
            expr
        RPAREN
    ;

		    //  LPAREN '\\lblneg' id expr RPAREN
            //   LPAREN '\\lblpos' id expr RPAREN


/**
Identifier in JML are

* normal java identifier or
* symbols that begin with a backslash '\'


We try to minimize the specific usage of specific DL keywords,
and catch false usage later.
*/
id:     IDENTIFIER | THIS | SUPER;

/**
Types are just normal identifiers, e.g.

* boolean
* byte
* char
* short
* int
* long
* \bigintv
* \seq
* \locset
* nullMod? id ('[]')
*/
jmlTypeType:  (NULLABLE | NON_NULL)? typeType
            ;
