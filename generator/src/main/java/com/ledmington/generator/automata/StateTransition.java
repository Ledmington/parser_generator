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

/**
 * A transition of a finite-state automaton. It is essentially a triplet of (source, destination, symbol).
 *
 * @param from The source of this transition.
 * @param to The destination of this transition.
 * @param character The symbol that needs to be consumed in order to traverse this transition.
 */
public record StateTransition(State from, State to, char character) {

	/** The symbol of an epsilon transition. */
	public static final char EPSILON = (char) -1;

	/**
	 * Creates a new transition with the given source, destination and character.
	 *
	 * @param from The source of this transition.
	 * @param to The destination of this transition.
	 * @param character The symbol that needs to be consumed in order to traverse this transition.
	 */
	public StateTransition {
		Objects.requireNonNull(from);
		Objects.requireNonNull(to);
	}
}
