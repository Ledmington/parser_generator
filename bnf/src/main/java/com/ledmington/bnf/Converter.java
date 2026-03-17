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

import java.util.HashMap;
import java.util.Map;

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

/** A class to convert and EBNF grammar into a BNF grammar. */
public final class Converter {

	private Converter() {}

	/**
	 * Converts the given EBNF grammar into a BNF one.
	 *
	 * @param g The EBNF grammar to be converted.
	 * @return The converted BNF grammar.
	 */
	public static BNFGrammar convertToBnf(final Grammar g) {
		final Map<BNFNonTerminal, BNFExpression> productions = new HashMap<>();
		for (final Production p : g.getProductions().keySet()) {
			final BNFNonTerminal start = new BNFNonTerminal(p.start().name());
			final BNFExpression exp = convertToBnfExpression(start, p.result());
			productions.put(start, exp);
		}
		return new BNFGrammar(productions);
	}

	private static BNFExpression convertToBnfExpression(final BNFNonTerminal start, final Expression exp) {
		return switch (exp) {
			case Terminal t -> new BNFTerminal(t.literal());
			case NonTerminal nt -> new BNFNonTerminal(nt.name());
			case Sequence s ->
				new BNFSequence(s.nodes().stream()
						.map(n -> convertToBnfExpression(start, n))
						.toList());
			case Or or ->
				new BNFAlternation(or.nodes().stream()
						.map(n -> convertToBnfExpression(start, n))
						.toList());
			case ZeroOrOne zoo -> new BNFAlternation(convertToBnfExpression(start, zoo.inner()), BNFTerminal.EPSILON);
			case ZeroOrMore zom -> {
				final BNFExpression e = convertToBnfExpression(start, zom.inner());
				yield new BNFAlternation(e, start, BNFTerminal.EPSILON);
			}
			case OneOrMore oom -> {
				final BNFExpression e = convertToBnfExpression(start, oom.inner());
				yield new BNFAlternation(e, start);
			}
			default -> throw new IllegalArgumentException(String.format("Unknown EBNF node '%s'.", exp));
		};
	}
}
