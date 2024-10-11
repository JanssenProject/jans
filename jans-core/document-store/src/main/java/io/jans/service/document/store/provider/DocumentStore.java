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
	String saveDocument(String path, String description, String documentContent, Charset charset, String module);

	/**
	 * Save document stream into store
	 */
	String saveDocumentStream(String path, String description, InputStream documentStream, String module);

	/**
	 * Save binary document stream into store
	 */
	String saveBinaryDocumentStream(String path, String description, InputStream documentStream, String module);

	/**
	 * Load document from store
	 */
	String readDocument(String path, Charset charset);

	/**
	 * Load document from store as stream
	 */
	InputStream readDocumentAsStream(String path);

	/**
	 * Load binary document from store as stream
	 */
	InputStream readBinaryDocumentAsStream(String path);

	/**
	 * Removes an object document from store
	 */
	boolean removeDocument(String path);

	/**
	 * Rename an object in document store
	 */
	String renameDocument(String currentPath, String destinationPath);

	/**
	 * Find documents by modules list
	 */
	List<T> findDocumentsByModules(List<String> moduleList, String ... attributes);

	DocumentStoreType getProviderType();

}