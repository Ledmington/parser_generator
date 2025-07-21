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

import java.util.Iterator;
import java.util.List;

/** A collection of various utilities. */
public final class Utils {

	private Utils() {}

	/**
	 * Returns a String representing the given tree with the given level of indentation.
	 *
	 * @param root The root of the tree to be serialized.
	 * @param indent The level of indentation to use.
	 * @return An indented String representation of the tree.
	 */
	public static String prettyPrint(final Node root, final String indent) {
		final StringBuilder sb = new StringBuilder();
		prettyPrint(sb, root, "", indent);
		return sb.toString();
	}

	private static void prettyPrint(
			final StringBuilder sb, final Node root, final String indentString, final String indent) {
		switch (root) {
			case Grammar g -> {
				sb.append(indentString).append("Grammar {\n");
				if (!g.productions().isEmpty()) {
					final Iterator<Production> it = g.productions().iterator();
					do {
						prettyPrint(sb, it.next(), indentString + indent, indent);
						sb.append('\n');
					} while (it.hasNext());
				}
				sb.append(indentString).append("}");
			}
			case Production p -> {
				sb.append(indentString).append("Production {\n");
				prettyPrint(sb, p.start(), indentString + indent, indent);
				sb.append('\n');
				prettyPrint(sb, p.result(), indentString + indent, indent);
				sb.append('\n').append(indentString).append("}");
			}
			case Sequence c -> prettyPrintList(sb, "Sequence", c.nodes(), indentString, indent);
			case Alternation a -> prettyPrintList(sb, "Alternation", a.nodes(), indentString, indent);
			case Repetition r -> prettyPrintContainer(sb, "Repetition", r.inner(), indentString, indent);
			case OptionalNode o -> prettyPrintContainer(sb, "OptionalNode", o.inner(), indentString, indent);
			case NonTerminal n ->
				sb.append(indentString)
						.append("NonTerminal { ")
						.append(n.name())
						.append(" }");
			case Terminal t ->
				sb.append(indentString)
						.append("Terminal { ")
						.append(t.literal())
						.append(" }");
			default ->
				throw new IllegalArgumentException(String.format(
						"Unknown Node of type %s.", root.getClass().getSimpleName()));
		}
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
			final String indentString,
			final String indent) {
		sb.append(indentString).append(nodeName).append(" {\n");
		if (!nodes.isEmpty()) {
			prettyPrint(sb, nodes.getFirst(), indentString + indent, indent);
			sb.append('\n');
			for (int i = 1; i < nodes.size(); i++) {
				prettyPrint(sb, nodes.get(i), indentString + indent, indent);
				sb.append('\n');
			}
		}
		sb.append(indentString).append("}");
	}

	private static boolean needsEscaping(final char ch) {
		return ch == '\'' || ch == '\"' || ch == '\\';
	}

	private static boolean needsEscaping(final String s) {
		for (int i = 0; i < s.length(); i++) {
			if (needsEscaping(s.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	public static String getEscapeCharacter(final char ch) {
		return (needsEscaping(ch) ? "\\" : "") + ch;
	}

	private static String escape(final String s) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			if (needsEscaping(s.charAt(i))) {
				sb.append('\\');
			}
			sb.append(s.charAt(i));
		}
		return sb.toString();
	}

	public static String getEscapedString(final String literal) {
		if (needsEscaping(literal)) {
			return escape(literal);
		}
		return literal;
	}
}
