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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

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

	public boolean isLexerProduction() {
		// TODO: maybe cache this value?
		final Queue<Node> q = new ArrayDeque<>();
		final Set<Node> visited = new HashSet<>();
		q.add(result);
		while (!q.isEmpty()) {
			final Node n = q.remove();
			if (visited.contains(n)) {
				continue;
			}
			visited.add(n);
			switch (n) {
				case Terminal ignored -> {}
				case NonTerminal ignored -> {
					return false;
				}
				case OptionalNode o -> q.add(o.inner());
				case Repetition r -> q.add(r.inner());
				case Alternation a -> q.addAll(a.nodes());
				case Sequence s -> q.addAll(s.nodes());
				default -> throw new IllegalArgumentException(String.format("Unknown node: '%s'.", n));
			}
		}
		return true;
	}

	public boolean isSkippable() {
		return isLexerProduction() && start.name().charAt(0) == '_';
	}
}
