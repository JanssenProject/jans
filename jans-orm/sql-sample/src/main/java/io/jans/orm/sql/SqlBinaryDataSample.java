/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.security.SecureRandom;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleBinaryEntry;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * Sample which check binary data persistence in binary and string columns.
 * Table required for this sample:
 *
 * CREATE TABLE "jansBinEntry" (doc_id character varying(64) PRIMARY KEY,
 *     dn character varying(128), "objectClass" character varying(48),
 *     "displayName" character varying(128), "jansData" bytea, "jansDataStr" text);
 *
 * @author Yuriy Movchan Date: 09/08/2026
 */
public final class SqlBinaryDataSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlBinaryDataSample.class);

    private SqlBinaryDataSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        byte[] binaryData = new byte[1024];
        byte[] binaryData2 = new byte[2048];
        new SecureRandom().nextBytes(binaryData);
        new SecureRandom().nextBytes(binaryData2);

        // Add dummy entry with binary data
        SimpleBinaryEntry newEntry = new SimpleBinaryEntry();
        newEntry.setDn(String.format("inum=%s,ou=binary,o=jans", System.currentTimeMillis()));
        newEntry.setDisplayName("Binary data sample");
        newEntry.setData(binaryData);
        newEntry.setDataStr(binaryData);

        sqlEntryManager.persist(newEntry);
        LOG.info("Added entry: {}", newEntry);

        // Find added entry and check binary data
        SimpleBinaryEntry foundEntry = sqlEntryManager.find(SimpleBinaryEntry.class, newEntry.getDn());
        LOG.info("Found entry: {}", foundEntry);

        checkEquals("data after persist", binaryData, foundEntry.getData());
        checkEquals("dataStr after persist", binaryData, foundEntry.getDataStr());

        // Update binary data
        foundEntry.setData(binaryData2);
        foundEntry.setDataStr(binaryData2);
        sqlEntryManager.merge(foundEntry);

        SimpleBinaryEntry updatedEntry = sqlEntryManager.find(SimpleBinaryEntry.class, newEntry.getDn());
        LOG.info("Updated entry: {}", updatedEntry);

        checkEquals("data after merge", binaryData2, updatedEntry.getData());
        checkEquals("dataStr after merge", binaryData2, updatedEntry.getDataStr());

        // Remove entry
        sqlEntryManager.remove(newEntry.getDn(), SimpleBinaryEntry.class);
        LOG.info("Removed entry: {}", newEntry.getDn());

        sqlEntryManager.destroy();
    }

    private static void checkEquals(String name, byte[] expected, byte[] actual) {
        if (Arrays.equals(expected, actual)) {
            LOG.info("OK: {} is equal to original binary data", name);
        } else {
            LOG.error("FAILURE: {} is not equal to original binary data", name);
        }
    }

}
