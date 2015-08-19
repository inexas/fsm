package com.inexas.util;

import java.util.*;

/**
 * The strict map is an extension of a normal map that:
 * <ul>
 * <li>Is type safe (as Map is)
 * <li>Throws an exception if get(K) can't get his man
 * <li>Throws an exception if put(K,V) would replace an existing entry
 * <li>Can be locked with lock() so that it is immutable
 * </ul>
 * The class is designed primarily to help coding.
 * 
 * @author Keith Whittingham
 * @version $Revision: 1.2 $
 */
public class StrictMap<K, V> implements Map<K, V> {
	private final Map<K, V> map;
	private boolean locked = false;
	private boolean getCanReturnNull;

	// c o n s t r u c t o r s . . .

	public StrictMap(int initialCapacity) {
		map = new HashMap<>(initialCapacity);
	}

	public StrictMap() {
		map = new HashMap<>();
	}

	public void lock() {
		assert !locked : "Map has already been locked";
		locked = true;
	}

	// non-mutators...

	@Override
	public Set<K> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return map.entrySet();
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
	public boolean containsKey(Object key) {
		return map.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public V get(Object key) {
		final V returnValue = map.get(key);
		if((returnValue == null) && !getCanReturnNull) {
			throw new NotFoundException("Map does not contain key: " + key);
		}
		return returnValue;
	}

	// mutators...

	/**
	 * @throws LockedException
	 *             if the map has been locked
	 * @throws DuplicateException
	 *             if a key already exists
	 */
	@Override
	public V put(K key, V value) throws LockedException, DuplicateException {
		// If locked, throw...
		if(locked) {
			throw new LockedException("Invalid call: put(" + key + ", " + value + ");, map has been locked");
		}

		// If it's a duplicate entry, throw...
		if(map.put(key, value) != null) {
			throw new DuplicateException("Map already contains key: " + key);
		}

		// it's always null...
		return null;
	}

	/**
	 * @throws LockedException
	 *             if the map has been locked
	 * @throws DuplicateException
	 *             if a key already exists
	 */
	@Override
	public void putAll(Map<? extends K, ? extends V> toPut)
			throws LockedException, DuplicateException {
		if(locked) {
			throw new LockedException("Invalid call, map has been locked");
		}

		for(K key : toPut.keySet()) {
			if(map.containsKey(key)) {
				throw new DuplicateException("Map already contains key: " + key);
			}
		}

		map.putAll(toPut);
	}

	@Override
	public V remove(Object key) {
		if(locked) {
			throw new LockedException("Invalid call, map has been locked");
		}

		final V returnValue = map.remove(key);
		if(returnValue == null) {
			throw new NotFoundException("Map does not contain key: " + key);
		}
		return returnValue;
	}

	@Override
	public void clear() {
		if(locked) {
			throw new LockedException("Invalid call, map has been locked");
		}
		map.clear();
	}

	/**
	 * Delegates to Map
	 * 
	 * @see Object#toString()
	 */
	@Override
	public String toString() {
		return map.toString();
	}

	public boolean isGetCanReturnNull() {
		return getCanReturnNull;
	}

	public void setGetCanReturnNull(boolean getCanReturnNull) {
		this.getCanReturnNull = getCanReturnNull;
	}

	public boolean isLocked() {
		return locked;
	}

}
