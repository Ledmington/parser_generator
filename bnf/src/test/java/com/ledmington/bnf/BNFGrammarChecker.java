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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ledmington.utils.GraphUtils;

// TODO: move this into production code
public final class BNFGrammarChecker {

	private BNFGrammarChecker() {}

	public static void check(final BNFGrammar g) {
		// Build the graph of possible productions (linking each symbol to all symbols which it can produce)
		final Map<BNFNonTerminal, Set<BNFNonTerminal>> graph = new HashMap<>();
		for (final BNFProduction p : g.productions()) {
			graph.put(p.start(), new HashSet<>());
		}
		for (final BNFProduction p : g.productions()) {
			graph.get(p.start()).addAll(findAllNonTerminals(p.result()));
		}

		final Set<BNFNonTerminal> allNonTerminals = graph.entrySet().stream()
				.flatMap(e -> Stream.concat(Stream.of(e.getKey()), e.getValue().stream()))
				.collect(Collectors.toSet());

		// Ensure all symbols are reachable from the starting symbol
		final BNFNonTerminal startSymbol = g.productions().getFirst().start();
		final Set<BNFNonTerminal> reachableSymbols = GraphUtils.bfs(startSymbol, graph::get);

		final boolean allReachable = reachableSymbols.equals(allNonTerminals);
		final boolean allReachableExceptItself =
				without(reachableSymbols, startSymbol).equals(without(allNonTerminals, startSymbol));

		if (!allReachable && !allReachableExceptItself) {
			throw new IllegalArgumentException(
					String.format("The start symbol '%s' cannot reach all other symbols.", startSymbol.name()));
		}
	}

	private static <X> Set<X> without(final Set<X> s, final X toBeRemoved) {
		final Set<X> c = new HashSet<>(s);
		c.remove(toBeRemoved);
		return c;
	}

	private static Set<BNFNonTerminal> findAllNonTerminals(final BNFExpression exp) {
		return switch (exp) {
			case BNFTerminal _ -> Set.of();
			case BNFNonTerminal nt -> Set.of(nt);
			case BNFSequence s ->
				s.expressions().stream()
						.map(BNFGrammarChecker::findAllNonTerminals)
						.reduce(Set.of(), (a, b) -> Stream.concat(a.stream(), b.stream())
								.collect(Collectors.toSet()));
			case BNFAlternation or ->
				or.expressions().stream()
						.map(BNFGrammarChecker::findAllNonTerminals)
						.reduce(Set.of(), (a, b) -> Stream.concat(a.stream(), b.stream())
								.collect(Collectors.toSet()));
			default -> throw new IllegalArgumentException(String.format("Unknown BNF expression: '%s'.", exp));
		};
	}
}
