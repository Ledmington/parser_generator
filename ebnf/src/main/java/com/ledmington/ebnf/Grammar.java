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

import java.util.List;

/**
 * An object representing all the productions and symbols of an EBNF grammar.
 *
 * @param productions The ordered sequence of productions of the grammar.
 */
// TODO: does it need to be a List?
public record Grammar(List<Production> productions) implements Node {

	/**
	 * Creates a new Grammar with the given Productions.
	 *
	 * @param productions The list of productions of the grammar.
	 */
	public Grammar(final Production... productions) {
		this(List.of(productions));
	}
}
