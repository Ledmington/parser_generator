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

import com.ledmington.ebnf.Utils;

/** A finite-state automaton. */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
// TODO: to be removed
public final class FiniteStateAutomaton {

	private final State startingState;
	// FIXME: convert to Map<State, Map<Character, State>>? How to deal with NFAs?
	private final Set<StateTransition> transitions;

	/**
	 * Creates a new automaton with the given starting state and the given set of transitions.
	 *
	 * @param startingState The starting state of the automaton.
	 * @param transitions The set of transitions of this automaton.
	 */
	public FiniteStateAutomaton(final State startingState, final Set<StateTransition> transitions) {
		this.startingState = Objects.requireNonNull(startingState);
		this.transitions = new HashSet<>(transitions);
	}

	/**
	 * Returns the starting state of this automaton.
	 *
	 * @return The starting state of this automaton.
	 */
	public State startingState() {
		return startingState;
	}

	/**
	 * Returns the set of transitions of this automaton.
	 *
	 * @return The set of transitions of this automaton.
	 */
	public Set<StateTransition> transitions() {
		return transitions;
	}

	/**
	 * Returns an unmodifiable set of all state involved in this automaton.
	 *
	 * @return The states of this automaton.
	 */
	public Set<State> states() {
		return Stream.concat(Stream.of(startingState), transitions.stream().flatMap(t -> Stream.of(t.from(), t.to())))
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Returns a string graphviz representation of this automaton. Useful for debugging when visualized through tools
	 * such as <a href="https://graph.flyte.org">this</a>.
	 *
	 * @return A string graphviz representation of this automaton.
	 */
	public String toGraphviz() {
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
		sb.append("    __start__ -> ").append(startingState.name()).append(";\n");

		for (final StateTransition t : transitions) {
			sb.append("    ")
					.append(t.from().name())
					.append(" -> ")
					.append(t.to().name())
					.append(" [label=\"")
					.append(t.character() == StateTransition.EPSILON ? "ε" : (Utils.getEscapeCharacter(t.character())))
					.append("\"];\n");
		}

		sb.append("}\n");
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Automaton[startingState=" + startingState + ";transitions=" + transitions + "]";
	}

	@Override
	public int hashCode() {
		int h = 17;
		h = 31 * h + startingState.hashCode();
		h = 31 * h + transitions.hashCode();
		return h;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == null) {
			return false;
		}
		if (this == other) {
			return true;
		}
		if (!(other instanceof FiniteStateAutomaton a)) {
			return false;
		}
		return this.startingState.equals(a.startingState) && this.transitions.equals(a.transitions);
	}
}
