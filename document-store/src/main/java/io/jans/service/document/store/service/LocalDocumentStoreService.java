/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package io.jans.service.document.store.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.jans.service.document.store.LocalDocumentStore;
import io.jans.service.document.store.provider.DocumentStoreProvider;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class LocalDocumentStoreService extends BaseDocumentStoreService {

    @Inject
    @LocalDocumentStore
    private DocumentStoreProvider documentStoreProvider;

	@Override
	protected DocumentStoreProvider getDocumentStoreProvider() {
		return documentStoreProvider;
	}


}
