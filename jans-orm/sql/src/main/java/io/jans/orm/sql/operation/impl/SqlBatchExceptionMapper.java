/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.sql.operation.impl;

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.sql.model.BatchInsert;

/**
 * Maps a failure of an atomic batch insert to {@link DuplicateEntryException} (unique key violation)
 * or {@link PersistenceException} (anything else).
 *
 * <p>A unique violation is SQLState {@code 23505} (PostgreSQL) or SQLState {@code 23000} with vendor
 * error {@code 1062} (MySQL, MariaDB). The whole exception graph is searched: causes, suppressed
 * exceptions and {@link SQLException#getNextException()} chains, because QueryDSL wraps the driver
 * exception and a JDBC batch reports the row error as the next exception of a
 * {@link java.sql.BatchUpdateException}.</p>
 *
 * <p>The DN of the failing entry is reported when the driver message names the duplicate
 * {@code doc_id} value and that value is a key of the failing entries
 * (PostgreSQL: {@code Key (doc_id)=(...) already exists}; MySQL: {@code Duplicate entry '...' for key 'PRIMARY'}).
 * A failing statement with one row reports that row's DN. Otherwise the message carries the batch
 * index range of the failing statement.</p>
 *
 * @author Yuriy Movchan Date: 11/12/2026
 */
public final class SqlBatchExceptionMapper {

	public static final String POSTGRESQL_UNIQUE_VIOLATION_STATE = "23505";
	public static final String INTEGRITY_CONSTRAINT_VIOLATION_STATE = "23000";
	public static final int MYSQL_DUPLICATE_ENTRY_ERROR_CODE = 1062;

	private static final Pattern POSTGRESQL_DUPLICATE_DOC_ID = Pattern.compile("Key \\(\"?doc_id\"?\\)=\\((.*?)\\) already exists");
	private static final Pattern MYSQL_DUPLICATE_DOC_ID = Pattern.compile("Duplicate entry '(.*?)' for key '(?:[^']*\\.)?PRIMARY'");

	private SqlBatchExceptionMapper() {
	}

	/**
	 * Maps a batch failure.
	 *
	 * @param ex failure
	 * @param inserts all entries of the batch
	 * @param fromIndex index of the first entry of the failing statement (inclusive)
	 * @param toIndex index of the last entry of the failing statement (inclusive)
	 * @return {@link DuplicateEntryException} for a unique violation, otherwise {@link PersistenceException}
	 *         with {@code ex} as the cause
	 */
	public static PersistenceException map(Throwable ex, List<BatchInsert> inserts, int fromIndex, int toIndex) {
		if (findUniqueViolation(ex) == null) {
			if (ex instanceof PersistenceException) {
				return (PersistenceException) ex;
			}

			return new PersistenceException(String.format("Failed to add batch entries %d..%d", fromIndex, toIndex), ex);
		}

		String duplicateKey = findDuplicateKey(ex);
		if (duplicateKey != null) {
			// The second occurrence of a key fails, so search backwards
			for (int i = toIndex; i >= fromIndex; i--) {
				BatchInsert insert = inserts.get(i);
				if (duplicateKey.equals(insert.getKey())) {
					return new DuplicateEntryException(insert.getDn(), "Entry already exists: " + insert.getDn(), ex);
				}
			}
		}

		if (fromIndex == toIndex) {
			// The failing statement has one row: it is the duplicate
			String dn = inserts.get(fromIndex).getDn();
			return new DuplicateEntryException(dn, "Entry already exists: " + dn, ex);
		}

		String message = String.format("Entry already exists: one of batch entries %d..%d ('%s' .. '%s')", fromIndex, toIndex,
				inserts.get(fromIndex).getDn(), inserts.get(toIndex).getDn());

		return new DuplicateEntryException(null, message, ex);
	}

	public static boolean isUniqueViolation(Throwable ex) {
		return findUniqueViolation(ex) != null;
	}

	static SQLException findUniqueViolation(Throwable ex) {
		for (Throwable current : exceptionGraph(ex)) {
			if ((current instanceof SQLException) && isUniqueViolation((SQLException) current)) {
				return (SQLException) current;
			}
		}

		return null;
	}

	static boolean isUniqueViolation(SQLException ex) {
		String sqlState = ex.getSQLState();
		if (POSTGRESQL_UNIQUE_VIOLATION_STATE.equals(sqlState)) {
			return true;
		}

		return INTEGRITY_CONSTRAINT_VIOLATION_STATE.equals(sqlState) && (ex.getErrorCode() == MYSQL_DUPLICATE_ENTRY_ERROR_CODE);
	}

	/**
	 * @return the duplicate {@code doc_id} value named by the driver message, or {@code null}
	 */
	static String findDuplicateKey(Throwable ex) {
		for (Throwable current : exceptionGraph(ex)) {
			if (!(current instanceof SQLException) || (current.getMessage() == null)) {
				continue;
			}

			String message = current.getMessage();
			Matcher matcher = POSTGRESQL_DUPLICATE_DOC_ID.matcher(message);
			if (matcher.find()) {
				return matcher.group(1);
			}

			matcher = MYSQL_DUPLICATE_DOC_ID.matcher(message);
			if (matcher.find()) {
				return matcher.group(1);
			}
		}

		return null;
	}

	private static List<Throwable> exceptionGraph(Throwable ex) {
		List<Throwable> result = new ArrayList<>();
		Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		Deque<Throwable> queue = new ArrayDeque<>();
		if (ex != null) {
			queue.add(ex);
		}

		while (!queue.isEmpty()) {
			Throwable current = queue.poll();
			if (!visited.add(current)) {
				continue;
			}
			result.add(current);

			if (current.getCause() != null) {
				queue.add(current.getCause());
			}
			for (Throwable suppressed : current.getSuppressed()) {
				queue.add(suppressed);
			}
			if ((current instanceof SQLException) && (((SQLException) current).getNextException() != null)) {
				queue.add(((SQLException) current).getNextException());
			}
		}

		return result;
	}

}
