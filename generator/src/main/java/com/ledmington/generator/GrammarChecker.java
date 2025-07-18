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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.ledmington.ebnf.Alternation;
import com.ledmington.ebnf.Expression;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.OptionalNode;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Repetition;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.Terminal;

/** A class to check an EBNF grammar for correctness. */
public final class GrammarChecker {

	private GrammarChecker() {}

	/**
	 * Checks the given EBNF grammar for correctness. Throws a RuntimeException in case it's not.
	 *
	 * @param g The grammar to be checked.
	 * @return The name of the non-terminal symbol to start parsing from.
	 */
	public static String check(final Grammar g) {
		Objects.requireNonNull(g);

		// Gather all non-terminals
		final Set<String> allNonTerminals = new HashSet<>();
		for (final Production p : g.productions()) {
			allNonTerminals.add(p.start().name());
			allNonTerminals.addAll(findAllNonTerminals(p.result()));
		}

		for (final String name : allNonTerminals) {
			if (g.productions().stream().noneMatch(p -> p.start().name().equals(name))) {
				throw new UnusableNonTerminalException(name);
			}
		}

		final Set<String> uniqueNonTerminals =
				g.productions().stream().map(p -> p.start().name()).collect(Collectors.toUnmodifiableSet());
		if (uniqueNonTerminals.size() != g.productions().size()) {
			for (final Production prod : g.productions()) {
				if (g.productions().stream()
						.anyMatch(p -> !p.equals(prod)
								&& p.start().name().equals(prod.start().name()))) {
					throw new DuplicatedNonTerminalException(prod.start().name());
				}
			}
		}

		return findStartSymbol(g, allNonTerminals);
	}

	private static String findStartSymbol(final Grammar g, final Set<String> nonTerminals) {
		final Map<String, Set<String>> graph = new HashMap<>();
		for (final Production p : g.productions()) {
			final String s = p.start().name();
			final Set<String> outEdges = findAllNonTerminals(p.result());

			graph.put(s, outEdges);
		}
		for (final Map.Entry<String, Set<String>> e : graph.entrySet()) {
			final String possibleStartSymbol = e.getKey();
			final Set<String> visited = bfs(possibleStartSymbol, graph);
			if (visited.equals(nonTerminals)) {
				return possibleStartSymbol;
			}
		}
		throw new NoUniqueStartSymbolException();
	}

	private static Set<String> bfs(final String start, final Map<String, Set<String>> graph) {
		final Queue<String> q = new ArrayDeque<>();
		final Set<String> visited = new HashSet<>();
		q.add(start);
		while (!q.isEmpty()) {
			final String s = q.remove();
			if (visited.contains(s)) {
				continue;
			}
			visited.add(s);
			q.addAll(graph.get(s));
		}
		return visited;
	}

	private static Set<String> findAllNonTerminals(final Expression root) {
		final Set<String> nonTerminalNames = new HashSet<>();
		final Queue<Node> q = new ArrayDeque<>();
		final Set<Node> visited = new HashSet<>();
		q.add(root);
		while (!q.isEmpty()) {
			final Node n = q.remove();
			if (visited.contains(n)) {
				continue;
			}
			visited.add(n);
			switch (n) {
				case NonTerminal nt -> nonTerminalNames.add(nt.name());
				case Terminal ignored -> {}
				case Alternation a -> q.addAll(a.nodes());
				case Sequence c -> q.addAll(c.nodes());
				case Repetition r -> q.add(r.inner());
				case OptionalNode o -> q.add(o.inner());
				default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
			}
		}
		return nonTerminalNames;
	}
}
