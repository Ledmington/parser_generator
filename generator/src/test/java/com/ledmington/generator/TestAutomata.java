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
	private static String printAutomaton(final Automaton automaton) {
		final Set<State> allStates = Stream.concat(
						Stream.of(automaton.startingState()),
						automaton.transitions().stream().flatMap(t -> Stream.of(t.from(), t.to())))
				.collect(Collectors.toUnmodifiableSet());
		final StringBuilder sb = new StringBuilder();

		sb.append("digraph NFA {\n");
		sb.append("    rankdir=LR;\n");
		sb.append("    size=\"8,5\"\n");
		sb.append("    node [shape = doublecircle];\n");

		for (final State s : allStates) {
			if (s.isAccepting()) {
				sb.append("    ").append(s.name()).append(";\n");
			}
		}

		sb.append("    node [shape = circle];\n");
		sb.append("    __start__ [shape = point];\n");
		sb.append("    __start__ -> ").append(automaton.startingState().name()).append(";\n");

		for (final StateTransition t : automaton.transitions()) {
			sb.append("    ")
					.append(t.from().name())
					.append(" -> ")
					.append(t.to().name())
					.append(" [label=\"")
					.append(t.character() == StateTransition.EPSILON ? "ε" : t.character())
					.append("\"];\n");
		}

		sb.append("}\n");
		return sb.toString();
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
		final Automaton epsilonNFA = AutomataUtils.grammarToEpsilonNFA(g);
		assertDoesNotThrow(
				() -> AutomataUtils.assertEpsilonNFAValid(epsilonNFA),
				() -> String.format(
						"Expected this automaton to be valid but it wasn't:\n%s\n", printAutomaton(epsilonNFA)));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidNFA(final Grammar g) {
		final Automaton nfa = AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g));
		assertDoesNotThrow(
				() -> AutomataUtils.assertNFAValid(nfa),
				() -> String.format("Expected this automaton to be valid but it wasn't:\n%s\n", printAutomaton(nfa)));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidDFA(final Grammar g) {
		final Automaton dfa =
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)));
		assertDoesNotThrow(
				() -> AutomataUtils.assertDFAValid(dfa),
				() -> String.format("Expected this automaton to be valid but it wasn't:\n%s\n", printAutomaton(dfa)));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidMinimizedDFA(final Grammar g) {
		final Automaton minimizedDFA = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))));
		assertDoesNotThrow(
				() -> AutomataUtils.assertDFAValid(minimizedDFA),
				() -> String.format(
						"Expected this automaton to be valid but it wasn't:\n%s\n", printAutomaton(minimizedDFA)));
	}

	/*@ParameterizedTest
	@MethodSource("correctCases")
	void checkDFAMatches(final Grammar g, final List<String> correctInputs) {
		final Automaton dfa =
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)));
		for (final String input : correctInputs) {
			assertTrue(
					dfa.matches(input),
					() -> String.format(
							"Expected this automaton to match the input '%s' but it didn't.\n%s\n",
							input, printAutomaton(dfa)));
		}
	}

	@ParameterizedTest
	@MethodSource("correctCases")
	void checkMinimizedDFAMatches(final Grammar g, final List<String> correctInputs) {
		final Automaton minimizedDFA = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))));
		for (final String input : correctInputs) {
			assertTrue(
					minimizedDFA.matches(input),
					() -> String.format(
							"Expected this automaton to match the input '%s' but it didn't.\n%s\n",
							input, printAutomaton(minimizedDFA)));
		}
	}

	@ParameterizedTest
	@MethodSource("wrongCases")
	void checkDFADoesntMatch(final Grammar g, final List<String> wrongInputs) {
		final Automaton dfa =
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)));
		for (final String input : wrongInputs) {
			assertFalse(
					dfa.matches(input),
					() -> String.format(
							"Expected this automaton to NOT match the input '%s' but it did.\n%s\n",
							input, printAutomaton(dfa)));
		}
	}

	@ParameterizedTest
	@MethodSource("wrongCases")
	void checkMinimizedDFADoesntMatch(final Grammar g, final List<String> wrongInputs) {
		final Automaton minimizedDFA = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))));
		for (final String input : wrongInputs) {
			assertFalse(
					minimizedDFA.matches(input),
					() -> String.format(
							"Expected this automaton to NOT match the input '%s' but it did.\n%s\n",
							input, printAutomaton(minimizedDFA)));
		}
	}*/
}
