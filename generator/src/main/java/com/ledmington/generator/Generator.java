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

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.Optional;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.Utils;

public final class Generator {

	private static int OPTIONAL_COUNTER = 0;

	private Generator() {}

	public static String generate(
			final Node root, final String className, final String packageName, final String indent) {
		final StringBuilder sb = new StringBuilder();
		if (packageName != null && !packageName.isBlank()) {
			sb.append("package ").append(packageName).append(";\n\n");
		}
		sb.append("public final class ")
				.append(className)
				.append(" {")
				.append('\n')
				.append(indent + "private char[] v = null;\n")
				.append(indent + "private int pos = 0;\n")
				.append(indent + "private interface Node {}\n")
				.append(indent + "private record Terminal(String literal) implements Node {}\n")
				.append(indent + "private record Optional(Node inner) implements Node {}\n");

		sb.append(String.join(
				"\n",
				indent + "public Node parse(final String input) {",
				indent + indent + "this.v = input.toCharArray();",
				indent + indent + "this.pos = 0;",
				indent + indent + "final Node result;",
				indent + indent + "try {",
				indent + indent + indent + "result = parse_S();",
				indent + indent + "} catch (final ArrayIndexOutOfBoundsException e) {",
				indent + indent + indent + "return null;",
				indent + indent + "}",
				indent + indent + "return (pos < v.length) ? null : result;",
				indent + "}\n"));

		final Grammar g = (Grammar) root;
		for (final Production p : g.productions()) {
			sb.append(indent + "private Node parse_")
					.append(p.start().name())
					.append("() {\n")
					.append(indent + indent + "// ")
					.append(String.join(
							"\n" + indent + indent + "// ",
							Utils.prettyPrint(p.result(), "  ").split("\n")))
					.append("\n");

			switch (p.result()) {
				case Terminal t -> generateTerminal(sb, indent + indent, t);
				case Optional o -> generateOptional(sb, indent + indent, o);
				default -> sb.append(indent + indent + "return null;\n");
			}

			sb.append(indent + "}\n");
		}

		return sb.append("}").toString();
	}

	private static String getUniqueName(final Node n) {
		return switch (n) {
			case Terminal t -> "terminal_" + t.literal();
			case Optional ignored -> "optional_" + (OPTIONAL_COUNTER++);
			default -> throw new IllegalArgumentException(String.format("Unknown Node '%s'.", n));
		};
	}

	private static void generateTerminal(final StringBuilder sb, final String indent, final Terminal t) {
		final String literal = t.literal();
		sb.append(indent + "if (pos ");
		if (literal.length() > 1) {
			sb.append("+ ").append(literal.length() - 1).append(" ");
		}
		sb.append("< v.length && v[pos] == '").append(literal.charAt(0)).append("'");
		for (int i = 1; i < literal.length(); i++) {
			sb.append(" && v[pos+")
					.append(i)
					.append("] == '")
					.append(literal.charAt(i))
					.append("'");
		}
		sb.append(") {\n")
				.append(indent + indent + "this.pos += ")
				.append(literal.length())
				.append(";\n")
				.append(indent + indent + "return new Terminal(\"")
				.append(literal)
				.append("\");\n")
				.append(indent + "} else {\n")
				.append(indent + indent + "return null;\n")
				.append(indent + "}\n");
	}

	private static void generateOptional(final StringBuilder sb, final String indent, final Optional o) {
		sb.append(indent + "final Node inner = parse_" + getUniqueName(o.inner()) + "();\n")
				.append(indent + "return new Optional(inner);\n");
	}
}
