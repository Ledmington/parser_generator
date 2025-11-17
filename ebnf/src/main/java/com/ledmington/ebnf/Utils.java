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

import java.util.List;
import java.util.Map.Entry;

/** A collection of various utilities. */
public final class Utils {

	private static final char VERTICAL_LINE = '│';
	private static final char HORIZONTAL_LINE = '─';
	private static final char JOINT = '├';
	private static final char ANGLE = '└';

	private Utils() {}

	/**
	 * Returns a String representing the given tree with the given level of indentation.
	 *
	 * @param root The root of the tree to be serialized.
	 * @return An indented String representation of the tree.
	 */
	public static String prettyPrint(final Node root) {
		final StringBuilder sb = new StringBuilder();
		prettyPrint(sb, root, "", "");
		return sb.toString();
	}

	private static void prettyPrint(
			final StringBuilder sb, final Node n, final String indent, final String continuationIndent) {
		switch (n) {
			case Grammar g -> {
				sb.append("grammar\n");
				if (g.productions().isEmpty()) {
					return;
				}
				final List<Production> productions = g.productions().entrySet().stream()
						.sorted(Entry.comparingByValue())
						.map(Entry::getKey)
						.toList();
				final int len = productions.size();
				for (int i = 0; i < len - 1; i++) {
					prettyPrint(
							sb,
							productions.get(i),
							getJointIndent(continuationIndent),
							getLineIndent(continuationIndent));
				}
				prettyPrint(
						sb,
						productions.getLast(),
						getAngleIndent(continuationIndent),
						getEmptyIndent(continuationIndent));
			}
			case Production p ->
				prettyPrintList(sb, "production", List.of(p.start(), p.result()), indent, continuationIndent);
			case Terminal t ->
				sb.append(indent)
						.append("terminal '")
						.append(Utils.getEscapedString(t.literal()))
						.append("'\n");
			case NonTerminal nt ->
				sb.append(indent).append("non_terminal '").append(nt.name()).append("'\n");
			case Or or -> prettyPrintList(sb, "or", or.nodes(), indent, continuationIndent);
			case Sequence s -> prettyPrintList(sb, "sequence", s.nodes(), indent, continuationIndent);
			case ZeroOrOne zoo -> prettyPrintContainer(sb, "zero_or_one", zoo.inner(), indent, continuationIndent);
			case ZeroOrMore zom -> prettyPrintContainer(sb, "zero_or_more", zom.inner(), indent, continuationIndent);
			case OneOrMore oom -> prettyPrintContainer(sb, "one_or_more", oom.inner(), indent, continuationIndent);
			default -> throw new IllegalArgumentException(String.format("Unknown node: '%s'.", n));
		}
	}

	private static String getEmptyIndent(final String s) {
		return s + "   ";
	}

	private static String getLineIndent(final String s) {
		return s + ' ' + VERTICAL_LINE + ' ';
	}

	private static void prettyPrintContainer(
			final StringBuilder sb,
			final String nodeName,
			final Expression inner,
			final String indentString,
			final String indent) {
		prettyPrintList(sb, nodeName, List.of(inner), indentString, indent);
	}

	private static void prettyPrintList(
			final StringBuilder sb,
			final String nodeName,
			final List<Expression> nodes,
			final String indent,
			final String continuationIndent) {
		sb.append(indent).append(nodeName).append('\n');
		if (nodes.isEmpty()) {
			return;
		}
		for (int i = 0; i < nodes.size() - 1; i++) {
			prettyPrint(sb, nodes.get(i), getJointIndent(continuationIndent), getLineIndent(continuationIndent));
		}
		prettyPrint(sb, nodes.getLast(), getAngleIndent(continuationIndent), getEmptyIndent(continuationIndent));
	}

	private static String getJointIndent(final String s) {
		return s + " " + JOINT + HORIZONTAL_LINE;
	}

	private static String getAngleIndent(final String s) {
		return s + " " + ANGLE + HORIZONTAL_LINE;
	}

	/**
	 * Escapes all the characters in the given string, if needed.
	 *
	 * @param s The string to be escaped
	 * @return The same input with all characters escaped, if needed.
	 */
	public static String getEscapedString(final String s) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			sb.append(getEscapedCharacter(s.charAt(i)));
		}
		return sb.toString();
	}

	/**
	 * Escapes a single character, if needed.
	 *
	 * @param ch The character to be escaped.
	 * @return The same input character escaped, if needed.
	 */
	public static String getEscapedCharacter(final char ch) {
		return switch (ch) {
			case '\'' -> "\\'";
			case '\"' -> "\\\"";
			case '\\' -> "\\\\";
			case '\t' -> "\\t";
			case '\n' -> "\\n";
			case '\r' -> "\\r";
			case '\0' -> "\\u0000";
			case '\b' -> "\\b";
			case '\f' -> "\\f";
			default -> String.valueOf(ch);
		};
	}
}
