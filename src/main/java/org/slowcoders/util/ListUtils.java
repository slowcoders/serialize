package org.slowcoders.util;

import java.util.List;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class ListUtils {

	public static <T> ArrayList<T> newList(T[] items) {
		if (items == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(Arrays.asList(items));
	}

	public static <T> ArrayList<T> newList(Iterable<T> items) {
		ArrayList<T> list = new ArrayList<>();
		if (items != null) {
			for (Iterator<T> it = items.iterator(); it.hasNext();) {
				list.add(it.next());
			}
		}
		return list;
	}

}
