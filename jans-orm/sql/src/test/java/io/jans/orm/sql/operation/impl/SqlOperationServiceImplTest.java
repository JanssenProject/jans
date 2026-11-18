/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.sql.operation.impl;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.mockito.InOrder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.SQLQueryFactory;

import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.sql.operation.SupportedDbType;

public class SqlOperationServiceImplTest {

	private SqlConnectionProvider connectionProvider;
	private Connection connection;
	private SQLQueryFactory pooledQueryFactory;
	private SqlOperationServiceImpl operationService;

	@BeforeMethod
	public void setUp() throws SQLException {
		connectionProvider = mock(SqlConnectionProvider.class);
		connection = mock(Connection.class);
		pooledQueryFactory = new SQLQueryFactory(new Configuration(MySQLTemplates.DEFAULT), () -> connection);

		when(connectionProvider.getSqlQueryFactory()).thenReturn(pooledQueryFactory);
		when(connectionProvider.getSqlTemplates()).thenReturn(MySQLTemplates.DEFAULT);
		when(connectionProvider.getDbType()).thenReturn(SupportedDbType.MYSQL);
		when(connectionProvider.getSchemaName()).thenReturn("jans");
		when(connectionProvider.getConnection()).thenReturn(connection);
		when(connection.getAutoCommit()).thenReturn(true);

		operationService = new SqlOperationServiceImpl(new Properties(), connectionProvider);
	}

	@Test
	public void executeInTransaction_success_commitsRestoresAutoCommitAndClosesConnection() throws Exception {
		String result = operationService.executeInTransaction(() -> "done");

		assertEquals(result, "done");
		InOrder order = inOrder(connection);
		order.verify(connection).setAutoCommit(false);
		order.verify(connection).commit();
		order.verify(connection).setAutoCommit(true);
		order.verify(connection).close();
		verify(connection, never()).rollback();
	}

	@Test
	public void executeInTransaction_callbackThrows_rollsBackAndRethrowsSameException() throws Exception {
		IllegalStateException failure = new IllegalStateException("boom");

		IllegalStateException thrown = expectThrows(IllegalStateException.class,
				() -> operationService.executeInTransaction(() -> {
					throw failure;
				}));

		assertSame(thrown, failure);
		verify(connection).rollback();
		verify(connection, never()).commit();
		verify(connection).setAutoCommit(true);
		verify(connection).close();
	}

	@Test
	public void executeInTransaction_checkedException_rethrownUnchanged() throws Exception {
		Exception failure = new Exception("checked");

		Exception thrown = expectThrows(Exception.class, () -> operationService.executeInTransaction(() -> {
			throw failure;
		}));

		assertSame(thrown, failure);
		verify(connection).rollback();
	}

	@Test
	public void executeInTransaction_rollbackFails_doesNotRestoreAutoCommitButClosesConnection() throws Exception {
		doThrow(new SQLException("rollback failed")).when(connection).rollback();
		RuntimeException failure = new RuntimeException("boom");

		RuntimeException thrown = expectThrows(RuntimeException.class, () -> operationService.executeInTransaction(() -> {
			throw failure;
		}));

		assertSame(thrown, failure);
		assertEquals(thrown.getSuppressed().length, 1);
		verify(connection, never()).setAutoCommit(true);
		verify(connection).close();
	}

	@Test
	public void executeInTransaction_commitFails_rollsBackAndThrows() throws Exception {
		doThrow(new SQLException("commit failed")).when(connection).commit();

		expectThrows(SQLException.class, () -> operationService.executeInTransaction(() -> "x"));

		verify(connection).rollback();
		verify(connection).close();
	}

	@Test
	public void executeInTransaction_nestedCall_joinsOuterTransaction() throws Exception {
		AtomicBoolean innerRan = new AtomicBoolean();

		operationService.executeInTransaction(() -> operationService.executeInTransaction(() -> {
			innerRan.set(true);
			return null;
		}));

		assertTrue(innerRan.get());
		verify(connectionProvider, times(1)).getConnection();
		verify(connection, times(1)).commit();
		verify(connection, times(1)).close();
	}

	@Test
	public void executeInTransaction_bindsQueryFactoryOnlyWhileRunning() throws Exception {
		assertFalse(operationService.isInTransaction());
		assertSame(operationService.queryFactory(), pooledQueryFactory);

		operationService.executeInTransaction(() -> {
			assertTrue(operationService.isInTransaction());
			assertNotSame(operationService.queryFactory(), pooledQueryFactory);
			return null;
		});

		assertFalse(operationService.isInTransaction());
		assertSame(operationService.queryFactory(), pooledQueryFactory);
	}

	@Test
	public void executeInTransaction_afterFailure_threadIsUnbound() throws Exception {
		expectThrows(RuntimeException.class, () -> operationService.executeInTransaction(() -> {
			throw new RuntimeException("boom");
		}));

		assertFalse(operationService.isInTransaction());
	}

	@Test
	public void executeInTransaction_otherThread_doesNotSeeTransaction() throws Exception {
		AtomicBoolean otherThreadInTransaction = new AtomicBoolean(true);

		operationService.executeInTransaction(() -> {
			Thread thread = new Thread(() -> otherThreadInTransaction.set(operationService.isInTransaction()));
			thread.start();
			thread.join();
			return null;
		});

		assertFalse(otherThreadInTransaction.get());
	}

	@Test
	public void executeInTransaction_noConnection_throwsPersistenceException() {
		when(connectionProvider.getConnection()).thenThrow(new IllegalStateException("pool exhausted"));

		expectThrows(PersistenceException.class, () -> operationService.executeInTransaction(() -> "x"));
	}

	@Test
	public void executeInTransaction_nullCallback_throws() {
		expectThrows(RuntimeException.class, () -> operationService.executeInTransaction(null));
	}

	@Test
	public void lookupForUpdate_outsideTransaction_throwsIllegalState() {
		expectThrows(IllegalStateException.class, () -> operationService.lookupForUpdate("key", "jansPar"));
	}

}
