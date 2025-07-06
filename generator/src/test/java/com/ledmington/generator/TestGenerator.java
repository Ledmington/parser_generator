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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Concatenation;
import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.Optional;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.Utils;

public final class TestGenerator {

	private TestGenerator() {}

	private static final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	private static final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
	private static final StandardJavaFileManager standardFileManager =
			compiler.getStandardFileManager(diagnostics, Locale.US, StandardCharsets.UTF_8);

	@BeforeAll
	static void setup() {
		if (compiler == null) {
			throw new RuntimeException("No compiler available. Run with a JDK.");
		}
	}

	private static final List<Arguments> TEST_CASES = List.of(
			Arguments.of(
					new Grammar(new Production(new NonTerminal("S"), new Terminal("a"))),
					List.of("a"),
					List.of("", "b", "aa")),
			Arguments.of(
					new Grammar(new Production(new NonTerminal("S"), new Terminal("abc"))),
					List.of("abc"),
					List.of("", "a", "b", "c", "ab", "bc", "cba")),
			Arguments.of(
					new Grammar(new Production(new NonTerminal("S"), new Optional(new Terminal("a")))),
					List.of("", "a"),
					List.of("b", "aa")),
			Arguments.of(
					new Grammar(
							new Production(new NonTerminal("S"), new NonTerminal("T")),
							new Production(new NonTerminal("T"), new Terminal("a"))),
					List.of("a"),
					List.of("", "b", "aa")),
			Arguments.of(
					new Grammar(
							new Production(new NonTerminal("S"), new NonTerminal("T")),
							new Production(new NonTerminal("T"), new Optional(new Terminal("a")))),
					List.of("", "a"),
					List.of("b", "aa")),
			Arguments.of(
					new Grammar(new Production(
							new NonTerminal("S"), new Concatenation(new Terminal("a"), new Terminal("b")))),
					List.of("ab"),
					List.of("", "a", "b", "aab", "abb", "c")));

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
							"Compilation failed.%n%s%n",
							diagnostics.getDiagnostics().stream()
									.map(d -> String.format(
											"Error at line %,d, column %,d: %s%n",
											d.getLineNumber(), d.getColumnNumber(), d.getMessage(Locale.US)))
									.collect(Collectors.joining("\n"))));

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
		final String sourceCode = Generator.generate(g, className, "", "\t");

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
		final String sourceCode = Generator.generate(g, className, "", "\t");

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
		final String text1 = Generator.generate(g, "MyParser", "", "\t");
		final String text2 = Generator.generate(g, "MyParser", "", "\t");
		assertEquals(
				text1,
				text2,
				() -> String.format(
						"The generator generated two different sources for the following grammar.\n%s\n\n --- Source 1 --- \n%s\n --- \n --- Source 2 --- \n%s\n --- ",
						Utils.prettyPrint(g, "  "), text1, text2));
	}
}
