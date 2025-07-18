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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.ledmington.ebnf.Alternation;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Terminal;

// FIXME: find a better name or place for these functions
public final class AutomataUtils {

	private AutomataUtils() {}

	public static Automaton grammarToNFA(final Grammar g) {
		final Set<Production> prod =
				g.productions().stream().filter(Production::isLexerProduction).collect(Collectors.toUnmodifiableSet());
		final Set<State> states = new HashSet<>();
		final Set<StateTransition> transitions = new HashSet<>();

		final State start = new State("Start", false);
		states.add(start);
		final State end = new State("End", true);
		states.add(end);

		for (final Production p : prod) {
			convertNode(p.result(), states, transitions, start, end);
		}

		{
			states.stream().sorted(Comparator.comparing(State::name)).forEach(s -> {
				System.out.printf(" %-5s : ", s.name());
				for (final StateTransition t : transitions) {
					if (!t.from().equals(s)) {
						continue;
					}
					System.out.printf("%c|%-5s ", t.character(), t.to().name());
				}
				System.out.println();
			});
		}

		// Check that there is at least one final state
		if (states.stream().noneMatch(State::isFinal)) {
			throw new IllegalArgumentException("No final states.");
		}

		// Check that all transitions make sense
		for (final StateTransition t : transitions) {
			if (!states.contains(t.from())) {
				throw new IllegalArgumentException("Invalid transition from unexisting state.");
			}
			if (!states.contains(t.to())) {
				throw new IllegalArgumentException("Invalid transition to unexisting state.");
			}
		}

		// Check that all states are reachable from the starting state (1 strongly connected component)
		{
			final Queue<State> q = new ArrayDeque<>();
			final Set<State> visited = new HashSet<>();
			q.add(start);

			while (!q.isEmpty()) {
				final State s = q.remove();
				if (visited.contains(s)) {
					continue;
				}
				visited.add(s);

				for (final StateTransition t : transitions) {
					if (!t.from().equals(s)) {
						continue;
					}
					q.add(t.to());
				}
			}
			if (!visited.equals(states)) {
				throw new IllegalArgumentException(String.format(
						"The following nodes are not reachable from the start node: %s.",
						states.stream()
								.filter(s -> !visited.contains(s))
								.map(State::name)
								.collect(Collectors.joining(", "))));
			}
		}

		return new Automaton(start, transitions);
	}

	private static void convertNode(
			final Node n,
			final Set<State> states,
			final Set<StateTransition> transitions,
			final State start,
			final State end) {
		switch (n) {
			case Terminal t -> convertTerminal(t, states, transitions, start, end);
			case Alternation a -> convertAlternation(a, states, transitions, start, end);
			default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
		}
	}

	private static void convertAlternation(
			final Alternation a,
			final Set<State> states,
			final Set<StateTransition> transitions,
			final State start,
			final State end) {
		for (final Node n : a.nodes()) {
			final State newStart = new State();
			final State newEnd = new State();
			states.add(newStart);
			states.add(newEnd);
			transitions.add(new StateTransition(start, newStart, StateTransition.EPSILON));
			transitions.add(new StateTransition(newEnd, end, StateTransition.EPSILON));
			convertNode(n, states, transitions, newStart, newEnd);
		}
	}

	private static void convertTerminal(
			final Terminal t,
			final Set<State> states,
			final Set<StateTransition> transitions,
			final State start,
			final State end) {
		State prev = new State();
		states.add(prev);
		transitions.add(new StateTransition(start, prev, StateTransition.EPSILON));
		for (int i = 0; i < t.literal().length(); i++) {
			final State s = new State();
			states.add(s);
			transitions.add(new StateTransition(prev, s, t.literal().charAt(i)));
			prev = s;
		}
		transitions.add(new StateTransition(prev, end, StateTransition.EPSILON));
	}

	public static Automaton epsilonNFAtoNFA(final Automaton epsilonNFA) {
		// TODO
		return epsilonNFA;
	}

	public static Automaton NFAtoDFA(final Automaton nfa) {
		// TODO
		return nfa;
	}

	public static Automaton minimizeDFA(final Automaton dfa) {
		// TODO
		return dfa;
	}
}
