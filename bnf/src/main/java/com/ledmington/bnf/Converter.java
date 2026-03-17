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
import java.util.List;
import java.util.Optional;

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
		final List<Production> ebnfProductions = g.getProductions();
		NON_TERMINAL_COUNTER = 0;
		List<BNFProduction> productions = new ArrayList<>();
		for (final Production p : ebnfProductions) {
			final BNFNonTerminal start = new BNFNonTerminal(p.start().name());
			final List<BNFProduction> prods = convertToBnfProductions(start, p.result());
			productions = mergeProductions(productions, prods);
		}
		return new BNFGrammar(productions);
	}

	/** NOTE: creates a new List. */
	private static List<BNFProduction> mergeProductions(
			final List<BNFProduction> productions, final List<BNFProduction> newProductions) {
		final List<BNFProduction> result = new ArrayList<>(productions);
		for (final BNFProduction p : newProductions) {
			final Optional<BNFProduction> tmp = productions.stream()
					.filter(x -> x.start().equals(p.start()))
					.findAny();
			if (tmp.isPresent()) {
				// Another production for the same NonTerminal was already present, we merge them inside an Or
				final BNFProduction old = tmp.orElseThrow();
				result.add(new BNFProduction(p.start(), new BNFAlternation(old.result(), p.result())));
			} else {
				result.add(p);
			}
		}
		return result;
	}

	private static List<BNFProduction> convertToBnfProductions(final BNFNonTerminal root, final Expression exp) {
		List<BNFProduction> productions = new ArrayList<>();
		switch (exp) {
			case Terminal t -> productions.add(new BNFProduction(root, new BNFTerminal(t.literal())));
			case NonTerminal nt -> productions.add(new BNFProduction(root, new BNFNonTerminal(nt.name())));
			case Or or -> {
				final List<BNFExpression> expressions = new ArrayList<>();
				for (final Expression e : or.expressions()) {
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
				productions.add(new BNFProduction(root, new BNFAlternation(expressions)));
			}
			case Sequence s -> {
				final List<BNFExpression> expressions = new ArrayList<>();
				for (final Expression e : s.expressions()) {
					if (e instanceof NonTerminal(final String name)) {
						expressions.add(new BNFNonTerminal(name));
					} else if (e instanceof final Terminal t) {
						expressions.add(new BNFTerminal(t.literal()));
					} else {
						// The "special case" is a sequence of either a terminal or a non-terminal followed by something
						// else.
						// In this specific case, just for readability, the "something else" receives the root name with
						// "_tail"
						final boolean isSpecialCase = s.expressions().size() == 2
								&& Utils.isSymbol(s.expressions().getFirst())
								&& !Utils.isSymbol(s.expressions().get(1));
						final BNFNonTerminal tmp =
								isSpecialCase ? new BNFNonTerminal(root.name() + "_tail") : getNewNonTerminal();
						productions = mergeProductions(productions, convertToBnfProductions(tmp, e));
						expressions.add(tmp);
					}
				}
				productions.add(new BNFProduction(root, new BNFSequence(expressions)));

				System.out.printf("Converted:%n%s -> %s", root, Utils.printAsGrammar(exp));
				System.out.printf("Into:%n%s%n", BNFUtils.printAsGrammar(new BNFGrammar(productions)));
			}
			case ZeroOrOne zoo -> {
				// x -> y?
				//
				// x -> y | epsilon
				final BNFNonTerminal tmp = getNewNonTerminal();
				final List<BNFProduction> converted = convertToBnfProductions(tmp, zoo.inner());
				productions = mergeProductions(productions, converted);
				productions.add(new BNFProduction(root, new BNFAlternation(tmp, BNFTerminal.EPSILON)));
			}
			case ZeroOrMore zom -> {
				// x -> y*
				//
				// x -> x_tail
				// x_tail -> y x_tail | epsilon
				final BNFNonTerminal tail = new BNFNonTerminal(root.name() + "_tail");
				final BNFNonTerminal tmp = getNewNonTerminal();
				final List<BNFProduction> converted = convertToBnfProductions(tmp, zom.inner());
				productions = mergeProductions(productions, converted);
				productions.add(new BNFProduction(root, tail));
				productions.add(
						new BNFProduction(tail, new BNFAlternation(new BNFSequence(tmp, tail), BNFTerminal.EPSILON)));
			}
			case OneOrMore oom -> {
				// x -> y+
				//
				// x -> y x_tail
				// x_tail -> y x_tail | epsilon
				final BNFNonTerminal tail = new BNFNonTerminal(root.name() + "_tail");
				final BNFNonTerminal tmp = getNewNonTerminal();
				final List<BNFProduction> converted = convertToBnfProductions(tmp, oom.inner());
				productions = mergeProductions(productions, converted);
				productions.add(new BNFProduction(root, new BNFSequence(tmp, tail)));
				productions.add(
						new BNFProduction(tail, new BNFAlternation(new BNFSequence(tmp, tail), BNFTerminal.EPSILON)));
			}
			default -> throw new IllegalArgumentException(String.format("Unknown EBNF node '%s'.", exp));
		}
		return productions;
	}

	private static BNFNonTerminal getNewNonTerminal() {
		return new BNFNonTerminal("non_terminal_" + (NON_TERMINAL_COUNTER++));
	}
}
