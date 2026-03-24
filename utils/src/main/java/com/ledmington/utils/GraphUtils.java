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

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/** Collection of common utilities and algorithms on graphs. */
public final class GraphUtils {

	private GraphUtils() {}

	/**
	 * Breadth-First Search.
	 *
	 * @param graph The graph represented as a map giving the set of neighbors for each node.
	 * @param start The starting node.
	 * @return The set of visited nodes (it always includes the starting node).
	 * @param <X> The type of a node.
	 */
	public static <X> Set<X> bfs(final Map<X, Set<X>> graph, final X start) {
		final Queue<X> q = new ArrayDeque<>();
		final Set<X> visited = new HashSet<>();
		q.add(start);
		while (!q.isEmpty()) {
			final X current = q.remove();
			if (visited.contains(current)) {
				continue;
			}
			visited.add(current);
			q.addAll(graph.get(current));
		}
		return visited;
	}
}
