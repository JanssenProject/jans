/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.service;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;

import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.provider.DocumentStore;
import io.jans.service.document.store.provider.DocumentStoreProvider;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@SuppressWarnings("rawtypes")
public abstract class BaseDocumentStoreService<T> implements DocumentStore<T> {

	@Inject
    private Logger log;

	public boolean hasDocument(String path) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.hasDocument(path);
	}

	@Override
	public String saveDocument(String path, String description, String documentContent, Charset charset, String module) {
    	DocumentStoreProvider<?> documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.saveDocument(path, description, documentContent, charset, module);
	}

	@Override
	public String saveDocumentStream(String path, String description, InputStream documentStream, String module) {
    	DocumentStoreProvider<?> documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.saveDocumentStream(path, description, documentStream, module);
	}

    @Override
	public String saveBinaryDocumentStream(String path, String description, InputStream documentStream,
			String module) {
		return saveDocumentStream(path, description, documentStream, module);
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
	public InputStream readBinaryDocumentAsStream(String path) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.readBinaryDocumentAsStream(path);
	}

	@Override
	public String renameDocument(String currentPath, String destinationPath) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.renameDocument(currentPath, destinationPath);
	}

	@Override
	public boolean removeDocument(String path) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.removeDocument(path);
	}

	@Override
	public List<T> findDocumentsByModules(List moduleList, String ... attributes) {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.findDocumentsByModules(moduleList, attributes);
	}

	@Override
	public DocumentStoreType getProviderType() {
    	DocumentStoreProvider documentStoreProvider = getDocumentStoreProvider();

		return documentStoreProvider.getProviderType();
	}

    protected abstract DocumentStoreProvider getDocumentStoreProvider();

}
