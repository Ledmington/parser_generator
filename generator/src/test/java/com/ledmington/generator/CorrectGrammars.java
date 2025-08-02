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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.params.provider.Arguments;

import com.ledmington.ebnf.Expression;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.OneOrMore;
import com.ledmington.ebnf.Or;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.ZeroOrMore;
import com.ledmington.ebnf.ZeroOrOne;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CorrectGrammars {

	private CorrectGrammars() {}

	public static final List<Arguments> TEST_CASES = List.of(
			Arguments.of(g(p(nt("start"), t("a"))), List.of("a"), List.of("", "b", "aa")),
			Arguments.of(g(p(nt("start"), t("abc"))), List.of("abc"), List.of("", "a", "b", "c", "ab", "bc", "cba")),
			Arguments.of(g(p(nt("start"), zero_or_one(t("a")))), List.of("", "a"), List.of("b", "aa")),
			Arguments.of(g(p(nt("start"), nt("t")), p(nt("t"), t("a"))), List.of("a"), List.of("", "b", "aa")),
			Arguments.of(
					g(p(nt("start"), nt("t")), p(nt("t"), zero_or_one(t("a")))), List.of("", "a"), List.of("b", "aa")),
			Arguments.of(
					g(p(nt("start"), cat(t("a"), t("b")))), List.of("ab"), List.of("", "a", "b", "aab", "abb", "c")),
			Arguments.of(
					g(p(nt("start"), cat(t("a"), zero_or_one(t("b")), t("c")))),
					List.of("ac", "abc"),
					List.of("", "a", "c", "ab", "bc")),
			Arguments.of(
					g(p(nt("start"), cat(zero_or_one(t("a")), zero_or_one(t("b")), zero_or_one(t("c"))))),
					List.of("", "a", "b", "c", "ab", "ac", "bc", "abc"),
					List.of("ba", "ca", "cb", "cba", "abb", "acb", "d")),
			Arguments.of(
					g(p(nt("start"), zero_or_one(cat(t("a"), t("b"))))),
					List.of("", "ab"),
					List.of("a", "b", "ba", "aa", "bb")),
			Arguments.of(
					g(
							p(nt("start"), cat(zero_or_one(cat(nt("t"), t("b"))), zero_or_one(cat(t("c"), nt("u"))))),
							p(nt("t"), t("a")),
							p(nt("u"), t("d"))),
					List.of("", "ab", "cd", "abcd"),
					List.of("a", "b", "c", "d", "bc", "ad", "abc", "bcd")),
			Arguments.of(
					g(
							p(nt("start"), cat(nt("t"), nt("u"), nt("v"))),
							p(nt("t"), zero_or_one(t("a"))),
							p(nt("u"), zero_or_one(t("b"))),
							p(nt("v"), zero_or_one(t("c")))),
					List.of("", "a", "b", "c", "ab", "ac", "bc", "abc"),
					List.of("cb", "ca", "ba", "cba", "aba", "bcb", "aabc", "abd")),
			Arguments.of(
					g(p(nt("start"), cat(nt("t"), t("a"))), p(nt("t"), t("a"))),
					List.of("aa"),
					List.of("", "a", "aaa")),
			Arguments.of(
					g(p(nt("start"), zero_or_more(t("a")))),
					List.of("", "a", "aa", "aaa", "aaaa", "aaaaa"),
					List.of("b", "ab", "aba", "bab")),
			Arguments.of(
					g(p(nt("start"), one_or_more(t("a")))),
					List.of("a", "aa", "aaa", "aaaa"),
					List.of("", "ba", "ab", "aba", "aab", "bab")),
			Arguments.of(
					g(p(nt("start"), cat(zero_or_more(cat(t("a"), t("b"))), t("c")))),
					List.of("c", "abc", "ababc", "abababc"),
					List.of("", "a", "b", "ab", "abcab", "aabc", "abbc")),
			Arguments.of(
					g(p(nt("start"), zero_or_more(cat(t("a"), zero_or_one(t("b")), t("c"))))),
					List.of("", "ac", "abc", "acac", "abcabc", "acabc", "abcac"),
					List.of("a", "b", "c", "ab", "bc", "aac", "acc", "abbc")),
			Arguments.of(
					g(p(nt("start"), alt(t("a"), t("b")))), List.of("a", "b"), List.of("", "ab", "aa", "bb", "ba")),
			Arguments.of(
					g(p(nt("start"), zero_or_more(alt(t("a"), t("b"))))),
					List.of("", "a", "b", "aa", "ab", "bb", "ba", "aba", "bab", "aaa"),
					List.of("c", "ac", "cb", "bc", "abc")),
			Arguments.of(
					g(
							p(
									nt("start"),
									cat(
											zero_or_one(nt("sign")),
											alt(
													nt("zero"),
													cat(nt("digit_excluding_zero"), zero_or_more(nt("digit")))))),
							p(nt("sign"), alt(t("+"), t("-"))),
							p(nt("zero"), t("0")),
							p(
									nt("digit_excluding_zero"),
									alt(t("1"), t("2"), t("3"), t("4"), t("5"), t("6"), t("7"), t("8"), t("9"))),
							p(nt("digit"), alt(nt("zero"), nt("digit_excluding_zero")))),
					List.of("0", "+0", "-0", "1", "+99", "-69", "123456789"),
					List.of("", "01", "001", "1a", "1.0")),
			Arguments.of(
					g(p(nt("start"), cat(t("'"), alt(t("a"), t("b")), t("'")))),
					List.of("'a'", "'b'"),
					List.of("", "'a", "a'", "'''", "'a''", "'a'b'")),
			Arguments.of(
					g(p(nt("start"), cat(t("\""), alt(t("a"), t("b")), t("\"")))),
					List.of("\"a\"", "\"b\""),
					List.of("", "\"a", "a\"", "\"\"\"", "\"a\"\"", "\"a\"b\"")),
			Arguments.of(
					g(p(nt("start"), cat(t("\\"), alt(t("n"), t("t"))))),
					List.of("\\n", "\\t"),
					List.of("", "\\", "\n", "\t", "n", "t")));

	private static Grammar g(final Production... productions) {
		return new Grammar(Arrays.stream(productions).collect(Collectors.toMap(Production::start, Production::result)));
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

	private static Sequence cat(final Expression... expressions) {
		return new Sequence(expressions);
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

	private static Or alt(final Expression... expressions) {
		return new Or(expressions);
	}
}
