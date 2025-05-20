/*
 * parser-gen - Parser Generator
 * Copyright (C) 2023-2025 Filippo Barbari <filippo.barbari@gmail.com>
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
package com.ledmington.parser;

import java.util.List;
import java.util.Objects;

// TODO: find a better name to avoid clash
public final class Iterator<X> {

	private final List<X> list;
	private final int n;
	private int index = 0;

	public Iterator(final List<X> list) {
		this.list = Objects.requireNonNull(list);
		this.n = list.size();
	}

	public boolean hasNext() {
		return index >= 0 && index < n;
	}

	public void move() {
		index++;
	}

	public X current() {
		return list.get(index);
	}

	public void moveBack() {
		index--;
	}
}
