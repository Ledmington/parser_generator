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
package com.ledmington.ebnf;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** A parser of EBNF grammars. */
public final class Parser {

	private static final List<BiPredicate<List<Object>, Integer>> TRANSFORMATIONS = List.of(
			(v, i) -> {
				if (i + 3 >= v.size()) {
					return false;
				}
				if (v.get(i) instanceof final NonTerminal start
						&& v.get(i + 1).equals(Symbols.EQUAL_SIGN)
						&& v.get(i + 2) instanceof final Expression n
						&& v.get(i + 3).equals(Symbols.SEMICOLON)) {
					v.subList(i, i + 4).clear();
					v.add(i, new Production(start, n));
					return true;
				}
				return false;
			},
			(v, i) -> {
				if (i + 1 < v.size()
						&& v.get(i) instanceof final Production first
						&& v.get(i + 1) instanceof final Production second) {
					v.subList(i, i + 2).clear();
					v.add(i, new Grammar(first, second));
					return true;
				}
				return false;
			},
			(v, i) -> {
				if (i + 1 < v.size()
						&& v.get(i) instanceof Grammar(final List<Production> productions)
						&& v.get(i + 1) instanceof final Production second) {
					v.subList(i, i + 2).clear();
					v.add(
							i,
							new Grammar(Stream.concat(productions.stream(), Stream.of(second))
									.toList()));
					return true;
				}
				return false;
			},
			(v, i) -> {
				if (i + 1 < v.size()
						&& v.get(i) instanceof Grammar(final List<Production> productions)
						&& v.get(i + 1) instanceof Grammar(final List<Production> productions1)) {
					v.subList(i, i + 2).clear();
					v.add(
							i,
							new Grammar(Stream.concat(productions.stream(), productions1.stream())
									.toList()));
					return true;
				}
				return false;
			},
			(v, i) -> {
				if (i + 2 < v.size()
						&& v.get(i) instanceof final Expression first
						&& v.get(i + 1).equals(Symbols.COMMA)
						&& v.get(i + 2) instanceof final Expression second) {
					v.subList(i, i + 3).clear();
					v.add(i, new Concatenation(first, second));
					return true;
				}
				return false;
			},
			(v, i) -> {
				if (v.get(i) instanceof Concatenation(final List<Expression> nodes)
						&& nodes.stream().anyMatch(exp -> exp instanceof Concatenation)) {
					v.set(
							i,
							new Concatenation(nodes.stream()
									.flatMap(exp -> exp instanceof Concatenation(final List<Expression> nodes1)
											? nodes1.stream()
											: Stream.of(exp))
									.toList()));
					return true;
				}
				return false;
			},
			(v, i) -> {
				if (i + 2 < v.size()
						&& v.get(i) instanceof final Expression first
						&& v.get(i + 1).equals(Symbols.VERTICAL_LINE)
						&& v.get(i + 2) instanceof final Expression second) {
					v.subList(i, i + 3).clear();
					v.add(i, new Alternation(first, second));
					return true;
				}
				return false;
			},
			(v, i) -> {
				if (v.get(i) instanceof Alternation(final List<Expression> nodes)
						&& nodes.stream().anyMatch(exp -> exp instanceof Alternation)) {
					v.set(
							i,
							new Alternation(nodes.stream()
									.flatMap(exp -> exp instanceof Alternation(final List<Expression> nodes1)
											? nodes1.stream()
											: Stream.of(exp))
									.toList()));
					return true;
				}
				return false;
			},
			(v, i) -> {
				if (i + 2 < v.size()
						&& v.get(i).equals(Symbols.LEFT_SQUARE_BRACKET)
						&& v.get(i + 1) instanceof final Expression n
						&& v.get(i + 2).equals(Symbols.RIGHT_SQUARE_BRACKET)) {
					v.subList(i, i + 3).clear();
					v.add(i, new Optional(n));
					return true;
				}
				return false;
			},
			(v, i) -> {
				if (i + 2 < v.size()
						&& v.get(i).equals(Symbols.LEFT_CURLY_BRACKET)
						&& v.get(i + 1) instanceof final Expression n
						&& v.get(i + 2).equals(Symbols.RIGHT_CURLY_BRACKET)) {
					v.subList(i, i + 3).clear();
					v.add(i, new Repetition(n));
					return true;
				}
				return false;
			});

	private Parser() {}

	/**
	 * Parses the given String as an EBNF grammar.
	 *
	 * @param input A String which contains an EBNF grammar. May contain comments.
	 * @return A new Grammar object representing the parsed grammar.
	 */
	public static Grammar parse(final String input) {
		try {
			final String stripped = removeComments(input);
			final List<Token> tokens = tokenize(stripped);
			return parse(tokens);
		} catch (final IndexOutOfBoundsException ioobe) {
			throw new ParsingException(ioobe);
		}
	}

	private static String removeComments(final String input) {
		final StringBuilder sb = new StringBuilder();
		final int n = input.length();
		boolean ignore = false;
		for (int i = 0; i < n; i++) {
			if (ignore) {
				if (i + 1 < n && input.charAt(i) == '*' && input.charAt(i + 1) == ')') {
					ignore = false;
					i++;
				}
			} else {
				if (i + 1 < n && input.charAt(i) == '(' && input.charAt(i + 1) == '*') {
					ignore = true;
				} else {
					sb.append(input.charAt(i));
				}
			}
		}
		return sb.toString();
	}

	private static List<Token> tokenize(final String input) {
		final List<Token> tokens = new ArrayList<>();
		final StringCharacterIterator it = new StringCharacterIterator(input);
		while (it.current() != CharacterIterator.DONE) {
			final char ch = it.current();
			if (ch == ' ' || ch == '\t' || ch == '\n') {
				skipWhitespaces(it);
			} else if (Character.isAlphabetic(ch)) {
				tokens.add(readWord(it));
			} else if (ch == '=') {
				tokens.add(Symbols.EQUAL_SIGN);
				it.next();
			} else if (ch == ';') {
				tokens.add(Symbols.SEMICOLON);
				it.next();
			} else if (ch == ',') {
				tokens.add(Symbols.COMMA);
				it.next();
			} else if (ch == '|') {
				tokens.add(Symbols.VERTICAL_LINE);
				it.next();
			} else if (ch == '[') {
				tokens.add(Symbols.LEFT_SQUARE_BRACKET);
				it.next();
			} else if (ch == ']') {
				tokens.add(Symbols.RIGHT_SQUARE_BRACKET);
				it.next();
			} else if (ch == '{') {
				tokens.add(Symbols.LEFT_CURLY_BRACKET);
				it.next();
			} else if (ch == '}') {
				tokens.add(Symbols.RIGHT_CURLY_BRACKET);
				it.next();
			} else if (ch == '\"') {
				tokens.add(readStringLiteral(it));
			} else {
				throw new ParsingException(String.format("Unknown character: '%c' (U+%04X).", ch, (int) ch));
			}
		}
		return tokens;
	}

	private static StringLiteral readStringLiteral(final StringCharacterIterator it) {
		if (it.current() != '\"') {
			throw new AssertionError("Expected string literal to start with '\"'.");
		}
		it.next();
		final StringBuilder sb = new StringBuilder();
		while (it.current() != CharacterIterator.DONE && it.current() != '\"') {
			final int idx = it.getIndex();
			if (it.current() == '\\') {
				it.next();
				if (it.current() == '\"') {
					sb.append('\"');
				} else if (it.current() == 'n') {
					sb.append("\\n");
				} else if (it.current() == 't') {
					sb.append("\\t");
				} else {
					it.setIndex(idx);
				}
			} else {
				sb.append(it.current());
			}
			it.next();
		}
		it.next();
		return new StringLiteral(sb.toString());
	}

	private static Word readWord(final StringCharacterIterator it) {
		final StringBuilder sb = new StringBuilder();
		while (it.current() != CharacterIterator.DONE && Character.isAlphabetic(it.current())) {
			sb.append(it.current());
			it.next();
		}
		return new Word(sb.toString());
	}

	private static void skipWhitespaces(final StringCharacterIterator it) {
		while (it.current() != CharacterIterator.DONE
				&& (it.current() == ' ' || it.current() == '\t' || it.current() == '\n')) {
			it.next();
		}
	}

	private static Grammar parse(final List<Token> tokens) {
		// ugly method: the alternative is to manually convert the EBNF grammar for EBNF grammars to be left-recursive
		// and then implement it that way
		final List<Object> v = new ArrayList<>(tokens);

		// First hard-coded pass: convert all string literals into terminal symbols
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i) instanceof StringLiteral(final String literal)) {
				v.set(i, new Terminal(literal));
			}
		}

		// Second hard-coded pass: concatenate all successive words into a single non-terminal symbol
		for (int i = 0; i < v.size(); i++) {
			if (v.get(i) instanceof Word(final String word)) {
				// concatenate all successive words into a single NonTerminal
				final StringBuilder sb = new StringBuilder();
				sb.append(word);
				v.remove(i);
				while (v.get(i) instanceof Word(final String word2)) {
					sb.append(' ').append(word2);
					v.remove(i);
				}
				v.add(i, new NonTerminal(sb.toString()));
			}
		}

		while (v.size() > 1) {
			// do one pass
			final int initialSize = v.size();
			for (int i = 0; i < v.size(); ) {
				boolean atLeastOneMatch = false;
				for (final BiPredicate<List<Object>, Integer> bp : TRANSFORMATIONS) {
					if (bp.test(v, i)) {
						atLeastOneMatch = true;
						break;
					}
				}
				// move only if we didn't apply any transformation
				if (!atLeastOneMatch) {
					i++;
				}
			}
			if (v.size() == initialSize) {
				throw new ParsingException(String.format(
						"Unknown parsing state:%n%s",
						IntStream.range(0, v.size())
								.mapToObj(i -> String.format(
										" %3d : %n%s",
										i,
										v.get(i) instanceof Token
												? v.get(i).toString()
												: ((Node) v.get(i)).prettyPrint(" ".repeat(6))))
								.collect(Collectors.joining("\n"))));
			}
		}

		if (v.size() != 1) {
			throw new AssertionError();
		}
		if (v.getFirst() instanceof final Production p) {
			return new Grammar(p);
		}
		if (!(v.getFirst() instanceof final Grammar g)) {
			throw new ParsingException(
					String.format("Expected root element to be a grammar but was '%s'.", v.getFirst()));
		}

		return g;
	}
}
