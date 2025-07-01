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
package com.ledmington.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.NonTerminal;
import com.ledmington.ebnf.Optional;
import com.ledmington.ebnf.Production;
import com.ledmington.ebnf.Terminal;
import com.ledmington.ebnf.Utils;

public final class TestSerializer {

	private static final Path JAVAC_PATH = Path.of(System.getProperty("java.home"), "bin", "javac");
	private static Path tempDir = null;

	@BeforeAll
	static void setup() {
		try {
			tempDir = Files.createTempDirectory("TestSerializer-");
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String getFileName(String absolutePath) {
		if (absolutePath.contains(File.separator)) {
			final int idx = absolutePath.lastIndexOf(File.separator);
			absolutePath = absolutePath.substring(idx + 1);
		}
		if (absolutePath.contains(".")) {
			final int idx = absolutePath.lastIndexOf(".");
			absolutePath = absolutePath.substring(0, idx);
		}
		return absolutePath;
	}

	private static void tryCase(final Grammar g, final String input, final boolean correct) throws IOException {
		final Process p = new ProcessBuilder()
				.command("java", "-cp", tempDir.toString(), "Main.java", input)
				.directory(tempDir.toFile())
				.start();
		final int expectedExitCode = correct ? 0 : 255;
		try {
			assertEquals(expectedExitCode, p.waitFor(), () -> {
				String line;
				final StringBuilder sbOut;
				final StringBuilder sbErr;

				try {
					final BufferedReader outReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
					sbOut = new StringBuilder();
					while ((line = outReader.readLine()) != null) {
						sbOut.append(line).append('\n');
					}

					final BufferedReader errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					sbErr = new StringBuilder();
					while ((line = errReader.readLine()) != null) {
						sbErr.append(line).append('\n');
					}
				} catch (final IOException e) {
					throw new RuntimeException(e);
				}

				return String.format(
						"Expected grammar\n%s\nto%s be able to parse '%s'.\n --- STDOUT --- \n%s\n --- \n --- STDERR --- \n%s\n ---",
						Utils.prettyPrint(g, "  "), correct ? "" : " NOT", input, sbOut, sbErr);
			});
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static void tryCorrect(final Grammar g, final String input) throws IOException {
		tryCase(g, input, true);
	}

	private static void tryWrong(final Grammar g, final String input) throws IOException {
		tryCase(g, input, false);
	}

	private static Stream<Arguments> examples() {
		return Stream.of(
				Arguments.of(
						new Grammar(new Production(new NonTerminal("S"), new Terminal("a"))),
						List.of("a"),
						List.of("", "b", "aa")),
				Arguments.of(
						new Grammar(new Production(new NonTerminal("S"), new Optional(new Terminal("a")))),
						List.of("", "a"),
						List.of("b", "aa")));
	}

	@ParameterizedTest
	@MethodSource("examples")
	void simple(final Grammar g, final List<String> correctCases, final List<String> wrongCases) throws IOException {
		final Path tempFilePath;
		tempFilePath = Files.createTempFile(tempDir, "TestSerializer_", "_simple.java");
		final String filename = getFileName(tempFilePath.toString());

		final String text = Serializer.serialize(g, filename, "", "\t");
		Files.writeString(tempFilePath, text, StandardCharsets.UTF_8);

		try {
			new ProcessBuilder()
					.command(JAVAC_PATH.toString(), tempDir + File.separator + filename + ".java")
					.start()
					.waitFor();
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}

		Files.writeString(
				Path.of(tempDir.toString(), "Main.java"),
				String.join(
						"\n",
						"public final class Main {",
						"    public static void main(final String[] args) {",
						"        if (args.length != 1) {",
						"            throw new RuntimeException(\"Expected only 1 argument.\");",
						"        }",
						"        System.exit(" + filename + ".parse(args[0])==null?-1:0);",
						"        return;",
						"    }",
						"}"),
				StandardCharsets.UTF_8);

		try {
			new ProcessBuilder()
					.command(JAVAC_PATH.toString(), "Main.java", tempFilePath.toString())
					.start()
					.waitFor();
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}

		for (final String input : correctCases) {
			tryCorrect(g, input);
		}

		for (final String input : wrongCases) {
			tryWrong(g, input);
		}
	}
}
