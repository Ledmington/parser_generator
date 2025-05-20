/*
 * parser-gen - Parser Generator
 * Copyright (C) 2023-2025 Filippo Barbari <filippo.barbari@gmail.com>
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
package com.ledmington.parser;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;

public final class Parser {

	private Parser() {}

	public static Grammar parse(final String input) {
		try {
			final String stripped = removeComments(input);
			final List<Token> tokens = tokenize(stripped);
			final List<ProductionSet> productions = parse(tokens);
			return new Grammar(productions);
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
			sb.append(it.current());
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

	private static List<ProductionSet> parse(final List<Token> tokens) {
		final List<ProductionSet> productions = new ArrayList<>();
		final Iterator<Token> it = new Iterator<>(tokens);
		while (it.hasNext()) {
			productions.add(parseProductionSet(it));
		}
		return productions;
	}

	private static ProductionSet parseProductionSet(final Iterator<Token> it) {
		final NonTerminal id = parseId(it);
		if (!it.current().equals(Symbols.EQUAL_SIGN)) {
			throw new ParsingException("Expected '='.");
		}
		it.move();
		final List<Node> productions = new ArrayList<>();
		while (it.hasNext() && !it.current().equals(Symbols.SEMICOLON)) {
			productions.add(parseProduction(it));
		}
		if (!it.hasNext()) {
			throw new ParsingException("Expected semicolon but found early end of file.");
		}
		if (!it.current().equals(Symbols.SEMICOLON)) {
			throw new ParsingException(String.format("Expected a semicolon but found '%s'.", it.current()));
		}
		it.move();
		return new ProductionSet(id, productions);
	}

	private static Node parseProduction(final Iterator<Token> it) {
		final List<Node> nodes = new ArrayList<>();
		boolean commaFound = false;
		while (it.hasNext() && !it.current().equals(Symbols.SEMICOLON)) {
			if (it.current() instanceof StringLiteral(final String literal)) {
				if (nodes.size() > 1 && !commaFound) {
					throw new ParsingException("Expected a comma.");
				}
				commaFound = false;
				nodes.add(new Terminal(literal));
			} else if (it.current().equals(Symbols.COMMA)) {
				if (nodes.isEmpty()) {
					throw new ParsingException("Extra comma at the beginning of a production.");
				}
				if (commaFound) {
					throw new ParsingException("Extra comma in the middle of a production.");
				}
				commaFound = true;
			} else if (it.current() instanceof Word) {
				nodes.add(parseId(it));
				// TODO: ugly, remove this
				it.moveBack();
			} else {
				throw new ParsingException(String.format("Unknown token: '%s'.", it.current()));
			}
			it.move();
		}
		if (commaFound) {
			throw new ParsingException("Extra comma at the end of production.");
		}
		return new Sequence(nodes);
	}

	private static NonTerminal parseId(final Iterator<Token> it) {
		if (!(it.current() instanceof Word(final String w))) {
			throw new ParsingException(String.format("Expected a word but found '%s'.", it.current()));
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(w);
		it.move();
		while (it.hasNext() && it.current() instanceof Word(final String word)) {
			sb.append(' ').append(word);
			it.move();
		}
		return new NonTerminal(sb.toString());
	}
}
