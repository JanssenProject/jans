package org.gluu.service.document.store.provider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.gluu.service.document.store.conf.DocumentStoreType;

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
	 * @throws IOException 
	 */
	boolean saveDocument(String path, String documentContent, Charset charset) throws IOException;

	/**
	 * Save document stream into store
	 * @throws IOException 
	 */
	boolean saveDocumentStream(String path, InputStream documentStream) throws IOException;

	/**
	 * Load document from store
	 */
	String readDocument(String path, Charset charset) throws IOException;

	/**
	 * Load document from store as stream
	 */
	public InputStream readDocumentAsStream(String path)  throws IOException;

	/**
	 * Removes an object document from store
	 */
	boolean removeDocument(String path) throws IOException;

	/**
	 * Rename an object in document store
	 */
	boolean renameDocument(String currentPath, String destinationPath) throws IOException;

	public abstract DocumentStoreType getProviderType();

}