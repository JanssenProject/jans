/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.sql.operation.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import com.querydsl.core.QueryException;

import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.model.AttributeData;
import io.jans.orm.sql.model.BatchInsert;

/**
 * Unit tests for {@link SqlBatchExceptionMapper}
 *
 * @author Yuriy Movchan Date: 11/12/2026
 */
public class SqlBatchExceptionMapperTest {

	private static final String PG_DUPLICATE_MESSAGE = "ERROR: duplicate key value violates unique constraint \"jansToken_pkey\"\n"
			+ "  Detail: Key (doc_id)=(%s) already exists.";

	private final List<BatchInsert> inserts = buildInserts(5);

	@Test
	public void postgreSqlUniqueViolationMapsToDuplicateWithDn() {
		SQLException driverException = new SQLException(String.format(PG_DUPLICATE_MESSAGE, "key_2"), "23505");
		QueryException queryException = new QueryException("Caught SQLException for insert", driverException);

		PersistenceException mapped = SqlBatchExceptionMapper.map(queryException, inserts, 0, 4);

		assertTrue(mapped instanceof DuplicateEntryException);
		assertEquals(((DuplicateEntryException) mapped).getDn(), "uid=2,ou=test,o=jans");
		assertSame(mapped.getCause(), queryException);
	}

	@Test
	public void postgreSqlBatchNextExceptionMapsToDuplicateWithDn() {
		BatchUpdateException batchException = new BatchUpdateException("Batch entry 3 INSERT ... was aborted", "23505", new int[0]);
		batchException.setNextException(new SQLException(String.format(PG_DUPLICATE_MESSAGE, "key_3"), "23505"));
		QueryException queryException = new QueryException("Caught BatchUpdateException for insert", batchException);

		PersistenceException mapped = SqlBatchExceptionMapper.map(queryException, inserts, 1, 4);

		assertTrue(mapped instanceof DuplicateEntryException);
		assertEquals(((DuplicateEntryException) mapped).getDn(), "uid=3,ou=test,o=jans");
	}

	@Test
	public void nextExceptionIsSearchedWhenOuterStateIsGeneric() {
		BatchUpdateException batchException = new BatchUpdateException("Batch entry 0 was aborted", null, new int[0]);
		batchException.setNextException(new SQLException(String.format(PG_DUPLICATE_MESSAGE, "key_0"), "23505"));

		PersistenceException mapped = SqlBatchExceptionMapper.map(new QueryException(batchException), inserts, 0, 4);

		assertTrue(mapped instanceof DuplicateEntryException);
		assertEquals(((DuplicateEntryException) mapped).getDn(), "uid=0,ou=test,o=jans");
	}

	@Test
	public void mySqlDuplicateEntryMapsToDuplicateWithDn() {
		SQLException driverException = new SQLIntegrityConstraintViolationException(
				"Duplicate entry 'key_4' for key 'jansToken.PRIMARY'", "23000", 1062);

		PersistenceException mapped = SqlBatchExceptionMapper.map(new QueryException(driverException), inserts, 0, 4);

		assertTrue(mapped instanceof DuplicateEntryException);
		assertEquals(((DuplicateEntryException) mapped).getDn(), "uid=4,ou=test,o=jans");
	}

	@Test
	public void duplicateKeyOutsideFailingRangeReportsIndexRange() {
		SQLException driverException = new SQLException(String.format(PG_DUPLICATE_MESSAGE, "key_0"), "23505");

		PersistenceException mapped = SqlBatchExceptionMapper.map(new QueryException(driverException), inserts, 2, 3);

		assertTrue(mapped instanceof DuplicateEntryException);
		assertNull(((DuplicateEntryException) mapped).getDn());
		assertTrue(mapped.getMessage().contains("2..3"), mapped.getMessage());
	}

	@Test
	public void uniqueViolationWithoutKeyDetailReportsIndexRange() {
		SQLException driverException = new SQLException("ERROR: duplicate key value violates unique constraint \"jansToken_pkey\"", "23505");

		PersistenceException mapped = SqlBatchExceptionMapper.map(new QueryException(driverException), inserts, 1, 3);

		assertTrue(mapped instanceof DuplicateEntryException);
		assertNull(((DuplicateEntryException) mapped).getDn());
		assertTrue(mapped.getMessage().contains("1..3"), mapped.getMessage());
		assertTrue(mapped.getMessage().contains("uid=1,ou=test,o=jans"), mapped.getMessage());
		assertTrue(mapped.getMessage().contains("uid=3,ou=test,o=jans"), mapped.getMessage());
	}

	@Test
	public void singleRowStatementWithoutKeyDetailReportsItsDn() {
		SQLException driverException = new SQLException("ERROR: duplicate key value violates unique constraint \"jansToken_uid\"", "23505");

		PersistenceException mapped = SqlBatchExceptionMapper.map(new QueryException(driverException), inserts, 3, 3);

		assertTrue(mapped instanceof DuplicateEntryException);
		assertEquals(((DuplicateEntryException) mapped).getDn(), "uid=3,ou=test,o=jans");
	}

	@Test
	public void sameKeyTwiceReportsSecondOccurrence() {
		List<BatchInsert> withDuplicate = new ArrayList<>(inserts);
		withDuplicate.add(new BatchInsert("UID=1,ou=test,o=jans", "key_1", "jansToken", Collections.<AttributeData>emptyList()));
		SQLException driverException = new SQLException(String.format(PG_DUPLICATE_MESSAGE, "key_1"), "23505");

		PersistenceException mapped = SqlBatchExceptionMapper.map(new QueryException(driverException), withDuplicate, 0, 5);

		assertEquals(((DuplicateEntryException) mapped).getDn(), "UID=1,ou=test,o=jans");
	}

	@Test
	public void otherSqlStateMapsToPersistenceException() {
		SQLException driverException = new SQLException("ERROR: could not serialize access", "40001");
		QueryException queryException = new QueryException(driverException);

		PersistenceException mapped = SqlBatchExceptionMapper.map(queryException, inserts, 0, 4);

		assertFalse(mapped instanceof DuplicateEntryException);
		assertSame(mapped.getCause(), queryException);
	}

	@Test
	public void mySqlOtherIntegrityViolationIsNotDuplicate() {
		SQLException driverException = new SQLIntegrityConstraintViolationException(
				"Cannot add or update a child row: a foreign key constraint fails", "23000", 1452);

		assertFalse(SqlBatchExceptionMapper.isUniqueViolation(driverException));
		assertFalse(SqlBatchExceptionMapper.map(driverException, inserts, 0, 4) instanceof DuplicateEntryException);
	}

	@Test
	public void persistenceExceptionWithoutUniqueViolationIsKept() {
		PersistenceException original = new PersistenceException("Failed to find attribute type for 'x'");

		assertSame(SqlBatchExceptionMapper.map(original, inserts, 0, 4), original);
	}

	@Test
	public void suppressedUniqueViolationIsFound() {
		RuntimeException wrapper = new RuntimeException("wrapper");
		wrapper.addSuppressed(new SQLException("duplicate", "23505"));

		assertTrue(SqlBatchExceptionMapper.isUniqueViolation(wrapper));
	}

	@Test
	public void cyclicNextExceptionChainTerminates() {
		SQLException first = new SQLException("first", "08006");
		SQLException second = new SQLException("second", "08006");
		first.setNextException(second);
		second.setNextException(first);

		assertFalse(SqlBatchExceptionMapper.isUniqueViolation(first));
	}

	private static List<BatchInsert> buildInserts(int count) {
		List<BatchInsert> result = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			result.add(new BatchInsert(String.format("uid=%d,ou=test,o=jans", i), "key_" + i, "jansToken",
					Collections.<AttributeData>emptyList()));
		}

		return result;
	}

}
