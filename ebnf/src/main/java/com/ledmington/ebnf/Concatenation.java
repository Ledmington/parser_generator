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
 * An element of an EBNF grammar which represents an ordered sequence of expressions.
 *
 * @param nodes The ordered sequence of expressions.
 */
public record Concatenation(List<Expression> nodes) implements Expression {

	/**
	 * Creates a new Concatenation Node with given expressions.
	 *
	 * @param nodes The concatenated expressions.
	 */
	public Concatenation(final Expression... nodes) {
		this(List.of(nodes));
	}

	@Override
	public String prettyPrint(final String indent) {
		final StringBuilder sb = new StringBuilder();
		sb.append(indent).append("Concatenation {\n");
		if (!nodes.isEmpty()) {
			sb.append(nodes.getFirst().prettyPrint(indent + "  "));
			for (int i = 1; i < nodes.size(); i++) {
				sb.append('\n').append(nodes.get(i).prettyPrint(indent + "  "));
			}
			sb.append('\n');
		}
		sb.append(indent).append("}");
		return sb.toString();
	}
}
