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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** An object representing all the productions and symbols of an EBNF grammar. */
public final class Grammar {

	private final String startSymbol;
	private final Map<Production, Integer> productions;
	private final List<Production> parserProductions; // pre-computed
	private final List<Production> lexerProductions; // pre-computed

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
		this.productions = Map.copyOf(productions);
		this.startSymbol = findStartSymbol(productions.keySet());

		// temporary list to hold parser productions
		final List<Production> tmp = new ArrayList<>();
		this.lexerProductions = new ArrayList<>();
		splitProductions(productions, lexerProductions, tmp);
		this.parserProductions = simplifyProductions(tmp);
	}

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

	/**
	 * Converts the terminal symbols in the given expression into corresponding fake non-terminal symbols and adding
	 * them into the lexer productions.
	 */
	private static Expression convertExpression(
			final Supplier<String> name, final List<Production> lexerProductions, final Expression e) {
		return switch (e) {
			case Terminal t -> {
				final Optional<Production> replacement = lexerProductions.stream()
						.filter(p -> p.result() instanceof Terminal(final String literal, final boolean ignored)
								&& t.literal().equals(literal))
						.findFirst();
				// Avoid generating fake non-terminal nodes for terminals already present in other productions
				if (replacement.isPresent()) {
					yield replacement.orElseThrow().start();
				} else {
					final String newName = name.get();
					final NonTerminal nt = new NonTerminal(newName);
					lexerProductions.add(new Production(nt, t));
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
			case Sequence s -> s.nodes().stream().anyMatch(Grammar::containsAtLeastOneTerminal);
			case Or or -> or.nodes().stream().anyMatch(Grammar::containsAtLeastOneTerminal);
			default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
		};
	}

	/**
	 * Simplifies the given list of parser productions by returning a new list of productions. A production is defined
	 * to be simple when its corresponding expression is either a terminal symbol, a non-terminal symbol or any
	 * composite node containing only terminal or non-terminal symbols. In other words, we can say that a simple
	 * production has its corresponding expression tree with maximum depth 2.
	 *
	 * @param complexParserProductions The productions to be simplified.
	 * @return A new list of simplified productions.
	 */
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
							case OneOrMore ignored -> "one_or_more_" + (oneOrMoreCounter++);
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

	/**
	 * Checks whether the given expression is 'simple'. An expression is considered simple if it is a terminal symbol, a
	 * non-terminal symbol or any other composite expression made only of terminal and non-terminal symbols.
	 *
	 * @param result The expression to be checked.
	 * @return {@code true} if it is a 'simple' expression, {@code false} otherwise.
	 */
	public static boolean isSimpleProduction(final Expression result) {
		return switch (result) {
			case Terminal ignored -> true;
			case NonTerminal ignored -> true;
			case ZeroOrOne(final Expression inner) -> inner instanceof NonTerminal || inner instanceof Terminal;
			case ZeroOrMore(final Expression inner) -> inner instanceof NonTerminal || inner instanceof Terminal;
			case OneOrMore(final Expression inner) -> inner instanceof NonTerminal || inner instanceof Terminal;
			case Sequence(final List<Expression> nodes) ->
				nodes.stream().allMatch(n -> n instanceof NonTerminal || n instanceof Terminal);
			case Or(final List<Expression> nodes) ->
				nodes.stream().allMatch(n -> n instanceof NonTerminal || n instanceof Terminal);
			default -> false;
		};
	}

	private static String findStartSymbol(final Set<Production> productions) {
		final Set<String> allNonTerminals = findAllNonTerminals(productions);
		checkNonTerminals(allNonTerminals, productions);

		final Map<String, Set<String>> graph = getReachabilityGraph(productions);

		final List<String> possibleStartSymbols = findPossibleStartSymbols(graph);

		if (possibleStartSymbols.isEmpty()) {
			throw new NoUniqueStartSymbolException();
		}
		final int expectedStartSymbols = 1;
		if (possibleStartSymbols.size() == expectedStartSymbols) {
			return possibleStartSymbols.getFirst();
		}
		throw new NoUniqueStartSymbolException(String.format(
				"The following symbols are possible starting symbols: %s.",
				possibleStartSymbols.stream().sorted().map("'{}'"::formatted).collect(Collectors.joining(", "))));
	}

	private static List<String> findPossibleStartSymbols(final Map<String, Set<String>> graph) {
		final List<String> possibleStartSymbols = new ArrayList<>();
		for (final Map.Entry<String, Set<String>> e : graph.entrySet()) {
			final String possibleStartSymbol = e.getKey();
			final Set<String> visited = bfs(possibleStartSymbol, graph);

			// A possible start symbol is a non-terminal symbol which can reach all other symbols (meaning, except
			// itself): it does not matter whether it can reach itself.
			final boolean canReachEverybody = visited.size() == graph.size();
			final boolean canReacheEverybodyExceptItself =
					visited.size() == (graph.size() - 1) && !visited.contains(possibleStartSymbol);
			if (canReachEverybody || canReacheEverybodyExceptItself) {
				possibleStartSymbols.add(possibleStartSymbol);
			}
		}
		return possibleStartSymbols;
	}

	private static Map<String, Set<String>> getReachabilityGraph(final Set<Production> productions) {
		final Map<String, Set<String>> graph = new HashMap<>();
		productions.stream()
				.filter(p -> !Production.isSkippable(p.start().name()))
				.forEach(p -> {
					final String s = p.start().name();
					final Set<String> outEdges = findAllNonTerminals(p.result());
					graph.put(s, outEdges);
				});
		return graph;
	}

	/** Looks for non-terminal symbols which do not have a corresponding production. */
	private static void checkNonTerminals(final Set<String> allNonTerminals, final Set<Production> productions) {
		for (final String name : allNonTerminals) {
			if (productions.stream().noneMatch(nt -> nt.start().name().equals(name))) {
				throw new UnknownNonTerminalException(name);
			}
		}
	}

	private static Set<String> findAllNonTerminals(final Set<Production> productions) {
		final Set<String> allNonTerminals = new HashSet<>();
		for (final Production p : productions) {
			allNonTerminals.add(p.start().name());
			allNonTerminals.addAll(findAllNonTerminals(p.result()));
		}

		// remove all skippable lexer symbols
		productions.stream()
				.filter(p -> Production.isSkippable(p.start().name()))
				.forEach(p -> allNonTerminals.remove(p.start().name()));

		return allNonTerminals;
	}

	/**
	 * Collects all non-terminal symbols 'mentioned' in the given expression root.
	 *
	 * @param root The expression to explore.
	 * @return A set of the names of all non-terminal symbols which appear in the expression.
	 */
	public static Set<String> findAllNonTerminals(final Expression root) {
		final Set<String> nonTerminalNames = new HashSet<>();
		final Queue<Node> q = new ArrayDeque<>();
		final Set<Node> visited = new HashSet<>();
		q.add(root);
		while (!q.isEmpty()) {
			final Node n = q.remove();
			if (!visited.add(n)) {
				continue;
			}
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
		// This is a slightly modified Breadth-First Search. The only modification is that it does not add the starting
		// node at the beginning. So, the search starts from its neighbors and the result will not automatically include
		// the starting node.

		final Queue<String> q = new ArrayDeque<>();
		final Set<String> visited = new HashSet<>();

		// Here, instead of directly adding the starting node, we skip it and add its neighbors
		if (graph.containsKey(start)) {
			q.addAll(graph.get(start));
		}

		while (!q.isEmpty()) {
			final String s = q.remove();
			if (!visited.add(s)) {
				continue;
			}
			if (graph.containsKey(s)) {
				q.addAll(graph.get(s));
			}
		}
		return visited;
	}

	/**
	 * Returns the productions, each with its corresponding priority.
	 *
	 * @return The productions.
	 */
	public Map<Production, Integer> getProductions() {
		return productions;
	}

	/**
	 * Returns the start symbol.
	 *
	 * @return The start symbol.
	 */
	public String getStartSymbol() {
		return startSymbol;
	}

	/**
	 * Returns the sorted list of productions belonging to the parser.
	 *
	 * @return The sorted list of parser productions.
	 */
	public List<Production> getParserProductions() {
		return parserProductions;
	}

	/**
	 * Returns the sorted list of productions belonging to the lexer.
	 *
	 * @return The sorted list of lexer productions.
	 */
	public List<Production> getLexerProductions() {
		return lexerProductions;
	}

	@Override
	public int hashCode() {
		int h = 17;
		h = 31 * h + productions.hashCode();
		h = 31 * h + startSymbol.hashCode();
		return h;
	}

	@Override
	public String toString() {
		return "Grammar(productions=" + productions + ";startSymbol=" + startSymbol + ")";
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
		return this.productions.equals(g.productions) && this.startSymbol.equals(g.startSymbol);
	}
}
