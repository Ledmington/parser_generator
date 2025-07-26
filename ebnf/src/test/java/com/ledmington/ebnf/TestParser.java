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
			Arguments.of("a=\"a\";", g(p("a", t("a")))),
			Arguments.of("a=B;B=\"a\";", g(p("a", nt("B")), p("B", t("a")))),
			Arguments.of("(**)a=\"a\";", g(p("a", t("a")))),
			Arguments.of("a(**)=\"a\";", g(p("a", t("a")))),
			Arguments.of("a=(**)\"a\";", g(p("a", t("a")))),
			Arguments.of("a=\"a\"(**);", g(p("a", t("a")))),
			Arguments.of("a=\"a\";(**)", g(p("a", t("a")))),
			Arguments.of("my_symbol = \"a\";", g(p("my_symbol", t("a")))),
			Arguments.of("a = \"a\" \"b\";", g(p("a", seq(t("a"), t("b"))))),
			Arguments.of("a=\"a\";b=\"b\";", g(p("a", t("a")), p("b", t("b")))),
			Arguments.of("a=\"a\";b=a;", g(p("a", t("a")), p("b", nt("a")))),
			Arguments.of("a=b;b=a;", g(p("a", nt("b")), p("b", nt("a")))),
			Arguments.of("a=\"a\"|\"b\";", g(p("a", alt(t("a"), t("b"))))),
			Arguments.of("a=\"a\"? \"b\";", g(p("a", seq(zero_or_one(t("a")), t("b"))))),
			Arguments.of("a=\"a\"* \"b\";", g(p("a", seq(zero_or_more(t("a")), t("b"))))),
			Arguments.of("a=\"a\"+ \"b\";", g(p("a", seq(one_or_more(t("a")), t("b"))))),
			Arguments.of("a=\"\\\"\";", g(p("a", t("\"")))),
			Arguments.of("a=\"a\"|\"b\"|\"c\";", g(p("a", alt(t("a"), t("b"), t("c"))))),
			Arguments.of("S=\"a\"|(\"b\" \"c\");", g(p("S", alt(t("a"), seq(t("b"), t("c")))))),
			//
			Arguments.of(
					readFile("ebnf.g"),
					g(
							p("grammar", one_or_more(nt("production"))),
							p(
									"production",
									seq(
											zero_or_one(alt(nt("parser_production"), nt("lexer_production"))),
											nt("SEMICOLON"))),
							p("parser_production", seq(nt("PARSER_SYMBOL"), nt("EQUALS"), nt("parser_expression"))),
							p("lexer_production", seq(nt("LEXER_SYMBOL"), nt("EQUALS"), nt("lexer_expression"))),
							p(
									"parser_expression",
									alt(
											nt("PARSER_SYMBOL"),
											nt("LEXER_SYMBOL"),
											seq(nt("parser_expression"), nt("QUESTION_MARK")),
											seq(nt("parser_expression"), nt("PLUS")),
											seq(nt("parser_expression"), nt("ASTERISK")),
											seq(nt("parser_expression"), nt("VERTICAL_LINE"), nt("parser_expression")),
											seq(
													nt("LEFT_PARENTHESIS"),
													nt("parser_expression"),
													nt("RIGHT_PARENTHESIS")),
											seq(nt("parser_expression"), nt("parser_expression")))),
							p(
									"lexer_expression",
									alt(
											seq(nt("lexer_expression"), nt("QUESTION_MARK")),
											seq(nt("lexer_expression"), nt("PLUS")),
											seq(nt("lexer_expression"), nt("ASTERISK")),
											seq(nt("lexer_expression"), nt("VERTICAL_LINE"), nt("lexer_expression")),
											seq(
													nt("LEFT_PARENTHESIS"),
													nt("lexer_expression"),
													nt("RIGHT_PARENTHESIS")))),
							p("SEMICOLON", t(";")),
							p("UNDERSCORE", t("_")))),
			Arguments.of(readFile("number.g"), g()));

	private static final List<String> INVALID_TEST_CASES = List.of(
			"=",
			";",
			"a",
			"a=\";",
			"a=\"a\",;",
			"a=,\"a\";",
			"a=\"a\",,\"a\";",
			"a=\"a\nb\";",
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

	private static Production p(final String name, final Expression exp) {
		return new Production(nt(name), exp);
	}

	private static NonTerminal nt(final String name) {
		return new NonTerminal(name);
	}

	private static Terminal t(final String literal) {
		return new Terminal(literal);
	}

	private static Sequence seq(final Expression... expressions) {
		return new Sequence(expressions);
	}

	private static Or alt(final Expression... expressions) {
		return new Or(expressions);
	}

	private static ZeroOrOne zero_or_one(final Expression inner) {
		return new ZeroOrOne(inner);
	}

	private static OneOrMore one_or_more(final Expression inner) {
		return new OneOrMore(inner);
	}

	private static ZeroOrMore zero_or_more(final Expression exp) {
		return new ZeroOrMore(exp);
	}

	private static Stream<Arguments> correctTestCases() {
		return CORRECT_TEST_CASES.stream();
	}

	@ParameterizedTest
	@MethodSource("correctTestCases")
	void correct(final String input, final Grammar expected) {
		final Grammar actual = Parser.parse(input);
		assertEquals(
				Utils.prettyPrint(expected, "  "),
				Utils.prettyPrint(actual, "  "),
				() -> String.format(
						"Expected the first grammar but parsed the second one.%n%s%n%s%n",
						Utils.prettyPrint(expected, "  "), Utils.prettyPrint(actual, "  ")));
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
