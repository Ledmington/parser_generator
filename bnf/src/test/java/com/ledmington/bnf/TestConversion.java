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
package com.ledmington.bnf;

import static com.ledmington.bnf.TestingUtilities.bnf;
import static com.ledmington.bnf.TestingUtilities.ebnf;
import static com.ledmington.bnf.TestingUtilities.nt;
import static com.ledmington.bnf.TestingUtilities.one_or_more;
import static com.ledmington.bnf.TestingUtilities.or;
import static com.ledmington.bnf.TestingUtilities.seq;
import static com.ledmington.bnf.TestingUtilities.t;
import static com.ledmington.bnf.TestingUtilities.zero_or_more;
import static com.ledmington.bnf.TestingUtilities.zero_or_one;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Utils;

public final class TestConversion {

	private static Stream<Arguments> testCases() {
		return Stream.of(
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", t("a")))),
						bnf(Map.ofEntries(Map.entry("start", new BNFTerminal("a"))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", nt("A")), Map.entry("A", t("a")))),
						bnf(Map.ofEntries(
								Map.entry("start", new BNFNonTerminal("A")), Map.entry("A", new BNFTerminal("a"))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", zero_or_one(t("a"))))),
						bnf(Map.ofEntries(
								Map.entry("start", new BNFAlternation(new BNFTerminal("a"), BNFTerminal.EPSILON))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", zero_or_more(t("a"))))),
						bnf(Map.ofEntries(Map.entry(
								"start",
								new BNFAlternation(
										new BNFTerminal("a"), new BNFNonTerminal("start"), BNFTerminal.EPSILON))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", one_or_more(t("a"))))),
						bnf(Map.ofEntries(Map.entry(
								"start", new BNFAlternation(new BNFTerminal("a"), new BNFNonTerminal("start")))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", seq(t("a"), t("b"))))),
						bnf(Map.ofEntries(
								Map.entry("start", new BNFSequence(new BNFTerminal("a"), new BNFTerminal("b")))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", or(t("a"), t("b"))))),
						bnf(Map.ofEntries(
								Map.entry("start", new BNFAlternation(new BNFTerminal("a"), new BNFTerminal("b")))))));
	}

	@ParameterizedTest
	@MethodSource("testCases")
	void conversion(final Grammar input, final BNFGrammar expected) {
		final BNFGrammar actual = Converter.convertToBnf(input);
		assertEquals(
				expected,
				actual,
				() -> String.format(
						" --- INPUT EBNF GRAMMAR --- %n%s%n --- EXPECTED BNF GRAMMAR --- %n%s%n --- ACTUAL BNF OUTPUT --- %n%s%n",
						Utils.prettyPrint(input), BNFUtils.prettyPrint(expected), BNFUtils.prettyPrint(actual)));
	}
}
