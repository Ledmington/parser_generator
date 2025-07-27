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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.Terminal;

public final class TestGrammarChecker {
	@Test
	void multipleStartSymbols() {
		assertThrows(
				NoUniqueStartSymbolException.class,
				() -> GrammarChecker.check(new Grammar(Map.of(
						new NonTerminal("S"), new Terminal("a"),
						new NonTerminal("T"), new Terminal("b")))));
	}

	@Test
	void unusableNonTerminals() {
		assertThrows(
				UnknownNonTerminalException.class,
				() -> GrammarChecker.check(new Grammar(Map.of(new NonTerminal("S"), new NonTerminal("T")))));
	}

	@Test
	void duplicatedNonTerminals() {
		assertThrows(
				DuplicatedNonTerminalException.class,
				() -> GrammarChecker.check(new Grammar(Map.of(
						new NonTerminal("S"), new Terminal("a"),
						new NonTerminal("S"), new Terminal("b")))));
	}
}
