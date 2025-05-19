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
			Arguments.of(new Grammar("a", "S"), "grammar a;start S;"),
			Arguments.of(new Grammar("a", "S"), " grammar a;start S;"),
			Arguments.of(new Grammar("a", "S"), "grammar a ;start S;"));

	private static final List<String> INVALID_TEST_CASES = List.of("grammara;", "grammar a", "gramma a;");

	private static Stream<Arguments> correctTestCases() {
		return CORRECT_TEST_CASES.stream();
	}

	@ParameterizedTest
	@MethodSource("correctTestCases")
	void correct(final Grammar expected, final String input) {
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
