package com.ledmington.generator.automata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DFABuilder {

	private  State startingState=null;
	private Map<State, Map<Character, State>> transitions=new HashMap<>();

	public DFABuilder(){}

	public DFABuilder addTransition(final State from,final char symbol,final State to) {
		Objects.requireNonNull(from);
		Objects.requireNonNull(to);
		if(!transitions.containsKey(from)){
			transitions.put(from,new HashMap<>());
		}
		transitions.get(from).put(symbol,to);
		return this;
	}

	public DFA build() {
		return new DFA(startingState,transitions);
	}
}
