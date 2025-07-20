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
package com.ledmington.generator.automata;

import java.util.Objects;

public final class State {

	private static int ID = 1;
	private final String name;
	private final boolean isAccepting;

	public State(final String name, final boolean isAccepting) {
		Objects.requireNonNull(name);
		if (name.isBlank()) {
			throw new IllegalArgumentException("Empty name.");
		}
		this.name = name;
		this.isAccepting = isAccepting;
	}

	public State() {
		this("S" + (ID++), false);
	}

	public State(final boolean isFinal) {
		this("S" + (ID++), isFinal);
	}

	public String name() {
		return name;
	}

	public boolean isAccepting() {
		return isAccepting;
	}

	@Override
	public String toString() {
		return "State[name=" + name + ";isAccepting=" + isAccepting + "]";
	}

	@Override
	public int hashCode() {
		int h = 17;
		h = 31 * h + name.hashCode();
		h = 31 * h + (isAccepting ? 1 : 0);
		return h;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == null) {
			return false;
		}
		if (this == other) {
			return true;
		}
		if (!(other instanceof State s)) {
			return false;
		}
		return this.name.equals(s.name) && this.isAccepting == s.isAccepting;
	}
}
