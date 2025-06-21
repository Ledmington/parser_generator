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

public final class Utils {

	private Utils() {}

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
					prettyPrint(sb, g.productions().getFirst(), indentString + indent, indent);
					sb.append('\n');
					for (int i = 1; i < g.productions().size(); i++) {
						prettyPrint(sb, g.productions().get(i), indentString + indent, indent);
						sb.append('\n');
					}
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
			case Concatenation c -> {
				sb.append(indentString).append("Concatenation {\n");
				if (!c.nodes().isEmpty()) {
					prettyPrint(sb, c.nodes().getFirst(), indentString + indent, indent);
					sb.append('\n');
					for (int i = 1; i < c.nodes().size(); i++) {
						prettyPrint(sb, c.nodes().get(i), indentString + indent, indent);
						sb.append('\n');
					}
				}
				sb.append(indentString).append("}");
			}
			case Alternation a -> {
				sb.append(indentString).append("Alternation {\n");
				if (!a.nodes().isEmpty()) {
					prettyPrint(sb, a.nodes().getFirst(), indentString + indent, indent);
					sb.append('\n');
					for (int i = 1; i < a.nodes().size(); i++) {
						prettyPrint(sb, a.nodes().get(i), indentString + indent, indent);
						sb.append('\n');
					}
				}
				sb.append(indentString).append("}");
			}
			case Repetition r -> {
				sb.append(indentString).append("Repetition {\n");
				prettyPrint(sb, r.inner(), indentString + indent, indent);
				sb.append('\n').append(indentString).append("}");
			}
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
}
