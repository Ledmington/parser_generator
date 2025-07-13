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
package com.ledmington.ebnf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class TestParser {

	private static final List<Arguments> CORRECT_TEST_CASES = List.of(
			Arguments.of("a=\"a\";", g(p(nt("a"), t("a")))),
			Arguments.of("(**)a=\"a\";", g(p(nt("a"), t("a")))),
			Arguments.of("a(**)=\"a\";", g(p(nt("a"), t("a")))),
			Arguments.of("a=(**)\"a\";", g(p(nt("a"), t("a")))),
			Arguments.of("a=\"a\"(**);", g(p(nt("a"), t("a")))),
			Arguments.of("a=\"a\";(**)", g(p(nt("a"), t("a")))),
			Arguments.of("my symbol = \"a\";", g(p(nt("my symbol"), t("a")))),
			Arguments.of("a = \"a\", \"b\";", g(p(nt("a"), cat(t("a"), t("b"))))),
			Arguments.of("a=\"a\";b=\"b\";", g(p(nt("a"), t("a")), p(nt("b"), t("b")))),
			Arguments.of("a=\"a\";b=a;", g(p(nt("a"), t("a")), p(nt("b"), nt("a")))),
			Arguments.of("a=b;b=a;", g(p(nt("a"), nt("b")), p(nt("b"), nt("a")))),
			Arguments.of("a=\"a\"|\"b\";", g(p(nt("a"), alt(t("a"), t("b"))))),
			Arguments.of("a=[\"a\"], \"b\";", g(p(nt("a"), cat(opt(t("a")), t("b"))))),
			Arguments.of("a={\"a\"}, \"b\";", g(p(nt("a"), cat(rep(t("a")), t("b"))))),
			Arguments.of("a=\"\\\"\";", g(p(nt("a"), t("\"")))),
			Arguments.of("a=\"a\"|\"b\"|\"c\";", g(p(nt("a"), alt(t("a"), t("b"), t("c"))))),
			Arguments.of("a1=\"a\";", g(p(nt("a1"), t("a")))),
			Arguments.of("S=\"a\"|(\"b\",\"c\");", g(p(nt("S"), alt(t("a"), cat(t("b"), t("c")))))),
			//
			Arguments.of(
					readFile("ebnf.g"),
					g(
							p(
									nt("letter"),
									alt(
											t("A"), t("B"), t("C"), t("D"), t("E"), t("F"), t("G"), t("H"), t("I"),
											t("J"), t("K"), t("L"), t("M"), t("N"), t("O"), t("P"), t("Q"), t("R"),
											t("S"), t("T"), t("U"), t("V"), t("W"), t("X"), t("Y"), t("Z"), t("a"),
											t("b"), t("c"), t("d"), t("e"), t("f"), t("g"), t("h"), t("i"), t("j"),
											t("k"), t("l"), t("m"), t("n"), t("o"), t("p"), t("q"), t("r"), t("s"),
											t("t"), t("u"), t("v"), t("w"), t("x"), t("y"), t("z"))),
							p(
									nt("digit"),
									alt(
											t("0"), t("1"), t("2"), t("3"), t("4"), t("5"), t("6"), t("7"), t("8"),
											t("9"))),
							p(
									nt("symbol"),
									alt(
											t("["), t("]"), t("{"), t("}"), t("("), t(")"), t("<"), t(">"), t("'"),
											t("="), t("|"), t("."), t(","), t(";"), t("-"), t("+"), t("*"), t("?"),
											t("\\n"), t("\\t"))),
							p(
									nt("character without quotes"),
									alt(nt("letter"), nt("digit"), nt("symbol"), t("_"), t(" "))),
							p(nt("identifier"), cat(nt("letter"), rep(alt(nt("letter"), nt("digit"), t("_"))))),
							p(nt("whitespace"), rep(alt(t(" "), t("\\n"), t("\\t")))),
							p(
									nt("terminal"),
									cat(
											t("\""),
											nt("character without quotes"),
											rep(nt("character without quotes")),
											t("\""))),
							p(nt("terminator"), t(";")),
							p(
									nt("term"),
									alt(
											cat(
													alt(
															cat(
																	t("["),
																	nt("whitespace"),
																	nt("rhs"),
																	nt("whitespace"),
																	t("]")),
															t("{")),
													nt("whitespace"),
													nt("rhs"),
													nt("whitespace"),
													t("}")),
											nt("terminal"),
											nt("identifier"))),
							p(
									nt("concatenation"),
									cat(
											nt("whitespace"),
											nt("term"),
											nt("whitespace"),
											rep(cat(t(","), nt("whitespace"), nt("term"), nt("whitespace"))))),
							p(
									nt("alternation"),
									cat(
											nt("whitespace"),
											nt("concatenation"),
											nt("whitespace"),
											rep(cat(t("|"), nt("whitespace"), nt("concatenation"), nt("whitespace"))))),
							p(nt("rhs"), nt("alternation")),
							p(nt("lhs"), nt("identifier")),
							p(
									nt("rule"),
									cat(
											nt("lhs"),
											nt("whitespace"),
											t("="),
											nt("whitespace"),
											nt("rhs"),
											nt("whitespace"),
											nt("terminator"))),
							p(nt("grammar"), rep(cat(nt("whitespace"), nt("rule"), nt("whitespace")))))));

	private static final List<String> INVALID_TEST_CASES = List.of(
			"=",
			";",
			"a",
			"a=\";",
			"a=\"a\",;",
			"a=,\"a\";",
			"a=\"a\",,\"a\";",
			"a=\"a\nb\";",
			"a_b=\"a\";",
			"_a=\"a\";",
			"1=\"a\";",
			"1a=\"a\";",
			"a=(\"a\";",
			"a=\"a\");",
			"a=(\"a\";)");

	private static String readFile(final String filename) {
		final URL url = Thread.currentThread().getContextClassLoader().getResource(filename);
		try {
			return Files.readString(Path.of(Objects.requireNonNull(url).getPath()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Grammar g(final Production... productions) {
		return new Grammar(productions);
	}

	private static Production p(final NonTerminal nt, final Expression exp) {
		return new Production(nt, exp);
	}

	private static NonTerminal nt(final String name) {
		return new NonTerminal(name);
	}

	private static Terminal t(final String literal) {
		return new Terminal(literal);
	}

	private static Concatenation cat(final Expression... expressions) {
		return new Concatenation(expressions);
	}

	private static Alternation alt(final Expression... expressions) {
		return new Alternation(expressions);
	}

	private static OptionalNode opt(final Expression inner) {
		return new OptionalNode(inner);
	}

	private static Repetition rep(final Expression exp) {
		return new Repetition(exp);
	}

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
