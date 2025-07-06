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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.ledmington.ebnf.Concatenation;
import com.ledmington.ebnf.Expression;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.Optional;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Repetition;
import com.ledmington.ebnf.Terminal;

public final class Generator {

	private static final Map<Node, String> NODE_NAMES = new HashMap<>();

	private Generator() {}

	public static String generate(
			final Node root, final String className, final String packageName, final String indent) {
		generateNames(root);

		final boolean atLeastOneOptional = NODE_NAMES.keySet().stream().anyMatch(n -> n instanceof Optional);
		final boolean atLeastOneConcatenation = NODE_NAMES.keySet().stream().anyMatch(n -> n instanceof Concatenation);
		final boolean atLeastOneRepetition = NODE_NAMES.keySet().stream().anyMatch(n -> n instanceof Repetition);

		final IndentedStringBuilder sb = new IndentedStringBuilder(indent);
		if (packageName != null && !packageName.isBlank()) {
			sb.append("package ").append(packageName).append(";\n\n");
		}
		if (atLeastOneConcatenation || atLeastOneRepetition) {
			sb.append("import java.util.List;\n").append("import java.util.ArrayList;\n");
		}
		sb.append("import java.util.Stack;\n")
				.append("public final class ")
				.append(className)
				.append(" {\n")
				.indent()
				.append("private char[] v = null;\n")
				.append("private int pos = 0;\n")
				.append("private Stack<Integer> stack = new Stack<>();\n")
				.append("private interface Node {}\n")
				.append("private record Terminal(String literal) implements Node {}\n");
		if (atLeastOneOptional) {
			sb.append("private record Optional(Node inner) implements Node {}\n");
		}
		if (atLeastOneConcatenation) {
			sb.append("private record Sequence(List<Node> nodes) implements Node {}\n");
		}
		if (atLeastOneRepetition) {
			sb.append("private record Repetition(List<Node> nodes) implements Node {};\n");
		}
		sb.append("public Node parse(final String input) {\n")
				.indent()
				.append("this.v = input.toCharArray();\n")
				.append("this.pos = 0;\n")
				.append("final Node result;\n")
				.append("try {\n")
				.indent()
				.append("result = parse_S();\n")
				.deindent()
				.append("} catch (final ArrayIndexOutOfBoundsException e) {\n")
				.indent()
				.append("return null;\n")
				.deindent()
				.append("}\n")
				.append("return (pos < v.length || !stack.isEmpty()) ? null : result;\n")
				.deindent()
				.append("}\n");

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
				case Grammar g -> {
					for (final Production p : g.productions()) {
						generateNonTerminal(sb, p.start(), p.result());
						q.add(p.result());
					}
				}
				case Terminal t -> generateTerminal(sb, NODE_NAMES.get(t), t);
				case Optional opt -> {
					generateOptional(sb, NODE_NAMES.get(opt), opt);
					q.add(opt.inner());
				}
				case NonTerminal ignored -> {
					// No need to generate anything here because we already handle non-terminals when visiting
					// the grammar's productions
				}
				case Concatenation c -> {
					generateConcatenation(sb, NODE_NAMES.get(c), c);
					q.addAll(c.nodes());
				}
				case Repetition r -> {
					generateRepetition(sb, NODE_NAMES.get(r), r);
					q.add(r.inner());
				}
				default -> throw new IllegalArgumentException(String.format("Unknown node '%s'.", n));
			}
		}

		return sb.deindent().append("}").toString();
	}

	private static void generateRepetition(final IndentedStringBuilder sb, final String name, final Repetition r) {
		sb.append("private Node parse_" + name + "() {\n")
				.indent()
				.append("final List<Node> nodes = new ArrayList<>();\n")
				.append("while (true) {\n")
				.indent()
				.append("final Node n = parse_" + NODE_NAMES.get(r.inner()) + "();\n")
				.append("if (n == null) {\n")
				.indent()
				.append("break;\n")
				.deindent()
				.append("}\n")
				.deindent()
				.append("}\n")
				.append("return new Repetition(nodes);\n")
				.deindent()
				.append("}\n");
	}

	private static void generateConcatenation(
			final IndentedStringBuilder sb, final String name, final Concatenation c) {
		sb.append("private Node parse_" + name + "() {\n")
				.indent()
				.append("final List<Node> nodes = new ArrayList<>();\n")
				.append("stack.push(this.pos);\n");

		final List<Expression> seq = c.nodes();
		for (int i = 0; i < seq.size(); i++) {
			final String nodeName = "n_" + i;
			sb.append("final Node " + nodeName + " = parse_" + NODE_NAMES.get(seq.get(i)) + "();\n")
					.append("if (" + nodeName + " == null) {\n")
					.indent()
					.append("this.pos = stack.pop();")
					.append("return null;\n")
					.deindent()
					.append("}\n")
					.append("nodes.add(" + nodeName + ");\n");
		}

		sb.append("stack.pop();\n")
				.append("return new Sequence(nodes);\n")
				.deindent()
				.append("}\n");
	}

	private static void generateNonTerminal(
			final IndentedStringBuilder sb, final NonTerminal start, final Expression result) {
		sb.append("private Node parse_")
				.append(NODE_NAMES.get(start))
				.append("() {\n")
				.indent()
				.append("return parse_" + NODE_NAMES.get(result) + "();\n")
				.deindent()
				.append("}\n");
	}

	private static void generateNames(final Node root) {
		final Queue<Node> q = new ArrayDeque<>();
		final Set<Node> visited = new HashSet<>();
		q.add(root);

		int terminalCounter = 0;
		int optionalCounter = 0;
		int concatenationCounter = 0;
		int repetitionCounter = 0;
		while (!q.isEmpty()) {
			final Node n = q.remove();
			if (visited.contains(n)) {
				continue;
			}
			visited.add(n);
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
					q.add(opt.inner());
					optionalCounter++;
				}
				case Concatenation c -> {
					NODE_NAMES.put(c, "concatenation_" + concatenationCounter);
					concatenationCounter++;
					q.addAll(c.nodes());
				}
				case Repetition r -> {
					NODE_NAMES.put(r, "repetition_" + repetitionCounter);
					repetitionCounter++;
					q.add(r.inner());
				}
				default -> throw new IllegalArgumentException(String.format("Unknown Node '%s'.", n));
			}
		}
	}

	private static void generateTerminal(final IndentedStringBuilder sb, final String name, final Terminal t) {
		sb.append("private Node parse_" + name + "() {\n");
		final String literal = t.literal();
		sb.indent().append("if (pos ");
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
				.indent()
				.append("this.pos += ")
				.append(literal.length())
				.append(";\n")
				.append("return new Terminal(\"")
				.append(literal)
				.append("\");\n")
				.deindent()
				.append("} else {\n")
				.indent()
				.append("return null;\n")
				.deindent()
				.append("}\n")
				.deindent()
				.append("}\n");
	}

	private static void generateOptional(final IndentedStringBuilder sb, final String name, final Optional o) {
		sb.append("private Node parse_" + name + "() {\n")
				.indent()
				.append("final Node inner = parse_" + NODE_NAMES.get(o.inner()) + "();\n")
				.append("return new Optional(inner);\n")
				.deindent()
				.append("}\n");
	}
}
