/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.mockito.invocation.InvocationOnMock;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DataEntry;
import io.jans.orm.annotation.Version;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.VersionMismatchException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.model.base.BaseEntry;
import io.jans.orm.search.filter.Filter;

/**
 * A map-backed stand-in for {@link PersistenceEntryManager} that emulates enough of a SQL backend's
 * behavior for {@code OrmTraceStore} to be exercised by the shared {@code TraceStoreContractTest}
 * suite: {@code contains}, {@code persist} (throwing
 * {@link EntryPersistenceException} with <strong>no</strong> {@code DuplicateEntryException} cause
 * on a duplicate DN, exactly like SQL), {@code find} (throwing when the DN is absent; returns a
 * defensive copy, like a real backend deserializing a fresh row, so a caller mutating the found
 * entity never aliases the stored state before a subsequent CAS check), {@code merge},
 * {@code updateWithVersion} (compare-and-swaps the entity's {@code @Version} field against
 * the stored value, throwing {@link VersionMismatchException} on a mismatch), {@code findEntries},
 * {@code findPagedEntries} and {@code countEntries} — evaluating {@code AND}/{@code OR}/
 * {@code EQUALITY} (including the {@code multiValued()} marker used for
 * {@code jansTraceCapKeys}/{@code jansTraceTokenKeys}) and {@code LESS_OR_EQUAL} filters by
 * reflecting on each entity's {@code @AttributeName} fields, and applying the entity's
 * {@code @DataEntry} default sort before any {@code limit}, as the SQL backend does.
 *
 * <p>Not a general-purpose ORM fake: it only supports the filter shapes and method overloads
 * {@code OrmTraceStore} actually issues.
 *
 * @author Yuriy Movchan
 */
final class FakeEntryManagerBehavior {

	private static final Map<Class<?>, Map<String, Field>> ATTRIBUTE_CACHE = new ConcurrentHashMap<>();

	private final Map<Class<?>, Map<String, Object>> storesByClass = new HashMap<>();

	private static final Map<PersistenceEntryManager, FakeEntryManagerBehavior> BY_MOCK = new ConcurrentHashMap<>();

	private final PersistenceEntryManager mock = mock(PersistenceEntryManager.class);

	private int pendingVersionConflicts;

	private Consumer<Object> concurrentWriter;

	private FakeEntryManagerBehavior() {
		wire();
	}

	static PersistenceEntryManager create() {
		FakeEntryManagerBehavior behavior = new FakeEntryManagerBehavior();
		BY_MOCK.put(behavior.mock, behavior);
		return behavior.mock;
	}

	/**
	 * Makes the next {@code count} {@code updateWithVersion} calls lose a race: each first lets
	 * {@code writer} change the stored entry, bumps its version, then throws
	 * {@link VersionMismatchException}.
	 */
	static void injectVersionConflicts(PersistenceEntryManager manager, int count, Consumer<Object> writer) {
		FakeEntryManagerBehavior behavior = BY_MOCK.get(manager);
		behavior.pendingVersionConflicts = count;
		behavior.concurrentWriter = writer;
	}

	private Map<String, Object> storeFor(Class<?> entryClass) {
		return storesByClass.computeIfAbsent(entryClass, c -> new LinkedHashMap<>());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void wire() {
		when(mock.contains(anyString(), any(Class.class)))
				.thenAnswer(inv -> storeFor(inv.getArgument(1)).containsKey((String) inv.getArgument(0)));

		doAnswer(inv -> {
			Object entity = inv.getArgument(0);
			String dn = dnOf(entity);
			Map<String, Object> store = storeFor(entity.getClass());
			if (store.containsKey(dn)) {
				// SQL does not throw io.jans.orm.exception.operation.DuplicateEntryException; it
				// wraps every persist failure in a plain EntryPersistenceException.
				throw new EntryPersistenceException("Duplicate entry: " + dn);
			}
			initVersionIfNeeded(entity);
			store.put(dn, entity);
			return null;
		}).when(mock).persist(any());

		doAnswer(inv -> {
			Object entity = inv.getArgument(0);
			String dn = dnOf(entity);
			Map<String, Object> store = storeFor(entity.getClass());
			if (!store.containsKey(dn)) {
				throw new EntryPersistenceException("No such entry: " + dn);
			}
			store.put(dn, entity);
			return null;
		}).when(mock).merge(any());

		doAnswer(inv -> {
			Object entity = inv.getArgument(0);
			String dn = dnOf(entity);
			Class<?> entryClass = entity.getClass();
			Map<String, Object> store = storeFor(entryClass);
			Object current = store.get(dn);
			if (current == null) {
				throw new EntryPersistenceException("No such entry: " + dn);
			}
			Field versionField = versionFieldOrNull(entryClass);
			if (versionField == null) {
				throw new UnsupportedOperationException(
						"FakeEntryManagerBehavior: no @Version field on " + entryClass);
			}
			if (pendingVersionConflicts > 0) {
				pendingVersionConflicts--;
				concurrentWriter.accept(current);
				Long racedVersion = (Long) versionField.get(current);
				versionField.set(current, (racedVersion == null ? 0L : racedVersion) + 1);
				throw new VersionMismatchException(dn, (Long) versionField.get(entity));
			}
			Long expected = (Long) versionField.get(entity);
			Long storedVersion = (Long) versionField.get(current);
			long storedValue = storedVersion == null ? 0L : storedVersion;
			if (expected == null || expected.longValue() != storedValue) {
				throw new VersionMismatchException(dn, expected);
			}
			long newVersion = storedValue + 1;
			versionField.set(entity, newVersion);
			store.put(dn, entity);
			return null;
		}).when(mock).updateWithVersion(any());

		when(mock.find(anyString(), any(Class.class), any())).thenAnswer(inv -> {
			String dn = inv.getArgument(0);
			Class<?> entryClass = inv.getArgument(1);
			Object entity = storeFor(entryClass).get(dn);
			if (entity == null) {
				throw new EntryPersistenceException("No such entry: " + dn);
			}
			// A defensive copy, like a real backend deserializing a fresh row: without it, a caller
			// mutating the found entity before updateWithVersion would alias the very state the CAS
			// check above is supposed to compare against.
			return shallowCopy(entity);
		});

		when(mock.findEntries(anyString(), any(Class.class), any(Filter.class)))
				.thenAnswer(inv -> matchAll(inv, -1));
		when(mock.findEntries(anyString(), any(Class.class), any(Filter.class), anyInt()))
				.thenAnswer(inv -> matchAll(inv, (Integer) inv.getArgument(3)));
		when(mock.findEntries(anyString(), any(Class.class), any(Filter.class), any(String[].class)))
				.thenAnswer(inv -> matchAll(inv, -1));

		when(mock.findPagedEntries(anyString(), any(Class.class), any(Filter.class), any(), anyString(),
				any(SortOrder.class), anyInt(), anyInt(), anyInt())).thenAnswer(this::pagedAnswer);

		when(mock.countEntries(anyString(), any(Class.class), any(Filter.class)))
				.thenAnswer(inv -> matchAll(inv, -1).size());
	}

	/**
	 * Like {@code SqlEntryManager.findEntriesImpl}: the entity's {@code @DataEntry} default sort
	 * ({@code sortByName}, falling back to {@code sortBy}) is applied inside the query, i.e.
	 * before {@code limit} truncates the result.
	 */
	private List<Object> matchAll(InvocationOnMock inv, int limit) {
		String baseDn = inv.getArgument(0);
		Class<?> entryClass = inv.getArgument(1);
		Filter filter = inv.getArgument(2);
		List<Object> matched = new ArrayList<>();
		for (Map.Entry<String, Object> entry : storeFor(entryClass).entrySet()) {
			if (underBase(entry.getKey(), baseDn) && matches(filter, entry.getValue())) {
				matched.add(entry.getValue());
			}
		}
		matched.sort(defaultSortComparator(entryClass));
		if (limit > 0 && matched.size() > limit) {
			return new ArrayList<>(matched.subList(0, limit));
		}
		return matched;
	}

	private static Comparator<Object> defaultSortComparator(Class<?> entryClass) {
		Comparator<Object> comparator = (a, b) -> 0;
		for (String attributeName : defaultSortAttributes(entryClass)) {
			comparator = comparator.thenComparing((a, b) -> compare(attrValue(a, attributeName), attrValue(b, attributeName)));
		}
		return comparator;
	}

	private static String[] defaultSortAttributes(Class<?> entryClass) {
		DataEntry dataEntry = entryClass.getAnnotation(DataEntry.class);
		if (dataEntry == null) {
			return new String[0];
		}
		return dataEntry.sortByName().length > 0 ? dataEntry.sortByName() : dataEntry.sortBy();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object pagedAnswer(InvocationOnMock inv) {
		String baseDn = inv.getArgument(0);
		Class<?> entryClass = inv.getArgument(1);
		Filter filter = inv.getArgument(2);
		String sortBy = inv.getArgument(4);
		SortOrder sortOrder = inv.getArgument(5);
		int start = inv.getArgument(6);
		int count = inv.getArgument(7);

		List<Object> matched = new ArrayList<>();
		for (Map.Entry<String, Object> entry : storeFor(entryClass).entrySet()) {
			if (underBase(entry.getKey(), baseDn) && matches(filter, entry.getValue())) {
				matched.add(entry.getValue());
			}
		}
		// SQL puts the requested sort first and the entity default sort after it.
		Comparator<Object> requested = (a, b) -> {
			int cmp = compare(attrValue(a, sortBy), attrValue(b, sortBy));
			return sortOrder == SortOrder.DESCENDING ? -cmp : cmp;
		};
		matched.sort(requested.thenComparing(defaultSortComparator(entryClass)));

		int total = matched.size();
		int from = Math.min(Math.max(start, 0), total);
		int to = Math.min(from + Math.max(count, 0), total);
		List<Object> page = matched.subList(from, to);

		PagedResult result = new PagedResult();
		result.setStart(start);
		result.setTotalEntriesCount(total);
		result.setEntriesCount(page.size());
		result.setEntries(new ArrayList<>(page));
		return result;
	}

	private static boolean underBase(String dn, String baseDn) {
		return dn.equals(baseDn) || dn.endsWith("," + baseDn);
	}

	private static String dnOf(Object entity) {
		return ((BaseEntry) entity).getDn();
	}

	private static boolean matches(Filter filter, Object entity) {
		switch (filter.getType()) {
		case AND:
			for (Filter sub : filter.getFilters()) {
				if (!matches(sub, entity)) {
					return false;
				}
			}
			return true;
		case OR:
			for (Filter sub : filter.getFilters()) {
				if (matches(sub, entity)) {
					return true;
				}
			}
			return false;
		case EQUALITY:
			return matchesEquality(filter, entity);
		case LESS_OR_EQUAL:
			return matchesLessOrEqual(filter, entity);
		case GREATER_OR_EQUAL:
			return matchesGreaterOrEqual(filter, entity);
		default:
			throw new UnsupportedOperationException("FakeEntryManagerBehavior does not support: " + filter.getType());
		}
	}

	private static boolean matchesEquality(Filter filter, Object entity) {
		Object attrValue = attrValue(entity, filter.getAttributeName());
		Object expected = filter.getAssertionValue();
		if (Boolean.TRUE.equals(filter.getMultiValued())) {
			if (!(attrValue instanceof List)) {
				return false;
			}
			for (Object value : (List<?>) attrValue) {
				if (String.valueOf(value).equals(String.valueOf(expected))) {
					return true;
				}
			}
			return false;
		}
		return attrValue != null && String.valueOf(attrValue).equals(String.valueOf(expected));
	}

	private static boolean matchesLessOrEqual(Filter filter, Object entity) {
		Object attrValue = attrValue(entity, filter.getAttributeName());
		if (!(attrValue instanceof Number)) {
			return false;
		}
		long actual = ((Number) attrValue).longValue();
		long expected = ((Number) filter.getAssertionValue()).longValue();
		return actual <= expected;
	}

	private static boolean matchesGreaterOrEqual(Filter filter, Object entity) {
		Object attrValue = attrValue(entity, filter.getAttributeName());
		if (!(attrValue instanceof Number)) {
			return false;
		}
		return ((Number) attrValue).longValue() >= ((Number) filter.getAssertionValue()).longValue();
	}

	@SuppressWarnings("unchecked")
	private static int compare(Object a, Object b) {
		if (a == null && b == null) {
			return 0;
		}
		if (a == null) {
			return -1;
		}
		if (b == null) {
			return 1;
		}
		if (a instanceof Comparable) {
			return ((Comparable<Object>) a).compareTo(b);
		}
		return String.valueOf(a).compareTo(String.valueOf(b));
	}

	private static Object attrValue(Object entity, String attributeName) {
		Field field = attributesOf(entity.getClass()).get(attributeName);
		if (field == null) {
			throw new IllegalArgumentException(
					"FakeEntryManagerBehavior has no @AttributeName '" + attributeName + "' on " + entity.getClass());
		}
		try {
			return field.get(entity);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Map<String, Field> attributesOf(Class<?> entryClass) {
		return ATTRIBUTE_CACHE.computeIfAbsent(entryClass, c -> {
			Map<String, Field> map = new HashMap<>();
			for (Field field : c.getDeclaredFields()) {
				AttributeName annotation = field.getAnnotation(AttributeName.class);
				if (annotation != null) {
					String name = annotation.name().isEmpty() ? field.getName() : annotation.name();
					field.setAccessible(true);
					map.put(name, field);
				}
			}
			return map;
		});
	}

	// -- @Version support (compare-and-swap) -------------------------------------------

	private static Field versionFieldOrNull(Class<?> entryClass) {
		for (Field field : entryClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(Version.class)) {
				field.setAccessible(true);
				return field;
			}
		}
		return null;
	}

	private static void initVersionIfNeeded(Object entity) {
		Field versionField = versionFieldOrNull(entity.getClass());
		if (versionField == null) {
			return;
		}
		try {
			if (versionField.get(entity) == null) {
				versionField.set(entity, 0L);
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * A shallow field-for-field copy of a jans-orm entity (walking every declared field, including
	 * inherited ones such as {@code BaseEntry.dn}), so {@code find()} can hand back a fresh object
	 * per call instead of the live reference held in {@link #storesByClass} -- the same separation
	 * a real backend gets for free by deserializing each row anew.
	 */
	@SuppressWarnings("unchecked")
	private static <T> T shallowCopy(T source) {
		try {
			T copy = (T) source.getClass().getDeclaredConstructor().newInstance();
			for (Class<?> c = source.getClass(); c != null; c = c.getSuperclass()) {
				for (Field field : c.getDeclaredFields()) {
					if (Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					field.setAccessible(true);
					field.set(copy, field.get(source));
				}
			}
			return copy;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to copy " + source.getClass(), e);
		}
	}

}
