/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.security.SecureRandom;

import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import io.jans.orm.test.model.SimpleBinaryEntry;

/**
 * Binary data persistence with @BinaryData annotation. jansDataBin column should has
 * binary type (bytea/BLOB) to store raw binary data without base64 conversion and
 * jansDataStr column should has string type to check base64 fallback.
 *
 * Table required for this test (PostgreSQL):
 *
 * CREATE TABLE "jansBinEntry" (doc_id character varying(64) PRIMARY KEY,
 *     dn character varying(128), "objectClass" character varying(48),
 *     "displayName" character varying(128), "jansDataBin" bytea, "jansDataStr" text);
 *
 * Test is skipped when DB has no jansBinEntry table.
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public class BinaryDataTest extends BaseOrmTest {

	private SimpleBinaryEntry persistedEntry;

	private byte[] binaryData;
	private byte[] binaryData2;

	@AfterClass(alwaysRun = true)
	public void cleanup() {
		if ((entryManager != null) && (persistedEntry != null)) {
			try {
				entryManager.remove(persistedEntry.getDn(), SimpleBinaryEntry.class);
			} catch (Exception ex) {
				// Test cleanup should not fail test run
			}
		}
	}

	@Test
	public void createEntryWithBinaryData() {
		binaryData = new byte[1024];
		binaryData2 = new byte[2048];
		new SecureRandom().nextBytes(binaryData);
		new SecureRandom().nextBytes(binaryData2);

		SimpleBinaryEntry newEntry = new SimpleBinaryEntry();
		newEntry.setDn(String.format("inum=%s,ou=binary,o=jans", getRandomInum()));
		newEntry.setDisplayName("Binary data test");
		newEntry.setDataBin(binaryData);
		newEntry.setDataStr(binaryData);

		try {
			entryManager.persist(newEntry);
		} catch (Exception ex) {
			throw new SkipException("DB has no jansBinEntry table/objectClass. See test javadoc for DDL: " + ex.getMessage());
		}

		persistedEntry = newEntry;
	}

	@Test(dependsOnMethods = "createEntryWithBinaryData")
	public void readBinaryData() {
		SimpleBinaryEntry foundEntry = entryManager.find(SimpleBinaryEntry.class, persistedEntry.getDn());

		assertNotNull(foundEntry);
		assertEquals(foundEntry.getDataBin(), binaryData);
		assertEquals(foundEntry.getDataStr(), binaryData);
	}

	@Test(dependsOnMethods = "readBinaryData")
	public void updateBinaryData() {
		SimpleBinaryEntry foundEntry = entryManager.find(SimpleBinaryEntry.class, persistedEntry.getDn());
		foundEntry.setDataBin(binaryData2);
		foundEntry.setDataStr(binaryData2);

		entryManager.merge(foundEntry);

		SimpleBinaryEntry updatedEntry = entryManager.find(SimpleBinaryEntry.class, persistedEntry.getDn());
		assertEquals(updatedEntry.getDataBin(), binaryData2);
		assertEquals(updatedEntry.getDataStr(), binaryData2);
	}

	@Test(dependsOnMethods = "updateBinaryData")
	public void noOpMergeKeepsBinaryData() {
		SimpleBinaryEntry foundEntry = entryManager.find(SimpleBinaryEntry.class, persistedEntry.getDn());

		entryManager.merge(foundEntry);

		SimpleBinaryEntry unchangedEntry = entryManager.find(SimpleBinaryEntry.class, persistedEntry.getDn());
		assertEquals(unchangedEntry.getDataBin(), binaryData2);
		assertEquals(unchangedEntry.getDataStr(), binaryData2);
	}

}
