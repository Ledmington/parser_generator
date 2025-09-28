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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.ledmington.ebnf.gen.EBNFParser;
import com.ledmington.ebnf.gen.EBNFParser.grammar;
import com.ledmington.ebnf.gen.EBNFParser.lexer_alternation;
import com.ledmington.ebnf.gen.EBNFParser.lexer_concatenation;
import com.ledmington.ebnf.gen.EBNFParser.lexer_expression;
import com.ledmington.ebnf.gen.EBNFParser.lexer_primary;
import com.ledmington.ebnf.gen.EBNFParser.lexer_repetition;
import com.ledmington.ebnf.gen.EBNFParser.parser_expression;
import com.ledmington.ebnf.gen.EBNFParser.production;
import com.ledmington.ebnf.gen.EBNFParser.sequence_0;
import com.ledmington.ebnf.gen.EBNFParser.sequence_1;

public final class EBNFReader {

	private EBNFReader() {}

	public static Grammar read(final String input) {
		final EBNFParser parser = new EBNFParser();
		return convertGrammar(parser.parse(input));
	}

	private static Grammar convertGrammar(final EBNFParser.Node n) {
		if (!(n instanceof grammar(final List<production> p))) {
			throw new IllegalArgumentException(String.format("Unknown node: expected grammar but was '%s'.", n));
		}
		final Map<Production, Integer> productions = new HashMap<>();
		for (int i = 0; i < p.size(); i++) {
			final int priority = i + 1;
			productions.put(convertProduction(p.get(i)), priority);
		}
		return new Grammar(productions);
	}

	private static Production convertProduction(final production p) {
		if (p.or_0().match() instanceof final EBNFParser.lexer_production lp) {
			return new Production(
					new NonTerminal(lp.LEXER_SYMBOL().literal()), convertLexerExpression(lp.lexer_expression()));
		} else if (p.or_0().match() instanceof final EBNFParser.parser_production pp) {
			return new Production(
					new NonTerminal(pp.PARSER_SYMBOL().literal()), convertParserExpression(pp.parser_expression()));
		} else {
			throw new IllegalArgumentException(String.format("Unknown production: '%s'.", p));
		}
	}

	private static Expression convertLexerExpression(final lexer_expression expr) {
		return convertLexerAlternation(expr.lexer_alternation());
	}

	private static Expression convertLexerAlternation(final lexer_alternation alt) {
		final List<Expression> nodes = Stream.concat(
						Stream.of(alt.lexer_concatenation()),
						alt.zero_or_more_0().sequence_0().stream().map(sequence_0::lexer_concatenation))
				.map(EBNFReader::convertLexerConcatenation)
				.toList();
		return nodes.size() > 1 ? new Or(nodes) : nodes.getFirst();
	}

	private static Expression convertLexerConcatenation(final lexer_concatenation seq) {
		System.out.println("lexer_concatenation: " + seq);
		final List<Expression> nodes = Stream.concat(
						Stream.of(seq.lexer_repetition()), seq.zero_or_more_1().lexer_repetition().stream())
				.map(EBNFReader::convertLexerRepetition)
				.toList();
		return nodes.size() > 1 ? new Sequence(nodes) : nodes.getFirst();
	}

	private static Expression convertLexerRepetition(final lexer_repetition rep) {
		System.out.println("lexer_repetition: " + rep);
		final Expression expr = convertLexerPrimary(rep.lexer_primary());
		if (rep.zero_or_one_0().match() == null) {
			return expr;
		}
		return switch (rep.zero_or_one_0().quantifier().match()) {
			default -> expr;
		};
	}

	private static Expression convertLexerPrimary(final lexer_primary prim) {
		if (prim.match() instanceof EBNFParser.Terminal(final String literal)) {
			return new Terminal(trimDoubleQuotes(literal));
		} else {
			return convertLexerExpression(((sequence_1) prim.match()).lexer_expression());
		}
	}

	private static Expression convertParserExpression(final parser_expression expr) {
		System.out.printf("parser_expression: %s%n", expr);
		return null;
	}

	private static String trimDoubleQuotes(final String literal) {
		final int n = literal.length();
		if (literal.charAt(0) != '"' || literal.charAt(n - 1) != '"') {
			throw new AssertionError();
		}
		return literal.substring(1, n - 1);
	}
}
