/*
 * parser-gen - Parser Generator
 * Copyright (C) 2025-2026 Filippo Barbari <filippo.barbari@gmail.com>
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

import static com.ledmington.generator.CorrectGrammars.g;
import static com.ledmington.generator.CorrectGrammars.nt;
import static com.ledmington.generator.CorrectGrammars.one_or_more;
import static com.ledmington.generator.CorrectGrammars.or;
import static com.ledmington.generator.CorrectGrammars.p;
import static com.ledmington.generator.CorrectGrammars.seq;
import static com.ledmington.generator.CorrectGrammars.t;
import static com.ledmington.generator.CorrectGrammars.zero_or_more;
import static com.ledmington.generator.CorrectGrammars.zero_or_one;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.Utils;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class TestFirstFollowSets {

	private record TestCase(
			Grammar input, Map<NonTerminal, Set<Terminal>> firstSets, Map<NonTerminal, Set<Terminal>> followSets) {}

	private static final List<TestCase> CORRECT_GRAMMARS = List.of(
			new TestCase(
					g(p("start", t("a"))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(t("terminal_0")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(Terminal.END_OF_INPUT)))),
			new TestCase(
					g(p("start", seq(t("a"), t("b")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(t("terminal_0")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(Terminal.END_OF_INPUT)))),
			new TestCase(
					g(p("start", or(t("a"), t("b")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(t("terminal_0"), t("terminal_1")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(Terminal.END_OF_INPUT)))),
			new TestCase(
					g(p("start", zero_or_one(t("a")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(t("terminal_0"), Terminal.EPSILON))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(Terminal.END_OF_INPUT)))),
			new TestCase(
					g(p("start", one_or_more(t("a")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(t("terminal_0")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(Terminal.END_OF_INPUT)))),
			new TestCase(
					g(p("start", zero_or_more(t("a")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(t("terminal_0"), Terminal.EPSILON))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(Terminal.END_OF_INPUT)))),
			new TestCase(
					g(p("start", seq(nt("A"), nt("B"))), p("A", t("a")), p("B", t("b"))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(t("A")))),
					Map.ofEntries(Map.entry(nt("start"), Set.of(Terminal.END_OF_INPUT)))),
			new TestCase(
					g(p("start", seq(t("A"), or(t("B"), t("C")), t("D")))),
					Map.ofEntries(
							Map.entry(nt("start"), Set.of(t("terminal_0"))),
							Map.entry(nt("or_0"), Set.of(t("terminal_2"), t("terminal_1")))),
					Map.ofEntries(
							Map.entry(nt("start"), Set.of(Terminal.END_OF_INPUT)),
							Map.entry(nt("or_0"), Set.of(t("terminal_3"))))),
			new TestCase(
					g(p("start", seq(zero_or_one(t("a")), zero_or_one(t("b")), zero_or_one(t("c"))))),
					Map.ofEntries(
							Map.entry(
									nt("start"),
									Set.of(t("terminal_0"), t("terminal_1"), t("terminal_2"), Terminal.EPSILON)),
							Map.entry(nt("zero_or_one_0"), Set.of(t("terminal_0"), Terminal.EPSILON)),
							Map.entry(nt("zero_or_one_1"), Set.of(t("terminal_1"), Terminal.EPSILON)),
							Map.entry(nt("zero_or_one_2"), Set.of(t("terminal_2"), Terminal.EPSILON))),
					Map.ofEntries(
							Map.entry(nt("start"), Set.of(Terminal.END_OF_INPUT)),
							Map.entry(
									nt("zero_or_one_0"),
									Set.of(Terminal.END_OF_INPUT, t("terminal_1"), t("terminal_2"))),
							Map.entry(nt("zero_or_one_1"), Set.of(Terminal.END_OF_INPUT, t("terminal_2"))),
							Map.entry(nt("zero_or_one_2"), Set.of(Terminal.END_OF_INPUT)))));

	private static String printSets(final Map<NonTerminal, Set<Terminal>> sets) {
		final StringBuilder sb = new StringBuilder();
		sets.entrySet().stream()
				.sorted(Comparator.comparing(e -> e.getKey().name()))
				.forEach(e -> sb.append(e.getKey())
						.append(" -> ")
						.append(e.getValue().stream()
								// synthetic terminals first
								.sorted(Comparator.comparing(t -> !((Terminal) t).isSynthetic())
										.thenComparing(t -> ((Terminal) t).literal()))
								.toList())
						.append('\n'));
		return sb.toString();
	}

	private static Stream<Arguments> justFirstSets() {
		return CORRECT_GRAMMARS.stream().map(tc -> Arguments.of(tc.input(), tc.firstSets()));
	}

	@ParameterizedTest
	@MethodSource("justFirstSets")
	void checkFirstSets(final Grammar input, final Map<NonTerminal, Set<Terminal>> expected) {
		final Map<NonTerminal, Set<Terminal>> actual = GrammarUtils.computeFirstSets(input);
		assertDoesNotThrow(() -> GrammarUtils.checkFirstSets(actual));
		assertEquals(
				expected,
				actual,
				() -> String.format(
						" --- Grammar --- \n%s\n --- Expected FIRST set --- \n%s\n --- Actual FIRST set --- \n%s\n",
						Utils.prettyPrint(input), printSets(expected), printSets(actual)));
	}

	private static Stream<Arguments> justFollowSets() {
		return CORRECT_GRAMMARS.stream()
				.map(tc -> Arguments.of(tc.input(), tc.followSets(), tc.input().getStartSymbol()));
	}

	@ParameterizedTest
	@MethodSource("justFollowSets")
	void checkFollowSets(final Grammar input, final Map<NonTerminal, Set<Terminal>> expected) {
		final Map<NonTerminal, Set<Terminal>> firstSets = GrammarUtils.computeFirstSets(input);
		final Map<NonTerminal, Set<Terminal>> actual = GrammarUtils.computeFollowSets(input, firstSets);
		assertDoesNotThrow(() -> GrammarUtils.checkFollowSets(input, actual));
		assertEquals(
				expected,
				actual,
				() -> String.format(
						" --- Grammar --- \n%s\n --- Expected FOLLOW set --- \n%s\n --- Actual FOLLOW set --- \n%s\n",
						Utils.prettyPrint(input), printSets(expected), printSets(actual)));
	}
}
