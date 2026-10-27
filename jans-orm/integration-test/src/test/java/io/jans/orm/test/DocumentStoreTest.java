/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.search.filter.Filter;
import io.jans.orm.test.model.Document;

/**
 * DB document store entry create and search by multi-valued service filter.
 * It's automated version of SqlDocumentStoreSample and LdapDocumentStoreSample.
 *
 * @author Yuriy Movchan Date: 09/09/2026
 */
public class DocumentStoreTest extends BaseOrmTest {

	private static final String DOCUMENTS_BASE_DN = "ou=document,o=jans";

	private Document persistedDocument;
	private String serviceName;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if ((entryManager != null) && (persistedDocument != null)) {
			try {
				entryManager.remove(persistedDocument.getDn(), Document.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void createDocument() {
		String inum = getRandomInum();
		serviceName = "test-service-" + inum;

		Document document = new Document();
		document.setInum(inum);
		document.setDn(String.format("inum=%s,%s", inum, DOCUMENTS_BASE_DN));
		document.setDocument("TEST DATA");
		document.setDisplayName(String.format("doc%s.txt", inum));
		document.setDescription("test description");
		document.setCreationDate(new Date());
		document.setJansEnabled(true);
		// jansService column is single valued in SQL schema, same as in production Document model
		document.setJansService(serviceName);

		entryManager.persist(document);

		persistedDocument = document;
	}

	@Test(dependsOnMethods = "createDocument")
	public void findDocumentByDn() {
		Document foundDocument = entryManager.find(Document.class, persistedDocument.getDn());

		assertNotNull(foundDocument);
		assertEquals(foundDocument.getDocument(), "TEST DATA");
		assertEquals(foundDocument.getDisplayName(), persistedDocument.getDisplayName());
		assertTrue(foundDocument.isJansEnabled());
		assertEquals(foundDocument.getJansService(), serviceName);
	}

	@Test(dependsOnMethods = "createDocument")
	public void searchDocumentByService() {
		Filter filter = Filter.createEqualityFilter("jansService", serviceName);
		List<Document> documents = entryManager.findEntries(DOCUMENTS_BASE_DN, Document.class, filter);

		assertNotNull(documents);
		assertEquals(documents.size(), 1);
		assertEquals(documents.get(0).getInum(), persistedDocument.getInum());
	}

}
