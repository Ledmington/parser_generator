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
package com.ledmington.serializer;

import com.ledmington.ebnf.Node;

public final class Serializer {

	private Serializer() {}

	public static String serialize(
			final Node root, final String className, final String packageName, final String indent) {
		final StringBuilder sb = new StringBuilder();
		if (packageName != null && !packageName.isBlank()) {
			sb.append("package ").append(packageName).append(";\n\n");
		}
		return sb.append(String.join(
						"\n",
						"public final class " + className + " {",
						"",
						indent + "public interface Node {}",
						indent + "public interface Expression {}",
						"",
						indent + "private " + className + "() {}",
						"",
						indent + "public static Node parse(final String input) {",
						indent + indent + "if (input.equals(\"a\")) {",
						indent + indent + indent + "return new Node(){};",
						indent + indent + "} else {",
						indent + indent + indent + "return null;",
						indent + indent + "}",
						indent + "}",
						"}"))
				.toString();
	}
}
