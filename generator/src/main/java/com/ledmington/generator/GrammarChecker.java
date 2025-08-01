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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.ledmington.ebnf.Expression;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.OneOrMore;
import com.ledmington.ebnf.Or;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.ZeroOrMore;
import com.ledmington.ebnf.ZeroOrOne;

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
		for (final Map.Entry<NonTerminal, Expression> e : g.productions().entrySet()) {
			allNonTerminals.add(e.getKey().name());
			allNonTerminals.addAll(findAllNonTerminals(e.getValue()));
		}

		for (final String name : allNonTerminals) {
			if (g.productions().keySet().stream().noneMatch(nt -> nt.name().equals(name))) {
				throw new UnknownNonTerminalException(name);
			}
		}

		// remove all skippable lexer symbols
		g.productions().entrySet().stream()
				.filter(e -> Production.isSkippable(e.getKey().name()))
				.forEach(e -> allNonTerminals.remove(e.getKey().name()));

		return findStartSymbol(g, allNonTerminals);
	}

	private static String findStartSymbol(final Grammar g, final Set<String> nonTerminals) {
		// Building the graph of reachable symbols
		final Map<String, Set<String>> graph = new HashMap<>();
		g.productions().entrySet().stream()
				.filter(e -> !Production.isSkippable(e.getKey().name()))
				.forEach(e -> {
					final String s = e.getKey().name();
					final Set<String> outEdges = findAllNonTerminals(e.getValue());
					graph.put(s, outEdges);
				});

		final List<String> possibleStartSymbols = new ArrayList<>();
		for (final Map.Entry<String, Set<String>> e : graph.entrySet()) {
			final String possibleStartSymbol = e.getKey();
			final Set<String> visited = bfs(possibleStartSymbol, graph);
			// remove all lexer symbols
			// g.lexerProductions().forEach(p -> visited.remove(p.getKey().name()));
			if (visited.equals(nonTerminals)) {
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
}
