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
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ledmington.ebnf.Alternation;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.OptionalNode;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Repetition;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.Terminal;

// FIXME: find a better name or place for these functions
public final class AutomataUtils {

	private AutomataUtils() {}

	public static Automaton grammarToEpsilonNFA(final Grammar g) {
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
			case OptionalNode o -> convertOptionalNode(o, states, transitions, start, end);
			case Repetition r -> convertRepetition(r, states, transitions, start, end);
			case Alternation a -> convertAlternation(a, states, transitions, start, end);
			case Sequence s -> convertSequence(s, states, transitions, start, end);
			default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
		}
	}

	private static void convertRepetition(
			final Repetition r,
			final Set<State> states,
			final Set<StateTransition> transitions,
			final State start,
			final State end) {
		final State a = new State();
		final State b = new State();
		states.add(a);
		states.add(b);

		transitions.add(new StateTransition(start, end, StateTransition.EPSILON));
		transitions.add(new StateTransition(start, a, StateTransition.EPSILON));
		convertNode(r.inner(), states, transitions, a, b);
		transitions.add(new StateTransition(b, a, StateTransition.EPSILON));
		transitions.add(new StateTransition(b, end, StateTransition.EPSILON));
	}

	private static void convertSequence(
			final Sequence s,
			final Set<State> states,
			final Set<StateTransition> transitions,
			final State start,
			final State end) {
		State prev = new State();
		states.add(prev);
		transitions.add(new StateTransition(start, prev, StateTransition.EPSILON));
		for (int i = 0; i < s.nodes().size(); i++) {
			final State tmp = new State();
			states.add(tmp);
			convertNode(s.nodes().get(i), states, transitions, prev, tmp);
			prev = tmp;
		}
		transitions.add(new StateTransition(prev, end, StateTransition.EPSILON));
	}

	private static void convertOptionalNode(
			final OptionalNode o,
			final Set<State> states,
			final Set<StateTransition> transitions,
			final State start,
			final State end) {
		final State a = new State();
		final State b = new State();
		states.add(a);
		states.add(b);

		transitions.add(new StateTransition(start, end, StateTransition.EPSILON));
		transitions.add(new StateTransition(start, a, StateTransition.EPSILON));
		convertNode(o.inner(), states, transitions, a, b);
		transitions.add(new StateTransition(b, end, StateTransition.EPSILON));
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

	private static Set<State> epsilonClosure(final State state, final Set<StateTransition> transitions) {
		final Stack<State> stack = new Stack<>();
		final Set<State> closure = new HashSet<>();
		stack.push(state);
		closure.add(state);

		while (!stack.isEmpty()) {
			final State s = stack.pop();
			for (final StateTransition t : transitions) {
				if (!(t.from().equals(s) && t.character() == StateTransition.EPSILON)) {
					continue;
				}
				final State to = t.to();
				if (closure.contains(to)) {
					continue;
				}

				closure.add(to);
				stack.push(to);
			}
		}

		return closure;
	}

	public static Automaton epsilonNFAtoNFA(final Automaton epsilonNFA) {
		final Set<State> oldStates = epsilonNFA.transitions().stream()
				.flatMap(t -> Stream.of(t.from(), t.to()))
				.collect(Collectors.toUnmodifiableSet());
		final Map<State, State> stateMapping = new HashMap<>();

		for (final State s : oldStates) {
			if (stateMapping.containsKey(s)) {
				continue;
			}
			final Set<State> closure = epsilonClosure(s, epsilonNFA.transitions());

			final boolean isAccepting = closure.stream().anyMatch(State::isFinal);
			final State newState = new State(isAccepting);
			closure.forEach(state -> stateMapping.put(state, newState));
		}
		final Set<StateTransition> newTransitions = new HashSet<>();
		for (final StateTransition t : epsilonNFA.transitions()) {
			newTransitions.add(
					new StateTransition(stateMapping.get(t.from()), stateMapping.get(t.to()), t.character()));
		}
		return new Automaton(stateMapping.get(epsilonNFA.startingState()), newTransitions);
	}

	public static Automaton NFAtoDFA(final Automaton nfa) {
		// TODO
		return nfa;
	}

	public static Automaton minimizeDFA(final Automaton dfa) {
		// TODO
		return dfa;
	}

	public static void assertEpsilonNFAValid(final Automaton automaton) {
		final Set<State> allStates = Stream.concat(
						Stream.of(automaton.startingState()),
						automaton.transitions().stream().flatMap(t -> Stream.of(t.from(), t.to())))
				.collect(Collectors.toUnmodifiableSet());

		// At least one final state
		if (allStates.stream().noneMatch(State::isFinal)) {
			throw new IllegalArgumentException("No final state in the given automaton.");
		}

		// Only one strongly connect component
		final Queue<State> q = new ArrayDeque<>();
		final Set<State> visited = new HashSet<>();
		q.add(automaton.startingState());

		while (!q.isEmpty()) {
			final State s = q.remove();
			if (visited.contains(s)) {
				continue;
			}
			visited.add(s);

			for (final StateTransition t : automaton.transitions()) {
				if (t.from().equals(s)) {
					q.add(t.to());
				}
			}
		}

		if (!visited.equals(allStates)) {
			throw new IllegalArgumentException("The automaton is not a single strongly connected component.");
		}
	}

	public static void assertNFAValid(final Automaton automaton) {
		assertEpsilonNFAValid(automaton);

		if (automaton.transitions().stream().anyMatch(t -> t.character() == StateTransition.EPSILON)) {
			throw new IllegalArgumentException("Epsilon transitions found in an NFA.");
		}
	}

	public static void assertDFAValid(final Automaton automaton) {
		assertNFAValid(automaton);

		final Set<State> allStates = Stream.concat(
						Stream.of(automaton.startingState()),
						automaton.transitions().stream().flatMap(t -> Stream.of(t.from(), t.to())))
				.collect(Collectors.toUnmodifiableSet());

		for (final State s : allStates) {
			final Set<Character> transitions = new HashSet<>();
			for (final StateTransition t : automaton.transitions()) {
				if (!t.from().equals(s)) {
					continue;
				}
				if (transitions.contains(t.character())) {
					throw new IllegalArgumentException(String.format(
							"State '%s' has two transitions with the same character '%c' (U+%04X).",
							s, t.character(), (int) t.character()));
				}
				transitions.add(t.character());
			}
		}
	}
}
