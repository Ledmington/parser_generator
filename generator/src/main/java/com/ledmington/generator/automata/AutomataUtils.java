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
	public static NFA grammarToEpsilonNFA(final Grammar g) {
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
	public static NFA grammarToEpsilonNFA(final List<Production> lexerProductions) {
		final AutomataUtils converter = new AutomataUtils();
		return converter.convertGrammarToEpsilonNFA(lexerProductions);
	}

	private NFA convertGrammarToEpsilonNFA(final List<Production> lexerProductions) {
		final NFABuilder builder = NFA.builder();

		final State globalStart = state();

		for (final Production p : lexerProductions) {
			final String productionName = p.start().name();
			final State productionStart = state();
			final State productionEnd = acceptingState(productionName);

			builder.addTransition(globalStart, StateTransition.EPSILON, productionStart);

			convertNode(p.result(), builder, productionStart, productionEnd);
		}

		return builder.start(globalStart).build();
	}

	private void convertNode(final Node n, final NFABuilder builder, final State start, final State end) {
		switch (n) {
			case Terminal t -> convertTerminal(t, builder, start, end);
			case ZeroOrOne zoo -> convertZeroOrOne(zoo, builder, start, end);
			case ZeroOrMore zom -> convertZeroOrMore(zom, builder, start, end);
			case OneOrMore oom -> convertOneOrMore(oom, builder, start, end);
			case Or a -> convertAlternation(a, builder, start, end);
			case Sequence s -> convertSequence(s, builder, start, end);
			default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
		}
	}

	private void convertOneOrMore(final OneOrMore oom, final NFABuilder builder, final State start, final State end) {
		final State a = state();
		final State b = state();

		builder.addTransition(start, StateTransition.EPSILON, a);
		convertNode(oom.inner(), builder, a, b);
		builder.addTransition(b, StateTransition.EPSILON, a);
		builder.addTransition(b, StateTransition.EPSILON, end);
	}

	private void convertZeroOrMore(final ZeroOrMore zom, final NFABuilder builder, final State start, final State end) {
		final State a = state();
		final State b = state();

		builder.addTransition(start, StateTransition.EPSILON, end);
		builder.addTransition(start, StateTransition.EPSILON, a);
		convertNode(zom.inner(), builder, a, b);
		builder.addTransition(b, StateTransition.EPSILON, a);
		builder.addTransition(b, StateTransition.EPSILON, end);
	}

	private void convertSequence(final Sequence s, final NFABuilder builder, final State start, final State end) {
		State prev = state();
		builder.addTransition(start, StateTransition.EPSILON, prev);
		for (int i = 0; i < s.nodes().size(); i++) {
			final State tmp = state();
			convertNode(s.nodes().get(i), builder, prev, tmp);
			prev = tmp;
		}
		builder.addTransition(prev, StateTransition.EPSILON, end);
	}

	private void convertZeroOrOne(final ZeroOrOne zoo, final NFABuilder builder, final State start, final State end) {
		final State a = state();
		final State b = state();

		builder.addTransition(start, StateTransition.EPSILON, end);
		builder.addTransition(start, StateTransition.EPSILON, a);
		convertNode(zoo.inner(), builder, a, b);
		builder.addTransition(b, StateTransition.EPSILON, end);
	}

	private void convertAlternation(final Or or, final NFABuilder builder, final State start, final State end) {
		for (final Node n : or.nodes()) {
			final State newStart = state();
			final State newEnd = state();
			builder.addTransition(start, StateTransition.EPSILON, newStart);
			builder.addTransition(newEnd, StateTransition.EPSILON, end);
			convertNode(n, builder, newStart, newEnd);
		}
	}

	private void convertTerminal(final Terminal t, final NFABuilder builder, final State start, final State end) {
		State prev = state();
		builder.addTransition(start, StateTransition.EPSILON, prev);
		for (int i = 0; i < t.literal().length(); i++) {
			final State s = state();
			builder.addTransition(prev, t.literal().charAt(i), s);
			prev = s;
		}
		builder.addTransition(prev, StateTransition.EPSILON, end);
	}

	private static Set<State> epsilonClosure(final State state, final NFA nfa) {
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
			if (neighbors.containsKey(StateTransition.EPSILON)) {
				q.addAll(neighbors.get(StateTransition.EPSILON));
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
	public static NFA epsilonNFAtoNFA(final NFA epsilonNFA) {
		final AutomataUtils converter = new AutomataUtils();
		return converter.convertEpsilonNFAToNFA(epsilonNFA);
	}

	private NFA convertEpsilonNFAToNFA(final NFA epsilonNFA) {
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
							? acceptingState(((AcceptingState) closure.stream()
											.filter(State::isAccepting)
											.findFirst()
											.orElseThrow())
									.tokenName())
							: state());
		}

		final NFABuilder builder = NFA.builder();
		for (final State s : oldStates) {
			final Set<State> closure = epsilonClosures.get(s);

			for (final State cs : closure) {
				final Map<Character, Set<State>> neighbors = epsilonNFA.neighbors(cs);
				for (final Map.Entry<Character, Set<State>> e : neighbors.entrySet()) {
					final char symbol = e.getKey();
					if (symbol != StateTransition.EPSILON) {
						for (final State qq : e.getValue()) {
							for (final State q : epsilonClosures.get(qq)) {
								builder.addTransition(stateMapping.get(s), symbol, stateMapping.get(q));
							}
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

			for (final Map.Entry<Character, Set<State>> e : builder.neighbors(s).entrySet()) {
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
			builder.removeAll(unreachableStates);
		}

		return builder.build();
	}

	private static Set<State> move(final Set<State> states, final char symbol, final NFA nfa) {
		final Set<State> result = new HashSet<>();
		for (final State s : states) {
			if (nfa.neighbors(s).containsKey(symbol)) {
				result.addAll(nfa.neighbors(s).get(symbol));
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
	public static DFA NFAtoDFA(final NFA nfa) {
		final AutomataUtils converter = new AutomataUtils();
		return converter.convertNFAToDFA(nfa);
	}

	private DFA convertNFAToDFA(final NFA nfa) {
		final Map<Set<State>, State> stateMapping = new HashMap<>();
		final Set<StateTransition> newTransitions = new HashSet<>();
		final Queue<Set<State>> queue = new LinkedList<>();
		final DFABuilder builder=DFA.builder();

		final Set<Character> alphabet = nfa.states().stream()
				.flatMap(s -> nfa.neighbors(s).keySet().stream())
				.collect(Collectors.toUnmodifiableSet());

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
				final Set<State> moveSet = move(currentSet, symbol, nfa);
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

				builder.addTransition(fromDFAState, symbol,toDFAState);
			}
		}

		return builder.build();
	}

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

		return new FiniteStateAutomaton(newStart, newTransitions);
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

			for (final Map.Entry<Character, Set<State>> e : nfa.neighbors(s).entrySet()) {
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

		if (finiteStateAutomaton.states().stream()
				.anyMatch(s -> finiteStateAutomaton.neighbors(s).containsKey(StateTransition.EPSILON))) {
			throw new IllegalArgumentException("Epsilon transitions found in an NFA.");
		}
	}

	/**
	 * Checks whether the given automaton is a valid DFA.
	 *
	 * @param finiteStateAutomaton The DFA to check.
	 */
	public static void assertDFAValid(final DFA finiteStateAutomaton) {
		assertNFAValid(finiteStateAutomaton);

		final Set<State> allStates = Stream.concat(
						Stream.of(finiteStateAutomaton.startingState()),
						finiteStateAutomaton.transitions().stream().flatMap(t -> Stream.of(t.from(), t.to())))
				.collect(Collectors.toUnmodifiableSet());

		for (final State s : allStates) {
			final Set<Character> transitions = new HashSet<>();
			for (final StateTransition t : finiteStateAutomaton.transitions()) {
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
