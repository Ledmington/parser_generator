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

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class NFAImpl implements NFA {

	private final State start;
	private final Map<State, Map<Character, Set<State>>> transitions;

	public NFAImpl(final State startingState, final Map<State, Map<Character, Set<State>>> transitions) {
		this.start = Objects.requireNonNull(startingState);
		this.transitions = Objects.requireNonNull(transitions);
	}

	@Override
	public State startingState() {
		return start;
	}

	@Override
	public Set<State> states() {
		final Set<State> allStates = new HashSet<>();
		for (final Map.Entry<State, Map<Character, Set<State>>> e : transitions.entrySet()) {
			allStates.add(e.getKey());
			for (final Map.Entry<Character, Set<State>> e2 : e.getValue().entrySet()) {
				allStates.addAll(e2.getValue());
			}
		}
		return allStates;
	}

	@Override
	public Map<Character, Set<State>> neighbors(final State s) {
		return transitions.get(s);
	}

	@Override
	public String toString() {
		return "NFA[start=" + start + ";transitions=" + transitions + "]";
	}

	@Override
	public int hashCode() {
		int h = 17;
		h = 31 * h + start.hashCode();
		h = 31 * h + transitions.hashCode();
		return h;
	}

	@Override
	public boolean equals(final Object other) {
		if (other == null) {
			return false;
		}
		if (this == other) {
			return true;
		}
		if (!(other instanceof final NFAImpl nfa)) {
			return false;
		}
		return this.start.equals(nfa.start) && this.transitions.equals(nfa.transitions);
	}
}
