letter = "A" | "B" | "C" | "D" | "E" | "F" | "G"
       | "H" | "I" | "J" | "K" | "L" | "M" | "N"
       | "O" | "P" | "Q" | "R" | "S" | "T" | "U"
       | "V" | "W" | "X" | "Y" | "Z" | "a" | "b"
       | "c" | "d" | "e" | "f" | "g" | "h" | "i"
       | "j" | "k" | "l" | "m" | "n" | "o" | "p"
       | "q" | "r" | "s" | "t" | "u" | "v" | "w"
       | "x" | "y" | "z" ;

digit = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;

symbol = "[" | "]" | "{" | "}" | "(" | ")" | "<" | ">"
       | "'" | "=" | "|" | "." | "," | ";" | "-"
       | "+" | "*" | "?" | "\n" | "\t" ;

character without quotes = letter | digit | symbol | "_" | " " ;
identifier = letter , { letter | digit | "_" } ;

whitespace = { " " | "\n" | "\t" } ;

terminal = "\"" , character without quotes , { character without quotes } , "\"" ;

terminator = ";" ;

term = "[" , whitespace , rhs , whitespace , "]"
     | "{" , whitespace , rhs , whitespace , "}"
     | terminal
     | identifier ;

concatenation = whitespace , factor , whitespace , { ",", whitespace , factor , whitespace } ;
alternation = whitespace , concatenation , whitespace , { "|" , whitespace , concatenation , whitespace } ;

rhs = alternation ;
lhs = identifier ;

rule = lhs , whitespace , "=" , whitespace , rhs , whitespace , terminator ;

grammar = { whitespace , rule , whitespace } ;
