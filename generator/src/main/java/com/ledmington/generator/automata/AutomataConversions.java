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

import com.ledmington.ebnf.Grammar;

public final class AutomataConversions {

	private AutomataConversions() {}

	public static NFA convertGrammarToEpsilonNFA(final Grammar g) {
		final StateFactory stateFactory = new StateFactory();
		final GrammarToEpsilonNFA g2enfa = new GrammarToEpsilonNFA(stateFactory);
		return g2enfa.convert(g);
	}

	public static NFA convertGrammarToNFA(final Grammar g) {
		final StateFactory stateFactory = new StateFactory();
		final GrammarToEpsilonNFA g2enfa = new GrammarToEpsilonNFA(stateFactory);
		final EpsilonNFAToNFA enfa2nfa = new EpsilonNFAToNFA(stateFactory);
		return enfa2nfa.convert(g2enfa.convert(g));
	}

	public static DFA convertGrammarToDFA(final Grammar g) {
		final StateFactory stateFactory = new StateFactory();
		final GrammarToEpsilonNFA g2enfa = new GrammarToEpsilonNFA(stateFactory);
		final EpsilonNFAToNFA enfa2nfa = new EpsilonNFAToNFA(stateFactory);
		final NFAToDFA nfa2dfa = new NFAToDFA(stateFactory);
		return nfa2dfa.convert(enfa2nfa.convert(g2enfa.convert(g)));
	}

	public static DFA convertGrammarToMinimizedDFA(final Grammar g) {
		final StateFactory stateFactory = new StateFactory();
		final GrammarToEpsilonNFA g2enfa = new GrammarToEpsilonNFA(stateFactory);
		final EpsilonNFAToNFA enfa2nfa = new EpsilonNFAToNFA(stateFactory);
		final NFAToDFA nfa2dfa = new NFAToDFA(stateFactory);
		final DFAMinimizer DFAmin = new DFAMinimizer(stateFactory);
		return DFAmin.minimize(nfa2dfa.convert(enfa2nfa.convert(g2enfa.convert(g))));
	}
}
