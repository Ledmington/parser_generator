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

public record Alternation(List<Expression> nodes) implements Expression {
	public Alternation(final Expression... nodes) {
		this(List.of(nodes));
	}

	@Override
	public String prettyPrint(final String indent) {
		final StringBuilder sb = new StringBuilder();
		sb.append(indent).append("Alternation {\n");
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
