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
package com.ledmington.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public final class TestBfs {

	private TestBfs() {}

	@Test
	void reachability() {
		final Map<Integer, Set<Integer>> graph = Map.ofEntries(
				Map.entry(0, Set.of(1)),
				Map.entry(1, Set.of(3)),
				Map.entry(2, Set.of(1, 4)),
				Map.entry(3, Set.of()),
				Map.entry(4, Set.of()));

		assertEquals(Set.of(0, 1, 3), GraphUtils.bfs(graph, 0));
		assertEquals(Set.of(1, 3), GraphUtils.bfs(graph, 1));
		assertEquals(Set.of(1, 2, 3, 4), GraphUtils.bfs(graph, 2));
		assertEquals(Set.of(3), GraphUtils.bfs(graph, 3));
		assertEquals(Set.of(4), GraphUtils.bfs(graph, 4));
	}
}
