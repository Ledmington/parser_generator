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

/** The proper RuntimeException for a grammar which has non-terminal symbols without a corresponding production. */
public final class UnusableNonTerminalException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 8643487188267532790L;

	/**
	 * Creates a new instance with a message for the given non-terminal symbol.
	 *
	 * @param nonTerminalName The name of the non-terminal symbol which does not have a corresponding production.
	 */
	public UnusableNonTerminalException(final String nonTerminalName) {
		super(String.format("The non-terminal '%s' does not have a production.", nonTerminalName));
	}
}
