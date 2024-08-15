/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.ldap;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.ldap.impl.LdapEntryManager;
import io.jans.orm.ldap.model.Document;
import io.jans.orm.ldap.persistence.LdapEntryManagerSample;
import io.jans.orm.search.filter.Filter;

/**
 * @author Yuriy Movchan Date: 08/08/2024
 */
public final class LdapDocumentStoreSample {

    private static final Logger LOG = LoggerFactory.getLogger(LdapDocumentStoreSample.class);

    private LdapDocumentStoreSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        LdapEntryManagerSample entryManagerSample = new LdapEntryManagerSample();

        // Create LDAP entry manager
        LdapEntryManager entryManager = entryManagerSample.createLdapEntryManager();
        
        Document oxDocument = new Document();
		oxDocument.setInum(String.valueOf(System.currentTimeMillis()));
		String dn = String.format("inum=%s,ou=document,o=jans", oxDocument.getInum());

		oxDocument.setDocument("TEST DATA");
    	oxDocument.setDisplayName(String.format("doc%s.txt", oxDocument.getInum()));
		oxDocument.setDn(dn);
		oxDocument.setDescription("test description");
		oxDocument.setJansEnabled(true);
		oxDocument.setJansService(Arrays.asList("jans-config-api", "jans-kc-link", "jans-scim"));

		entryManager.persist(oxDocument);
    	
        // Find all documents which have specified object classes defined in Document
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
