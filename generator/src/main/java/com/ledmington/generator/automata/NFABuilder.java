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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class NFABuilder {

	private State startingState = null;
	private final Map<State, Map<Character, Set<State>>> transitions = new HashMap<>();

	public NFABuilder() {}

	public NFABuilder addTransition(final State from, final char symbol, final State to) {
		Objects.requireNonNull(from);
		Objects.requireNonNull(to);
		if (!transitions.containsKey(from)) {
			transitions.put(from, new HashMap<>());
		}
		final Map<Character, Set<State>> m = transitions.get(from);
		if (!m.containsKey(symbol)) {
			m.put(symbol, new HashSet<>());
		}
		m.get(symbol).add(to);
		return this;
	}

	public Map<Character, Set<State>> neighbors(final State s) {
		return transitions.get(s);
	}

	public NFABuilder start(final State startingState) {
		if (this.startingState != null) {
			throw new IllegalArgumentException("Cannot set starting state twice.");
		}
		this.startingState = Objects.requireNonNull(startingState);
		return this;
	}

	public NFA build() {
		return new NFAImpl(startingState, transitions);
	}

	public void removeAll(final Set<State> unreachableStates) {
		for (final State s : unreachableStates) {
			transitions.remove(s);
		}
		for (final State s : transitions.keySet()) {
			for (final char symbol : transitions.get(s).keySet()) {
				transitions.get(s).get(symbol).removeAll(unreachableStates);
			}
		}
	}
}
