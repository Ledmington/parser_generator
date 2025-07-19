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

	private static Set<State> epsilonClosure(final Set<State> states, final Set<StateTransition> transitions) {
		final Set<State> result = new HashSet<>();
		for (final State s : states) {
			result.addAll(epsilonClosure(s, transitions));
		}
		return result;
	}

	public static Automaton epsilonNFAtoNFA(final Automaton epsilonNFA) {
		final State oldStartState = epsilonNFA.startingState();
		final Set<State> oldStates = epsilonNFA.transitions().stream()
				.flatMap(t -> Stream.of(t.from(), t.to()))
				.collect(Collectors.toUnmodifiableSet());
		final Set<State> oldAcceptingStates =
				oldStates.stream().filter(State::isFinal).collect(Collectors.toUnmodifiableSet());
		final Set<State> newStates = new HashSet<>();
		final Set<StateTransition> oldTransitions = epsilonNFA.transitions();
		final Set<StateTransition> newTransitions = new HashSet<>();
		final Map<State, State> stateMapping = new HashMap<>();

		for (final State s : oldStates) {
			final State newState = new State(s.name(), oldAcceptingStates.contains(s));
			stateMapping.put(s, newState);
			newStates.add(newState);
		}

		for (final State s : oldStates) {
			final Set<State> closure = epsilonClosure(s, oldTransitions);
			// for (final String symbol : alphabet) {
			final Set<State> targets = new HashSet<>(closure);
			/*for (final State c : closure) {
				for (final StateTransition t : oldTransitions) {
					if (t.from().equals(c)) {
						targets.add();
					}
				}
				// targets.addAll(transitions.getOrDefault(c, new HashMap<>()).getOrDefault(symbol, new HashSet<>()));
			}*/
			final Set<State> closureTargets = epsilonClosure(targets, oldTransitions);
			for (final State target : closureTargets) {
				for (final StateTransition t : oldTransitions) {
					if (t.from().equals(s) && t.to().equals(target)) {
						newTransitions.add(
								new StateTransition(stateMapping.get(s), stateMapping.get(target), t.character()));
					}
				}
				// newTransitions.add(new StateTransition(stateMapping.get(s), symbol, stateMapping.get(target)));
			}
			// }
		}

		return new Automaton(oldStartState, newTransitions);
	}

	public static Automaton NFAtoDFA(final Automaton nfa) {
		// TODO
		return nfa;
	}

	public static Automaton minimizeDFA(final Automaton dfa) {
		// TODO
		return dfa;
	}

	public static void assertAutomatonValid(final Automaton automaton) {
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
}
