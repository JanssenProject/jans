/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.provider;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import io.jans.service.document.store.conf.DocumentStoreType;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
public interface DocumentStore<T> {

	/**
	 * Method to check if there is key in cache
	 */
	boolean hasDocument(String path);

	/**
	 * Save document into store
	 */
	boolean saveDocument(String path, String documentContent, Charset charset, List<String> moduleList);

	/**
	 * Save document stream into store
	 */
	boolean saveDocumentStream(String path, InputStream documentStream, List<String> moduleList);

	/**
	 * Load document from store
	 */
	String readDocument(String path, Charset charset);

	/**
	 * Load document from store as stream
	 */
	public InputStream readDocumentAsStream(String path) ;

	/**
	 * Removes an object document from store
	 */
	boolean removeDocument(String path);

	/**
	 * Rename an object in document store
	 */
	boolean renameDocument(String currentPath, String destinationPath);

	public abstract DocumentStoreType getProviderType();

}