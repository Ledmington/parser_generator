/*
 * parser-gen - Parser Generator
 * Copyright (C) 2023-2025 Filippo Barbari <filippo.barbari@gmail.com>
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
package com.ledmington.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public final class TestParser {

	private static final List<Arguments> CORRECT_TEST_CASES = List.of(
			Arguments.of(
					"a=\"a\";", new Grammar(new ProductionSet(new NonTerminal("a"), new Sequence(new Terminal("a"))))),
			Arguments.of(
					"(**)a=\"a\";",
					new Grammar(new ProductionSet(new NonTerminal("a"), new Sequence(new Terminal("a"))))),
			Arguments.of(
					"a(**)=\"a\";",
					new Grammar(new ProductionSet(new NonTerminal("a"), new Sequence(new Terminal("a"))))),
			Arguments.of(
					"a=(**)\"a\";",
					new Grammar(new ProductionSet(new NonTerminal("a"), new Sequence(new Terminal("a"))))),
			Arguments.of(
					"a=\"a\"(**);",
					new Grammar(new ProductionSet(new NonTerminal("a"), new Sequence(new Terminal("a"))))),
			Arguments.of(
					"a=\"a\";(**)",
					new Grammar(new ProductionSet(new NonTerminal("a"), new Sequence(new Terminal("a"))))),
			Arguments.of(
					"my symbol = \"a\";",
					new Grammar(new ProductionSet(new NonTerminal("my symbol"), new Sequence(new Terminal("a"))))),
			Arguments.of(
					"a = \"a\", \"b\";",
					new Grammar(new ProductionSet(
							new NonTerminal("a"), new Sequence(new Terminal("a"), new Terminal("b"))))),
			Arguments.of(
					"a=\"a\";b=\"b\";",
					new Grammar(
							new ProductionSet(new NonTerminal("a"), new Sequence(new Terminal("a"))),
							new ProductionSet(new NonTerminal("b"), new Sequence(new Terminal("b"))))),
			Arguments.of(
					"a=\"a\";b=a;",
					new Grammar(
							new ProductionSet(new NonTerminal("a"), new Sequence(new Terminal("a"))),
							new ProductionSet(new NonTerminal("b"), new Sequence(new NonTerminal("a"))))),
			Arguments.of(
					"a=b;b=a;",
					new Grammar(
							new ProductionSet(new NonTerminal("a"), new Sequence(new NonTerminal("b"))),
							new ProductionSet(new NonTerminal("b"), new Sequence(new NonTerminal("a"))))));

	private static final List<String> INVALID_TEST_CASES =
			List.of("=", ";", "a", "a=\";", "a=\"a\",;", "a=,\"a\";", "a=\"a\",,\"a\";");

	private static Stream<Arguments> correctTestCases() {
		return CORRECT_TEST_CASES.stream();
	}

	@ParameterizedTest
	@MethodSource("correctTestCases")
	void correct(final String input, final Grammar expected) {
		assertEquals(expected, Parser.parse(input));
	}

	private static Stream<Arguments> invalidTestCases() {
		return INVALID_TEST_CASES.stream().map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("invalidTestCases")
	void invalid(final String input) {
		assertThrows(ParsingException.class, () -> Parser.parse(input));
	}
}
