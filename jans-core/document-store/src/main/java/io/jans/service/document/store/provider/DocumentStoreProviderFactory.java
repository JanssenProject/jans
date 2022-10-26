/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.provider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.LocalDocumentStore;
import io.jans.service.document.store.conf.DocumentStoreType;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class DocumentStoreProviderFactory {

    @Inject
    private Logger log;

    @Inject
    private DocumentStoreConfiguration documentStoreConfiguration;

    @Inject
    @Any
    private Instance<DocumentStoreProvider> instance;

    @Produces
    @ApplicationScoped
    public DocumentStoreProvider getDocumentStoreProvider() {
        log.debug("Started to create document store provider");


        return getDocumentStoreProvider(documentStoreConfiguration);
    }

    public DocumentStoreProvider getDocumentStoreProvider(DocumentStoreConfiguration documentStoreConfiguration) {
		DocumentStoreType documentStoreType = documentStoreConfiguration.getDocumentStoreType();

        if (documentStoreType == null) {
            log.error("Failed to initialize DocumentStoreProvider, DocumentStoreProviderType is null. Fallback to LOCAL type.");
            documentStoreType = DocumentStoreType.LOCAL;
        }

        // Create proxied bean
        DocumentStoreProvider documentStoreProvider = null;
        switch (documentStoreType) {
            case LOCAL:
            	documentStoreProvider = instance.select(LocalDocumentStoreProvider.class).get();
                break;
            case JCA:
            	documentStoreProvider = instance.select(JcaDocumentStoreProvider.class).get();
                break;
            case WEB_DAV:
            	documentStoreProvider = instance.select(WebDavDocumentStoreProvider.class).get();
                break;
        }

        if (documentStoreProvider == null) {
            throw new RuntimeException("Failed to initialize DocumentStoreProvider, DocumentStoreProviderType is unsupported: " + documentStoreType);
        }

        documentStoreProvider.create();

        return documentStoreProvider;
	}

    @Produces
    @ApplicationScoped
    @LocalDocumentStore
    public DocumentStoreProvider getLocalDocumentStoreProvider() {
        log.debug("Started to create local document store provider");

        DocumentStoreType documentStoreType = DocumentStoreType.LOCAL;
        DocumentStoreProvider documentStoreProvider = instance.select(LocalDocumentStoreProvider.class).get();

        if (documentStoreProvider == null) {
            throw new RuntimeException("Failed to initialize DocumentStoreProvider, DocumentStoreProviderType is unsupported: " + documentStoreType);
        }

        documentStoreProvider.create();

        return documentStoreProvider;
    }

}
