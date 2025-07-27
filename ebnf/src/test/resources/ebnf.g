grammar = production+ ;
production = ( parser_production | lexer_production )? SEMICOLON ;
parser_production = PARSER_SYMBOL EQUALS parser_expression ;
lexer_production = LEXER_SYMBOL EQUALS lexer_expression ;

parser_expression = PARSER_SYMBOL
                  | LEXER_SYMBOL
                  | parser_expression QUESTION_MARK
                  | parser_expression PLUS
                  | parser_expression ASTERISK
                  | parser_expression VERTICAL_LINE parser_expression
                  | LEFT_PARENTHESIS parser_expression RIGHT_PARENTHESIS
                  | parser_expression parser_expression ;

lexer_expression = lexer_expression QUESTION_MARK
                 | lexer_expression PLUS
                 | lexer_expression ASTERISK
                 | lexer_expression VERTICAL_LINE lexer_expression
                 | LEFT_PARENTHESIS lexer_expression RIGHT_PARENTHESIS
                 | DOUBLE_QUOTES .+ DOUBLE_QUOTES ;

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
LEXER_SYMBOL = ( "A" | "B" | "C" | "D" | "E" | "F" | "G"
               | "H" | "I" | "J" | "K" | "L" | "M" | "N"
               | "O" | "P" | "Q" | "R" | "S" | "T" | "U"
               | "V" | "W" | "X" | "Y" | "Z" | "_" )+ ;
PARSER_SYMBOL = ( "a" | "b" | "c" | "d" | "e" | "f" | "g"
                | "h" | "i" | "j" | "k" | "l" | "m" | "n"
                | "o" | "p" | "q" | "r" | "s" | "t" | "u"
                | "v" | "w" | "x" | "y" | "z" | "_" )+ ;
_WHITESPACE = (" " | "\t" | "\n" )* ;
