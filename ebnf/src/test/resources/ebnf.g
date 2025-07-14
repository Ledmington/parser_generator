grammar = { rule } ;
rule = lhs , EQUAL SIGN , rhs , SEMICOLON ;
lhs = identifier ;
rhs = alternation ;
identifier = LETTER , { LETTER | DIGIT | UNDERSCORE } ;
terminal = DOUBLE QUOTES , character without quotes , { character without quotes } , DOUBLE QUOTES ;
character without quotes = LETTER | DIGIT | SYMBOL | UNDERSCORE | SPACE ;
concatenation = term , { COMMA , term } ;
alternation = concatenation , { VERTICAL LINE , concatenation } ;
term = ( OPEN SQUARE BRACKET , rhs , CLOSED SQUARE BRACKET )
     | ( OPEN CURLY BRACKET , rhs , CLOSED CURLY BRACKET )
     | terminal
     | identifier ;

SEMICOLON = ";" ;
EQUAL SIGN = "=" ;
UNDERSCORE = "_" ;
SPACE = " " ;
DOUBLE QUOTES = "\"" ;
OPEN SQUARE BRACKET = "[" ;
CLOSED SQUARE BRACKET = "]" ;
OPEN CURLY BRACKET = "{" ;
CLOSED CURLY BRACKET = "}" ;
COMMA = "," ;
VERTICAL LINE = "|" ;
WHITESPACE = { " " | "\n" | "\t" } ;
DIGIT = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
LETTER = "A" | "B" | "C" | "D" | "E" | "F" | "G"
       | "H" | "I" | "J" | "K" | "L" | "M" | "N"
       | "O" | "P" | "Q" | "R" | "S" | "T" | "U"
       | "V" | "W" | "X" | "Y" | "Z" | "a" | "b"
       | "c" | "d" | "e" | "f" | "g" | "h" | "i"
       | "j" | "k" | "l" | "m" | "n" | "o" | "p"
       | "q" | "r" | "s" | "t" | "u" | "v" | "w"
       | "x" | "y" | "z" ;
SYMBOL = "[" | "]" | "{" | "}" | "(" | ")" | "<" | ">"
       | "'" | "=" | "|" | "." | "," | ";" | "-"
       | "+" | "*" | "?" | "\n" | "\t" ;
