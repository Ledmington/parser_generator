/*
 * parser-gen - Parser Generator
 * Copyright (C) 2025-2025 Filippo Barbari <filippo.barbari@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.ledmington.ebnf;

import java.util.Objects;

/**
 * The most important element of an EBNF grammar, maps a non-terminal symbol to an expression which represents the
 * possible expansions.
 *
 * @param start The non-terminal symbol which can be substituted with the expression on the right-hand side of this
 *     production.
 * @param result The expression to replace the non-terminal.c
 */
public record Production(NonTerminal start, Expression result) implements Node {

	/**
	 * Creates a new production with the given non-terminal symbol and the given expression.
	 *
	 * @param start The non-terminal symbol representing this production.
	 * @param result The expression which the symbol maps to.
	 */
	public Production {
		Objects.requireNonNull(start);
		Objects.requireNonNull(result);
	}

	/**
	 * Checks where the given production name corresponds to a lexer production.
	 *
	 * @param productionName The name of the production to check.
	 * @return True is the given production belongs to a lexer, false otherwise.
	 */
	public static boolean isLexerProduction(final String productionName) {
		return productionName
				.chars()
				.allMatch(ch -> ch == '_' || (Character.isAlphabetic(ch) && Character.isUpperCase(ch)));
	}

	/**
	 * Checks where the given production name corresponds to a skippable production.
	 *
	 * @param productionName The name of the production to check.
	 * @return True is the given production is skippable, false otherwise.
	 */
	public static boolean isSkippable(final String productionName) {
		return isLexerProduction(productionName) && productionName.charAt(0) == '_';
	}
}
