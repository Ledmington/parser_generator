/*
 * parser-gen - Parser Generator
 * Copyright (C) 2025-2026 Filippo Barbari <filippo.barbari@gmail.com>
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
package com.ledmington.bnf;

import com.ledmington.ebnf.Grammar;

/** A class to convert and EBNF grammar into a BNF grammar. */
public final class Converter {

	private Converter() {}

	/**
	 * Converts the given EBNF grammar into a BNF one.
	 *
	 * @param g The EBNF grammar to be converted.
	 * @return The converted BNF grammar.
	 */
	public static BNFGrammar convertToBnf(final Grammar g) {
		throw new Error("Not implemented.");
	}
}
