S = SIGN number ;
number = ZERO | non_zero ;
non_zero = DIGIT_EXCLUDING_ZERO DIGIT* ;
ZERO = "0" ;
SIGN = ( "+" | "-" )? ;
DIGIT_EXCLUDING_ZERO = "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
DIGIT = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9" ;
