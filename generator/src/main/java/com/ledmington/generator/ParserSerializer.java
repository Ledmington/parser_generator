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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.ledmington.ebnf.Expression;
import com.ledmington.ebnf.Node;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.OneOrMore;
import com.ledmington.ebnf.Or;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.ZeroOrMore;
import com.ledmington.ebnf.ZeroOrOne;

/** An helper class to generate java code for a given set of parser productions. */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class ParserSerializer {

	private final IndentedStringBuilder sb;
	private final Set<String> tokenNames;
	private final Map<Node, String> globalNodeNames;

	/**
	 * Creates a new ParserSerializer.
	 *
	 * @param sb The StringBuilder to use.
	 * @param tokenNames The set of non-terminals corresponding to lexer productions.
	 * @param nodeNames A map of the name for each node in the grammar.
	 */
	public ParserSerializer(
			final IndentedStringBuilder sb, final Set<String> tokenNames, final Map<Node, String> nodeNames) {
		this.sb = Objects.requireNonNull(sb);
		this.tokenNames = Objects.requireNonNull(tokenNames);
		this.globalNodeNames = Objects.requireNonNull(nodeNames);
	}

	private boolean isToken(final String tokenName) {
		return this.tokenNames.contains(tokenName);
	}

	/**
	 * Generates java code of an LL(*) parser for the given list of productions.
	 *
	 * @param parserProductions The productions to be used.
	 */
	@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
	public void generateParser(final List<Production> parserProductions) {
		for (final Production p : parserProductions) {
			final String newNodeName = p.start().name();
			switch (p.result()) {
				case NonTerminal nt ->
					sb.append("public record ")
							.append(newNodeName)
							.append('(')
							.append(resolveTypeName(nt))
							.append(' ')
							.append(nt.name())
							.append(") implements NonTerminal {\n")
							.indent()
							.append("@Override\n")
							.append("public String name() {\n")
							.indent()
							.append("return \"" + newNodeName + "\";\n")
							.deindent()
							.append("}\n")
							.append("@Override\n")
							.append("public Node match() {\n")
							.indent()
							.append("return " + nt.name() + ";\n")
							.deindent()
							.append("}\n")
							.deindent()
							.append("}\n");
				case Sequence(final List<Expression> nodes) -> {
					final Map<String, Integer> typeCounts = new HashMap<>();
					for (final Expression exp : nodes) {
						final String typeName = resolveTypeName(exp);
						final String nodeName = globalNodeNames.get(exp);
						final String fullName = typeName + " " + nodeName;
						typeCounts.put(fullName, typeCounts.getOrDefault(fullName, 0) + 1);
					}
					final Map<String, Integer> nameCounter = new HashMap<>();
					final List<String> nodeNames = new ArrayList<>();
					for (final Expression exp : nodes) {
						final String typeName = resolveTypeName(exp);
						final String nodeName = globalNodeNames.get(exp);
						final String fullName = typeName + " " + nodeName;
						if (typeCounts.get(fullName) == 1) {
							nodeNames.add(nodeName);
						} else {
							final int count = nameCounter.getOrDefault(fullName, 0);
							nodeNames.add(nodeName + "_" + count);
							nameCounter.put(fullName, count + 1);
						}
					}
					sb.append("public record ")
							.append(newNodeName)
							.append('(')
							.append(IntStream.range(0, nodes.size())
									.mapToObj(i -> resolveTypeName(nodes.get(i)) + " " + nodeNames.get(i))
									.collect(Collectors.joining(", ")))
							.append(") implements Sequence {\n")
							.indent()
							.append("@Override\n")
							.append("public String name() {\n")
							.indent()
							.append("return \"")
							.append(newNodeName)
							.append("\";\n")
							.deindent()
							.append("}\n")
							.append("@Override\n")
							.append("public List<Node> nodes() {\n")
							.indent()
							.append("return List.of(")
							.append(IntStream.range(0, nodes.size())
									.mapToObj(nodeNames::get)
									.collect(Collectors.joining(", ")))
							.append(");\n")
							.deindent()
							.append("}\n")
							.deindent()
							.append("}\n");
				}
				case Or ignored ->
					sb.append("public record ")
							.append(newNodeName)
							.append("(Node match) implements Or {\n")
							.indent()
							.append("@Override\n")
							.append("public String name() {\n")
							.indent()
							.append("return \"")
							.append(newNodeName)
							.append("\";\n")
							.deindent()
							.append("}\n")
							.deindent()
							.append("}\n");
				case ZeroOrOne(final Expression inner) ->
					sb.append("public record ")
							.append(newNodeName)
							.append("(" + resolveTypeName(inner) + " " + globalNodeNames.get(inner)
									+ ") implements ZeroOrOne {\n")
							.indent()
							.append("@Override\n")
							.append("public String name() {\n")
							.indent()
							.append("return \"")
							.append(newNodeName)
							.append("\";\n")
							.deindent()
							.append("}\n")
							.append("@Override\n")
							.append("public Node match() {\n")
							.indent()
							.append("return " + globalNodeNames.get(inner) + ";\n")
							.deindent()
							.append("}\n")
							.deindent()
							.append("}\n");
				case ZeroOrMore(final Expression inner) ->
					sb.append("public record ")
							.append(newNodeName)
							.append("(List<")
							.append(resolveTypeName(inner))
							.append("> ")
							.append(globalNodeNames.get(inner))
							.append(") implements ZeroOrMore {\n")
							.indent()
							.append("@Override\n")
							.append("public String name() {\n")
							.indent()
							.append("return \"")
							.append(newNodeName)
							.append("\";\n")
							.deindent()
							.append("}\n")
							.append("@Override\n")
							.append("public List<Node> nodes() {\n")
							.indent()
							.append("return ")
							.append(globalNodeNames.get(inner))
							.append(".stream().map(n -> (Node) n).toList();\n")
							.deindent()
							.append("}\n")
							.deindent()
							.append("}\n");
				case OneOrMore(final Expression inner) ->
					sb.append("public record ")
							.append(newNodeName)
							.append("(List<")
							.append(resolveTypeName(inner))
							.append("> ")
							.append(globalNodeNames.get(inner))
							.append(") implements OneOrMore {\n")
							.indent()
							.append("@Override\n")
							.append("public String name() {\n")
							.indent()
							.append("return \"")
							.append(newNodeName)
							.append("\";\n")
							.deindent()
							.append("}\n")
							.append("@Override\n")
							.append("public List<Node> nodes() {\n")
							.indent()
							.append("return ")
							.append(globalNodeNames.get(inner))
							.append(".stream().map(n -> (Node) n).toList();\n")
							.deindent()
							.append("}\n")
							.deindent()
							.append("}\n");
				default -> throw new IllegalArgumentException(String.format("Unknown node: '%s'.", p.result()));
			}
		}

		sb.append("private Terminal parseTerminal(final TokenType expected) {\n")
				.indent()
				.append("if (pos < v.length && v[pos].type() == expected) {\n")
				.indent()
				.append("return new Terminal(v[pos++].content());\n")
				.deindent()
				.append("}\n")
				.append("return null;\n")
				.deindent()
				.append("}\n");

		for (final Production p : parserProductions) {
			final NonTerminal start = p.start();
			final Expression result = p.result();
			final String productionName = start.name();

			switch (result) {
				case NonTerminal nt -> generateNonTerminal(start, nt);
				case Sequence s -> generateSequence(productionName, s);
				case Or or -> generateOr(productionName, or);
				case ZeroOrOne zoo -> generateZeroOrOne(productionName, zoo);
				case ZeroOrMore zom -> generateZeroOrMore(productionName, zom);
				case OneOrMore oom -> generateOneOrMore(productionName, oom);
				default -> throw new IllegalArgumentException(String.format("Unknown node: '%s'", result));
			}
		}
	}

	private String resolveTypeName(final Expression exp) {
		return switch (exp) {
			case NonTerminal nt -> isToken(nt.name()) ? "Terminal" : nt.name();
			case Sequence ignored -> "Sequence";
			case ZeroOrMore ignored -> "ZeroOrMore";
			case ZeroOrOne ignored -> "ZeroOrOne";
			default -> throw new IllegalArgumentException(String.format("Unknown node: '%s'.", exp));
		};
	}

	private void generateOr(final String productionName, final Or or) {
		sb.append("private " + productionName + " parse_" + productionName + "() {\n")
				.indent();
		final List<Expression> nodes = or.nodes();
		for (int i = 0; i < nodes.size(); i++) {
			final Expression exp = nodes.get(i);
			final String nodeName = "n_" + i;
			final String actualName = globalNodeNames.get(exp);
			final String innerTypeName = resolveTypeName(exp);
			sb.append("final " + innerTypeName + " " + nodeName + " = ");
			if (exp instanceof NonTerminal && isToken(actualName)) {
				sb.append("parseTerminal(TokenType." + actualName + ");\n");
			} else {
				sb.append("parse_" + actualName + "();\n");
			}
			sb.append("if (" + nodeName + " != null) {\n")
					.indent()
					.append("return new ")
					.append(productionName)
					.append("(" + nodeName + ");\n")
					.deindent()
					.append("}\n");
		}
		sb.append("return null;\n").deindent().append("}\n");
	}

	private void generateZeroOrMore(final String productionName, final ZeroOrMore zom) {
		final String actualName = globalNodeNames.get(zom.inner());
		final String innerTypeName = resolveTypeName(zom.inner());
		sb.append("private " + productionName + " parse_" + productionName + "() {\n")
				.indent()
				.append("final List<" + innerTypeName + "> nodes = new ArrayList<>();\n")
				.append("while (true) {\n")
				.indent()
				.append("final " + innerTypeName + " n = ");
		if (zom.inner() instanceof NonTerminal && isToken(actualName)) {
			sb.append("parseTerminal(TokenType." + actualName + ")");
		} else {
			sb.append("parse_" + actualName + "()");
		}
		sb.append(";\n");
		if (!(zom.inner() instanceof ZeroOrMore) && !(zom.inner() instanceof ZeroOrOne)) {
			sb.append("if (n == null) {\n")
					.indent()
					.append("break;\n")
					.deindent()
					.append("}\n");
		}
		sb.append("nodes.add(n);\n")
				.deindent()
				.append("}\n")
				.append("return new ")
				.append(productionName)
				.append("(nodes);\n")
				.deindent()
				.append("}\n");
	}

	private void generateOneOrMore(final String productionName, final OneOrMore oom) {
		final String actualName = globalNodeNames.get(oom.inner());
		final String innerTypeName = resolveTypeName(oom.inner());
		sb.append("private " + productionName + " parse_" + productionName + "() {\n")
				.indent()
				.append("final " + innerTypeName + " n_0 = ");
		if (oom.inner() instanceof NonTerminal && isToken(actualName)) {
			sb.append("parseTerminal(TokenType." + actualName + ")");
		} else {
			sb.append("parse_" + actualName + "()");
		}
		sb.append(";\n");
		if (!(oom.inner() instanceof ZeroOrMore) && !(oom.inner() instanceof ZeroOrOne)) {
			sb.append("if (n_0 == null) {\n")
					.indent()
					.append("return null;\n")
					.deindent()
					.append("}\n");
		}
		sb.append("final List<" + innerTypeName + "> nodes = new ArrayList<>();\n")
				.append("nodes.add(n_0);\n")
				.append("while (true) {\n")
				.indent()
				.append("final " + innerTypeName + " n = ");
		if (oom.inner() instanceof NonTerminal && isToken(actualName)) {
			sb.append("parseTerminal(TokenType." + actualName + ")");
		} else {
			sb.append("parse_" + actualName + "()");
		}
		sb.append(";\n");
		if (!(oom.inner() instanceof ZeroOrMore) && !(oom.inner() instanceof ZeroOrOne)) {
			sb.append("if (n == null) {\n")
					.indent()
					.append("break;\n")
					.deindent()
					.append("}\n");
		}
		sb.append("nodes.add(n);\n")
				.deindent()
				.append("}\n")
				.append("return new " + productionName + "(nodes);\n")
				.deindent()
				.append("}\n");
	}

	private void generateSequence(final String productionName, final Sequence s) {
		sb.append("private " + productionName + " parse_" + productionName + "() {\n")
				.indent()
				.append("stack.push(this.pos);\n");

		final List<Expression> seq = s.nodes();
		for (int i = 0; i < seq.size(); i++) {
			final Expression exp = seq.get(i);
			final String nodeName = "n_" + i;
			final String actualName = globalNodeNames.get(exp);
			final String innerTypeName = resolveTypeName(exp);
			sb.append("final " + innerTypeName + " " + nodeName + " = ");
			if (exp instanceof NonTerminal && isToken(actualName)) {
				sb.append("parseTerminal(TokenType." + actualName + ")");
			} else {
				sb.append("parse_" + actualName + "()");
			}
			sb.append(";\n");
			if (!(exp instanceof ZeroOrMore) && !(exp instanceof ZeroOrOne)) {
				sb.append("if (" + nodeName + " == null) {\n")
						.indent()
						.append("this.pos = stack.pop();\n")
						.append("return null;\n")
						.deindent()
						.append("}\n");
			}
		}

		sb.append("stack.pop();\n")
				.append("return new " + productionName + "("
						+ IntStream.range(0, seq.size()).mapToObj(i -> "n_" + i).collect(Collectors.joining(", "))
						+ ");\n")
				.deindent()
				.append("}\n");
	}

	private void generateNonTerminal(final NonTerminal start, final Expression result) {
		final String typeName = globalNodeNames.get(start);
		final String innerTypeName = resolveTypeName(result);
		sb.append("private " + typeName + " parse_" + typeName + "() {\n")
				.indent()
				.append("final " + innerTypeName + " inner = ");
		if (result instanceof NonTerminal(final String tokenName) && isToken(tokenName)) {
			sb.append("parseTerminal(TokenType." + tokenName + ")");
		} else {
			sb.append("parse_" + globalNodeNames.get(result) + "()");
		}
		sb.append(";\n");

		if (result instanceof ZeroOrOne || result instanceof ZeroOrMore) {
			sb.append("return new " + typeName + "(inner);\n");
		} else {
			sb.append("return inner == null ? null : new " + typeName + "(inner);\n");
		}
		sb.deindent().append("}\n");
	}

	private void generateZeroOrOne(final String productionName, final ZeroOrOne zoo) {
		final String innerName = globalNodeNames.get(zoo.inner());
		final String innerTypeName = resolveTypeName(zoo.inner());
		sb.append("private " + productionName + " parse_" + productionName + "() {\n")
				.indent()
				.append("final " + innerTypeName + " inner = ");
		if (zoo.inner() instanceof NonTerminal && isToken(innerName)) {
			sb.append("parseTerminal(TokenType." + innerName + ")");
		} else {
			sb.append("parse_" + innerName + "()");
		}
		sb.append(";\n")
				.append("return new " + productionName + "(inner);\n")
				.deindent()
				.append("}\n");
	}
}
