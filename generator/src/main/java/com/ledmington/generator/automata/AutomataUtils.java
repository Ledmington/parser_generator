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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** A collection of common algorithms on finite-state automata. */
public final class AutomataUtils {
	// FIXME: find a better name or place for these functions

	private AutomataUtils() {}

	/**
	 * Checks whether the given automaton is a valid epsilon-NFA.
	 *
	 * @param nfa The epsilon-NFA to check.
	 */
	public static void assertEpsilonNFAValid(final NFA nfa) {
		final Set<State> allStates = nfa.states();

		// At least one final state
		if (allStates.stream().noneMatch(State::isAccepting)) {
			throw new IllegalArgumentException("No final state in the given automaton.");
		}

		// Only one strongly connected component
		final Queue<State> q = new ArrayDeque<>();
		final Set<State> visited = new HashSet<>();
		q.add(nfa.startingState());

		while (!q.isEmpty()) {
			final State s = q.remove();
			if (visited.contains(s)) {
				continue;
			}
			visited.add(s);

			final Map<Character, Set<State>> neighbors = nfa.neighbors(s);
			if (neighbors == null) {
				continue;
			}
			for (final Map.Entry<Character, Set<State>> e : neighbors.entrySet()) {
				q.addAll(e.getValue());
			}
		}

		if (!visited.equals(allStates)) {
			throw new IllegalArgumentException("The automaton is not a single strongly connected component.");
		}

		for (final State s : allStates) {
			if (!s.isAccepting() && nfa.neighbors(s).isEmpty()) {
				// A dead-end node is a non-accepting node with no outgoing edges
				throw new IllegalArgumentException("The automaton contains dead-end nodes.");
			}
		}
	}

	/**
	 * Checks whether the given automaton is a valid NFA without epsilon transitions.
	 *
	 * @param finiteStateAutomaton The NFA to check.
	 */
	public static void assertNFAValid(final NFA finiteStateAutomaton) {
		assertEpsilonNFAValid(finiteStateAutomaton);

		if (finiteStateAutomaton.states().stream().anyMatch(s -> {
			final Map<Character, Set<State>> neighbors = finiteStateAutomaton.neighbors(s);
			if (neighbors == null) {
				return false;
			}
			return neighbors.containsKey(NFA.EPSILON);
		})) {
			throw new IllegalArgumentException("Epsilon transitions found in an NFA.");
		}
	}

	/**
	 * Checks whether the given automaton is a valid DFA.
	 *
	 * @param finiteStateAutomaton The DFA to check.
	 */
	public static void assertDFAValid(final DFA finiteStateAutomaton) {
		assertNFAValid(new NFA() {
			@Override
			public Map<Character, Set<State>> neighbors(final State s) {
				final Map<Character, Set<State>> m = new HashMap<>();
				final Map<Character, State> neighbors = finiteStateAutomaton.neighbors(s);
				if (neighbors != null) {
					for (final Map.Entry<Character, State> e : neighbors.entrySet()) {
						m.put(e.getKey(), Set.of(e.getValue()));
					}
				}
				return m;
			}

			@Override
			public State startingState() {
				return finiteStateAutomaton.startingState();
			}

			@Override
			public Set<State> states() {
				return finiteStateAutomaton.states();
			}

			@Override
			public Map<String, Integer> priorities() {
				return Map.of();
			}
		});
	}
}
