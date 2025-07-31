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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.ledmington.ebnf.Expression;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.OneOrMore;
import com.ledmington.ebnf.Or;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.ZeroOrMore;
import com.ledmington.ebnf.ZeroOrOne;

/** A collection of common operations to perform on EBNF grammars. */
public final class GrammarUtils {

	private GrammarUtils() {}

	/**
	 * Splits the given set of productions into the ones going into a lexer and the ones going into a parser.
	 *
	 * @param productions The set of grammar productions.
	 * @param lexerProductions The productions belonging to a lexer.
	 * @param parserProductions The productions belonging to a parser.
	 */
	public static void splitProductions(
			final Map<NonTerminal, Expression> productions,
			final List<Production> lexerProductions,
			final List<Production> parserProductions) {
		// Divide all trivial lexer productions from the rest
		productions.entrySet().stream()
				.filter(e -> Production.isLexerProduction(e.getKey().name()))
				.forEach(e -> lexerProductions.add(new Production(e.getKey(), e.getValue())));
		productions.entrySet().stream()
				.filter(e -> !Production.isLexerProduction(e.getKey().name()))
				.forEach(e -> parserProductions.add(new Production(e.getKey(), e.getValue())));

		// Convert all terminal symbols still in the parser into "anonymous" non-terminal ones
		final Supplier<String> nameSupplier = new Supplier<>() {
			private int id = 0;

			@Override
			public String get() {
				return "terminal_" + (id++);
			}
		};
		for (int i = 0; i < parserProductions.size(); i++) {
			final Production p = parserProductions.get(i);
			if (containsAtLeastOneTerminal(p.result())) {
				parserProductions.set(
						i, new Production(p.start(), convertExpression(nameSupplier, lexerProductions, p.result())));
			}
		}

		// Final sort by name the productions
		lexerProductions.sort(Comparator.comparing(a -> a.start().name()));
		parserProductions.sort(Comparator.comparing(a -> a.start().name()));
	}

	private static Expression convertExpression(
			final Supplier<String> name, final List<Production> lexerProductions, final Expression e) {
		return switch (e) {
			case Terminal t -> {
				final Optional<Production> replacement = lexerProductions.stream()
						.filter(p -> p.result() instanceof Terminal(final String literal)
								&& t.literal().equals(literal))
						.findFirst();
				// Avoid generating fake non-terminal nodes for terminals already present in other productions
				if (replacement.isPresent()) {
					yield replacement.orElseThrow().start();
				} else {
					final String newName = name.get();
					final NonTerminal nt = new NonTerminal(newName);
					lexerProductions.add(new Production(nt, t));
					// NODE_NAMES.put(nt, newName);
					yield nt;
				}
			}
			case NonTerminal nt -> nt;
			case ZeroOrOne o -> new ZeroOrOne(convertExpression(name, lexerProductions, o.inner()));
			case ZeroOrMore r -> new ZeroOrMore(convertExpression(name, lexerProductions, r.inner()));
			case Sequence s ->
				new Sequence(s.nodes().stream()
						.map(n -> convertExpression(name, lexerProductions, n))
						.toList());
			case Or a ->
				new Or(a.nodes().stream()
						.map(n -> convertExpression(name, lexerProductions, n))
						.toList());
			default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", e));
		};
	}

	private static boolean containsAtLeastOneTerminal(final Node n) {
		return switch (n) {
			case Terminal ignored -> true;
			case NonTerminal ignored -> false;
			case ZeroOrOne zoo -> containsAtLeastOneTerminal(zoo.inner());
			case ZeroOrMore zom -> containsAtLeastOneTerminal(zom.inner());
			case OneOrMore oom -> containsAtLeastOneTerminal(oom.inner());
			case Sequence s -> s.nodes().stream().anyMatch(GrammarUtils::containsAtLeastOneTerminal);
			case Or or -> or.nodes().stream().anyMatch(GrammarUtils::containsAtLeastOneTerminal);
			default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
		};
	}
}
