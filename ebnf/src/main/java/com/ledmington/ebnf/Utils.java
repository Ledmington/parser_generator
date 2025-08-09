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
import java.util.Map;

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
					final Iterator<Map.Entry<NonTerminal, Expression>> it =
							g.productions().entrySet().iterator();
					do {
						final Map.Entry<NonTerminal, Expression> cur = it.next();
						prettyPrint(sb, new Production(cur.getKey(), cur.getValue()), indentString + indent, indent);
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
			case Or a -> prettyPrintList(sb, "Or", a.nodes(), indentString, indent);
			case ZeroOrMore zom -> prettyPrintContainer(sb, "ZeroOrMore", zom.inner(), indentString, indent);
			case ZeroOrOne zoo -> prettyPrintContainer(sb, "ZeroOrOne", zoo.inner(), indentString, indent);
			case OneOrMore oom -> prettyPrintContainer(sb, "OneOrMore", oom.inner(), indentString, indent);
			case NonTerminal n ->
				sb.append(indentString)
						.append("NonTerminal { ")
						.append(n.name())
						.append(" }");
			case Terminal t ->
				sb.append(indentString)
						.append("Terminal { ")
						.append(Utils.getEscapedString(t.literal()))
						.append(" }");
			default ->
				throw new IllegalArgumentException(String.format(
						"Unknown Node of type %s.", root.getClass().getSimpleName()));
		}
	}

	private static String getEscapedString(final String literal) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < literal.length(); i++) {
			sb.append(getEscapedCharacter(literal.charAt(i)));
		}
		return sb.toString();
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
			default -> "" + ch;
		};
	}
}
