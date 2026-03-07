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
import static com.ledmington.generator.CorrectGrammars.p;
import static com.ledmington.generator.CorrectGrammars.t;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.Utils;

public final class TestFirstFollowSets {

	private record TestCase(
			Grammar input, Map<NonTerminal, Set<Terminal>> firstSets, Map<NonTerminal, Set<Terminal>> followSets) {}

	private static final List<TestCase> CORRECT_GRAMMARS = List.of(new TestCase(
			g(p("start", t("a"))),
			Map.of(nt("start"), Set.of(t("terminal_0"))),
			Map.of(
					nt("start"),
					Set.of(GrammarUtils.END_OF_INPUT_TERMINAL),
					nt("terminal_0"),
					Set.of(GrammarUtils.END_OF_INPUT_TERMINAL))));

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
						Utils.prettyPrint(input),
						expected.entrySet().stream()
								.map(e -> String.format("%s -> %s", e.getKey(), e.getValue()))
								.collect(Collectors.joining("\n")),
						actual.entrySet().stream()
								.map(e -> String.format("%s -> %s", e.getKey(), e.getValue()))
								.collect(Collectors.joining("\n"))));
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
						Utils.prettyPrint(input),
						expected.entrySet().stream()
								.map(e -> String.format("%s -> %s", e.getKey(), e.getValue()))
								.collect(Collectors.joining("\n")),
						actual.entrySet().stream()
								.map(e -> String.format("%s -> %s", e.getKey(), e.getValue()))
								.collect(Collectors.joining("\n"))));
	}
}
