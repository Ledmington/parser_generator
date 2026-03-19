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

	@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
								Map.entry(
										"start",
										new BNFAlternation(new BNFNonTerminal("non_terminal_0"), BNFTerminal.EPSILON)),
								Map.entry("non_terminal_0", new BNFTerminal("a"))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", zero_or_more(t("a"))))),
						bnf(Map.ofEntries(
								Map.entry("start", new BNFNonTerminal("start_tail")),
								Map.entry(
										"start_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFNonTerminal("non_terminal_0"),
														new BNFNonTerminal("start_tail")),
												BNFTerminal.EPSILON)),
								Map.entry("non_terminal_0", new BNFTerminal("a"))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", one_or_more(t("a"))))),
						bnf(Map.ofEntries(
								Map.entry(
										"start",
										new BNFSequence(
												new BNFNonTerminal("non_terminal_0"),
												new BNFNonTerminal("start_tail"))),
								Map.entry(
										"start_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFNonTerminal("non_terminal_0"),
														new BNFNonTerminal("start_tail")),
												BNFTerminal.EPSILON)),
								Map.entry("non_terminal_0", new BNFTerminal("a"))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", seq(t("a"), t("b"))))),
						bnf(Map.ofEntries(
								Map.entry("start", new BNFSequence(new BNFTerminal("a"), new BNFTerminal("b")))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry("start", or(t("a"), t("b"))))),
						bnf(Map.ofEntries(
								Map.entry("start", new BNFAlternation(new BNFTerminal("a"), new BNFTerminal("b")))))),
				Arguments.of(
						ebnf(Map.ofEntries(
								Map.entry("expr", seq(nt("term"), zero_or_more(seq(or(t("+"), t("-")), nt("term"))))),
								Map.entry(
										"term", seq(nt("factor"), zero_or_more(seq(or(t("*"), t("/")), nt("factor"))))),
								Map.entry("factor", or(nt("number"), seq(t("("), nt("expr"), t(")")))),
								Map.entry("number", one_or_more(nt("digit"))),
								Map.entry(
										"digit",
										or(
												t("0"), t("1"), t("2"), t("3"), t("4"), t("5"), t("6"), t("7"), t("8"),
												t("9"))))),
						bnf(Map.ofEntries(
								Map.entry(
										"expr",
										new BNFSequence(new BNFNonTerminal("term"), new BNFNonTerminal("expr_tail"))),
								Map.entry(
										"expr_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFTerminal("+"),
														new BNFNonTerminal("term"),
														new BNFNonTerminal("expr_tail")),
												new BNFSequence(
														new BNFTerminal("-"),
														new BNFNonTerminal("term"),
														new BNFNonTerminal("expr_tail")),
												BNFTerminal.EPSILON)),
								Map.entry(
										"term",
										new BNFSequence(new BNFNonTerminal("factor"), new BNFNonTerminal("term_tail"))),
								Map.entry(
										"term_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFTerminal("*"),
														new BNFNonTerminal("factor"),
														new BNFNonTerminal("term_tail")),
												new BNFSequence(
														new BNFTerminal("/"),
														new BNFNonTerminal("factor"),
														new BNFNonTerminal("term_tail")),
												BNFTerminal.EPSILON)),
								Map.entry(
										"factor",
										new BNFAlternation(
												new BNFNonTerminal("number"),
												new BNFSequence(
														new BNFTerminal("("),
														new BNFNonTerminal("expr"),
														new BNFTerminal(")")))),
								Map.entry(
										"number",
										new BNFSequence(
												new BNFNonTerminal("digit"), new BNFNonTerminal("number_tail"))),
								Map.entry(
										"number_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFNonTerminal("digit"), new BNFNonTerminal("number_tail")),
												BNFTerminal.EPSILON)),
								Map.entry(
										"digit",
										new BNFAlternation(
												new BNFTerminal("0"),
												new BNFTerminal("1"),
												new BNFTerminal("2"),
												new BNFTerminal("3"),
												new BNFTerminal("4"),
												new BNFTerminal("5"),
												new BNFTerminal("6"),
												new BNFTerminal("7"),
												new BNFTerminal("8"),
												new BNFTerminal("9")))))),
				Arguments.of(
						ebnf(Map.ofEntries(
								Map.entry(
										"if_stmt",
										seq(
												t("if"),
												nt("expr"),
												t("then"),
												nt("stmt"),
												zero_or_one(seq(t("else"), nt("stmt"))))),
								Map.entry("stmt", or(t("pass"), nt("if_stmt"))),
								Map.entry("expr", or(t("true"), t("false"))))),
						bnf(Map.ofEntries(
								Map.entry(
										"if_stmt",
										new BNFSequence(
												new BNFTerminal("if"),
												new BNFNonTerminal("expr"),
												new BNFTerminal("then"),
												new BNFNonTerminal("stmt"),
												new BNFNonTerminal("else_part"))),
								Map.entry(
										"else_part",
										new BNFAlternation(
												new BNFSequence(new BNFTerminal("else"), new BNFNonTerminal("stmt")),
												BNFTerminal.EPSILON)),
								Map.entry(
										"stmt",
										new BNFAlternation(new BNFTerminal("pass"), new BNFNonTerminal("if_stmt"))),
								Map.entry(
										"expr",
										new BNFAlternation(new BNFTerminal("true"), new BNFTerminal("false")))))),
				Arguments.of(
						ebnf(Map.ofEntries(
								Map.entry("list", seq(t("["), zero_or_one(nt("elements")), t("]"))),
								Map.entry("elements", seq(nt("element"), zero_or_more(seq(t(","), nt("element"))))),
								Map.entry("element", nt("identifier")),
								Map.entry("identifier", seq(t("a"), zero_or_more(or(t("a"), t("1"))))))),
						bnf(Map.ofEntries(
								Map.entry(
										"list",
										new BNFSequence(
												new BNFTerminal("["),
												new BNFNonTerminal("opt_elements"),
												new BNFTerminal("]"))),
								Map.entry(
										"opt_elements",
										new BNFAlternation(new BNFNonTerminal("elements"), BNFTerminal.EPSILON)),
								Map.entry(
										"elements",
										new BNFSequence(
												new BNFNonTerminal("element"), new BNFNonTerminal("elements_tail"))),
								Map.entry(
										"elements_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFTerminal(","),
														new BNFNonTerminal("element"),
														new BNFNonTerminal("elements_tail")),
												BNFTerminal.EPSILON)),
								Map.entry("element", new BNFNonTerminal("identifier")),
								Map.entry(
										"identifier",
										new BNFSequence(new BNFTerminal("a"), new BNFNonTerminal("identifier_tail"))),
								Map.entry(
										"identifier_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFTerminal("a"), new BNFNonTerminal("identifier_tail")),
												new BNFSequence(
														new BNFTerminal("1"), new BNFNonTerminal("identifier_tail")),
												BNFTerminal.EPSILON))))),
				Arguments.of(
						ebnf(Map.ofEntries(
								Map.entry("pattern", zero_or_more(nt("group"))),
								Map.entry("group", seq(t("("), zero_or_more(or(t("a"), t("b"))), t(")"))))),
						bnf(Map.ofEntries(
								Map.entry(
										"pattern",
										new BNFAlternation(
												new BNFSequence(
														new BNFNonTerminal("group"), new BNFNonTerminal("pattern")),
												BNFTerminal.EPSILON)),
								Map.entry(
										"group",
										new BNFSequence(
												new BNFTerminal("("),
												new BNFNonTerminal("group_content"),
												new BNFTerminal(")"))),
								Map.entry(
										"group_content",
										new BNFAlternation(
												new BNFSequence(
														new BNFTerminal("a"), new BNFNonTerminal("group_content")),
												new BNFSequence(
														new BNFTerminal("b"), new BNFNonTerminal("group_content")),
												BNFTerminal.EPSILON))))),
				Arguments.of(
						ebnf(Map.ofEntries(
								Map.entry("call", seq(t("f"), t("("), zero_or_one(nt("args")), t(")"))),
								Map.entry("args", seq(nt("expr"), zero_or_more(seq(t(","), nt("expr"))))),
								Map.entry("expr", or(t("a"), t("1"))))),
						bnf(Map.ofEntries(
								Map.entry(
										"call",
										new BNFSequence(
												new BNFTerminal("f"),
												new BNFTerminal("("),
												new BNFNonTerminal("opt_args"),
												new BNFTerminal(")"))),
								Map.entry(
										"opt_args",
										new BNFAlternation(new BNFNonTerminal("args"), BNFTerminal.EPSILON)),
								Map.entry(
										"args",
										new BNFSequence(new BNFNonTerminal("expr"), new BNFNonTerminal("args_tail"))),
								Map.entry(
										"args_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFTerminal(","),
														new BNFNonTerminal("expr"),
														new BNFNonTerminal("args_tail")),
												BNFTerminal.EPSILON)),
								Map.entry("expr", new BNFAlternation(new BNFTerminal("a"), new BNFTerminal("1")))))),
				Arguments.of(
						ebnf(Map.ofEntries(
								Map.entry("word", one_or_more(t("a"))),
								Map.entry("sentence", seq(nt("word"), zero_or_more(seq(t(" "), nt("word"))), t("."))))),
						bnf(Map.ofEntries(
								Map.entry(
										"word", new BNFSequence(new BNFTerminal("a"), new BNFNonTerminal("word_tail"))),
								Map.entry(
										"word_tail",
										new BNFAlternation(
												new BNFSequence(new BNFTerminal("a"), new BNFNonTerminal("word_tail")),
												BNFTerminal.EPSILON)),
								Map.entry(
										"sentence",
										new BNFSequence(
												new BNFNonTerminal("word"),
												new BNFNonTerminal("sentence_tail"),
												new BNFTerminal("."))),
								Map.entry(
										"sentence_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFTerminal(" "),
														new BNFTerminal("a"),
														new BNFNonTerminal("sentence_tail")),
												BNFTerminal.EPSILON))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry(
								"type",
								or(
										t("int"),
										t("float"),
										seq(
												t("array"),
												t("<"),
												nt("type"),
												zero_or_more(seq(t(","), nt("type"))),
												t(">")))))),
						bnf(Map.ofEntries(
								Map.entry(
										"type",
										new BNFAlternation(
												new BNFTerminal("int"),
												new BNFTerminal("float"),
												new BNFSequence(
														new BNFTerminal("array"),
														new BNFTerminal("<"),
														new BNFNonTerminal("type"),
														new BNFNonTerminal("type_list_tail"),
														new BNFTerminal(">")))),
								Map.entry(
										"type_list_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFTerminal(","),
														new BNFNonTerminal("type"),
														new BNFNonTerminal("type_list_tail")),
												BNFTerminal.EPSILON))))),
				Arguments.of(
						ebnf(Map.ofEntries(Map.entry(
								"decl",
								seq(
										t("var"),
										t("a"),
										zero_or_one(seq(t("="), t("expr"))),
										zero_or_more(seq(t(","), t("a"), zero_or_one(seq(t("="), t("expr"))))))))),
						bnf(Map.ofEntries(
								Map.entry(
										"decl",
										new BNFSequence(
												new BNFTerminal("var"),
												new BNFTerminal("a"),
												new BNFNonTerminal("opt_init"),
												new BNFNonTerminal("decl_tail"))),
								Map.entry(
										"opt_init",
										new BNFAlternation(
												new BNFSequence(new BNFTerminal("="), new BNFTerminal("expr")),
												BNFTerminal.EPSILON)),
								Map.entry(
										"decl_tail",
										new BNFAlternation(
												new BNFSequence(
														new BNFTerminal(","),
														new BNFTerminal("a"),
														new BNFNonTerminal("opt_init"),
														new BNFNonTerminal("decl_tail")),
												BNFTerminal.EPSILON))))));
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
