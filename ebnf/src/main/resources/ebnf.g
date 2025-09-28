grammar = production+ ;
production = ( parser_production | lexer_production ) SEMICOLON ;
parser_production = PARSER_SYMBOL EQUALS parser_expression ;
lexer_production = LEXER_SYMBOL EQUALS lexer_expression ;

// parser expressions
parser_expression = PARSER_SYMBOL
                  | LEXER_SYMBOL
                  | LEFT_PARENTHESIS parser_expression RIGHT_PARENTHESIS
                  | parser_expression QUESTION_MARK
                  | parser_expression PLUS
                  | parser_expression ASTERISK
                  | parser_expression VERTICAL_LINE parser_expression
                  | parser_expression parser_expression ;

/*
Lexer expressions
Unrolled to fix left-recursion
*/
lexer_expression = lexer_alternation ;
lexer_alternation = lexer_concatenation ( VERTICAL_LINE lexer_concatenation )* ;
lexer_concatenation = lexer_repetition lexer_repetition* ;
lexer_repetition = lexer_primary quantifier? ;
lexer_primary = STRING_LITERAL
              | LEFT_PARENTHESIS lexer_expression RIGHT_PARENTHESIS ;

quantifier = QUESTION_MARK | PLUS | ASTERISK ;

SEMICOLON = ";" ;
EQUALS = "=" ;
UNDERSCORE = "_" ;
QUESTION_MARK = "?" ;
PLUS = "+" ;
ASTERISK = "*" ;
LEFT_PARENTHESIS = "(" ;
RIGHT_PARENTHESIS = ")" ;
DOUBLE_QUOTES = "\"" ;
VERTICAL_LINE = "|" ;

SYMBOL = SEMICOLON | EQUALS | UNDERSCORE | QUESTION_MARK | PLUS | ASTERISK | LEFT_PARENTHESIS | RIGHT_PARENTHESIS ;
LEXER_SYMBOL = ( UPPERCASE_LETTER | UNDERSCORE )+ ;
PARSER_SYMBOL = ( LOWERCASE_LETTER | UNDERSCORE )+ ;
STRING_LITERAL = DOUBLE_QUOTES ( LETTER | SYMBOL )* DOUBLE_QUOTES ;

LETTER = LOWERCASE_LETTER | UPPERCASE_LETTER ;
LOWERCASE_LETTER = "a" | "b" | "c" | "d" | "e" | "f" | "g"
                 | "h" | "i" | "j" | "k" | "l" | "m" | "n"
                 | "o" | "p" | "q" | "r" | "s" | "t" | "u"
                 | "v" | "w" | "x" | "y" | "z" ;
UPPERCASE_LETTER = "A" | "B" | "C" | "D" | "E" | "F" | "G"
                 | "H" | "I" | "J" | "K" | "L" | "M" | "N"
                 | "O" | "P" | "Q" | "R" | "S" | "T" | "U"
                 | "V" | "W" | "X" | "Y" | "Z" ;

_WHITESPACE = (" " | "\t" | "\n" )* ;
_LINE_COMMENT = "//" .* "\n" ;
_MULTI_LINE_COMMENT = "/*" .* "*/" ;
