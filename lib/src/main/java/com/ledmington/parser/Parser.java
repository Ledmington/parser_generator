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

import java.util.List;

public final class Parser {

	private Parser() {}

	public static Grammar parse(final String input) {
		final String stripped = removeComments(input);
		final List<Token> tokens = tokenize(stripped);
		final List<Node> nodes = parse(tokens);

		if (!(nodes.getFirst() instanceof GrammarDeclaration(final String grammarName))) {
			throw new ParsingException("Expected grammar declaration.");
		}

		if (!(nodes.get(1) instanceof StartSymbolDeclaration(final String startSymbolName))) {
			throw new ParsingException("Expected start symbol declaration.");
		}

		return new Grammar(grammarName, startSymbolName);
	}

	private static String removeComments(final String input) {
		final StringBuilder sb = new StringBuilder();
		final int n = input.length();
		int i = 0;
		while (i < n) {
			if (i + 1 < n && input.charAt(i) == '/' && input.charAt(i + 1) == '/') {
				while (i < n && input.charAt(i) != '\n') {
					i++;
				}
			}
			sb.append(input.charAt(i));
			i++;
		}
		return sb.toString();
	}

	private static List<Token> tokenize(final String input) {
		return List.of();
	}

	private static List<Node> parse(final List<Token> tokens) {
		return List.of();
	}
}
