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

import java.util.Objects;

public final class IndentedStringBuilder {

	private final String indent;
	private int indentLevel = 0;
	private final StringBuilder sb = new StringBuilder();

	public IndentedStringBuilder(final String indent) {
		this.indent = Objects.requireNonNull(indent);
	}

	private void add(final String s) {
		if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
			sb.append(indent.repeat(indentLevel));
		}

		if (s == null) {
			sb.append("null");
			return;
		}

		for (int i = 0; i < s.length(); i++) {
			final char ch = s.charAt(i);
			sb.append(ch);
			if (ch == '\n' && i < s.length() - 1) {
				sb.append(indent.repeat(indentLevel));
			}
		}
	}

	public IndentedStringBuilder append(final String s) {
		add(s);
		return this;
	}

	public IndentedStringBuilder append(final char c) {
		add(String.valueOf(c));
		return this;
	}

	public IndentedStringBuilder append(final int x) {
		add(String.valueOf(x));
		return this;
	}

	public IndentedStringBuilder indent() {
		indentLevel++;
		return this;
	}

	public IndentedStringBuilder deindent() {
		indentLevel--;
		return this;
	}

	public String toString() {
		return sb.toString();
	}
}
