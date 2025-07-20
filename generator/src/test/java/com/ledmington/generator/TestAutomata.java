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
package com.ledmington.generator;

import static com.ledmington.generator.CorrectGrammars.TEST_CASES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Grammar;
import com.ledmington.generator.automata.AutomataUtils;
import com.ledmington.generator.automata.Automaton;
import com.ledmington.generator.automata.State;
import com.ledmington.generator.automata.StateTransition;

public final class TestAutomata {

	private TestAutomata() {}

	// just for debugging: outputs graphviz code to be used in tools such as https://graph.flyte.org
	private static void printAutomaton(final Automaton automaton) {
		final Set<State> allStates = Stream.concat(
						Stream.of(automaton.startingState()),
						automaton.transitions().stream().flatMap(t -> Stream.of(t.from(), t.to())))
				.collect(Collectors.toUnmodifiableSet());

		System.out.println("digraph NFA {");
		System.out.println("    rankdir=LR;");
		System.out.println("    size=\"8,5\"");
		System.out.println("    node [shape = doublecircle];");

		for (final State s : allStates) {
			if (s.isAccepting()) {
				System.out.printf("    %s;\n", s.name());
			}
		}

		System.out.println("    node [shape = circle];");
		System.out.println("    __start__ [shape = point];");
		System.out.printf("    __start__ -> %s;\n", automaton.startingState().name());

		for (final StateTransition t : automaton.transitions()) {
			System.out.printf(
					"    %s -> %s [label=\"%s\"];\n",
					t.from().name(), t.to().name(), t.character() == StateTransition.EPSILON ? "ε" : t.character());
		}

		System.out.println("}");
	}

	public static Stream<Arguments> onlyGrammars() {
		return TEST_CASES.stream().map(tc -> Arguments.of(tc.get()[0]));
	}

	public static Stream<Arguments> correctCases() {
		return TEST_CASES.stream().map(tc -> Arguments.of(tc.get()[0], tc.get()[1]));
	}

	public static Stream<Arguments> wrongCases() {
		return TEST_CASES.stream().map(tc -> Arguments.of(tc.get()[0], tc.get()[2]));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidEpsilonNFA(final Grammar g) {
		assertDoesNotThrow(() -> AutomataUtils.assertEpsilonNFAValid(AutomataUtils.grammarToEpsilonNFA(g)));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidNFA(final Grammar g) {
		assertDoesNotThrow(() ->
				AutomataUtils.assertNFAValid(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidDFA(final Grammar g) {
		assertDoesNotThrow(() -> AutomataUtils.assertDFAValid(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)))));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidMinimizedDFA(final Grammar g) {
		assertDoesNotThrow(() -> AutomataUtils.assertDFAValid(AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))))));
	}

	@ParameterizedTest
	@MethodSource("correctCases")
	void checkDFAMatches(final Grammar g, final List<String> correctInputs) {
		final Automaton dfa =
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)));
		for (final String input : correctInputs) {
			assertTrue(dfa.matches(input));
		}
	}

	@ParameterizedTest
	@MethodSource("correctCases")
	void checkMinimizedDFAMatches(final Grammar g, final List<String> correctInputs) {
		final Automaton dfa = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))));
		for (final String input : correctInputs) {
			assertTrue(dfa.matches(input));
		}
	}

	@ParameterizedTest
	@MethodSource("wrongCases")
	void checkDFADoesntMatch(final Grammar g, final List<String> wrongInputs) {
		final Automaton dfa =
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)));
		for (final String input : wrongInputs) {
			assertFalse(dfa.matches(input));
		}
	}

	@ParameterizedTest
	@MethodSource("wrongCases")
	void checkMinimizedDFADoesntMatch(final Grammar g, final List<String> wrongInputs) {
		final Automaton dfa = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))));
		for (final String input : wrongInputs) {
			assertFalse(dfa.matches(input));
		}
	}
}
