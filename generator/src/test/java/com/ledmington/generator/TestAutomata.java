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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Grammar;
import com.ledmington.generator.automata.AutomataUtils;
import com.ledmington.generator.automata.Automaton;

public final class TestAutomata {

	private TestAutomata() {}

	public static Stream<Arguments> onlyGrammars() {
		return TEST_CASES.stream().map(tc -> Arguments.of(tc.get()[0]));
	}

	@SuppressWarnings("unchecked")
	public static Stream<Arguments> correctCases() {
		// since the produced automata match an arbitrarily long sequence of the given tokens, we check if they match
		// the same sequence multiple times
		return TEST_CASES.stream()
				.map(tc -> Arguments.of(
						tc.get()[0],
						((List<String>) tc.get()[1])
								.stream()
										.flatMap(s -> Stream.of(s, s.repeat(2), s.repeat(3)))
										.distinct()
										.toList()));
	}

	@SuppressWarnings("unchecked")
	public static Stream<Arguments> wrongCases() {
		// since the produced automata match an arbitrarily long sequence of the given tokens, we make sure to remove
		// from the wrong inputs the ones obtainable by just repeating one of the correct input sequences
		return TEST_CASES.stream().map(tc -> {
			final List<String> correct = (List<String>) tc.get()[1];
			final List<String> wrong = (List<String>) tc.get()[2];
			final List<String> out = new ArrayList<>();
			for (final String s : wrong) {
				boolean valid = true;
				for (final String c : correct) {
					if (c.length() > s.length() || c.isEmpty()) {
						continue;
					}
					for (int i = 1; i < 10; i++) {
						if (c.repeat(i).equals(s)) {
							valid = false;
							break;
						}
					}
					if (!valid) {
						break;
					}
				}
				if (valid) {
					out.add(s);
				}
			}
			return Arguments.of(tc.get()[0], out);
		});
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
	@MethodSource("correctCases")
	void checkDFAMatches(final Grammar g, final List<String> correctInputs) {
		final Automaton dfa =
				AutomataUtils.NFAtoDFA(AutomataUtils.epsilonNFAtoNFA(AutomataUtils.grammarToEpsilonNFA(g)));
		for (final String input : correctInputs) {
			assertTrue(
					dfa.matches(input),
					() -> String.format(
							"Expected this automaton to match the input '%s' but it didn't.\n%s\n",
							input, dfa.toGraphviz()));
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
							input, minimizedDFA.toGraphviz()));
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
							input, dfa.toGraphviz()));
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
							input, minimizedDFA.toGraphviz()));
		}
	}
}
