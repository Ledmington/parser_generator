/*
 * parser-gen - Parser Generator
 * Copyright (C) 2023-2025 Filippo Barbari <filippo.barbari@gmail.com>
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
package com.ledmington.parser;

import java.util.Objects;

public final class Grammar {

	private final String name;
	private final String startingSymbol;

	public Grammar(final String name, final String startingSymbol) {
		this.name = Objects.requireNonNull(name);
		this.startingSymbol = Objects.requireNonNull(startingSymbol);
	}

	public int hashCode() {
		int h = 17;
		h = 31 * h + name.hashCode();
		h = 31 * h + startingSymbol.hashCode();
		return h;
	}

	public String toString() {
		return "Grammar(name=" + name + ";start=" + startingSymbol + ")";
	}

	public boolean equals(final Object other) {
		if (other == null) {
			return false;
		}
		if (this == other) {
			return true;
		}
		if (!(other instanceof final Grammar g)) {
			return false;
		}
		return this.name.equals(g.name) && this.startingSymbol.equals(g.startingSymbol);
	}
}
