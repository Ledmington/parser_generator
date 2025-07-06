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
import java.util.Map;
import java.util.Queue;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.Optional;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.Utils;

public final class Generator {

	private static final Map<Node, String> NODE_NAMES = new HashMap<>();

	private Generator() {}

	public static String generate(
			final Node root, final String className, final String packageName, final String indent) {
		generateNames(root);

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

		final Queue<Node> q = new ArrayDeque<>();
		q.add(root);

		while (!q.isEmpty()) {
			final Node n = q.remove();
			switch (n) {
				case Grammar g -> {
					for (final Production p : g.productions()) {
						sb.append(indent + "private Node parse_")
								.append(NODE_NAMES.get(p.start()))
								.append("() {\n")
								.append(indent + indent + "// ")
								.append(String.join(
										"\n" + indent + indent + "// ",
										Utils.prettyPrint(p.result(), "  ").split("\n")))
								.append("\n")
								.append(indent + indent + "return parse_" + NODE_NAMES.get(p.result()) + "();\n")
								.append(indent + "}\n");
						q.add(p.result());
					}
				}
				case Terminal t -> generateTerminal(sb, indent, NODE_NAMES.get(t), t);
				case Optional opt -> {
					generateOptional(sb, indent, NODE_NAMES.get(opt), opt);
					q.add(opt.inner());
				}
				default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
			}
		}

		return sb.append("}").toString();
	}

	private static void generateNames(final Node root) {
		final Queue<Node> q = new ArrayDeque<>();
		q.add(root);

		int terminalCounter = 0;
		int optionalCounter = 0;
		while (!q.isEmpty()) {
			final Node n = q.remove();
			switch (n) {
				case Grammar g -> {
					for (final Production p : g.productions()) {
						q.add(p.start());
						q.add(p.result());
					}
				}
				case Terminal t -> {
					NODE_NAMES.put(t, "terminal_" + terminalCounter);
					terminalCounter++;
				}
				case NonTerminal nt -> NODE_NAMES.put(nt, nt.name().replace(' ', '_'));
				case Optional opt -> {
					NODE_NAMES.put(opt, "optional_" + optionalCounter);
					optionalCounter++;
				}
				default -> throw new IllegalArgumentException(String.format("Unknown Node '%s'.", n));
			}
		}
	}

	private static void generateTerminal(
			final StringBuilder sb, final String indent, final String name, final Terminal t) {
		sb.append(indent + "private Node parse_" + name + "() {\n");
		final String literal = t.literal();
		sb.append(indent + indent + "if (pos ");
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
				.append(indent + indent + indent + "this.pos += ")
				.append(literal.length())
				.append(";\n")
				.append(indent + indent + indent + "return new Terminal(\"")
				.append(literal)
				.append("\");\n")
				.append(indent + indent + "} else {\n")
				.append(indent + indent + indent + "return null;\n")
				.append(indent + indent + "}\n")
				.append(indent + "}\n");
	}

	private static void generateOptional(
			final StringBuilder sb, final String indent, final String name, final Optional o) {
		sb.append(indent + "private Node parse_" + name + "() {\n")
				.append(indent + indent + "final Node inner = parse_" + NODE_NAMES.get(o.inner()) + "();\n")
				.append(indent + indent + "return new Optional(inner);\n")
				.append(indent + "}\n");
	}
}
