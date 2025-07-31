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

import java.util.Map;
import java.util.Objects;

/**
 * An object representing all the productions and symbols of an EBNF grammar.
 *
 * @param productions The productions which map each non-terminal symbol to its corresponding expression.
 */
public record Grammar(Map<NonTerminal, Expression> productions) implements Node {

	/**
	 * Creates a new Grammar with the given Map of productions.
	 *
	 * @param productions The productions of this grammar.
	 */
	public Grammar {
		Objects.requireNonNull(productions);
	}
}
