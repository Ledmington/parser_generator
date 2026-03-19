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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ledmington.ebnf.Expression;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.OneOrMore;
import com.ledmington.ebnf.Or;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.Utils;
import com.ledmington.ebnf.ZeroOrMore;
import com.ledmington.ebnf.ZeroOrOne;

/** A class to convert and EBNF grammar into a BNF grammar. */
public final class Converter {

	private static int NON_TERMINAL_COUNTER = 0;

	private Converter() {}

	/**
	 * Converts the given EBNF grammar into a BNF one.
	 *
	 * @param g The EBNF grammar to be converted.
	 * @return The converted BNF grammar.
	 */
	public static BNFGrammar convertToBnf(final Grammar g) {
		final List<Production> ebnfProductions = g.getProductions().entrySet().stream()
				.sorted(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.toList();
		NON_TERMINAL_COUNTER = 0;
		Map<BNFNonTerminal, BNFExpression> productions = new HashMap<>();
		for (final Production p : ebnfProductions) {
			final BNFNonTerminal start = new BNFNonTerminal(p.start().name());
			final Map<BNFNonTerminal, BNFExpression> prods = convertToBnfProductions(start, p.result());
			productions = mergeProductions(productions, prods);
		}
		return new BNFGrammar(productions);
	}

	/** NOTE: creates a new Map. */
	private static Map<BNFNonTerminal, BNFExpression> mergeProductions(
			final Map<BNFNonTerminal, BNFExpression> productions,
			final Map<BNFNonTerminal, BNFExpression> newProductions) {
		final Map<BNFNonTerminal, BNFExpression> result = new HashMap<>(productions);
		for (final Map.Entry<BNFNonTerminal, BNFExpression> e : newProductions.entrySet()) {
			if (productions.containsKey(e.getKey())) {
				throw new IllegalStateException(String.format(
						"Old productions already contained a production for non-terminal symbol '%s'.",
						e.getKey().name()));
			}
			result.put(e.getKey(), e.getValue());
		}
		return result;
	}

	private static Map<BNFNonTerminal, BNFExpression> convertToBnfProductions(
			final BNFNonTerminal root, final Expression exp) {
		Map<BNFNonTerminal, BNFExpression> productions = new HashMap<>();
		switch (exp) {
			case Terminal t -> productions.put(root, new BNFTerminal(t.literal()));
			case NonTerminal nt -> productions.put(root, new BNFNonTerminal(nt.name()));
			case Or or -> {
				final List<BNFExpression> expressions = new ArrayList<>();
				for (final Expression e : or.nodes()) {
					if (e instanceof NonTerminal(final String name)) {
						expressions.add(new BNFNonTerminal(name));
					} else if (e instanceof final Terminal t) {
						expressions.add(new BNFTerminal(t.literal()));
					} else {
						final BNFNonTerminal tmp = getNewNonTerminal();
						productions = mergeProductions(productions, convertToBnfProductions(tmp, e));
						expressions.add(tmp);
					}
				}
				productions.put(root, new BNFAlternation(expressions));
			}
			case Sequence s -> {
				final List<BNFExpression> expressions = new ArrayList<>();
				for (final Expression e : s.nodes()) {
					if (e instanceof NonTerminal(final String name)) {
						expressions.add(new BNFNonTerminal(name));
					} else if (e instanceof final Terminal t) {
						expressions.add(new BNFTerminal(t.literal()));
					} else {
						// The "special case" is a sequence of either a terminal or a non-terminal followed by something
						// else.
						// In this specific case, just for readability, the "something else" receives the root name with
						// "_tail"
						final boolean isSpecialCase = s.nodes().size() == 2
								&& (s.nodes().getFirst() instanceof Terminal
										|| s.nodes().getFirst() instanceof NonTerminal)
								&& !(s.nodes().get(1) instanceof Terminal
										|| s.nodes().get(1) instanceof NonTerminal);
						final BNFNonTerminal tmp =
								isSpecialCase ? new BNFNonTerminal(root.name() + "_tail") : getNewNonTerminal();
						productions = mergeProductions(productions, convertToBnfProductions(tmp, e));
						expressions.add(tmp);
					}
				}
				productions.put(root, new BNFSequence(expressions));

				System.out.printf("Converted:%n%s -> %s", root, Utils.prettyPrint(exp));
				System.out.printf("Into:%n%s%n", BNFUtils.prettyPrint(new BNFGrammar(productions)));
			}
			case ZeroOrOne zoo -> {
				// x -> y?
				//
				// x -> y | epsilon
				final BNFNonTerminal tmp = getNewNonTerminal();
				final Map<BNFNonTerminal, BNFExpression> converted = convertToBnfProductions(tmp, zoo.inner());
				productions = mergeProductions(productions, converted);
				productions.put(root, new BNFAlternation(tmp, BNFTerminal.EPSILON));
			}
			case ZeroOrMore zom -> {
				// x -> y*
				//
				// x -> x_tail
				// x_tail -> y x_tail | epsilon
				final BNFNonTerminal tail = new BNFNonTerminal(root.name() + "_tail");
				final BNFNonTerminal tmp = getNewNonTerminal();
				final Map<BNFNonTerminal, BNFExpression> converted = convertToBnfProductions(tmp, zom.inner());
				productions = mergeProductions(productions, converted);
				productions.put(root, tail);
				productions.put(tail, new BNFAlternation(new BNFSequence(tmp, tail), BNFTerminal.EPSILON));
			}
			case OneOrMore oom -> {
				// x -> y+
				//
				// x -> y x_tail
				// x_tail -> y x_tail | epsilon
				final BNFNonTerminal tail = new BNFNonTerminal(root.name() + "_tail");
				final BNFNonTerminal tmp = getNewNonTerminal();
				final Map<BNFNonTerminal, BNFExpression> converted = convertToBnfProductions(tmp, oom.inner());
				productions = mergeProductions(productions, converted);
				productions.put(root, new BNFSequence(tmp, tail));
				productions.put(tail, new BNFAlternation(new BNFSequence(tmp, tail), BNFTerminal.EPSILON));
			}
			default -> throw new IllegalArgumentException(String.format("Unknown EBNF node '%s'.", exp));
		}
		return productions;
	}

	private static BNFNonTerminal getNewNonTerminal() {
		return new BNFNonTerminal("non_terminal_" + (NON_TERMINAL_COUNTER++));
	}
}
