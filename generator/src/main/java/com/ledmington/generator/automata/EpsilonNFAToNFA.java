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
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public final class EpsilonNFAToNFA {

	private final StateFactory stateFactory;

	public EpsilonNFAToNFA(final StateFactory factory) {
		this.stateFactory = Objects.requireNonNull(factory);
	}

	public EpsilonNFAToNFA() {
		this(new StateFactory());
	}

	/**
	 * Converts the given epsilon-NFA to an NFA without epsilon transitions.
	 *
	 * @param epsilonNFA The epsilon-NFA to be converted.
	 * @return A new NFA without epsilon transitions.
	 */
	public NFA convert(final NFA epsilonNFA) {
		final Set<State> oldStates = epsilonNFA.states();
		final Map<State, Set<State>> epsilonClosures = new HashMap<>();

		// cache all epsilon-closures
		for (final State s : oldStates) {
			epsilonClosures.put(s, epsilonClosure(s, epsilonNFA));
		}

		final Map<State, State> stateMapping = new HashMap<>();
		for (final State s : oldStates) {
			final Set<State> closure = epsilonClosures.get(s);
			final boolean isAccepting = closure.stream().anyMatch(State::isAccepting);
			stateMapping.put(
					s,
					isAccepting
							? stateFactory.getNewAcceptingState(((AcceptingState) closure.stream()
											.filter(State::isAccepting)
											.findFirst()
											.orElseThrow())
									.tokenName())
							: stateFactory.getNewState());
		}

		final NFABuilder builder = NFA.builder();
		for (final State s : oldStates) {
			final Set<State> closure = epsilonClosures.get(s);

			for (final State cs : closure) {
				final Map<Character, Set<State>> neighbors = epsilonNFA.neighbors(cs);
				if (neighbors == null) {
					continue;
				}
				for (final Map.Entry<Character, Set<State>> e : neighbors.entrySet()) {
					final char symbol = e.getKey();
					if (symbol == NFA.EPSILON) {
						continue;
					}
					for (final State qq : e.getValue()) {
						for (final State q : epsilonClosures.get(qq)) {
							builder.addTransition(stateMapping.get(s), symbol, stateMapping.get(q));
						}
					}
				}
			}
		}

		final State newStartingState = stateMapping.get(epsilonNFA.startingState());

		// Explicit removing all states that are not reachable from the starting state
		final Queue<State> q = new ArrayDeque<>();
		final Set<State> visited = new HashSet<>();
		q.add(newStartingState);
		while (!q.isEmpty()) {
			final State s = q.remove();
			if (visited.contains(s)) {
				continue;
			}
			visited.add(s);

			final Map<Character, Set<State>> neighbors = builder.neighbors(s);
			if (neighbors == null) {
				continue;
			}
			for (final Map.Entry<Character, Set<State>> e : neighbors.entrySet()) {
				q.addAll(e.getValue());
			}
		}

		final Set<State> unreachableStates = new HashSet<>();
		for (final State newState : stateMapping.values()) {
			if (!visited.contains(newState)) {
				unreachableStates.add(newState);
			}
		}

		if (!unreachableStates.isEmpty()) {
			builder.removeStates(unreachableStates);
		}

		return builder.start(newStartingState)
				.priorities(epsilonNFA.priorities())
				.build();
	}

	private Set<State> epsilonClosure(final State state, final NFA nfa) {
		final Queue<State> q = new ArrayDeque<>();
		final Set<State> closure = new HashSet<>();
		final Set<State> visited = new HashSet<>();
		q.add(state);

		while (!q.isEmpty()) {
			final State s = q.remove();
			if (visited.contains(s)) {
				continue;
			}
			visited.add(s);
			closure.add(s);

			final Map<Character, Set<State>> neighbors = nfa.neighbors(s);
			if (neighbors != null && neighbors.containsKey(NFA.EPSILON)) {
				q.addAll(neighbors.get(NFA.EPSILON));
			}
		}

		return closure;
	}
}
