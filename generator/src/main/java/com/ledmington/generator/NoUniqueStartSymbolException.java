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

import java.io.Serial;

/** The proper RuntimeException for a case in which there is no clear start symbol in a given grammar. */
public final class NoUniqueStartSymbolException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 5878803702087464360L;

	/** Creates a new instance with a standard message. */
	public NoUniqueStartSymbolException() {
		super("No starting symbol found in the grammar.");
	}

	/**
	 * Creates a new NoUniqueStartSymbolException with the given message.
	 *
	 * @param message The message of this exception.
	 */
	public NoUniqueStartSymbolException(final String message) {
		super(message);
	}
}
