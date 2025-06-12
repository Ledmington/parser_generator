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

/** A common type for all elements in an EBNF grammar. */
public interface Node {

	/**
	 * Returns an indented String for easier analysis of the contents of the given Node. Note: pass a String with
	 * whitespaces and/or tabs as indentation.
	 *
	 * @param indent The String to be placed for each indentation level.
	 * @return An indented multi-line String representing the contents of this Node.
	 */
	// TODO: do we really need the user to pass an arbitrary string as the indentation?
	String prettyPrint(final String indent);
}
