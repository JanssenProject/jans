/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package org.gluu.service.document.store.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.inject.Inject;

import org.gluu.service.document.store.conf.DocumentStoreType;
import org.gluu.service.document.store.provider.DocumentStore;
import org.gluu.service.document.store.provider.DocumentStoreProvider;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@SuppressWarnings("rawtypes")
public abstract class BaseDocumentStoreService implements DocumentStore {

	@Inject
    private Logger log;

	public boolean hasDocument(String path) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.hasDocument(path);
	}

	@Override
	public boolean saveDocument(String path, String documentContent, Charset charset) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.saveDocument(path, documentContent, charset);
	}

	@Override
	public boolean saveDocumentStream(String path, InputStream documentStream) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.saveDocumentStream(path, documentStream);
	}

	@Override
	public String readDocument(String path, Charset charset)  {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.readDocument(path, charset);
	}

	@Override
	public InputStream readDocumentAsStream(String path)  {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.readDocumentAsStream(path);
	}

	@Override
	public boolean renameDocument(String currentPath, String destinationPath) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.renameDocument(currentPath, destinationPath);
	}

	@Override
	public boolean removeDocument(String path) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.removeDocument(path);
	}

	@Override
	public DocumentStoreType getProviderType() {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.getProviderType();
	}

    protected abstract DocumentStoreProvider getDocumentStoreProvider();

}
