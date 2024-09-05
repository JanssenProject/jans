/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.service;

import io.jans.service.document.store.provider.DocumentStoreProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class DocumentStoreService extends BaseDocumentStoreService {

	@Inject
    private DocumentStoreProvider documentStoreProvider;

	@Override
	protected DocumentStoreProvider getDocumentStoreProvider() {
		return documentStoreProvider;
	}

}
