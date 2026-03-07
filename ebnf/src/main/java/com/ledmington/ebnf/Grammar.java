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
package com.ledmington.ebnf;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/** An object representing all the productions and symbols of an EBNF grammar. */
public final class Grammar {

	private final Map<Production, Integer> productions;
	private final String startSymbol;

	/**
	 * Creates a new Grammar with the given Map of productions.
	 *
	 * @param productions The productions which map to their corresponding priority.
	 */
	public Grammar(final Map<Production, Integer> productions) {
		Objects.requireNonNull(productions);
		if (productions.isEmpty()) {
			throw new IllegalArgumentException("Empty productions.");
		}
		this.productions = productions;
		this.startSymbol = findStartSymbol(productions.keySet());
	}

	private static String findStartSymbol(final Set<Production> productions) {
		// Gather all non-terminals
		final Set<String> allNonTerminals = new HashSet<>();
		for (final Production p : productions) {
			allNonTerminals.add(p.start().name());
			allNonTerminals.addAll(findAllNonTerminals(p.result()));
		}

		for (final String name : allNonTerminals) {
			if (productions.stream().noneMatch(nt -> nt.start().name().equals(name))) {
				throw new UnknownNonTerminalException(name);
			}
		}

		// remove all skippable lexer symbols
		productions.stream()
				.filter(p -> Production.isSkippable(p.start().name()))
				.forEach(p -> allNonTerminals.remove(p.start().name()));

		// Building the graph of reachable symbols
		final Map<String, Set<String>> graph = new HashMap<>();
		productions.stream()
				.filter(p -> !Production.isSkippable(p.start().name()))
				.forEach(p -> {
					final String s = p.start().name();
					final Set<String> outEdges = findAllNonTerminals(p.result());
					graph.put(s, outEdges);
				});

		final List<String> possibleStartSymbols = new ArrayList<>();
		for (final Map.Entry<String, Set<String>> e : graph.entrySet()) {
			final String possibleStartSymbol = e.getKey();
			final Set<String> visited = bfs(possibleStartSymbol, graph);
			if (visited.equals(allNonTerminals)) {
				possibleStartSymbols.add(possibleStartSymbol);
			}
		}

		final int expectedStartSymbols = 1;
		if (possibleStartSymbols.isEmpty()) {
			throw new NoUniqueStartSymbolException();
		}
		if (possibleStartSymbols.size() == expectedStartSymbols) {
			return possibleStartSymbols.getFirst();
		}
		throw new NoUniqueStartSymbolException(String.format(
				"The following symbols are possible starting symbols: %s.",
				possibleStartSymbols.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", "))));
	}

	public static Set<String> findAllNonTerminals(final Expression root) {
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
				case Or or -> q.addAll(or.nodes());
				case Sequence c -> q.addAll(c.nodes());
				case ZeroOrMore zom -> q.add(zom.inner());
				case ZeroOrOne zoo -> q.add(zoo.inner());
				case OneOrMore oom -> q.add(oom.inner());
				default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
			}
		}
		return nonTerminalNames;
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
			if (graph.containsKey(s)) {
				q.addAll(graph.get(s));
			}
		}
		return visited;
	}

	public Map<Production, Integer> productions() {
		return productions;
	}

	public String getStartSymbol() {
		return startSymbol;
	}

	@Override
	public int hashCode() {
		int h = 17;
		h = 31 * h + productions.hashCode();
		return h;
	}

	@Override
	public String toString() {
		return "Grammar(productions=" + productions + ")";
	}

	@Override
	public boolean equals(final Object other) {
		if (other == null) {
			return false;
		}
		if (this == other) {
			return true;
		}
		if (!(other instanceof final Grammar g)) {
			return false;
		}
		return this.productions.equals(g.productions);
	}
}
