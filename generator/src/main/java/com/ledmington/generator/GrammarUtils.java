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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

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

	private GrammarUtils() {}

	/**
	 * Checks the given FIRST sets for validity, throwing an AssertionError if they are not.
	 *
	 * @param firstSets The FIRST sets to be checked.
	 */
	public static void checkFirstSets(final Map<NonTerminal, Set<Terminal>> firstSets) {
		for (final Map.Entry<NonTerminal, Set<Terminal>> e : firstSets.entrySet()) {
			if (e.getValue().isEmpty()) {
				throw new AssertionError(String.format(
						"FIRST set of symbol '%s' is empty.", e.getKey().name()));
			}
			if (e.getValue().contains(Terminal.END_OF_INPUT)) {
				throw new AssertionError(String.format(
						"FIRST set of symbol '%s' contains END_OF_INPUT terminal.",
						e.getKey().name()));
			}
		}
	}

	/**
	 * Computes the FIRST sets for all the non-terminal symbols in the given EBNF grammar.
	 *
	 * @param g The grammar to be used.
	 * @return The FIRST sets of all non-terminal symbols.
	 */
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
			case Sequence s -> {
				int i = 0;
				Set<Terminal> firstNext;
				do {
					firstNext = computeFirstSet(parserProductions, s.nodes().get(i));
					firstSet.addAll(firstNext);
					i++;
				} while (i < s.nodes().size() && firstNext.contains(Terminal.EPSILON));
			}
			case Or or -> or.nodes().forEach(e -> firstSet.addAll(computeFirstSet(parserProductions, e)));
			case OneOrMore oom -> firstSet.addAll(computeFirstSet(parserProductions, oom.inner()));
			case ZeroOrOne zoo -> {
				firstSet.add(Terminal.EPSILON);
				firstSet.addAll(computeFirstSet(parserProductions, zoo.inner()));
			}
			case ZeroOrMore zom -> {
				firstSet.add(Terminal.EPSILON);
				firstSet.addAll(computeFirstSet(parserProductions, zom.inner()));
			}
			default -> throw new AssertionError(String.format("Unknown node: '%s'.", expr));
		}

		return firstSet;
	}

	/**
	 * Checks that the given FOLLOW sets are valid for the given grammar, throwing an AssertionError if they are not.
	 *
	 * @param g The EBNF grammar to be used.
	 * @param followSets The FOLLOW sets to be checked.
	 */
	public static void checkFollowSets(final Grammar g, final Map<NonTerminal, Set<Terminal>> followSets) {
		if (!followSets.entrySet().stream()
				.filter(e -> e.getKey().name().equals(g.getStartSymbol()))
				.findFirst()
				.orElseThrow()
				.getValue()
				.contains(Terminal.END_OF_INPUT)) {
			throw new AssertionError(String.format(
					"FOLLOW set of start symbol '%s' does not contain '%s' (end of input) terminal.",
					g.getStartSymbol(), Terminal.END_OF_INPUT.literal()));
		}
		for (final Map.Entry<NonTerminal, Set<Terminal>> e : followSets.entrySet()) {
			if (e.getValue().isEmpty()) {
				throw new AssertionError(String.format(
						"FOLLOW set of symbol '%s' is empty.", e.getKey().name()));
			}
			if (e.getValue().contains(Terminal.EPSILON)) {
				throw new AssertionError(String.format(
						"FOLLOW set of symbol '%s' contains '%s' (empty terminal).",
						e.getKey().name(), Terminal.EPSILON.literal()));
			}
		}
	}

	/** Creates a copy of the given set and removes the EPSILON terminal, if it is present. */
	private static Set<Terminal> withoutEpsilon(final Set<Terminal> s) {
		final Set<Terminal> r = new HashSet<>(s);
		r.remove(Terminal.EPSILON);
		return r;
	}

	/**
	 * Computes the FOLLOW sets for all non-terminal symbols in the given EBNF grammar.
	 *
	 * @param g The grammar to be used.
	 * @return A set of all non-terminal symbol with their corresponding FOLLOW sets.
	 */
	public static Map<NonTerminal, Set<Terminal>> computeFollowSets(
			final Grammar g, final Map<NonTerminal, Set<Terminal>> firstSets) {
		// TODO: refactor this algorithm into different phases

		/*
		New algorithm idea:
		1. Create a graph with a node for each non-terminal in the grammar.
		2. Create edges with the following properties:
		2.1 if the production is a sequence, create an edge from the start of the production to the last non-terminal of the production; if the FIRST set of the last non-terminal in the sequence contains epsilon, then also create an edge to the second-to-last non-terminal in the sequence. Repeat until the sequence is over or you find a non-terminal whose FIRST set does not contain epsilon.
		2.2 if the production is an or (alternation), create an edge for each non-terminal in the alternation
		2.3 if the production is "A -> B? ;", create the edge A->B
		2.4 if the production is "A -> B+ ;", create the edge A->B
		2.5 if the production is "A -> B* ;", create the edge A->B
		3. Apply transitive property on edges (this may be skipped, since does not really simplify step 7 but uses more memory)
		4. Check the correctness of the graph: it must not have any cycle.
		5. Use topological sorting to find the "root" nodes: nodes which do not have any incoming edges.
		6. Initialize FOLLOW sets of each node as the empty set, except the starting symbol of the grammar which starts with '$'.
		6.1 Also, for each non-terminal X for which exists a production like "A -> ... X Y ... ;", add FIRST(Y) (without epsilon) to FOLLOW(X)
		7. For each "root" node identified in step 5, propagate its FOLLOW set to neighbors using BFS. This means that, for each edge A->B, add FOLLOW(A) to FOLLOW(B).

		This is an improvement over the old iterative one, since it avoids exploring the same productions over and over. It just builds a graph of relations, then uses it to propagate the FOLLOW sets.
		 */

		// FOLLOW sets dependency graph
		final Map<NonTerminal, Set<NonTerminal>> graph = new HashMap<>();

		// Create a node for each non-terminal
		for (final Production p : g.getParserProductions()) {
			graph.put(p.start(), new HashSet<>());
		}

		for (final Production p : g.getParserProductions()) {
			final NonTerminal a = p.start();
			final Expression expr = p.result();
			switch (expr) {
				case ZeroOrOne zoo -> graph.get(a).add((NonTerminal) zoo.inner());
				case ZeroOrMore zom -> graph.get(a).add((NonTerminal) zom.inner());
				case OneOrMore oom -> graph.get(a).add((NonTerminal) oom.inner());
				case Or or -> {
					for (final Expression b : or.nodes()) {
						// We know that every expression is a composite node made only of NonTerminals
						graph.get(a).add((NonTerminal) b);
					}
				}
				case Sequence s -> {
					for (int i = s.nodes().size() - 1; i >= 0; i--) {
						// We know that every expression is a composite node made only of NonTerminals
						final NonTerminal current = (NonTerminal) s.nodes().get(i);
						graph.get(a).add(current);
						final Set<Terminal> fs = firstSets.get(current);
						if (!fs.contains(Terminal.EPSILON)) {
							break;
						}
					}
				}
				default -> throw new IllegalArgumentException(String.format("Unknown expression node: '%s'.", expr));
			}
		}

		final Map<NonTerminal, Set<Terminal>> followSets = new HashMap<>();

		// Each FOLLOW sets starts empty
		for (final Production p : g.getParserProductions()) {
			followSets.put(p.start(), new HashSet<>());
		}

		// The FOLLOW set of the starting symbol starts with '$'
		followSets.entrySet().stream()
				.filter(e -> e.getKey().name().equals(g.getStartSymbol()))
				.findFirst()
				.orElseThrow()
				.getValue()
				.add(Terminal.END_OF_INPUT);

		// For each symbol X for which exists a production like "A -> ... X Y ... ;", we add FIRST(Y) (without epsilon)
		// to FOLLOW(X)
		for (final Production p : g.getParserProductions()) {
			if (!(p.result() instanceof Sequence(final List<Expression> nodes))) {
				continue;
			}
			for (int i = 0; i < nodes.size() - 1; i++) {
				// We know that every expression is a composite node made only of NonTerminals
				final NonTerminal current = (NonTerminal) nodes.get(i);

				// TODO: are these needed?
				// final NonTerminal next = (NonTerminal) nodes.get(i + 1);
				// followSets.get(current).addAll(withoutEpsilon(firstSets.get(next)));

				final Set<Terminal> firstSuffix = new HashSet<>();
				boolean nullable = true;

				for (int j = i + 1; j < nodes.size(); j++) {
					NonTerminal sym = (NonTerminal) nodes.get(j);
					Set<Terminal> f = firstSets.get(sym);

					firstSuffix.addAll(withoutEpsilon(f));

					if (!f.contains(Terminal.EPSILON)) {
						nullable = false;
						break;
					}
				}

				followSets.get(current).addAll(firstSuffix);

				if (nullable) {
					graph.get(p.start()).add(current);
				}
			}
		}

		// BFS (FOLLOW sets propagation)
		{
			// "topological sorting"
			final Queue<NonTerminal> q = graph.keySet().stream()
					.filter(n -> graph.values().stream().noneMatch(v -> v.contains(n)))
					.distinct()
					.collect(Collectors.toCollection(ArrayDeque::new));

			final Set<NonTerminal> visited = new HashSet<>();
			while (!q.isEmpty()) {
				final NonTerminal current = q.remove();
				if (visited.contains(current)) {
					continue;
				}
				visited.add(current);

				if (!graph.containsKey(current)) {
					continue;
				}

				final Set<NonTerminal> neighbors = graph.get(current);

				for (final NonTerminal b : neighbors) {
					if (!followSets.containsKey(b)) {
						continue;
					}
					followSets.get(b).addAll(followSets.get(current));
				}

				q.addAll(neighbors);
			}
		}

		return followSets;
	}
}
