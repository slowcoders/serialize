package org.slowcoders.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Set;

public class SoftValueMap<Key, Item> extends AbstractMap<Key, Item> {
	private static RefCleaner cleanerQ = new RefCleaner();
	private HashMap<Key, Chain<Item>> map = new HashMap<Key, Chain<Item>>();

	public synchronized Item get(Object key) {
		Chain<Item> ref = map.get(key);
		if (ref == null) {
			return null;
		}
		Item item = ref.get();
		if (item == null) {
			map.remove(key);
		}
		return item;
	}

	public synchronized Item put(Key key, Item item) {
		Chain<Item> ref = new Chain<Item>(this, key, item);
		Chain<Item> old = map.put(key, ref);
		return (old == null) ? null : old.get();
	}

	public synchronized void clear() {
		map.clear();
	}

	public synchronized Item remove(Object key) {
		Chain<Item> old = map.remove(key);
		return (old == null) ? null : old.get();
	}

	@Override
	@Deprecated
	public Set<Entry<Key, Item>> entrySet() {
		throw Debug.notImplemented();//return (Set<Entry<K, E>>)(Set)map.entrySet();
	}

	static class Chain<T> extends SoftReference<T> {
		private final Object key;
		private final SoftValueMap map;

		Chain(SoftValueMap map, Object key, T value) {
			super(value, cleanerQ);
			this.map = map;
			this.key = key;
		}

		public void remove() {
			synchronized (map) {
				map.remove(this.key);
			}
		}
	}

	private static class RefCleaner extends ReferenceQueue<Object> implements Runnable {

		RefCleaner() {
			new Thread(null,this, "WeakValue cleaner").start();
		}

		@Override
		public void run() {
			try {
				Chain<?> ref = (Chain<?>) this.remove();
				ref.remove();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
