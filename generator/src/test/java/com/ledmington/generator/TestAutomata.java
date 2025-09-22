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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.OneOrMore;
import com.ledmington.ebnf.Or;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.Utils;
import com.ledmington.generator.automata.AcceptingState;
import com.ledmington.generator.automata.AutomataUtils;
import com.ledmington.generator.automata.DFA;
import com.ledmington.generator.automata.EpsilonNFAToNFA;
import com.ledmington.generator.automata.GrammarToEpsilonNFA;
import com.ledmington.generator.automata.NFA;
import com.ledmington.generator.automata.State;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class TestAutomata {

	private TestAutomata() {}

	public static Stream<Arguments> onlyGrammars() {
		return TEST_CASES.stream().map(tc -> Arguments.of(tc.get()[0]));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidEpsilonNFA(final Grammar g) {
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final NFA epsilonNFA = grammar2ENFA.convert(g);
		assertDoesNotThrow(
				() -> AutomataUtils.assertEpsilonNFAValid(epsilonNFA),
				() -> String.format(
						"Expected this automaton to be valid but it wasn't:\n%s\n", epsilonNFA.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidNFA(final Grammar g) {
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final EpsilonNFAToNFA ENFA2NFA = new EpsilonNFAToNFA();
		final NFA nfa = ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(g));
		assertDoesNotThrow(
				() -> AutomataUtils.assertNFAValid(nfa),
				() -> String.format("Expected this automaton to be valid but it wasn't:\n%s\n", nfa.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidDFA(final Grammar g) {
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final EpsilonNFAToNFA ENFA2NFA = new EpsilonNFAToNFA();
		final DFA dfa = AutomataUtils.NFAtoDFA(ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(g)));
		assertDoesNotThrow(
				() -> AutomataUtils.assertDFAValid(dfa),
				() -> String.format("Expected this automaton to be valid but it wasn't:\n%s\n", dfa.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void checkValidMinimizedDFA(final Grammar g) {
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final EpsilonNFAToNFA ENFA2NFA = new EpsilonNFAToNFA();
		final DFA minimizedDFA = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(g))));
		assertDoesNotThrow(
				() -> AutomataUtils.assertDFAValid(minimizedDFA),
				() -> String.format(
						"Expected this automaton to be valid but it wasn't:\n%s\n", minimizedDFA.toGraphviz()));
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void epsilonNFADeterministicGeneration(final Grammar g) {
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final NFA epsilonNFA1 = grammar2ENFA.convert(g);
		final NFA epsilonNFA2 = grammar2ENFA.convert(g);
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
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final EpsilonNFAToNFA ENFA2NFA = new EpsilonNFAToNFA();
		final NFA nfa1 = ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(g));
		final NFA nfa2 = ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(g));
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
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final EpsilonNFAToNFA ENFA2NFA = new EpsilonNFAToNFA();
		final DFA dfa1 = AutomataUtils.NFAtoDFA(ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(g)));
		final DFA dfa2 = AutomataUtils.NFAtoDFA(ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(g)));
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
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final EpsilonNFAToNFA ENFA2NFA = new EpsilonNFAToNFA();
		final DFA minimizedDFA1 = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(g))));
		final DFA minimizedDFA2 = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(g))));
		assertEquals(
				minimizedDFA1,
				minimizedDFA2,
				() -> String.format(
						"Expected min-DFA generation to be deterministic, but the following grammar generated two different automata.%n%s%n%s%n%s%n",
						Utils.prettyPrint(g, "  "), minimizedDFA1.toGraphviz(), minimizedDFA2.toGraphviz()));
	}

	private record Match(String name, String content) {}

	List<Match> tryMatch(final DFA dfa, final String input) {
		final char[] v = input.toCharArray();
		int pos = 0;
		State currentState = dfa.startingState();
		final List<Match> matches = new ArrayList<>();
		int lastMatchStart = 0;
		int lastMatchEnd = 0;
		while (pos < v.length) {
			if (currentState.isAccepting()) {
				lastMatchEnd = pos;
			}
			final char ch = v[pos];
			if (dfa.neighbors(currentState).containsKey(ch)) {
				currentState = dfa.neighbors(currentState).get(ch);
				pos++;
			} else {
				// no more transitions, emit last match if present
				if (!currentState.isAccepting()) {
					throw new IllegalArgumentException("No match.");
				} else {
					final int length = lastMatchEnd - lastMatchStart;
					if (length == 0) {
						throw new IllegalArgumentException("Empty match.");
					}
					final AcceptingState as = (AcceptingState) currentState;
					matches.add(new Match(as.tokenName(), String.copyValueOf(v, lastMatchStart, length)));
					lastMatchStart = pos;
					lastMatchEnd = -1;
					currentState = dfa.startingState();
				}
			}
		}
		if (currentState.isAccepting()) {
			lastMatchEnd = pos;
		}
		final int length = lastMatchEnd - lastMatchStart;
		if (currentState.isAccepting() && length > 0) {
			final AcceptingState as = (AcceptingState) currentState;
			matches.add(new Match(as.tokenName(), String.copyValueOf(v, lastMatchStart, length)));
		}
		return matches;
	}

	@Test
	void checkDFAMatchesSubTokens() {
		final List<Production> productions = List.of(
				new Production(new NonTerminal("AN"), new Terminal("an")),
				new Production(
						new NonTerminal("ID"),
						new OneOrMore(new Or(new Terminal("a"), new Terminal("b"), new Terminal("n")))));
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final EpsilonNFAToNFA ENFA2NFA = new EpsilonNFAToNFA();
		final DFA dfa = AutomataUtils.NFAtoDFA(ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(productions)));
		final List<Match> tokens = tryMatch(dfa, "banana");
		assertEquals(List.of(new Match("ID", "banana")), tokens);
	}

	@Test
	void checkMinimizedDFAMatchesSubTokens() {
		final List<Production> productions = List.of(
				new Production(new NonTerminal("AN"), new Terminal("an")),
				new Production(
						new NonTerminal("ID"),
						new OneOrMore(new Or(new Terminal("a"), new Terminal("b"), new Terminal("n")))));
		final GrammarToEpsilonNFA grammar2ENFA = new GrammarToEpsilonNFA();
		final EpsilonNFAToNFA ENFA2NFA = new EpsilonNFAToNFA();
		final DFA dfa = AutomataUtils.minimizeDFA(
				AutomataUtils.NFAtoDFA(ENFA2NFA.convertEpsilonNFAToNFA(grammar2ENFA.convert(productions))));
		final List<Match> tokens = tryMatch(dfa, "banana");
		assertEquals(List.of(new Match("ID", "banana")), tokens);
	}
}
