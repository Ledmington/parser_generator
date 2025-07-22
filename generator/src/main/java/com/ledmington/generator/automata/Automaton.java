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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Automaton {

	private final State startingState;
	// FIXME: convert to Map<State, Map<Character, State>>? How to deal with NFAs?
	private final Set<StateTransition> transitions;

	public Automaton(final State startingState, final Set<StateTransition> transitions) {
		this.startingState = Objects.requireNonNull(startingState);
		this.transitions = new HashSet<>(transitions);
	}

	public State startingState() {
		return startingState;
	}

	public Set<StateTransition> transitions() {
		return transitions;
	}

	public Set<State> states() {
		return Stream.concat(Stream.of(startingState), transitions.stream().flatMap(t -> Stream.of(t.from(), t.to())))
				.collect(Collectors.toUnmodifiableSet());
	}

	public boolean matches(final String input) {
		// NOTE: the result of this method makes sense only if this automaton is a DFA
		State currentState = this.startingState;
		int i = 0;
		while (i < input.length()) {
			final char ch = input.charAt(i);
			boolean foundTransition = false;
			for (final StateTransition t : transitions) {
				if (t.from().equals(currentState) && t.character() == ch) {
					i++;
					currentState = t.to();
					foundTransition = true;
					break;
				}
			}
			if (!foundTransition) {
				return false;
			}
		}
		return currentState.isAccepting();
	}

	// just for debugging: outputs graphviz code to be used in tools such as https://graph.flyte.org
	public String toGraphviz() {
		final Set<State> allStates = states();
		final StringBuilder sb = new StringBuilder();

		sb.append("digraph Automaton {\n");
		sb.append("    rankdir=LR;\n");
		sb.append("    size=\"8,5\"\n");
		sb.append("    node [shape = doublecircle];\n");

		for (final State s : allStates) {
			if (s.isAccepting()) {
				sb.append("    ").append(s.name()).append(";\n");
			}
		}

		sb.append("    node [shape = circle];\n");
		sb.append("    __start__ [shape = point];\n");
		sb.append("    __start__ -> ").append(startingState.name()).append(";\n");

		for (final StateTransition t : transitions) {
			sb.append("    ")
					.append(t.from().name())
					.append(" -> ")
					.append(t.to().name())
					.append(" [label=\"")
					.append(t.character() == StateTransition.EPSILON ? "ε" : t.character())
					.append("\"];\n");
		}

		sb.append("}\n");
		return sb.toString();
	}
}
