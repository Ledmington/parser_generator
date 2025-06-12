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

/** A set of tokens represented by single characters. */
public enum Symbols implements Token {

	/** The semicolon symbol (U+003B). */
	SEMICOLON,

	/** The comma symbol (U+002C). */
	COMMA,

	/** The vertical line (or pipe?) symbol (U+007C). */
	VERTICAL_LINE,

	/** The left square bracket symbol (U+005B). */
	LEFT_SQUARE_BRACKET,

	/** The right square bracket symbol (U+005D). */
	RIGHT_SQUARE_BRACKET,

	/** The left curly bracket symbol (U+007B). */
	LEFT_CURLY_BRACKET,

	/** The right curly bracket symbol (U+07D). */
	RIGHT_CURLY_BRACKET,

	/** The equal sign symbol (U+003D). */
	EQUAL_SIGN
}
