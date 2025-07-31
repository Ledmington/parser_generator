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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Utils;
import com.ledmington.generator.automata.AutomataUtils;
import com.ledmington.generator.automata.Automaton;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class TestAutomata {

	private TestAutomata() {}

	public static Stream<Arguments> onlyGrammars() {
		return TEST_CASES.stream().map(tc -> Arguments.of(tc.get()[0]));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidEpsilonNFA(final Grammar g) {
		final Automaton epsilonNFA = AutomataUtils.grammarToEpsilonNFA(g);
		assertDoesNotThrow(
				() -> AutomataUtils.assertEpsilonNFAValid(epsilonNFA),
				() -> String.format(
						"Expected this automaton to be valid but it wasn't:\n%s\n", epsilonNFA.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidNFA(final Grammar g) {
		final Automaton nfa = AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g));
		assertDoesNotThrow(
				() -> AutomataUtils.assertNFAValid(nfa),
				() -> String.format("Expected this automaton to be valid but it wasn't:\n%s\n", nfa.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidDFA(final Grammar g) {
		final Automaton dfa =
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)));
		assertDoesNotThrow(
				() -> AutomataUtils.assertDFAValid(dfa),
				() -> String.format("Expected this automaton to be valid but it wasn't:\n%s\n", dfa.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidMinimizedDFA(final Grammar g) {
		final Automaton minimizedDFA = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))));
		assertDoesNotThrow(
				() -> AutomataUtils.assertDFAValid(minimizedDFA),
				() -> String.format(
						"Expected this automaton to be valid but it wasn't:\n%s\n", minimizedDFA.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void epsilonNFADeterministicGeneration(final Grammar g) {
		final Automaton epsilonNFA1 = AutomataUtils.grammarToEpsilonNFA(g);
		final Automaton epsilonNFA2 = AutomataUtils.grammarToEpsilonNFA(g);
		assertEquals(
				epsilonNFA1,
				epsilonNFA2,
				() -> String.format(
						"Expected epsilon-NFA generation to be deterministic, but the following grammar generated two different automata.%n%s%n%s%n%s%n",
						Utils.prettyPrint(g, "  "), epsilonNFA1.toGraphviz(), epsilonNFA2.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void nfaDeterministicGeneration(final Grammar g) {
		final Automaton nfa1 = AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g));
		final Automaton nfa2 = AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g));
		assertEquals(
				nfa1,
				nfa2,
				() -> String.format(
						"Expected NFA generation to be deterministic, but the following grammar generated two different automata.%n%s%n%s%n%s%n",
						Utils.prettyPrint(g, "  "), nfa1.toGraphviz(), nfa2.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void dfaDeterministicGeneration(final Grammar g) {
		final Automaton dfa1 =
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)));
		final Automaton dfa2 =
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)));
		assertEquals(
				dfa1,
				dfa2,
				() -> String.format(
						"Expected DFA generation to be deterministic, but the following grammar generated two different automata.%n%s%n%s%n%s%n",
						Utils.prettyPrint(g, "  "), dfa1.toGraphviz(), dfa2.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void minimizedDFADeterministicGeneration(final Grammar g) {
		final Automaton minimizedDFA1 = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))));
		final Automaton minimizedDFA2 = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g))));
		assertEquals(
				minimizedDFA1,
				minimizedDFA2,
				() -> String.format(
						"Expected min-DFA generation to be deterministic, but the following grammar generated two different automata.%n%s%n%s%n%s%n",
						Utils.prettyPrint(g, "  "), minimizedDFA1.toGraphviz(), minimizedDFA2.toGraphviz()));
	}
}
