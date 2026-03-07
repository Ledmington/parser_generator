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
package com.ledmington.generator;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.UnknownNonTerminalException;

/** A class to check an EBNF grammar for correctness. */
public final class GrammarChecker {

	private GrammarChecker() {}

	/**
	 * Checks the given EBNF grammar for correctness. Throws a RuntimeException in case it's not.
	 *
	 * @param g The grammar to be checked.
	 */
	public static void check(final Grammar g) {
		Objects.requireNonNull(g);

		// Gather all non-terminals
		final Set<String> allNonTerminals = new HashSet<>();
		for (final Map.Entry<Production, Integer> e : g.getProductions().entrySet()) {
			allNonTerminals.add(e.getKey().start().name());
			allNonTerminals.addAll(Grammar.findAllNonTerminals(e.getKey().result()));
		}

		for (final String name : allNonTerminals) {
			if (g.getProductions().keySet().stream()
					.noneMatch(nt -> nt.start().name().equals(name))) {
				throw new UnknownNonTerminalException(name);
			}
		}

		// remove all skippable lexer symbols
		g.getProductions().entrySet().stream()
				.filter(e -> Production.isSkippable(e.getKey().start().name()))
				.forEach(e -> allNonTerminals.remove(e.getKey().start().name()));
	}
}
