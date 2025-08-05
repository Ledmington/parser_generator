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

	private final State startingState;
	private final Map<State, Map<Character, Set<State>>> transitions;

	public NFAImpl(final State startingState, final Map<State, Map<Character, Set<State>>> transitions) {
		this.startingState = Objects.requireNonNull(startingState);
		this.transitions = Objects.requireNonNull(transitions);
	}

	@Override
	public State startingState() {
		return startingState;
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
}
