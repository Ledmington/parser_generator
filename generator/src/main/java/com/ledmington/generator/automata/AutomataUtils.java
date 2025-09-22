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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** A collection of common algorithms on finite-state automata. */
public final class AutomataUtils {
	// FIXME: find a better name or place for these functions

	private static final StateFactory stateFactory = new StateFactory();

	private AutomataUtils() {}

	/**
	 * Converts the given DFA into a minimized DFA.
	 *
	 * @param dfa The DFA to be minimized.
	 * @return A new minimized DFA.
	 */
	public static DFA minimizeDFA(final DFA dfa) {
		final AutomataUtils converter = new AutomataUtils();
		return converter.convertDFAToMinimizedDFA(dfa);
	}

	private DFA convertDFAToMinimizedDFA(final DFA dfa) {
		// Myhill-Nerode theorem
		final DFABuilder builder = DFA.builder();
		final List<State> oldStates = dfa.states().stream().toList();
		final Map<State, Integer> oldStatesIndex =
				IntStream.range(0, oldStates.size()).boxed().collect(Collectors.toMap(oldStates::get, i -> i));
		final int n = oldStates.size();
		final boolean[][] isDistinguishable = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			final State a = oldStates.get(i);
			for (int j = i + 1; j < n; j++) {
				final State b = oldStates.get(j);
				// any accepting state is trivially distinguishable from any non-accepting one
				if (a.isAccepting() != b.isAccepting()) {
					isDistinguishable[i][j] = true;
					isDistinguishable[j][i] = true;
				} else
				// two accepting states are trivially distinguishable if they refer to different tokens
				if (a.isAccepting()
						&& b.isAccepting()
						&& !((AcceptingState) a).tokenName().equals(((AcceptingState) b).tokenName())) {
					isDistinguishable[i][j] = true;
					isDistinguishable[j][i] = true;
				}
			}
		}

		boolean atLeastOnePairDistinguished;
		do {
			atLeastOnePairDistinguished = false;
			for (int i = 0; i < n; i++) {
				final State p = oldStates.get(i);
				final Map<Character, State> px = dfa.neighbors(p);
				if (px == null) {
					continue;
				}

				for (int j = i + 1; j < n; j++) {
					if (isDistinguishable[i][j]) {
						continue;
					}

					final State q = oldStates.get(j);
					final Map<Character, State> qx = dfa.neighbors(q);
					if (qx == null) {
						continue;
					}

					if (!px.keySet().equals(qx.keySet())) {
						isDistinguishable[i][j] = true;
						isDistinguishable[j][i] = true;
						atLeastOnePairDistinguished = true;
						continue;
					}
					for (final char x : px.keySet()) {
						final int pxx = oldStatesIndex.get(px.get(x));
						final int qxx = oldStatesIndex.get(qx.get(x));
						if (isDistinguishable[pxx][qxx]) {
							isDistinguishable[i][j] = true;
							isDistinguishable[j][i] = true;
							atLeastOnePairDistinguished = true;
							break;
						}
					}
				}
			}
		} while (atLeastOnePairDistinguished);

		// Create group of equivalent states
		final Map<State, Set<State>> equivalentGroups = new HashMap<>();
		for (int i = 0; i < n; i++) {
			final State a = oldStates.get(i);
			equivalentGroups.putIfAbsent(a, new HashSet<>(Set.of(a)));
			for (int j = i + 1; j < n; j++) {
				final State b = oldStates.get(j);
				if (!isDistinguishable[i][j]) {
					equivalentGroups.get(a).add(b);
					equivalentGroups.put(b, equivalentGroups.get(a));
				}
			}
		}

		// Create a new state for each equivalent state
		final Map<State, State> oldToNew = new HashMap<>();
		for (final Set<State> eqClass : new HashSet<>(equivalentGroups.values())) {
			final boolean isAccept = eqClass.stream().anyMatch(State::isAccepting);
			final State rep = isAccept
					? stateFactory.getNewAcceptingState(((AcceptingState) eqClass.stream()
									.filter(State::isAccepting)
									.findFirst()
									.orElseThrow())
							.tokenName())
					: stateFactory.getNewState();
			for (final State s : eqClass) {
				oldToNew.put(s, rep);
			}
		}

		// Add transitions
		for (final State src : oldStates) {
			final Map<Character, State> neighbors = dfa.neighbors(src);
			if (neighbors == null) {
				continue;
			}
			for (final Map.Entry<Character, State> e : neighbors.entrySet()) {
				builder.addTransition(oldToNew.get(src), e.getKey(), oldToNew.get(e.getValue()));
			}
		}

		final State newStart = oldToNew.get(dfa.startingState());

		return builder.start(newStart).build();
	}

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
