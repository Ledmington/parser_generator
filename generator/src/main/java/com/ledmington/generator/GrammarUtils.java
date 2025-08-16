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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;
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
			final Map<Production, Integer> productions,
			final List<Production> lexerProductions,
			final List<Production> parserProductions) {
		// Divide all trivial lexer productions from the rest
		productions.entrySet().stream()
				.filter(e -> Production.isLexerProduction(e.getKey().start().name()))
				.sorted(Entry.comparingByValue())
				.forEach(e -> lexerProductions.add(e.getKey()));
		productions.entrySet().stream()
				.filter(e -> !Production.isLexerProduction(e.getKey().start().name()))
				.forEach(e -> parserProductions.add(e.getKey()));

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
			case ZeroOrOne zoo -> new ZeroOrOne(convertExpression(name, lexerProductions, zoo.inner()));
			case OneOrMore oom -> new OneOrMore(convertExpression(name, lexerProductions, oom.inner()));
			case ZeroOrMore zom -> new ZeroOrMore(convertExpression(name, lexerProductions, zom.inner()));
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

	public static List<Production> simplifyProductions(final List<Production> complexParserProductions) {
		final Map<NonTerminal, Expression> productions = new LinkedHashMap<>();

		final Function<Node, NonTerminal> freshNT = new Function<>() {

			int sequenceCounter = 0;
			int orCounter = 0;
			int zeroOrOneCounter = 0;
			int zeroOrMoreCounter = 0;
			int oneOrMoreCounter = 0;

			@Override
			public NonTerminal apply(final Node n) {
				return new NonTerminal(
						switch (n) {
							case Sequence ignored -> "sequence_" + (sequenceCounter++);
							case Or ignored -> "or_" + (orCounter++);
							case ZeroOrOne ignored -> "zero_or_one_" + (zeroOrOneCounter++);
							case ZeroOrMore ignored -> "zero_or_more_" + (zeroOrMoreCounter++);
							case OneOrMore ignored -> "zero_or_one_" + (oneOrMoreCounter++);
							default -> throw new IllegalStateException(String.format("Unknown node: '%s'.", n));
						});
			}
		};

		for (final Production prod : complexParserProductions) {
			if (isSimpleProduction(prod.result())) {
				productions.put(prod.start(), prod.result());
				continue;
			}

			final Queue<Pair<NonTerminal, Expression>> q = new ArrayDeque<>();
			q.add(new Pair<>(prod.start(), prod.result()));

			while (!q.isEmpty()) {
				final Pair<NonTerminal, Expression> p = q.remove();
				final NonTerminal start = p.first();
				final Expression result = p.second();

				switch (result) {
					case NonTerminal nt -> productions.put(start, nt);
					case Sequence seq -> {
						final List<Expression> newElems = new ArrayList<>();
						for (final Expression e : seq.nodes()) {
							if (e instanceof NonTerminal) {
								newElems.add(e);
							} else {
								final NonTerminal tmp = freshNT.apply(e);
								newElems.add(tmp);
								q.add(new Pair<>(tmp, e));
							}
						}
						productions.put(start, new Sequence(newElems));
					}
					case Or or -> {
						final List<Expression> newOpts = new ArrayList<>();
						for (final Expression e : or.nodes()) {
							if (e instanceof NonTerminal) {
								newOpts.add(e);
							} else {
								final NonTerminal tmp = freshNT.apply(e);
								newOpts.add(tmp);
								q.add(new Pair<>(tmp, e));
							}
						}
						productions.put(start, new Or(newOpts));
					}
					case ZeroOrOne zoo -> {
						if (zoo.inner() instanceof NonTerminal) {
							productions.put(start, zoo);
						} else {
							final NonTerminal tmp = freshNT.apply(zoo.inner());
							productions.put(start, new ZeroOrOne(tmp));
							q.add(new Pair<>(tmp, zoo.inner()));
						}
					}
					case ZeroOrMore zom -> {
						if (zom.inner() instanceof NonTerminal) {
							productions.put(start, zom);
						} else {
							final NonTerminal tmp = freshNT.apply(zom.inner());
							productions.put(start, new ZeroOrMore(tmp));
							q.add(new Pair<>(tmp, zom.inner()));
						}
					}
					case OneOrMore oom -> {
						if (oom.inner() instanceof NonTerminal) {
							productions.put(start, oom);
						} else {
							final NonTerminal tmp = freshNT.apply(oom.inner());
							productions.put(start, new OneOrMore(tmp));
							q.add(new Pair<>(tmp, oom.inner()));
						}
					}
					default -> throw new IllegalArgumentException(String.format("Unknown node: '%s'.", result));
				}
			}
		}

		return productions.entrySet().stream()
				.map(e -> new Production(e.getKey(), e.getValue()))
				.toList();
	}

	static boolean isSimpleProduction(final Expression result) {
		return switch (result) {
			case Terminal ignored -> true;
			case NonTerminal ignored -> true;
			case ZeroOrOne zoo -> zoo.inner() instanceof NonTerminal;
			case ZeroOrMore zom -> zom.inner() instanceof NonTerminal;
			case OneOrMore oom -> oom.inner() instanceof NonTerminal;
			case Sequence s -> s.nodes().stream().allMatch(n -> n instanceof NonTerminal);
			case Or or -> or.nodes().stream().allMatch(n -> n instanceof NonTerminal);
			default -> false;
		};
	}
}
