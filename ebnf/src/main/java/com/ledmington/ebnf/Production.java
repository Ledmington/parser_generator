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
public sealed class Production implements Node permits LexerProduction {

	private final NonTerminal start;
	private final Expression result;

	public Production(final NonTerminal start, final Expression result) {
		this.start = Objects.requireNonNull(start);
		this.result = Objects.requireNonNull(result);
	}

	public NonTerminal start() {
		return start;
	}

	public Expression result() {
		return result;
	}

	@Override
	public int hashCode() {
		int h = 17;
		h = 31 * h + start.hashCode();
		h = 31 * h + result.hashCode();
		return h;
	}

	@Override
	public String toString() {
		return "Production[start=" + start + ";result=" + result + "]";
	}

	@Override
	public boolean equals(final Object other) {
		if (other == null) {
			return false;
		}
		if (this == other) {
			return true;
		}
		if (!(other instanceof Production p)) {
			return false;
		}
		return this.start.equals(p.start) && this.result.equals(p.result);
	}
}
