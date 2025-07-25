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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Alternation;
import com.ledmington.ebnf.Expression;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.OptionalNode;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Repetition;
import com.ledmington.ebnf.Sequence;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.Utils;

@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.MissingStaticMethodInNonInstantiatableClass"})
public final class TestGenerator {

	private TestGenerator() {}

	private static final List<Arguments> TEST_CASES = List.of(
			Arguments.of(g(p(nt("S"), t("a"))), List.of("a"), List.of("", "b", "aa")),
			Arguments.of(g(p(nt("S"), t("abc"))), List.of("abc"), List.of("", "a", "b", "c", "ab", "bc", "cba")),
			Arguments.of(g(p(nt("S"), opt(t("a")))), List.of("", "a"), List.of("b", "aa")),
			Arguments.of(g(p(nt("S"), nt("T")), p(nt("T"), t("a"))), List.of("a"), List.of("", "b", "aa")),
			Arguments.of(g(p(nt("S"), nt("T")), p(nt("T"), opt(t("a")))), List.of("", "a"), List.of("b", "aa")),
			Arguments.of(g(p(nt("S"), cat(t("a"), t("b")))), List.of("ab"), List.of("", "a", "b", "aab", "abb", "c")),
			Arguments.of(
					g(p(nt("S"), cat(t("a"), opt(t("b")), t("c")))),
					List.of("ac", "abc"),
					List.of("", "a", "c", "ab", "bc")),
			Arguments.of(
					g(p(nt("S"), cat(opt(t("a")), opt(t("b")), opt(t("c"))))),
					List.of("", "a", "b", "c", "ab", "ac", "bc", "abc"),
					List.of("ba", "ca", "cb", "cba", "abb", "acb", "d")),
			Arguments.of(
					g(p(nt("S"), opt(cat(t("a"), t("b"))))), List.of("", "ab"), List.of("a", "b", "ba", "aa", "bb")),
			Arguments.of(
					g(
							p(nt("S"), cat(opt(cat(nt("T"), t("b"))), opt(cat(t("c"), nt("U"))))),
							p(nt("T"), t("a")),
							p(nt("U"), t("d"))),
					List.of("", "ab", "cd", "abcd"),
					List.of("a", "b", "c", "d", "bc", "ad", "abc", "bcd")),
			Arguments.of(
					g(
							p(nt("S"), cat(nt("T"), nt("U"), nt("V"))),
							p(nt("T"), opt(t("a"))),
							p(nt("U"), opt(t("b"))),
							p(nt("V"), opt(t("c")))),
					List.of("", "a", "b", "c", "ab", "ac", "bc", "abc"),
					List.of("cb", "ca", "ba", "cba", "aba", "bcb", "aabc", "abd")),
			Arguments.of(
					g(p(nt("S"), cat(nt("T"), t("a"))), p(nt("T"), t("a"))), List.of("aa"), List.of("", "a", "aaa")),
			Arguments.of(
					g(p(nt("S"), rep(t("a")))),
					List.of("", "a", "aa", "aaa", "aaaa", "aaaaa"),
					List.of("b", "ab", "aba", "bab")),
			Arguments.of(
					g(p(nt("S"), cat(rep(cat(t("a"), t("b"))), t("c")))),
					List.of("c", "abc", "ababc", "abababc"),
					List.of("", "a", "b", "ab", "abcab", "aabc", "abbc")),
			Arguments.of(
					g(p(nt("S"), rep(cat(t("a"), opt(t("b")), t("c"))))),
					List.of("", "ac", "abc", "acac", "abcabc", "acabc", "abcac"),
					List.of("a", "b", "c", "ab", "bc", "aac", "acc", "abbc")),
			Arguments.of(g(p(nt("S"), alt(t("a"), t("b")))), List.of("a", "b"), List.of("", "ab", "aa", "bb", "ba")),
			Arguments.of(
					g(p(nt("S"), rep(alt(t("a"), t("b"))))),
					List.of("", "a", "b", "aa", "ab", "bb", "ba", "aba", "bab", "aaa"),
					List.of("c", "ac", "cb", "bc", "abc")),
			Arguments.of(
					g(
							p(
									nt("S"),
									cat(
											opt(nt("sign")),
											alt(nt("zero"), cat(nt("digit excluding zero"), rep(nt("digit")))))),
							p(nt("sign"), alt(t("+"), t("-"))),
							p(nt("zero"), t("0")),
							p(
									nt("digit excluding zero"),
									alt(t("1"), t("2"), t("3"), t("4"), t("5"), t("6"), t("7"), t("8"), t("9"))),
							p(nt("digit"), alt(nt("zero"), nt("digit excluding zero")))),
					List.of("0", "+0", "-0", "1", "+99", "-69", "123456789"),
					List.of("", "01", "001", "1a", "1.0")),
			Arguments.of(
					g(p(nt("S"), cat(t("'"), alt(t("a"), t("b")), t("'")))),
					List.of("'a'", "'b'"),
					List.of("", "'a", "a'", "'''", "'a''", "'a'b'")),
			Arguments.of(
					g(p(nt("S"), cat(t("\""), alt(t("a"), t("b")), t("\"")))),
					List.of("\"a\"", "\"b\""),
					List.of("", "\"a", "a\"", "\"\"\"", "\"a\"\"", "\"a\"b\"")),
			Arguments.of(
					g(p(nt("S"), cat(t("\\"), alt(t("n"), t("t"))))),
					List.of("\\n", "\\t"),
					List.of("", "\\", "\n", "\t", "n", "t")));

	private static Grammar g(final Production... productions) {
		return new Grammar(productions);
	}

	private static Production p(final NonTerminal nt, final Expression exp) {
		return new Production(nt, exp);
	}

	private static NonTerminal nt(final String name) {
		return new NonTerminal(name);
	}

	private static Terminal t(final String literal) {
		return new Terminal(literal);
	}

	private static Sequence cat(final Expression... expressions) {
		return new Sequence(expressions);
	}

	private static OptionalNode opt(final Expression inner) {
		return new OptionalNode(inner);
	}

	private static Repetition rep(final Expression exp) {
		return new Repetition(exp);
	}

	private static Alternation alt(final Expression... expressions) {
		return new Alternation(expressions);
	}

	private static Stream<Arguments> onlyGrammars() {
		return TEST_CASES.stream().map(tc -> Arguments.of(tc.get()[0]));
	}

	private static Stream<Arguments> correctCases() {
		return TEST_CASES.stream().map(tc -> Arguments.of(tc.get()[0], tc.get()[1]));
	}

	private static Stream<Arguments> wrongCases() {
		return TEST_CASES.stream().map(tc -> Arguments.of(tc.get()[0], tc.get()[2]));
	}

	private static final class JavaSourceFromString extends SimpleJavaFileObject {

		private final String code;

		JavaSourceFromString(final String className, final String code) {
			super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.code = code;
		}

		@Override
		public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
			return code;
		}
	}

	private static final class JavaClassObject extends SimpleJavaFileObject {

		private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

		public JavaClassObject(final String name, final Kind kind) {
			super(URI.create("bytes:///" + name), kind);
		}

		@Override
		public OutputStream openOutputStream() {
			return baos;
		}

		public byte[] getBytes() {
			return baos.toByteArray();
		}
	}

	private static final class MemoryClassLoader extends ClassLoader {

		private final Map<String, JavaClassObject> classes = new HashMap<>();

		public void addClass(final String name, final JavaClassObject jco) {
			classes.put(name, jco);
		}

		@Override
		protected Class<?> findClass(final String name) throws ClassNotFoundException {
			final JavaClassObject jco = classes.get(name);
			if (jco == null) {
				return super.findClass(name);
			}
			final byte[] bytes = jco.getBytes();
			return defineClass(name, bytes, 0, bytes.length);
		}
	}

	private static Class<?> compileJavaSource(final String className, final String sourceCode) {
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			throw new RuntimeException("No compiler available. Run with a JDK.");
		}

		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		final StandardJavaFileManager standardFileManager =
				compiler.getStandardFileManager(diagnostics, Locale.US, StandardCharsets.UTF_8);

		// Prepare source file object
		final JavaSourceFromString sourceObject = new JavaSourceFromString(className, sourceCode);

		// Special class loader to enable output bytecode in memory
		final MemoryClassLoader classLoader = new MemoryClassLoader();

		try (final JavaFileManager fileManager = new ForwardingJavaFileManager<>(standardFileManager) {
			@Override
			public JavaFileObject getJavaFileForOutput(
					final Location location,
					final String className,
					final JavaFileObject.Kind kind,
					final FileObject sibling) {
				final JavaClassObject jclassObject = new JavaClassObject(className, kind);
				classLoader.addClass(className, jclassObject);
				return jclassObject;
			}
		}) {

			// Compile the source code
			final CompilationTask task = compiler.getTask(
					null,
					fileManager,
					diagnostics,
					List.of("-Xdiags:verbose", "-Xlint:all", "-Werror"),
					null,
					List.of(sourceObject));
			final boolean success = task.call();

			assertTrue(
					success,
					() -> String.format(
							"Compilation failed.%n%s%n%s%n",
							diagnostics.getDiagnostics().stream()
									.map(d -> String.format(
											"Error at line %,d, column %,d: %s%n",
											d.getLineNumber(), d.getColumnNumber(), d.getMessage(Locale.US)))
									.collect(Collectors.joining("\n")),
							sourceCode));

			return classLoader.loadClass(className);
		} catch (final IOException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@ParameterizedTest
	@MethodSource("correctCases")
	void correctParsing(final Grammar g, final List<String> correctInputs)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
		final String className = "MyCorrectParser";
		final String sourceCode = Generator.generate(g, className, "", "S", "\t", true);

		final Class<?> klass = compileJavaSource(className, sourceCode);
		final Method entrypoint = klass.getMethod("parse", String.class);

		for (final String correct : correctInputs) {
			final Object obj = entrypoint.invoke(klass.getConstructors()[0].newInstance(), correct);
			assertNotNull(
					obj,
					// TODO: print parsed object
					() -> String.format(
							"Expected the following source code to be able to parse the input '%s' but it did not.%n%s%n",
							correct, sourceCode));
		}
	}

	@ParameterizedTest
	@MethodSource("wrongCases")
	void incorrectParsing(final Grammar g, final List<String> wrongInputs)
			throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
		final String className = "MyWrongParser";
		final String sourceCode = Generator.generate(g, className, "", "S", "\t", true);

		final Class<?> klass = compileJavaSource(className, sourceCode);
		final Method entrypoint = klass.getMethod("parse", String.class);

		for (final String wrong : wrongInputs) {
			// TODO: print parsed object
			final Object obj = entrypoint.invoke(klass.getConstructors()[0].newInstance(), wrong);
			assertNull(
					obj,
					() -> String.format(
							"Expected the following source code to NOT be able to parse the input '%s' but it did.%n%s%n",
							wrong, sourceCode));
		}
	}

	@ParameterizedTest
	@MethodSource("onlyGrammars")
	void determinism(final Grammar g) {
		final String text1 = Generator.generate(g, "MyParser", "", "S", "\t", true);
		final String text2 = Generator.generate(g, "MyParser", "", "S", "\t", true);
		assertEquals(
				text1,
				text2,
				() -> String.format(
						"The generator generated two different sources for the following grammar.\n%s\n\n --- Source 1 --- \n%s\n --- \n --- Source 2 --- \n%s\n --- ",
						Utils.prettyPrint(g, "  "), text1, text2));
	}
}
