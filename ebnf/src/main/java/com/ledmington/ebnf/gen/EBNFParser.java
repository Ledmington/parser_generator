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
package com.ledmington.ebnf.gen;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

public final class EBNFParser {
	private Token[] v = null;
	private int pos = 0;
	private final Stack<Integer> stack = new Stack<>();

	public interface Node {
		String name();
	}

	public record Terminal(String literal) implements Node {
		@Override
		public String name() {
			return "Terminal";
		}
	}

	public interface NonTerminal extends Node {
		Node match();
	}

	public interface ZeroOrOne extends Node {
		Node match();
	}

	public interface Sequence extends Node {
		List<Node> nodes();
	}

	public interface ZeroOrMore extends Node {
		List<Node> nodes();
	}

	public interface OneOrMore extends Node {
		List<Node> nodes();
	}

	public interface Or extends Node {
		Node match();
	}

	public record grammar(List<production> production) implements OneOrMore {
		@Override
		public String name() {
			return "grammar";
		}

		@Override
		public List<Node> nodes() {
			return production.stream().map(n -> (Node) n).toList();
		}
	}

	public record lexer_alternation(lexer_concatenation lexer_concatenation, zero_or_more_0 zero_or_more_0)
			implements Sequence {
		@Override
		public String name() {
			return "lexer_alternation";
		}

		@Override
		public List<Node> nodes() {
			return List.of(lexer_concatenation, zero_or_more_0);
		}
	}

	public record zero_or_more_0(List<sequence_0> sequence_0) implements ZeroOrMore {
		@Override
		public String name() {
			return "zero_or_more_0";
		}

		@Override
		public List<Node> nodes() {
			return sequence_0.stream().map(n -> (Node) n).toList();
		}
	}

	public record sequence_0(Terminal VERTICAL_LINE, lexer_concatenation lexer_concatenation) implements Sequence {
		@Override
		public String name() {
			return "sequence_0";
		}

		@Override
		public List<Node> nodes() {
			return List.of(VERTICAL_LINE, lexer_concatenation);
		}
	}

	public record lexer_concatenation(lexer_repetition lexer_repetition, zero_or_more_1 zero_or_more_1)
			implements Sequence {
		@Override
		public String name() {
			return "lexer_concatenation";
		}

		@Override
		public List<Node> nodes() {
			return List.of(lexer_repetition, zero_or_more_1);
		}
	}

	public record zero_or_more_1(List<lexer_repetition> lexer_repetition) implements ZeroOrMore {
		@Override
		public String name() {
			return "zero_or_more_1";
		}

		@Override
		public List<Node> nodes() {
			return lexer_repetition.stream().map(n -> (Node) n).toList();
		}
	}

	public record lexer_expression(lexer_alternation lexer_alternation) implements NonTerminal {
		@Override
		public String name() {
			return "lexer_expression";
		}

		@Override
		public Node match() {
			return lexer_alternation;
		}
	}

	public record lexer_primary(Node match) implements Or {
		@Override
		public String name() {
			return "lexer_primary";
		}
	}

	public record sequence_1(Terminal LEFT_PARENTHESIS, lexer_expression lexer_expression, Terminal RIGHT_PARENTHESIS)
			implements Sequence {
		@Override
		public String name() {
			return "sequence_1";
		}

		@Override
		public List<Node> nodes() {
			return List.of(LEFT_PARENTHESIS, lexer_expression, RIGHT_PARENTHESIS);
		}
	}

	public record lexer_production(Terminal LEXER_SYMBOL, Terminal EQUALS, lexer_expression lexer_expression)
			implements Sequence {
		@Override
		public String name() {
			return "lexer_production";
		}

		@Override
		public List<Node> nodes() {
			return List.of(LEXER_SYMBOL, EQUALS, lexer_expression);
		}
	}

	public record lexer_repetition(lexer_primary lexer_primary, zero_or_one_0 zero_or_one_0) implements Sequence {
		@Override
		public String name() {
			return "lexer_repetition";
		}

		@Override
		public List<Node> nodes() {
			return List.of(lexer_primary, zero_or_one_0);
		}
	}

	public record zero_or_one_0(quantifier quantifier) implements ZeroOrOne {
		@Override
		public String name() {
			return "zero_or_one_0";
		}

		@Override
		public Node match() {
			return quantifier;
		}
	}

	public record parser_expression(Node match) implements Or {
		@Override
		public String name() {
			return "parser_expression";
		}
	}

	public record sequence_2(Terminal LEFT_PARENTHESIS, parser_expression parser_expression, Terminal RIGHT_PARENTHESIS)
			implements Sequence {
		@Override
		public String name() {
			return "sequence_2";
		}

		@Override
		public List<Node> nodes() {
			return List.of(LEFT_PARENTHESIS, parser_expression, RIGHT_PARENTHESIS);
		}
	}

	public record sequence_3(parser_expression parser_expression, Terminal QUESTION_MARK) implements Sequence {
		@Override
		public String name() {
			return "sequence_3";
		}

		@Override
		public List<Node> nodes() {
			return List.of(parser_expression, QUESTION_MARK);
		}
	}

	public record sequence_4(parser_expression parser_expression, Terminal PLUS) implements Sequence {
		@Override
		public String name() {
			return "sequence_4";
		}

		@Override
		public List<Node> nodes() {
			return List.of(parser_expression, PLUS);
		}
	}

	public record sequence_5(parser_expression parser_expression, Terminal ASTERISK) implements Sequence {
		@Override
		public String name() {
			return "sequence_5";
		}

		@Override
		public List<Node> nodes() {
			return List.of(parser_expression, ASTERISK);
		}
	}

	public record sequence_6(
			parser_expression parser_expression_0, Terminal VERTICAL_LINE, parser_expression parser_expression_1)
			implements Sequence {
		@Override
		public String name() {
			return "sequence_6";
		}

		@Override
		public List<Node> nodes() {
			return List.of(parser_expression_0, VERTICAL_LINE, parser_expression_1);
		}
	}

	public record sequence_7(parser_expression parser_expression_0, parser_expression parser_expression_1)
			implements Sequence {
		@Override
		public String name() {
			return "sequence_7";
		}

		@Override
		public List<Node> nodes() {
			return List.of(parser_expression_0, parser_expression_1);
		}
	}

	public record parser_production(Terminal PARSER_SYMBOL, Terminal EQUALS, parser_expression parser_expression)
			implements Sequence {
		@Override
		public String name() {
			return "parser_production";
		}

		@Override
		public List<Node> nodes() {
			return List.of(PARSER_SYMBOL, EQUALS, parser_expression);
		}
	}

	public record production(or_0 or_0, Terminal SEMICOLON) implements Sequence {
		@Override
		public String name() {
			return "production";
		}

		@Override
		public List<Node> nodes() {
			return List.of(or_0, SEMICOLON);
		}
	}

	public record or_0(Node match) implements Or {
		@Override
		public String name() {
			return "or_0";
		}
	}

	public record quantifier(Node match) implements Or {
		@Override
		public String name() {
			return "quantifier";
		}
	}

	private Terminal parseTerminal(final TokenType expected) {
		if (pos < v.length && v[pos].type() == expected) {
			return new Terminal(v[pos++].content());
		}
		return null;
	}

	private grammar parse_grammar() {
		final production n_0 = parse_production();
		if (n_0 == null) {
			return null;
		}
		final List<production> nodes = new ArrayList<>();
		nodes.add(n_0);
		while (true) {
			final production n = parse_production();
			if (n == null) {
				break;
			}
			nodes.add(n);
		}
		return new grammar(nodes);
	}

	private lexer_alternation parse_lexer_alternation() {
		stack.push(this.pos);
		final lexer_concatenation n_0 = parse_lexer_concatenation();
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final zero_or_more_0 n_1 = parse_zero_or_more_0();
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new lexer_alternation(n_0, n_1);
	}

	private zero_or_more_0 parse_zero_or_more_0() {
		final List<sequence_0> nodes = new ArrayList<>();
		while (true) {
			final sequence_0 n = parse_sequence_0();
			if (n == null) {
				break;
			}
			nodes.add(n);
		}
		return new zero_or_more_0(nodes);
	}

	private sequence_0 parse_sequence_0() {
		stack.push(this.pos);
		final Terminal n_0 = parseTerminal(TokenType.VERTICAL_LINE);
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final lexer_concatenation n_1 = parse_lexer_concatenation();
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new sequence_0(n_0, n_1);
	}

	private lexer_concatenation parse_lexer_concatenation() {
		stack.push(this.pos);
		final lexer_repetition n_0 = parse_lexer_repetition();
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final zero_or_more_1 n_1 = parse_zero_or_more_1();
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new lexer_concatenation(n_0, n_1);
	}

	private zero_or_more_1 parse_zero_or_more_1() {
		final List<lexer_repetition> nodes = new ArrayList<>();
		while (true) {
			final lexer_repetition n = parse_lexer_repetition();
			if (n == null) {
				break;
			}
			nodes.add(n);
		}
		return new zero_or_more_1(nodes);
	}

	private lexer_expression parse_lexer_expression() {
		final lexer_alternation inner = parse_lexer_alternation();
		return inner == null ? null : new lexer_expression(inner);
	}

	private lexer_primary parse_lexer_primary() {
		final Terminal n_0 = parseTerminal(TokenType.STRING_LITERAL);
		if (n_0 != null) {
			return new lexer_primary(n_0);
		}
		final sequence_1 n_1 = parse_sequence_1();
		if (n_1 != null) {
			return new lexer_primary(n_1);
		}
		return null;
	}

	private sequence_1 parse_sequence_1() {
		stack.push(this.pos);
		final Terminal n_0 = parseTerminal(TokenType.LEFT_PARENTHESIS);
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final lexer_expression n_1 = parse_lexer_expression();
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		final Terminal n_2 = parseTerminal(TokenType.RIGHT_PARENTHESIS);
		if (n_2 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new sequence_1(n_0, n_1, n_2);
	}

	private lexer_production parse_lexer_production() {
		stack.push(this.pos);
		final Terminal n_0 = parseTerminal(TokenType.LEXER_SYMBOL);
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final Terminal n_1 = parseTerminal(TokenType.EQUALS);
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		final lexer_expression n_2 = parse_lexer_expression();
		if (n_2 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new lexer_production(n_0, n_1, n_2);
	}

	private lexer_repetition parse_lexer_repetition() {
		stack.push(this.pos);
		final lexer_primary n_0 = parse_lexer_primary();
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final zero_or_one_0 n_1 = parse_zero_or_one_0();
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new lexer_repetition(n_0, n_1);
	}

	private zero_or_one_0 parse_zero_or_one_0() {
		final quantifier inner = parse_quantifier();
		return new zero_or_one_0(inner);
	}

	private parser_expression parse_parser_expression() {
		final Terminal n_0 = parseTerminal(TokenType.PARSER_SYMBOL);
		if (n_0 != null) {
			return new parser_expression(n_0);
		}
		final Terminal n_1 = parseTerminal(TokenType.LEXER_SYMBOL);
		if (n_1 != null) {
			return new parser_expression(n_1);
		}
		final sequence_2 n_2 = parse_sequence_2();
		if (n_2 != null) {
			return new parser_expression(n_2);
		}
		final sequence_3 n_3 = parse_sequence_3();
		if (n_3 != null) {
			return new parser_expression(n_3);
		}
		final sequence_4 n_4 = parse_sequence_4();
		if (n_4 != null) {
			return new parser_expression(n_4);
		}
		final sequence_5 n_5 = parse_sequence_5();
		if (n_5 != null) {
			return new parser_expression(n_5);
		}
		final sequence_6 n_6 = parse_sequence_6();
		if (n_6 != null) {
			return new parser_expression(n_6);
		}
		final sequence_7 n_7 = parse_sequence_7();
		if (n_7 != null) {
			return new parser_expression(n_7);
		}
		return null;
	}

	private sequence_2 parse_sequence_2() {
		stack.push(this.pos);
		final Terminal n_0 = parseTerminal(TokenType.LEFT_PARENTHESIS);
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final parser_expression n_1 = parse_parser_expression();
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		final Terminal n_2 = parseTerminal(TokenType.RIGHT_PARENTHESIS);
		if (n_2 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new sequence_2(n_0, n_1, n_2);
	}

	private sequence_3 parse_sequence_3() {
		stack.push(this.pos);
		final parser_expression n_0 = parse_parser_expression();
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final Terminal n_1 = parseTerminal(TokenType.QUESTION_MARK);
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new sequence_3(n_0, n_1);
	}

	private sequence_4 parse_sequence_4() {
		stack.push(this.pos);
		final parser_expression n_0 = parse_parser_expression();
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final Terminal n_1 = parseTerminal(TokenType.PLUS);
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new sequence_4(n_0, n_1);
	}

	private sequence_5 parse_sequence_5() {
		stack.push(this.pos);
		final parser_expression n_0 = parse_parser_expression();
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final Terminal n_1 = parseTerminal(TokenType.ASTERISK);
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new sequence_5(n_0, n_1);
	}

	private sequence_6 parse_sequence_6() {
		stack.push(this.pos);
		final parser_expression n_0 = parse_parser_expression();
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final Terminal n_1 = parseTerminal(TokenType.VERTICAL_LINE);
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		final parser_expression n_2 = parse_parser_expression();
		if (n_2 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new sequence_6(n_0, n_1, n_2);
	}

	private sequence_7 parse_sequence_7() {
		stack.push(this.pos);
		final parser_expression n_0 = parse_parser_expression();
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final parser_expression n_1 = parse_parser_expression();
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new sequence_7(n_0, n_1);
	}

	private parser_production parse_parser_production() {
		stack.push(this.pos);
		final Terminal n_0 = parseTerminal(TokenType.PARSER_SYMBOL);
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final Terminal n_1 = parseTerminal(TokenType.EQUALS);
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		final parser_expression n_2 = parse_parser_expression();
		if (n_2 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new parser_production(n_0, n_1, n_2);
	}

	private production parse_production() {
		stack.push(this.pos);
		final or_0 n_0 = parse_or_0();
		if (n_0 == null) {
			this.pos = stack.pop();
			return null;
		}
		final Terminal n_1 = parseTerminal(TokenType.SEMICOLON);
		if (n_1 == null) {
			this.pos = stack.pop();
			return null;
		}
		stack.pop();
		return new production(n_0, n_1);
	}

	private or_0 parse_or_0() {
		final parser_production n_0 = parse_parser_production();
		if (n_0 != null) {
			return new or_0(n_0);
		}
		final lexer_production n_1 = parse_lexer_production();
		if (n_1 != null) {
			return new or_0(n_1);
		}
		return null;
	}

	private quantifier parse_quantifier() {
		final Terminal n_0 = parseTerminal(TokenType.QUESTION_MARK);
		if (n_0 != null) {
			return new quantifier(n_0);
		}
		final Terminal n_1 = parseTerminal(TokenType.PLUS);
		if (n_1 != null) {
			return new quantifier(n_1);
		}
		final Terminal n_2 = parseTerminal(TokenType.ASTERISK);
		if (n_2 != null) {
			return new quantifier(n_2);
		}
		return null;
	}

	public enum TokenType {
		ASTERISK,
		DOUBLE_QUOTES,
		EQUALS,
		LEFT_PARENTHESIS,
		LETTER,
		LEXER_SYMBOL,
		LOWERCASE_LETTER,
		PARSER_SYMBOL,
		PLUS,
		QUESTION_MARK,
		RIGHT_PARENTHESIS,
		SEMICOLON,
		STRING_LITERAL,
		SYMBOL,
		UNDERSCORE,
		UPPERCASE_LETTER,
		VERTICAL_LINE,
		_LINE_COMMENT,
		_MULTI_LINE_COMMENT,
		_WHITESPACE
	}

	public record Token(TokenType type, String content) {
		public Token {
			Objects.requireNonNull(type);
			Objects.requireNonNull(content);
		}
	}

	public static final class EBNFParser_Lexer {
		private final boolean[] isAccepting;
		private final boolean[] isSkippable;
		private final TokenType[] tokensToMatch;
		private final int[] offsets;
		private final char[] symbols;
		private final int[] destinations;

		public EBNFParser_Lexer() {
			final String encoded =
					"AAAAFwAAAt7/AAD/AP//AP////8A//////////////8AAAAAAAAAAAAA/wAAAP8AAP8AAAAAAAAAE///////////AAAABf////8AAAAKAAAADv////8AAAAIAAAAAAAAAAIAAAAR/////wAAAAMAAAALAAAAEwAAAAcAAAAJAAAAEgAAABAAAAAFAAAADAAAAAEAAAAAAAAAQgAAAKEAAAEBAAABNgAAAXMAAAFzAAABqAAAAgcAAAIHAAACBwAAAgcAAAIHAAACCQAAAgkAAAIJAAACDAAAAicAAAInAAAChgAAAoYAAAKhAAACoQAAAt4ACQAKACAAIgAoACkAKgArAC8AOwA9AD8AQQBCAEMARABFAEYARwBIAEkASgBLAEwATQBOAE8AUABRAFIAUwBUAFUAVgBXAFgAWQBaAF8AYQBiAGMAZABlAGYAZwBoAGkAagBrAGwAbQBuAG8AcABxAHIAcwB0AHUAdgB3AHgAeQB6AHwAIAAhACIAIwAkACUAJgAnACgAKQAqACsALAAtAC4ALwAwADEAMgAzADQANQA2ADcAOAA5ADoAOwA8AD0APgA/AEAAQQBCAEMARABFAEYARwBIAEkASgBLAEwATQBOAE8AUABRAFIAUwBUAFUAVgBXAFgAWQBaAFsAXABdAF4AXwBgAGEAYgBjAGQAZQBmAGcAaABpAGoAawBsAG0AbgBvAHAAcQByAHMAdAB1AHYAdwB4AHkAegB7AHwAfQB+AAoAIAAhACIAIwAkACUAJgAnACgAKQAqACsALAAtAC4ALwAwADEAMgAzADQANQA2ADcAOAA5ADoAOwA8AD0APgA/AEAAQQBCAEMARABFAEYARwBIAEkASgBLAEwATQBOAE8AUABRAFIAUwBUAFUAVgBXAFgAWQBaAFsAXABdAF4AXwBgAGEAYgBjAGQAZQBmAGcAaABpAGoAawBsAG0AbgBvAHAAcQByAHMAdAB1AHYAdwB4AHkAegB7AHwAfQB+AEEAQgBDAEQARQBGAEcASABJAEoASwBMAE0ATgBPAFAAUQBSAFMAVABVAFYAVwBYAFkAWgBfAGEAYgBjAGQAZQBmAGcAaABpAGoAawBsAG0AbgBvAHAAcQByAHMAdAB1AHYAdwB4AHkAegAiACgAKQAqACsAOwA9AD8AQQBCAEMARABFAEYARwBIAEkASgBLAEwATQBOAE8AUABRAFIAUwBUAFUAVgBXAFgAWQBaAF8AYQBiAGMAZABlAGYAZwBoAGkAagBrAGwAbQBuAG8AcABxAHIAcwB0AHUAdgB3AHgAeQB6AEEAQgBDAEQARQBGAEcASABJAEoASwBMAE0ATgBPAFAAUQBSAFMAVABVAFYAVwBYAFkAWgBfAGEAYgBjAGQAZQBmAGcAaABpAGoAawBsAG0AbgBvAHAAcQByAHMAdAB1AHYAdwB4AHkAegAgACEAIgAjACQAJQAmACcAKAApACoAKwAsAC0ALgAvADAAMQAyADMANAA1ADYANwA4ADkAOgA7ADwAPQA+AD8AQABBAEIAQwBEAEUARgBHAEgASQBKAEsATABNAE4ATwBQAFEAUgBTAFQAVQBWAFcAWABZAFoAWwBcAF0AXgBfAGAAYQBiAGMAZABlAGYAZwBoAGkAagBrAGwAbQBuAG8AcABxAHIAcwB0AHUAdgB3AHgAeQB6AHsAfAB9AH4AKgAvAAkACgAgAF8AYQBiAGMAZABlAGYAZwBoAGkAagBrAGwAbQBuAG8AcABxAHIAcwB0AHUAdgB3AHgAeQB6ACAAIQAiACMAJAAlACYAJwAoACkAKgArACwALQAuAC8AMAAxADIAMwA0ADUANgA3ADgAOQA6ADsAPAA9AD4APwBAAEEAQgBDAEQARQBGAEcASABJAEoASwBMAE0ATgBPAFAAUQBSAFMAVABVAFYAVwBYAFkAWgBbAFwAXQBeAF8AYABhAGIAYwBkAGUAZgBnAGgAaQBqAGsAbABtAG4AbwBwAHEAcgBzAHQAdQB2AHcAeAB5AHoAewB8AH0AfgBBAEIAQwBEAEUARgBHAEgASQBKAEsATABNAE4ATwBQAFEAUgBTAFQAVQBWAFcAWABZAFoAXwAiACgAKQAqACsAOwA9AD8AQQBCAEMARABFAEYARwBIAEkASgBLAEwATQBOAE8AUABRAFIAUwBUAFUAVgBXAFgAWQBaAF8AYQBiAGMAZABlAGYAZwBoAGkAagBrAGwAbQBuAG8AcABxAHIAcwB0AHUAdgB3AHgAeQB6AAAADwAAAA8AAAAPAAAAFgAAAA0AAAAFAAAACQAAAAgAAAAMAAAADgAAAAoAAAARAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAGAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAATAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAcAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAALAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAACAAAAAgAAAAIAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAAAMAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABUAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAAAMAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAAHAAAAAQAAAAEAAAABAAAAAQAAABIAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAIAAAAPAAAADwAAAA8AAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAABwAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABQAAAAUAAAAFAAAABUAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQAAAAEAAAABAAAAAQ=";
			final ByteBuffer bb =
					ByteBuffer.wrap(Base64.getDecoder().decode(encoded)).order(ByteOrder.BIG_ENDIAN);
			final int num_states = bb.getInt();
			final int num_destinations = bb.getInt();
			this.isAccepting = new boolean[num_states];
			this.isSkippable = new boolean[num_states];
			this.tokensToMatch = new TokenType[num_states];
			this.offsets = new int[num_states + 1];
			this.symbols = new char[num_destinations];
			this.destinations = new int[num_destinations];
			for (int i = 0; i < num_states; i++) {
				this.isAccepting[i] = bb.get() == (byte) 0xff;
			}
			for (int i = 0; i < num_states; i++) {
				this.isSkippable[i] = bb.get() == (byte) 0xff;
			}
			final TokenType[] tokenTypes = TokenType.values();
			for (int i = 0; i < num_states; i++) {
				final int x = bb.getInt();
				this.tokensToMatch[i] = x == -1 ? null : tokenTypes[x];
			}
			for (int i = 0; i < num_states + 1; i++) {
				this.offsets[i] = bb.getInt();
			}
			for (int i = 0; i < num_destinations; i++) {
				this.symbols[i] = bb.getChar();
			}
			for (int i = 0; i < num_destinations; i++) {
				this.destinations[i] = bb.getInt();
			}
		}

		private int transition(final int currentState, final char symbol) {
			final int start = this.offsets[currentState];
			final int end = this.offsets[currentState + 1];
			for (int i = start; i < end; i++) {
				if (this.symbols[i] == symbol) {
					return this.destinations[i];
				}
			}
			return -1;
		}

		public List<Token> tokenize(final String input) {
			final char[] v = input.toCharArray();
			int pos = 0;
			int lastTokenMatchStart = 0;
			int lastTokenMatchEnd = 0;
			final List<Token> tokens = new ArrayList<>();
			int currentState = 0;
			while (pos < v.length) {
				if (isAccepting[currentState]) {
					lastTokenMatchEnd = pos;
				}
				final char ch = v[pos];
				final int nextState = transition(currentState, ch);
				if (nextState != -1) {
					currentState = nextState;
					pos++;
				} else {
					if (isAccepting[currentState]) {
						final int length = lastTokenMatchEnd - lastTokenMatchStart;
						if (length == 0) {
							throw new IllegalArgumentException(
									String.format("No token emitted for empty match at index %,d.", pos));
						}
						if (!isSkippable[currentState]) {
							final String match = String.copyValueOf(v, lastTokenMatchStart, length);
							tokens.add(new Token(tokensToMatch[currentState], match));
						}
						lastTokenMatchStart = pos;
						lastTokenMatchEnd = -1;
						currentState = 0;
					} else {
						throw new IllegalArgumentException(String.format("Lexical error at index %,d.", pos));
					}
				}
			}
			if (isAccepting[currentState]) {
				lastTokenMatchEnd = pos;
			}
			final int length = lastTokenMatchEnd - lastTokenMatchStart;
			if (isAccepting[currentState] && length > 0 && !isSkippable[currentState]) {
				final String match = String.copyValueOf(v, lastTokenMatchStart, length);
				tokens.add(new Token(tokensToMatch[currentState], match));
			}
			return tokens;
		}
	}

	public Node parse(final String input) {
		final Node result;
		final EBNFParser_Lexer lexer = new EBNFParser_Lexer();
		try {
			this.v = lexer.tokenize(input).toArray(new Token[0]);
		} catch (final IllegalArgumentException e) {
			return null;
		}
		this.pos = 0;
		try {
			result = parse_grammar();
		} catch (final ArrayIndexOutOfBoundsException e) {
			return null;
		}
		return (pos == v.length && stack.isEmpty()) ? result : null;
	}
}
