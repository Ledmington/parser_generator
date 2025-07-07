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
package com.ledmington.cli;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.ledmington.ebnf.Grammar;
import com.ledmington.ebnf.Parser;
import com.ledmington.ebnf.Utils;
import com.ledmington.generator.Generator;

public class Main {

	private static void die(final String format, final Object... args) {
		System.err.printf(format, args);
		System.exit(-1);
	}

	public static void main(final String[] args) {
		String grammarFile = null;
		String outputFile = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-h", "--help" -> {
					System.out.println(String.join(
							"\n",
							"",
							" Parser Generator - A zero-dependency parser generator",
							"",
							" -h, --help             Displays this message and exits.",
							" -g, --grammar GRAMMAR  Reads the EBNF grammar from the given GRAMMAR file.",
							" -o, --output OUTPUT    Writes the resulting parser in the given OUTPUT file.",
							""));
					System.exit(0);
					return;
				}
				case "-g", "--grammar" -> {
					i++;
					if (grammarFile != null) {
						die("Cannot set grammar file twice, was already '%s'.%n", grammarFile);
					}
					grammarFile = args[i];
				}
				case "-o", "--output" -> {
					i++;
					if (outputFile != null) {
						die("Cannot set output file twice, was already '%s'.%n", outputFile);
					}
					outputFile = args[i];
				}
				default -> die("Unknown command-line argument: '%s'.%n", args[i]);
			}
		}

		if (grammarFile == null) {
			die("No grammar file was set.%n");
		}
		if (outputFile == null) {
			die("No output file was set.%n");
		}

		Objects.requireNonNull(grammarFile);
		Objects.requireNonNull(outputFile);

		final Grammar g;
		try {
			g = Parser.parse(Files.readString(Path.of(grammarFile)));
			System.out.println(Utils.prettyPrint(g, "  "));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		final String startSymbol = GrammarChecker.check(g);

		if (outputFile.endsWith(".java")) {
			outputFile = outputFile.substring(0, outputFile.length() - 5);
		}

		final Path outputPath = Path.of(outputFile + ".java").normalize().toAbsolutePath();
		try (final BufferedWriter bw = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
			final int idx = outputFile.lastIndexOf(File.separator);
			final String className = idx < 0 ? outputFile : outputFile.substring(idx + 1);
			final String packageName =
					idx < 0 ? "unknown" : outputFile.substring(0, idx).replace(File.separator, ".");
			final String indent = "\t";
			bw.write(Generator.generate(g, className, packageName, startSymbol, indent));
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
}
