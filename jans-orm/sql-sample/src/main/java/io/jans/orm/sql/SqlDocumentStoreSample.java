/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.Document;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 08/08/2024
 */
public final class SqlDocumentStoreSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlDocumentStoreSample.class);

    private SqlDocumentStoreSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample entryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager entryManager = entryManagerSample.createSqlEntryManager();
        
        Document document = new Document();
		document.setInum(String.valueOf(System.currentTimeMillis()));
		String dn = String.format("inum=%s,ou=document,o=jans", document.getInum());

		document.setDocument("TEST DATA");
    	document.setDisplayName(String.format("doc%s.txt", document.getInum()));
		document.setDn(dn);
		document.setDescription("test description");
		document.setJansEnabled(true);
		document.setJansService(Arrays.asList("jans-config-api", "jans-kc-link", "jans-scim"));

		entryManager.persist(document);

        // Find all documents which have entryManagerSample object classes defined in Document
        List<Document> documents = entryManager.findEntries("o=jans", Document.class, null);
        for (Document doc : documents) {
            LOG.debug("Document with display name: " + doc.getDisplayName());
        }

        Filter filter = Filter.createEqualityFilter("jansService", "jans-kc-link").multiValued();
        List<Document> documentsByFilter = entryManager.findEntries("o=jans", Document.class, filter);
        for (Document doc : documentsByFilter) {
            LOG.debug("Found document with display name: " + doc.getDisplayName());
        }
    }

}
