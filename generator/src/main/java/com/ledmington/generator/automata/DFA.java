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
package com.ledmington.generator.automata;

import java.util.Map;
import java.util.Set;

import com.ledmington.ebnf.Utils;

/** Common interface for deterministc finite-state automata. */
public interface DFA extends Automaton {

	/**
	 * Returns a new DFABuilder.
	 *
	 * @return A new DFABuilder.
	 */
	static DFABuilder builder() {
		return new DFABuilder();
	}

	/**
	 * Returns the "set" of neighbors of the given state.
	 *
	 * @param s The state the transitions start from.
	 * @return The "set" of neighbors.
	 */
	Map<Character, State> neighbors(final State s);

	default String toGraphviz() {
		final Set<State> allStates = states();
		final StringBuilder sb = new StringBuilder();

		sb.append("digraph Automaton {\n");
		sb.append("    rankdir=LR;\n");
		sb.append("    node [shape = doublecircle];\n");

		for (final State s : allStates) {
			if (s.isAccepting()) {
				sb.append("    ").append(s.name()).append(";\n");
			}
		}

		sb.append("    node [shape = circle];\n");
		sb.append("    __start__ [shape = point];\n");
		sb.append("    __start__ -> ").append(startingState().name()).append(";\n");

		for (final State src : allStates) {
			final Map<Character, State> neighbors = neighbors(src);
			if (neighbors == null) {
				continue;
			}
			for (final Map.Entry<Character, State> e : neighbors.entrySet()) {
				final char symbol = e.getKey();
				final State dst = e.getValue();
				sb.append("    ")
						.append(src.name())
						.append(" -> ")
						.append(dst.name())
						.append(" [label=\"")
						.append(symbol == NFA.EPSILON ? "ε" : (Utils.getEscapeCharacter(symbol)))
						.append("\"];\n");
			}
		}

		sb.append("}\n");
		return sb.toString();
	}
}
