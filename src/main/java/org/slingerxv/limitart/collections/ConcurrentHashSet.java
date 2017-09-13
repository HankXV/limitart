/*
 * Copyright (c) 2016-present The Limitart Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.slingerxv.limitart.collections;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 并发HashSet
 * 
 * @author hank
 *
 * @param <E>
 */
public class ConcurrentHashSet<E> extends AbstractSet<E> implements java.io.Serializable {

	private static final long serialVersionUID = -8672117787651310382L;

	private static final Object PRESENT = new Object();

	private final Map<E, Object> map;

	public ConcurrentHashSet() {
		map = new ConcurrentHashMap<>();
	}

	public ConcurrentHashSet(int initialCapacity) {
		map = new ConcurrentHashMap<>(initialCapacity);
	}

	@Override
	public Iterator<E> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public boolean add(E e) {
		return map.put(e, PRESENT) == null;
	}

	@Override
	public boolean remove(Object o) {
		return map.remove(o) == PRESENT;
	}

	@Override
	public void clear() {
		map.clear();
	}
}