/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.event.DeleteNotifier;
import io.jans.orm.model.AttributeData;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.SimpleClient;

/**
 * Client entry create, search and removal with DeleteNotifier and exportEntry.
 * It's automated version of SqlSimpleClientSample and SqlSimpleClientWithDeleteNotifierSample.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class ClientTest extends BaseOrmTest {

	private static final String CLIENTS_BASE_DN = "ou=clients,o=jans";

	private SimpleClient persistedClient;
	private String clientName;

	private AtomicInteger beforeRemoveCount = new AtomicInteger();
	private AtomicInteger afterRemoveCount = new AtomicInteger();
	private List<AttributeData> exportedEntry;

	private DeleteNotifier deleteNotifier;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if (entryManager == null) {
			return;
		}

		if (deleteNotifier != null) {
			entryManager.removeDeleteSubscriber(deleteNotifier);
		}

		if (persistedClient != null) {
			try {
				entryManager.remove(persistedClient.getDn(), SimpleClient.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void createClient() {
		String inum = getRandomInum();
		clientName = "test_acr_" + inum;

		SimpleClient newClient = new SimpleClient();
		newClient.setDn(String.format("inum=%s,%s", inum, CLIENTS_BASE_DN));
		newClient.setDefaultAcrValues(new String[] { clientName });
		newClient.setClientName(clientName);

		entryManager.persist(newClient);

		persistedClient = newClient;
	}

	@Test(dependsOnMethods = "createClient")
	public void searchClientByDisplayName() {
		Filter filter = Filter.createEqualityFilter("displayName", clientName);
		List<SimpleClient> results = entryManager.findEntries(CLIENTS_BASE_DN, SimpleClient.class, filter);

		assertNotNull(results);
		assertEquals(results.size(), 1);

		String[] acrs = results.get(0).getDefaultAcrValues();
		assertNotNull(acrs);
		assertEquals(acrs.length, 1);
		assertEquals(acrs[0], clientName);
	}

	@Test(dependsOnMethods = "searchClientByDisplayName")
	public void removeClientWithDeleteNotifier() {
		deleteNotifier = new DeleteNotifier() {
			@Override
			public void onBeforeRemove(String dn, String[] objectClasses) {
				beforeRemoveCount.incrementAndGet();
				exportedEntry = entryManager.exportEntry(dn, objectClasses[0]);
			}

			@Override
			public void onAfterRemove(String dn, String[] objectClasses) {
				afterRemoveCount.incrementAndGet();
			}
		};
		entryManager.addDeleteSubscriber(deleteNotifier);

		entryManager.removeRecursively(persistedClient.getDn(), SimpleClient.class);

		assertEquals(beforeRemoveCount.get(), 1);
		assertEquals(afterRemoveCount.get(), 1);

		// Entry should be exported in onBeforeRemove before it was removed
		assertNotNull(exportedEntry);
		assertTrue(exportedEntry.size() > 0);

		assertFalse(entryManager.contains(persistedClient.getDn(), SimpleClient.class));
		persistedClient = null;
	}

}
