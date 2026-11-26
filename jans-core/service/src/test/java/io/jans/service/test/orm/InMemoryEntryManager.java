/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.service.test.orm;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import com.querydsl.core.QueryException;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.annotation.AttributeName;
import io.jans.orm.annotation.DN;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.exception.operation.MissingEntrySearchException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;

/**
 * In-memory stand-in for {@link PersistenceEntryManager}, built with {@link Proxy}.
 *
 * <p>Only the methods listed in {@link #invoke} are implemented; every other method throws
 * {@link UnsupportedOperationException}. The store keeps copies of the entries, never the caller's
 * objects.</p>
 *
 * <p>Fidelity to the SQL backend:</p>
 * <ul>
 * <li>{@code persist} is atomic per DN. A second insert of the same DN throws the chain that
 * {@code SqlEntryManager.persist} throws: {@link EntryPersistenceException} caused by
 * {@link DuplicateEntryException} caused by a querydsl {@link QueryException} caused by an
 * {@link SQLException} with SQL state 23505. With the persistence type {@code spanner} or
 * {@code couchbase}, {@code persist} overwrites instead, as those backends do.</li>
 * <li>{@code find} and {@code merge} of a missing DN throw {@link EntryPersistenceException} caused by a
 * {@link MissingEntrySearchException} without a cause, as the SQL lookup does.</li>
 * <li>{@code encodeTime} returns ISO-8601 instants; filter values in that format are compared as dates.</li>
 * </ul>
 *
 * <p>Searches: {@code findEntries(base, class, filter[, count])} in DN order, the sorted
 * {@code findEntries(base, class, filter, attributes, sortBy, sortOrder, start, count, chunkSize)} and
 * {@code findPagedEntries} with the same parameters.</p>
 *
 * <p>Filters: {@code AND}, {@code OR}, {@code NOT}, {@code EQUALITY}, {@code LESS_OR_EQUAL},
 * {@code GREATER_OR_EQUAL}, {@code PRESENCE} and {@code SUBSTRING}, on fields annotated with
 * {@link AttributeName} (attribute names compare case-insensitively). String equality is exact. A
 * multi-valued field matches when any value matches. Subclasses may override
 * {@link #matches(Object, Filter)} to support more.</p>
 *
 * <p>DNs are compared exactly as given; a base DN matches every DN that ends with
 * {@code "," + baseDn} (case-insensitive), or equals it.</p>
 */
public class InMemoryEntryManager implements InvocationHandler {

    public static final String SQL_UNIQUE_VIOLATION_STATE = "23505";

    private final ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
    private final PersistenceEntryManager proxy;

    private volatile String persistenceType;
    private volatile BiFunction<String, Object[], RuntimeException> failureInjector;

    public InMemoryEntryManager() {
        this(PersistenceEntryManager.PERSITENCE_TYPES.sql.name());
    }

    public InMemoryEntryManager(String persistenceType) {
        this.persistenceType = persistenceType;
        this.proxy = (PersistenceEntryManager) Proxy.newProxyInstance(PersistenceEntryManager.class.getClassLoader(),
                new Class<?>[] { PersistenceEntryManager.class }, this);
    }

    public PersistenceEntryManager getEntryManager() {
        return proxy;
    }

    public void setPersistenceType(String persistenceType) {
        this.persistenceType = persistenceType;
    }

    /**
     * Sets a function that is called before every implemented method with the method name and the
     * arguments. When it returns an exception, the call throws it and the store is not changed.
     */
    public void setFailureInjector(BiFunction<String, Object[], RuntimeException> failureInjector) {
        this.failureInjector = failureInjector;
    }

    /**
     * @return how many times the method with this name was called through the proxy
     */
    public int getCallCount(String methodName) {
        AtomicInteger counter = calls.get(methodName);
        return counter == null ? 0 : counter.get();
    }

    /**
     * @return a copy of the stored entry, or {@code null}
     */
    public <T> T get(String dn, Class<T> entryClass) {
        Object stored = store.get(dn);
        if (stored == null) {
            return null;
        }

        return entryClass.cast(copy(stored));
    }

    /**
     * @return copies of all stored entries of this class, sorted by DN
     */
    public <T> List<T> getAll(Class<T> entryClass) {
        List<T> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : new TreeMap<>(store).entrySet()) {
            if (entryClass.isInstance(entry.getValue())) {
                result.add(entryClass.cast(copy(entry.getValue())));
            }
        }

        return result;
    }

    /**
     * Stores a copy of the entry without any checks, for test setup.
     */
    public void put(Object entry) {
        store.put(getDn(entry), copy(entry));
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }

    @Override
    public Object invoke(Object proxyInstance, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        Object[] arguments = args == null ? new Object[0] : args;
        Class<?>[] types = method.getParameterTypes();

        if (method.getDeclaringClass() == Object.class) {
            switch (name) {
            case "equals":
                return proxyInstance == arguments[0];
            case "hashCode":
                return System.identityHashCode(proxyInstance);
            case "toString":
                return "InMemoryEntryManager@" + Integer.toHexString(System.identityHashCode(proxyInstance));
            default:
                throw new UnsupportedOperationException(name);
            }
        }

        calls.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
        BiFunction<String, Object[], RuntimeException> injector = failureInjector;
        if (injector != null) {
            RuntimeException failure = injector.apply(name, arguments);
            if (failure != null) {
                throw failure;
            }
        }

        synchronized (store) {
            return dispatch(name, types, arguments);
        }
    }

    protected Object dispatch(String name, Class<?>[] types, Object[] args) {
        switch (name) {
        case "isAtomicBatchSupported":
        case "isTransactionSupported":
            return PersistenceEntryManager.PERSITENCE_TYPES.sql.name().equals(persistenceType);
        case "executeInTransaction":
            Map<String, Object> before = new java.util.HashMap<>();
            store.forEach((dn, entry) -> before.put(dn, copy(entry)));
            try {
                return ((io.jans.orm.TransactionCallback<?>) args[1]).doInTransaction();
            } catch (Exception ex) {
                store.clear();
                store.putAll(before);
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                throw new IllegalStateException(ex);
            }
        case "findForUpdate":
            Object locked = store.get(String.valueOf(args[1]));
            return locked == null ? null : copy(locked);
        case "persistNew":
            if (types.length == 1) {
                String dn = getDn(args[0]);
                if (store.putIfAbsent(dn, copy(args[0])) != null) {
                    throw duplicate(dn);
                }
                return null;
            }
            break;
        case "persist":
            if (types.length == 1) {
                persist(args[0]);
                return null;
            }
            break;
        case "merge":
            if (types.length == 1) {
                merge(args[0]);
                return null;
            }
            break;
        case "find":
            if ((types.length == 2) && (types[0] == Class.class)) {
                return find((Class<?>) args[0], args[1]);
            }
            break;
        case "contains":
            if ((types.length == 2) && (types[0] == String.class) && (types[1] == Class.class)) {
                return store.get((String) args[0]) != null && ((Class<?>) args[1]).isInstance(store.get((String) args[0]));
            }
            break;
        case "findEntries":
            if ((types.length == 3) && (types[2] == Filter.class)) {
                return findEntries((String) args[0], (Class<?>) args[1], (Filter) args[2], Integer.MAX_VALUE);
            }
            if ((types.length == 4) && (types[2] == Filter.class) && (types[3] == int.class)) {
                int count = (Integer) args[3];
                return findEntries((String) args[0], (Class<?>) args[1], (Filter) args[2], count <= 0 ? Integer.MAX_VALUE : count);
            }
            if ((types.length == 9) && (types[2] == Filter.class) && (types[3] == String[].class) && (types[4] == String.class)
                    && (types[5] == SortOrder.class)) {
                return findSortedEntries((String) args[0], (Class<?>) args[1], (Filter) args[2], (String) args[4], (SortOrder) args[5],
                        (Integer) args[6], (Integer) args[7]);
            }
            break;
        case "findPagedEntries":
            if ((types.length == 9) && (types[2] == Filter.class) && (types[3] == String[].class) && (types[4] == String.class)
                    && (types[5] == SortOrder.class)) {
                return findPagedEntries((String) args[0], (Class<?>) args[1], (Filter) args[2], (String) args[4], (SortOrder) args[5],
                        (Integer) args[6], (Integer) args[7]);
            }
            break;
        case "remove":
            if ((types.length == 4) && (types[2] == Filter.class)) {
                return remove((String) args[0], (Class<?>) args[1], (Filter) args[2], (Integer) args[3]);
            }
            if ((types.length == 2) && (types[0] == String.class) && (types[1] == Class.class)) {
                store.remove((String) args[0]);
                return null;
            }
            if ((types.length == 1) && (types[0] == String.class)) {
                store.remove((String) args[0]);
                return null;
            }
            if ((types.length == 1) && (types[0] == Object.class)) {
                store.remove(getDn(args[0]));
                return null;
            }
            break;
        case "getPersistenceType":
            return persistenceType;
        case "encodeTime":
            return args[1] == null ? null : ((Date) args[1]).toInstant().toString();
        case "decodeTime":
            return args[1] == null ? null : Date.from(Instant.parse((String) args[1]));
        case "hasBranchesSupport":
            return PersistenceEntryManager.PERSITENCE_TYPES.ldap.name().equals(persistenceType);
        case "hasExpirationSupport":
            return false;
        default:
            break;
        }

        throw new UnsupportedOperationException("InMemoryEntryManager does not implement " + name + " with "
                + types.length + " parameters");
    }

    protected void persist(Object entry) {
        String dn = getDn(entry);
        Object copy = copy(entry);
        if (isUpsertBackend()) {
            store.put(dn, copy);
            return;
        }

        if (store.putIfAbsent(dn, copy) != null) {
            throw duplicate(dn);
        }
    }

    protected void merge(Object entry) {
        String dn = getDn(entry);
        Object copy = copy(entry);
        if (store.computeIfPresent(dn, (k, v) -> copy) == null) {
            throw missing(dn);
        }
    }

    protected Object find(Class<?> entryClass, Object primaryKey) {
        String dn = String.valueOf(primaryKey);
        Object stored = store.get(dn);
        if ((stored == null) || !entryClass.isInstance(stored)) {
            throw missing(dn);
        }

        return copy(stored);
    }

    protected List<Object> findEntries(String baseDn, Class<?> entryClass, Filter filter, int count) {
        List<Object> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : new TreeMap<>(store).entrySet()) {
            if (result.size() >= count) {
                break;
            }
            if (isUnder(entry.getKey(), baseDn) && entryClass.isInstance(entry.getValue()) && matches(entry.getValue(), filter)) {
                result.add(copy(entry.getValue()));
            }
        }

        return result;
    }

    /**
     * The sorted search {@code findEntries(baseDn, class, filter, attributes, sortBy, sortOrder, start, count,
     * chunkSize)}: entries sorted by the {@code sortBy} attribute (missing values first), then by DN. A
     * {@code count} of 0 or less returns all entries from {@code start}; the chunk size is ignored.
     */
    protected List<Object> findSortedEntries(String baseDn, Class<?> entryClass, Filter filter, String sortBy, SortOrder sortOrder,
            int start, int count) {
        List<Object> matched = findEntries(baseDn, entryClass, filter, Integer.MAX_VALUE);
        Comparator<Object> order = Comparator.comparing(InMemoryEntryManager::getDn);
        io.jans.orm.annotation.DataEntry mapping = entryClass.getAnnotation(io.jans.orm.annotation.DataEntry.class);
        String[] keys = sortBy != null ? new String[] { sortBy } : mapping != null ? mapping.sortByName() : new String[0];
        for (int i = keys.length - 1; i >= 0; i--) {
            String key = keys[i];
            Comparator<Object> byAttribute = (left, right) -> {
                List<Object> leftValues = getValues(left, key);
                List<Object> rightValues = getValues(right, key);
                if (leftValues.isEmpty() || rightValues.isEmpty()) {
                    return Boolean.compare(!leftValues.isEmpty(), !rightValues.isEmpty());
                }
                return compare(leftValues.get(0), rightValues.get(0));
            };
            order = (sortOrder == SortOrder.DESCENDING ? byAttribute.reversed() : byAttribute).thenComparing(order);
        }
        matched.sort(order);

        int from = Math.min(Math.max(0, start), matched.size());
        int to = count <= 0 ? matched.size() : (int) Math.min((long) from + count, matched.size());

        return new ArrayList<>(matched.subList(from, to));
    }

    /**
     * {@code findPagedEntries(baseDn, class, filter, attributes, sortBy, sortOrder, start, count, chunkSize)}:
     * the page of {@link #findSortedEntries}, with the total number of matching entries.
     */
    protected PagedResult<Object> findPagedEntries(String baseDn, Class<?> entryClass, Filter filter, String sortBy, SortOrder sortOrder,
            int start, int count) {
        List<Object> page = findSortedEntries(baseDn, entryClass, filter, sortBy, sortOrder, start, count);

        PagedResult<Object> result = new PagedResult<>();
        result.setStart(start);
        result.setEntriesCount(page.size());
        result.setTotalEntriesCount(findEntries(baseDn, entryClass, filter, Integer.MAX_VALUE).size());
        result.setEntries(page);

        return result;
    }

    protected int remove(String baseDn, Class<?> entryClass, Filter filter, int count) {
        int removed = 0;
        for (Map.Entry<String, Object> entry : new TreeMap<>(store).entrySet()) {
            if ((count > 0) && (removed >= count)) {
                break;
            }
            if (isUnder(entry.getKey(), baseDn) && entryClass.isInstance(entry.getValue()) && matches(entry.getValue(), filter)
                    && store.remove(entry.getKey(), entry.getValue())) {
                removed++;
            }
        }

        return removed;
    }

    /**
     * @return the exception chain the SQL backend throws on an insert of an existing DN
     */
    public static EntryPersistenceException duplicate(String dn) {
        SQLException sqlException = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"jansClusterLock_pkey\"", SQL_UNIQUE_VIOLATION_STATE);
        QueryException queryException = new QueryException("Caught PSQLException for insert", sqlException);
        DuplicateEntryException duplicate = new DuplicateEntryException(dn, "Entry already exists: " + dn, queryException);

        return new EntryPersistenceException(String.format("Failed to persist entry: '%s'", dn), duplicate);
    }

    /**
     * @return the exception chain the SQL backend throws when an entry to read does not exist
     */
    public static EntryPersistenceException missing(String dn) {
        return new EntryPersistenceException(String.format("Failed to find entry: '%s'", dn),
                new MissingEntrySearchException(String.format("Failed to lookup entry by key: '%s'", dn)));
    }

    /**
     * @return the exception chain the SQL backend throws on an insert into a table that does not exist
     */
    public static EntryPersistenceException missingTable(String dn) {
        SQLException sqlException = new SQLException("ERROR: relation \"jansClusterLock\" does not exist", "42P01");
        QueryException queryException = new QueryException("Caught PSQLException for insert", sqlException);
        PersistenceException persistence = new PersistenceException("Failed to add entry", queryException);

        return new EntryPersistenceException(String.format("Failed to persist entry: '%s'", dn), persistence);
    }

    protected boolean isUpsertBackend() {
        return PersistenceEntryManager.PERSITENCE_TYPES.spanner.name().equals(persistenceType)
                || PersistenceEntryManager.PERSITENCE_TYPES.couchbase.name().equals(persistenceType);
    }

    protected static boolean isUnder(String dn, String baseDn) {
        if (baseDn == null) {
            return true;
        }
        String lowerDn = dn.toLowerCase(Locale.ROOT);
        String lowerBaseDn = baseDn.toLowerCase(Locale.ROOT);

        return lowerDn.equals(lowerBaseDn) || lowerDn.endsWith("," + lowerBaseDn);
    }

    /**
     * Evaluates a filter against an entry. {@code null} matches everything.
     */
    protected boolean matches(Object entry, Filter filter) {
        if (filter == null) {
            return true;
        }

        switch (filter.getType()) {
        case AND:
            for (Filter child : filter.getFilters()) {
                if (!matches(entry, child)) {
                    return false;
                }
            }
            return true;
        case OR:
            for (Filter child : filter.getFilters()) {
                if (matches(entry, child)) {
                    return true;
                }
            }
            return false;
        case NOT:
            return !matches(entry, filter.getFilters()[0]);
        case PRESENCE:
            return !getValues(entry, filter.getAttributeName()).isEmpty();
        case EQUALITY:
            return matchesValue(entry, filter, 0);
        case LESS_OR_EQUAL:
            return matchesValue(entry, filter, -1);
        case GREATER_OR_EQUAL:
            return matchesValue(entry, filter, 1);
        case SUBSTRING:
            return matchesSubstring(entry, filter);
        default:
            throw new UnsupportedOperationException("InMemoryEntryManager does not evaluate filter type " + filter.getType());
        }
    }

    private boolean matchesValue(Object entry, Filter filter, int direction) {
        List<Object> values = getValues(entry, filter.getAttributeName());
        Object assertion = filter.getAssertionValue();
        if (assertion == null) {
            return (direction == 0) && values.isEmpty();
        }

        for (Object value : values) {
            int comparison = compare(value, assertion);
            if ((direction == 0) && (comparison == 0)) {
                return true;
            }
            if ((direction < 0) && (comparison <= 0)) {
                return true;
            }
            if ((direction > 0) && (comparison >= 0)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesSubstring(Object entry, Filter filter) {
        for (Object value : getValues(entry, filter.getAttributeName())) {
            String text = String.valueOf(value);
            int position = 0;
            if (filter.getSubInitial() != null) {
                if (!text.startsWith(filter.getSubInitial())) {
                    continue;
                }
                position = filter.getSubInitial().length();
            }
            boolean matched = true;
            if (filter.getSubAny() != null) {
                for (String any : filter.getSubAny()) {
                    int index = text.indexOf(any, position);
                    if (index < 0) {
                        matched = false;
                        break;
                    }
                    position = index + any.length();
                }
            }
            if (matched && ((filter.getSubFinal() == null) || text.substring(position).endsWith(filter.getSubFinal()))) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static int compare(Object value, Object assertion) {
        if (value instanceof Date) {
            return ((Date) value).compareTo(toDate(assertion));
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString()).compareTo(new BigDecimal(assertion.toString()));
        }
        if (value instanceof Boolean) {
            return Boolean.compare((Boolean) value, Boolean.parseBoolean(assertion.toString()));
        }
        if ((value instanceof Comparable) && value.getClass().isInstance(assertion)) {
            return ((Comparable) value).compareTo(assertion);
        }

        return value.toString().compareTo(assertion.toString());
    }

    private static Date toDate(Object assertion) {
        if (assertion instanceof Date) {
            return (Date) assertion;
        }
        if (assertion instanceof Instant) {
            return Date.from((Instant) assertion);
        }

        return Date.from(Instant.parse(assertion.toString()));
    }

    /**
     * @return the non-null values of the field mapped to the attribute; empty when there is none
     */
    protected static List<Object> getValues(Object entry, String attributeName) {
        Field field = findAttributeField(entry.getClass(), attributeName);
        if (field == null) {
            return new ArrayList<>();
        }

        Object value = readField(field, entry);
        List<Object> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                if (item != null) {
                    result.add(item);
                }
            }
        } else if (value.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(value); i++) {
                Object item = Array.get(value, i);
                if (item != null) {
                    result.add(item);
                }
            }
        } else {
            result.add(value);
        }

        return result;
    }

    private static Field findAttributeField(Class<?> entryClass, String attributeName) {
        for (Field field : getAllFields(entryClass)) {
            AttributeName annotation = field.getAnnotation(AttributeName.class);
            if (annotation == null) {
                continue;
            }
            String name = annotation.name().isEmpty() ? field.getName() : annotation.name();
            if (name.equalsIgnoreCase(attributeName)) {
                return field;
            }
        }

        return null;
    }

    protected static String getDn(Object entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry is null");
        }
        for (Field field : getAllFields(entry.getClass())) {
            if (field.isAnnotationPresent(DN.class)) {
                Object dn = readField(field, entry);
                if (dn == null) {
                    throw new IllegalArgumentException("DN of entry is null");
                }
                return dn.toString();
            }
        }

        throw new IllegalArgumentException("Entry class " + entry.getClass() + " has no @DN field");
    }

    /**
     * @return a copy of the entry: a new instance with every field copied; dates, collections, maps and
     *         arrays are copied one level deep
     */
    @SuppressWarnings("unchecked")
    public static <T> T copy(T entry) {
        try {
            Constructor<?> constructor = entry.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            T result = (T) constructor.newInstance();
            for (Field field : getAllFields(entry.getClass())) {
                field.set(result, copyValue(field.get(entry)));
            }

            return result;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to copy entry of " + entry.getClass(), ex);
        }
    }

    private static Object copyValue(Object value) {
        if (value instanceof Date) {
            return new Date(((Date) value).getTime());
        }
        if (value instanceof List) {
            return new ArrayList<>((List<?>) value);
        }
        if (value instanceof Set) {
            return new LinkedHashSet<>((Set<?>) value);
        }
        if (value instanceof Map) {
            return new LinkedHashMap<>((Map<?, ?>) value);
        }
        if ((value != null) && value.getClass().isArray()) {
            int length = Array.getLength(value);
            Object result = Array.newInstance(value.getClass().getComponentType(), length);
            System.arraycopy(value, 0, result, 0, length);
            return result;
        }

        return value;
    }

    private static final Map<Class<?>, List<Field>> FIELDS = new ConcurrentHashMap<>();

    private static List<Field> getAllFields(Class<?> entryClass) {
        return FIELDS.computeIfAbsent(entryClass, type -> {
            List<Field> result = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (Class<?> current = type; (current != null) && (current != Object.class); current = current.getSuperclass()) {
                for (Field field : current.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);
                    if (seen.add(current.getName() + "#" + field.getName())) {
                        result.add(field);
                    }
                }
            }
            return result;
        });
    }

    private static Object readField(Field field, Object entry) {
        try {
            return field.get(entry);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Failed to read field " + field, ex);
        }
    }

    /**
     * @return field name to value, for comparing two entries in tests
     */
    public static Map<String, Object> describe(Object entry) {
        Map<String, Object> result = new HashMap<>();
        for (Field field : getAllFields(entry.getClass())) {
            result.put(field.getName(), readField(field, entry));
        }

        return result;
    }

}
