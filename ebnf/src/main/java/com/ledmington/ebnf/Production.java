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

	public Production {
		Objects.requireNonNull(start);
		Objects.requireNonNull(result);
	}

	private static boolean hasOnlyTerminals(final Expression e) {
		return switch (e) {
			case Terminal ignored -> true;
			case NonTerminal ignored -> false;
			case OptionalNode o -> hasOnlyTerminals(o.inner());
			case Repetition r -> hasOnlyTerminals(r.inner());
			case Sequence s -> s.nodes().stream().allMatch(Production::hasOnlyTerminals);
			case Alternation a -> a.nodes().stream().allMatch(Production::hasOnlyTerminals);
			default -> throw new IllegalArgumentException(String.format("Unknown node '%s'", e));
		};
	}

	public boolean isLexerProduction() {
		// TODO: maybe cache this value?
		return hasOnlyTerminals(result);
		// return result instanceof Terminal;
	}

	public boolean isSkippable() {
		return isLexerProduction() && start.name().charAt(0) == '_';
	}
}
