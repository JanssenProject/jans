/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.service;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import jakarta.inject.Inject;

import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.provider.DocumentStore;
import io.jans.service.document.store.provider.DocumentStoreProvider;
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
	public boolean saveDocument(String path, String documentContent, Charset charset, List moduleList) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.saveDocument(path, documentContent, charset, moduleList);
	}

	@Override
	public boolean saveDocumentStream(String path, InputStream documentStream, List moduleList) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.saveDocumentStream(path, documentStream, moduleList);
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
