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
package com.ledmington.generator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

/** A collection of common operations to perform on EBNF grammars. */
public final class GrammarUtils {

	private static final Terminal EMPTY_TERMINAL = new Terminal("ε", true);
	static final Terminal END_OF_INPUT_TERMINAL = new Terminal("$", true);

	private GrammarUtils() {}

	public static void checkFirstSets(final Map<NonTerminal, Set<Terminal>> firstSets) {
		for (final Map.Entry<NonTerminal, Set<Terminal>> e : firstSets.entrySet()) {
			if (e.getValue().isEmpty()) {
				throw new AssertionError(String.format(
						"FIRST set of symbol '%s' is empty.", e.getKey().name()));
			}
			if (e.getValue().contains(END_OF_INPUT_TERMINAL)) {
				throw new AssertionError(String.format(
						"FIRST set of symbol '%s' contains END_OF_INPUT terminal.",
						e.getKey().name()));
			}
		}
	}

	public static Map<NonTerminal, Set<Terminal>> computeFirstSets(final Grammar g) {
		final List<Production> parserProductions = g.getParserProductions();
		final Map<NonTerminal, Set<Terminal>> result = new HashMap<>();
		for (final Production production : parserProductions) {
			result.put(production.start(), computeFirstSet(parserProductions, production.result()));
		}

		return result;
	}

	private static Set<Terminal> computeFirstSet(final List<Production> parserProductions, final Expression expr) {
		final Set<Terminal> firstSet = new HashSet<>();

		switch (expr) {
			case NonTerminal nt -> {
				final Optional<Production> otherProd = parserProductions.stream()
						.filter(p -> p.start().equals(nt))
						.findFirst();
				if (otherProd.isPresent()) {
					firstSet.addAll(computeFirstSet(
							parserProductions, otherProd.orElseThrow().result()));
				} else {
					// if there are no production for this non-terminal, it is a fake one mapping directly to a lexer
					// production
					firstSet.add(new Terminal(nt.name()));
				}
			}
			case Sequence s ->
				firstSet.addAll(computeFirstSet(parserProductions, s.nodes().getFirst()));
			case Or or -> or.nodes().forEach(e -> firstSet.addAll(computeFirstSet(parserProductions, e)));
			case OneOrMore oom -> firstSet.addAll(computeFirstSet(parserProductions, oom.inner()));
			case ZeroOrOne zoo -> {
				firstSet.add(EMPTY_TERMINAL);
				firstSet.addAll(computeFirstSet(parserProductions, zoo.inner()));
			}
			case ZeroOrMore zom -> {
				firstSet.add(EMPTY_TERMINAL);
				firstSet.addAll(computeFirstSet(parserProductions, zom.inner()));
			}
			default -> throw new AssertionError(String.format("Unknown node: '%s'.", expr));
		}

		return firstSet;
	}

	public static void checkFollowSets(final Grammar g, final Map<NonTerminal, Set<Terminal>> followSets) {
		if (!followSets.entrySet().stream()
				.filter(e -> e.getKey().name().equals(g.getStartSymbol()))
				.findFirst()
				.orElseThrow()
				.getValue()
				.contains(END_OF_INPUT_TERMINAL)) {
			throw new AssertionError(String.format(
					"FOLLOW set of start symbol '%s' does not contain '%s' (end of input) terminal.",
					g.getStartSymbol(), END_OF_INPUT_TERMINAL.literal()));
		}
		for (final Map.Entry<NonTerminal, Set<Terminal>> e : followSets.entrySet()) {
			if (e.getValue().isEmpty()) {
				throw new AssertionError(String.format(
						"FOLLOW set of symbol '%s' is empty.", e.getKey().name()));
			}
			if (e.getValue().contains(EMPTY_TERMINAL)) {
				throw new AssertionError(String.format(
						"FOLLOW set of symbol '%s' contains '%s' (empty terminal).",
						e.getKey().name(), EMPTY_TERMINAL.literal()));
			}
		}
	}

	private static Set<Terminal> withoutEpsilon(final Set<Terminal> s) {
		final Set<Terminal> r = new HashSet<>(s);
		r.remove(EMPTY_TERMINAL);
		return r;
	}

	private static boolean containsEpsilon(final Set<Terminal> s) {
		return s.contains(EMPTY_TERMINAL);
	}

	private static int getFollowSetsSize(Map<NonTerminal, Set<Terminal>> followSets) {
		return followSets.values().stream().map(Set::size).reduce(0, Integer::sum);
	}

	public static Map<NonTerminal, Set<Terminal>> computeFollowSets(
			final Grammar g, final Map<NonTerminal, Set<Terminal>> firstSets) {
		final Map<NonTerminal, Set<Terminal>> followSets = new HashMap<>();

		// Each non-terminal starts with an empty set
		for (final Production p : g.getParserProductions()) {
			followSets.put(p.start(), new HashSet<>());
		}
		// The start symbol starts with the '$' symbol.
		followSets.entrySet().stream()
				.filter(e -> e.getKey().name().equals(g.getStartSymbol()))
				.findFirst()
				.orElseThrow()
				.getValue()
				.add(END_OF_INPUT_TERMINAL);

		// Repeating until there is no change
		int previousSize;
		do {
			previousSize = getFollowSetsSize(followSets);

			for (final Production p : g.getParserProductions()) {
				final NonTerminal start = p.start();
				final Expression expr = p.result();

				final Set<Terminal> fs = followSets.get(start);

				updateFollowSet(g, followSets, expr, fs);
			}
		} while (previousSize != getFollowSetsSize(followSets));

		return followSets;
	}

	private static void updateFollowSet(
			final Grammar g,
			final Map<NonTerminal, Set<Terminal>> followSets,
			final Expression expr,
			final Set<Terminal> currentFollowSet) {
		switch (expr) {
			case NonTerminal nt -> {
				// A -> B;
				if (!followSets.containsKey(nt)) {
					followSets.put(nt, new HashSet<>());
				}
				followSets.get(nt).addAll(currentFollowSet);
			}
			case Sequence seq -> {
				// A -> B C D ;
				final List<Expression> nodes = seq.nodes();
				for (int i = 0; i < nodes.size(); i++) {
					final Expression current = nodes.get(i);
					if (!(current instanceof final NonTerminal nt)) {
						continue;
					}
					if (!followSets.containsKey(nt)) {
						followSets.put(nt, new HashSet<>());
					}

					// If there is a next symbol
					if (i + 1 < nodes.size()) {
						final Expression next = nodes.get(i + 1);
						final Set<Terminal> firstNext = computeFirstSet(g.getParserProductions(), next);

						followSets.get(nt).addAll(withoutEpsilon(firstNext));
						if (containsEpsilon(firstNext)) {
							followSets.get(nt).addAll(currentFollowSet);
						}
					} else {
						// last symbol of the sequence
						followSets.get(nt).addAll(currentFollowSet);
					}
				}
			}
			case Or or -> {
				// A -> B | C | D ;
				final List<Expression> nodes = or.nodes();
				for (final Expression current : nodes) {
					if (!(current instanceof final NonTerminal nt)) {
						continue;
					}
					if (!followSets.containsKey(nt)) {
						followSets.put(nt, new HashSet<>());
					}

					followSets.get(nt).addAll(currentFollowSet);
				}
			}
			case ZeroOrOne zoo -> {
				// A -> B? ;
				if (zoo.inner() instanceof final NonTerminal nt) {
					if (!followSets.containsKey(nt)) {
						followSets.put(nt, new HashSet<>());
					}
					followSets.get(nt).addAll(currentFollowSet);
				}
			}
			case ZeroOrMore zom -> {
				// A -> B* ;
				if (zom.inner() instanceof final NonTerminal nt) {
					if (!followSets.containsKey(nt)) {
						followSets.put(nt, new HashSet<>());
					}
					followSets.get(nt).addAll(currentFollowSet);
				}
			}
			case OneOrMore oom -> {
				// A -> B+ ;
				if (oom.inner() instanceof final NonTerminal nt) {
					if (!followSets.containsKey(nt)) {
						followSets.put(nt, new HashSet<>());
					}
					followSets.get(nt).addAll(currentFollowSet);
				}
			}
			default -> throw new IllegalArgumentException(String.format("Unknown expression node: '%s'.", expr));
		}
	}
}
