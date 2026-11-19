/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.orm.sql.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.expectThrows;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import io.jans.orm.event.DeleteNotifier;
import io.jans.orm.exception.EntryDeleteException;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.orm.sql.operation.SqlOperationService;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;

public class SqlEntryManagerTest {

	private static final String DN = "uid=1,ou=test,o=jans";
	private static final String[] OBJECT_CLASSES = { "jansTest" };

	private SqlOperationService operationService;
	private DeleteNotifier subscriber;
	private SqlEntryManager entryManager;

	@BeforeMethod
	public void setUp() {
		operationService = mock(SqlOperationService.class);
		when(operationService.getConnectionProvider()).thenReturn(mock(SqlConnectionProvider.class));
		subscriber = mock(DeleteNotifier.class);

		entryManager = new SqlEntryManager(operationService);
		entryManager.addDeleteSubscriber(subscriber);
	}

	@Test
	public void removeByDn_notifiesAfterRemoveOnlyWhenTheRemovalCommits() throws Exception {
		ArgumentCaptor<Runnable> afterCommit = ArgumentCaptor.forClass(Runnable.class);

		entryManager.removeByDn(DN, OBJECT_CLASSES);

		verify(subscriber).onBeforeRemove(DN, OBJECT_CLASSES);
		verify(operationService).delete(anyString(), eq("jansTest"));
		verify(operationService).runAfterCommit(afterCommit.capture());
		verify(subscriber, never()).onAfterRemove(any(), any());

		afterCommit.getValue().run();

		verify(subscriber).onAfterRemove(DN, OBJECT_CLASSES);
	}

	@Test
	public void removeRecursivelyFromDn_notifiesAfterRemoveOnlyWhenTheRemovalCommits() throws Exception {
		ArgumentCaptor<Runnable> afterCommit = ArgumentCaptor.forClass(Runnable.class);

		entryManager.removeRecursivelyFromDn(DN, OBJECT_CLASSES);

		verify(operationService).deleteRecursively(anyString(), eq("jansTest"));
		verify(operationService).runAfterCommit(afterCommit.capture());
		verify(subscriber, never()).onAfterRemove(any(), any());

		afterCommit.getValue().run();

		verify(subscriber).onAfterRemove(DN, OBJECT_CLASSES);
	}

	@Test
	public void removeByDn_callerChangesObjectClassesBeforeCommit_notifiesRemovedObjectClasses() throws Exception {
		ArgumentCaptor<Runnable> afterCommit = ArgumentCaptor.forClass(Runnable.class);
		String[] objectClasses = { "jansTest" };

		entryManager.removeByDn(DN, objectClasses);
		objectClasses[0] = "jansOther";
		verify(operationService).runAfterCommit(afterCommit.capture());
		afterCommit.getValue().run();

		ArgumentCaptor<String[]> notified = ArgumentCaptor.forClass(String[].class);
		verify(subscriber).onAfterRemove(eq(DN), notified.capture());
		assertEquals(notified.getValue(), new String[] { "jansTest" });
	}

	@Test
	public void removeByDn_withoutTransaction_notifiesImmediately() {
		doAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(operationService).runAfterCommit(any());

		entryManager.removeByDn(DN, OBJECT_CLASSES);

		verify(subscriber).onAfterRemove(DN, OBJECT_CLASSES);
	}

	@Test
	public void removeByDn_deleteFails_neverNotifiesAfterRemove() throws Exception {
		doThrow(new EntryNotFoundException("failed")).when(operationService).delete(anyString(), anyString());

		expectThrows(EntryDeleteException.class, () -> entryManager.removeByDn(DN, OBJECT_CLASSES));

		verify(operationService, never()).runAfterCommit(any());
		verify(subscriber, never()).onAfterRemove(any(), any());
	}
}
