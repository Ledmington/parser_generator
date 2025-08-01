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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.OneOrMore;
import com.ledmington.ebnf.Or;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.ZeroOrMore;
import com.ledmington.ebnf.ZeroOrOne;
import com.ledmington.generator.GrammarUtils;

/** A collection of common algorithms on finite-state automata. */
public final class AutomataUtils {
	// FIXME: find a better name or place for these functions

	private int stateID = 1;

	private AutomataUtils() {}

	private State state() {
		return new State("S" + (stateID++));
	}

	private AcceptingState acceptingState(final String productionName) {
		return new AcceptingState("S" + (stateID++), productionName);
	}

	/**
	 * Converts the given grammar into an epsilon-NFA by first extracting the lexer productions.
	 *
	 * @param g The grammar to be converted.
	 * @return A new epsilon-NFA representing all the lexer productions of the grammar combined.
	 */
	public static Automaton grammarToEpsilonNFA(final Grammar g) {
		final List<Production> lexerProductions = new ArrayList<>();
		final List<Production> parserProductions = new ArrayList<>();
		GrammarUtils.splitProductions(g.productions(), lexerProductions, parserProductions);
		return grammarToEpsilonNFA(lexerProductions);
	}

	/**
	 * Converts the given list of productions into an epsilon-NFA.
	 *
	 * @param lexerProductions The productions to be converted.
	 * @return A new epsilon-NFA.
	 */
	public static Automaton grammarToEpsilonNFA(final List<Production> lexerProductions) {
		final AutomataUtils converter = new AutomataUtils();
		return converter.convertGrammarToEpsilonNFA(lexerProductions);
	}

	private Automaton convertGrammarToEpsilonNFA(final List<Production> lexerProductions) {
		final Set<StateTransition> transitions = new HashSet<>();

		final State globalStart = state();

		for (final Production p : lexerProductions) {
			final String productionName = p.start().name();
			final State productionStart = state();
			final State productionEnd = acceptingState(productionName);

			transitions.add(new StateTransition(globalStart, productionStart, StateTransition.EPSILON));

			convertNode(p.result(), transitions, productionStart, productionEnd);
		}

		return new Automaton(globalStart, transitions);
	}

	private void convertNode(final Node n, final Set<StateTransition> transitions, final State start, final State end) {
		switch (n) {
			case Terminal t -> convertTerminal(t, transitions, start, end);
			case ZeroOrOne zoo -> convertZeroOrOne(zoo, transitions, start, end);
			case ZeroOrMore zom -> convertZeroOrMore(zom, transitions, start, end);
			case OneOrMore oom -> convertOneOrMore(oom, transitions, start, end);
			case Or a -> convertAlternation(a, transitions, start, end);
			case Sequence s -> convertSequence(s, transitions, start, end);
			default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
		}
	}

	private void convertOneOrMore(
			final OneOrMore oom, final Set<StateTransition> transitions, final State start, final State end) {
		final State a = state();
		final State b = state();

		transitions.add(new StateTransition(start, a, StateTransition.EPSILON));
		convertNode(oom.inner(), transitions, a, b);
		transitions.add(new StateTransition(b, a, StateTransition.EPSILON));
		transitions.add(new StateTransition(b, end, StateTransition.EPSILON));
	}

	private void convertZeroOrMore(
			final ZeroOrMore zom, final Set<StateTransition> transitions, final State start, final State end) {
		final State a = state();
		final State b = state();

		transitions.add(new StateTransition(start, end, StateTransition.EPSILON));
		transitions.add(new StateTransition(start, a, StateTransition.EPSILON));
		convertNode(zom.inner(), transitions, a, b);
		transitions.add(new StateTransition(b, a, StateTransition.EPSILON));
		transitions.add(new StateTransition(b, end, StateTransition.EPSILON));
	}

	private void convertSequence(
			final Sequence s, final Set<StateTransition> transitions, final State start, final State end) {
		State prev = state();
		transitions.add(new StateTransition(start, prev, StateTransition.EPSILON));
		for (int i = 0; i < s.nodes().size(); i++) {
			final State tmp = state();
			convertNode(s.nodes().get(i), transitions, prev, tmp);
			prev = tmp;
		}
		transitions.add(new StateTransition(prev, end, StateTransition.EPSILON));
	}

	private void convertZeroOrOne(
			final ZeroOrOne zoo, final Set<StateTransition> transitions, final State start, final State end) {
		final State a = state();
		final State b = state();

		transitions.add(new StateTransition(start, end, StateTransition.EPSILON));
		transitions.add(new StateTransition(start, a, StateTransition.EPSILON));
		convertNode(zoo.inner(), transitions, a, b);
		transitions.add(new StateTransition(b, end, StateTransition.EPSILON));
	}

	private void convertAlternation(
			final Or or, final Set<StateTransition> transitions, final State start, final State end) {
		for (final Node n : or.nodes()) {
			final State newStart = state();
			final State newEnd = state();
			transitions.add(new StateTransition(start, newStart, StateTransition.EPSILON));
			transitions.add(new StateTransition(newEnd, end, StateTransition.EPSILON));
			convertNode(n, transitions, newStart, newEnd);
		}
	}

	private void convertTerminal(
			final Terminal t, final Set<StateTransition> transitions, final State start, final State end) {
		State prev = state();
		transitions.add(new StateTransition(start, prev, StateTransition.EPSILON));
		for (int i = 0; i < t.literal().length(); i++) {
			final State s = state();
			transitions.add(new StateTransition(prev, s, t.literal().charAt(i)));
			prev = s;
		}
		transitions.add(new StateTransition(prev, end, StateTransition.EPSILON));
	}

	private static Set<State> epsilonClosure(final State state, final Set<StateTransition> transitions) {
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

			for (final StateTransition t : transitions) {
				if (t.from().equals(s) && t.character() == StateTransition.EPSILON) {
					q.add(t.to());
				}
			}
		}

		return closure;
	}

	/**
	 * Converts the given epsilon-NFA to an NFA without epsilon transitions.
	 *
	 * @param epsilonNFA The epsilon-NFA to be converted.
	 * @return A new NFA without epsilon transitions.
	 */
	public static Automaton epsilonNFAtoNFA(final Automaton epsilonNFA) {
		final AutomataUtils converter = new AutomataUtils();
		return converter.convertEpsilonNFAToNFA(epsilonNFA);
	}

	private Automaton convertEpsilonNFAToNFA(final Automaton epsilonNFA) {
		final Set<State> oldStates = epsilonNFA.states();
		final Map<State, Set<State>> epsilonClosures = new HashMap<>();

		// cache all epsilon-closures
		for (final State s : oldStates) {
			epsilonClosures.put(s, epsilonClosure(s, epsilonNFA.transitions()));
		}

		final Map<State, State> stateMapping = new HashMap<>();
		for (final State s : oldStates) {
			final Set<State> closure = epsilonClosures.get(s);
			final boolean isAccepting = closure.stream().anyMatch(State::isAccepting);
			stateMapping.put(
					s,
					isAccepting
							? acceptingState(((AcceptingState) closure.stream()
											.filter(State::isAccepting)
											.findFirst()
											.orElseThrow())
									.tokenName())
							: state());
		}

		final Set<StateTransition> newTransitions = new HashSet<>();
		for (final State s : oldStates) {
			final Set<State> closure = epsilonClosures.get(s);

			for (final StateTransition t : epsilonNFA.transitions()) {
				if (t.character() != StateTransition.EPSILON && closure.contains(t.from())) {
					for (final State q : epsilonClosures.get(t.to())) {
						newTransitions.add(
								new StateTransition(stateMapping.get(s), stateMapping.get(q), t.character()));
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
			for (final StateTransition t : newTransitions) {
				if (t.from().equals(s)) {
					q.add(t.to());
				}
			}
		}

		final Set<State> unreachableStates = new HashSet<>();
		for (final State newState : stateMapping.values()) {
			if (!visited.contains(newState)) {
				unreachableStates.add(newState);
			}
		}

		if (!unreachableStates.isEmpty()) {
			newTransitions.removeIf(t -> unreachableStates.contains(t.from()) || unreachableStates.contains(t.to()));
		}

		return new Automaton(newStartingState, newTransitions);
	}

	private static Set<State> move(final Set<State> states, final char symbol, final Set<StateTransition> transitions) {
		final Set<State> result = new HashSet<>();
		for (final State s : states) {
			for (final StateTransition t : transitions) {
				if (t.from().equals(s) && t.character() == symbol) {
					result.add(t.to());
				}
			}
		}
		return result;
	}

	/**
	 * Converts the given NFA into a DFA.
	 *
	 * @param nfa The NFA to be converted.
	 * @return A new DFA.
	 */
	public static Automaton NFAtoDFA(final Automaton nfa) {
		final AutomataUtils converter = new AutomataUtils();
		return converter.convertNFAToDFA(nfa);
	}

	private Automaton convertNFAToDFA(final Automaton nfa) {
		final Map<Set<State>, State> stateMapping = new HashMap<>();
		final Set<StateTransition> newTransitions = new HashSet<>();
		final Queue<Set<State>> queue = new LinkedList<>();

		final Set<Character> alphabet =
				nfa.transitions().stream().map(StateTransition::character).collect(Collectors.toUnmodifiableSet());

		final Set<State> startSet = new HashSet<>();
		startSet.add(nfa.startingState());
		final boolean isAccepting = startSet.stream().anyMatch(State::isAccepting);
		final State dfaStartState = isAccepting
				? acceptingState(((AcceptingState) startSet.stream()
								.filter(State::isAccepting)
								.findFirst()
								.orElseThrow())
						.tokenName())
				: state();
		stateMapping.put(startSet, dfaStartState);
		queue.add(startSet);

		while (!queue.isEmpty()) {
			final Set<State> currentSet = queue.poll();
			final State fromDFAState = stateMapping.get(currentSet);

			for (final char symbol : alphabet) {
				final Set<State> moveSet = move(currentSet, symbol, nfa.transitions());
				if (moveSet.isEmpty()) {
					continue;
				}

				final State toDFAState;
				if (stateMapping.containsKey(moveSet)) {
					toDFAState = stateMapping.get(moveSet);
				} else {
					final boolean accept = moveSet.stream().anyMatch(State::isAccepting);
					toDFAState = accept
							? acceptingState(((AcceptingState) moveSet.stream()
											.filter(State::isAccepting)
											.findFirst()
											.orElseThrow())
									.tokenName())
							: state();
					stateMapping.put(moveSet, toDFAState);
					queue.add(moveSet);
				}

				newTransitions.add(new StateTransition(fromDFAState, toDFAState, symbol));
			}
		}

		return new Automaton(dfaStartState, newTransitions);
	}

	/**
	 * Converts the given DFA into a minimized DFA.
	 *
	 * @param dfa The DFA to be minimized.
	 * @return A new minimized DFA.
	 */
	public static Automaton minimizeDFA(final Automaton dfa) {
		final AutomataUtils converter = new AutomataUtils();
		return converter.convertDFAToMinimizedDFA(dfa);
	}

	private Automaton convertDFAToMinimizedDFA(final Automaton dfa) {
		// Myhill-Nerode theorem
		final List<State> oldStates = dfa.states().stream().toList();
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
				final Map<Character, State> px = dfa.transitions().stream()
						.filter(t -> t.from().equals(p))
						.collect(Collectors.toMap(StateTransition::character, StateTransition::to));

				for (int j = i + 1; j < n; j++) {
					final State q = oldStates.get(j);
					final Map<Character, State> qx = dfa.transitions().stream()
							.filter(t -> t.from().equals(q))
							.collect(Collectors.toMap(StateTransition::character, StateTransition::to));
					if (isDistinguishable[i][j]) {
						continue;
					}

					if (!px.keySet().equals(qx.keySet())) {
						isDistinguishable[i][j] = true;
						isDistinguishable[j][i] = true;
						atLeastOnePairDistinguished = true;
						continue;
					}
					for (final char x : px.keySet()) {
						final int pxx = oldStates.indexOf(px.get(x));
						final int qxx = oldStates.indexOf(qx.get(x));
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
					? acceptingState(((AcceptingState) eqClass.stream()
									.filter(State::isAccepting)
									.findFirst()
									.orElseThrow())
							.tokenName())
					: state();
			for (final State s : eqClass) {
				oldToNew.put(s, rep);
			}
		}

		// Add transitions
		final Set<StateTransition> newTransitions = new HashSet<>();
		for (final StateTransition t : dfa.transitions()) {
			final State from = oldToNew.get(t.from());
			final State to = oldToNew.get(t.to());
			newTransitions.add(new StateTransition(from, to, t.character()));
		}

		final State newStart = oldToNew.get(dfa.startingState());

		return new Automaton(newStart, newTransitions);
	}

	/**
	 * Checks whether the given automaton is a valid epsilon-NFA.
	 *
	 * @param automaton The epsilon-NFA to check.
	 */
	public static void assertEpsilonNFAValid(final Automaton automaton) {
		final Set<State> allStates = Stream.concat(
						Stream.of(automaton.startingState()),
						automaton.transitions().stream().flatMap(t -> Stream.of(t.from(), t.to())))
				.collect(Collectors.toUnmodifiableSet());

		// At least one final state
		if (allStates.stream().noneMatch(State::isAccepting)) {
			throw new IllegalArgumentException("No final state in the given automaton.");
		}

		// Only one strongly connected component
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

		final Set<State> outgoing =
				automaton.transitions().stream().map(StateTransition::from).collect(Collectors.toUnmodifiableSet());
		for (final State s : allStates) {
			if (!outgoing.contains(s) && !s.isAccepting()) {
				// A dead-end node is a non-accepting node with no outgoing edges
				throw new IllegalArgumentException("The automaton contains dead-end nodes.");
			}
		}
	}

	/**
	 * Checks whether the given automaton is a valid NFA without epsilon transitions.
	 *
	 * @param automaton The NFA to check.
	 */
	public static void assertNFAValid(final Automaton automaton) {
		assertEpsilonNFAValid(automaton);

		if (automaton.transitions().stream().anyMatch(t -> t.character() == StateTransition.EPSILON)) {
			throw new IllegalArgumentException("Epsilon transitions found in an NFA.");
		}
	}

	/**
	 * Checks whether the given automaton is a valid DFA.
	 *
	 * @param automaton The DFA to check.
	 */
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
