package org.gluu.oxtrust.util;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CollectionsUtil {

	private static final Comparator<?> NATURAL_COMPARATOR = Comparator.naturalOrder();

	private CollectionsUtil() {
	}

	public static <E> List<E> trimToEmpty(List<E> list) {
		return list == null ? Collections.<E>emptyList() : list;
	}

	public static <E> boolean isEmpty(List<E> list) {
		return list == null || list.isEmpty();
	}

	public static <E> boolean isNotEmpty(List<E> list) {
		return !isEmpty(list);
	}

	public static <E> Optional<E> first(List<E> list) {
		return list == null ? Optional.<E>absent() : Optional.fromNullable(Iterables.getFirst(list, null));
	}

	@SuppressWarnings("unchecked")
	public static <E extends Comparable<E>> boolean equalsUnordered(List<E> lhs, List<E> rhs) {
		return equalsUnordered(lhs, rhs, (Comparator<E>) NATURAL_COMPARATOR);
	}

	public static <E> boolean equalsUnordered(List<E> lhs, List<E> rhs, Comparator<E> comparator) {
		if (lhs.size() != rhs.size()) {
			return false;
		}

		return sorted(rhs, comparator).equals(sorted(lhs, comparator));
	}

	private static <E> List<E> sorted(List<E> list, Comparator<E> comparator) {
		return FluentIterable.from(list).toSortedList(comparator);
	}

}
