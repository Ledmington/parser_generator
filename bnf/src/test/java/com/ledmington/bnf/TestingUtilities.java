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

import java.util.HashMap;
import java.util.Map;

import com.ledmington.ebnf.Expression;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.OneOrMore;
import com.ledmington.ebnf.Or;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.ZeroOrMore;
import com.ledmington.ebnf.ZeroOrOne;

public final class TestingUtilities {

	private TestingUtilities() {}

	public static Grammar ebnf(final Map<String, Expression> productions) {
		final Map<Production, Integer> p = new HashMap<>();
		int index = 0;
		for (final Map.Entry<String, Expression> e : productions.entrySet()) {
			p.put(new Production(new NonTerminal(e.getKey()), e.getValue()), index++);
		}
		return new Grammar(p);
	}

	public static BNFGrammar bnf(final Map<String, BNFExpression> productions) {
		final Map<BNFNonTerminal, BNFExpression> prod = new HashMap<>();
		for (final Map.Entry<String, BNFExpression> p : productions.entrySet()) {
			prod.put(new BNFNonTerminal(p.getKey()), p.getValue());
		}
		return new BNFGrammar(prod);
	}

	public static NonTerminal nt(final String literal) {
		return new NonTerminal(literal);
	}

	public static Terminal t(final String literal) {
		return new Terminal(literal);
	}

	public static ZeroOrOne zero_or_one(final Expression inner) {
		return new ZeroOrOne(inner);
	}

	public static ZeroOrMore zero_or_more(final Expression inner) {
		return new ZeroOrMore(inner);
	}

	public static OneOrMore one_or_more(final Expression inner) {
		return new OneOrMore(inner);
	}

	public static Sequence seq(final Expression... expressions) {
		return new Sequence(expressions);
	}

	public static Or or(final Expression... expressions) {
		return new Or(expressions);
	}
}
