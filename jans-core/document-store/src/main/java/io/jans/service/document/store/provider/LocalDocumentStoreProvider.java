/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.provider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.LocalDocumentStoreConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.util.StringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class LocalDocumentStoreProvider extends DocumentStoreProvider<LocalDocumentStoreProvider> {

    @Inject
    private Logger log;

    @Inject
    private DocumentStoreConfiguration documentStoreConfiguration;

    private LocalDocumentStoreConfiguration localDocumentStoreConfiguration;
    
    private String baseLocation;

    public LocalDocumentStoreProvider() {
    }

    @PostConstruct
    public void init() {
        this.localDocumentStoreConfiguration = documentStoreConfiguration.getLocalConfiguration();
    }

	@Override
    public void create() {
    	log.debug("Starting LocalDocumentStoreProvider ...");

    	if (StringHelper.isEmpty(localDocumentStoreConfiguration.getBaseLocation())) {
    		String osName = System.getProperty("os.name");
    		if (StringHelper.isNotEmpty(osName) && osName.toLowerCase().startsWith("windows")) {
    			baseLocation = "";
    			return;
    		}
        	throw new IllegalArgumentException("Base location should not be empty");
    	}
    	
    	baseLocation = new File(localDocumentStoreConfiguration.getBaseLocation()).getAbsolutePath();
    }

	public void configure(DocumentStoreConfiguration documentStoreConfiguration) {
		this.log = LoggerFactory.getLogger(DocumentStoreConfiguration.class);
		this.documentStoreConfiguration = documentStoreConfiguration;
	}

    @PreDestroy
	@Override
    public void destroy() {
    	log.debug("Destroying LocalDocumentStoreProvider");

        log.debug("Destroyed LocalDocumentStoreProvider");
    }

    @Override
    public DocumentStoreType getProviderType() {
        return DocumentStoreType.LOCAL;
    }

	@Override
	public boolean hasDocument(String path) {
		log.debug("Has document: '{}'", path);

		if (StringHelper.isEmpty(path)) {
			throw new IllegalArgumentException("Specified path should not be empty!");
		}

		File file = buildFilePath(path);

		return file.exists();
	}

	@Override
	public boolean saveDocument(String path, String documentContent, Charset charset, List<String> moduleList) {
		log.debug("Save document: '{}'", path);

		File file = buildFilePath(path);
		if (!createParentPath(file)) {
			return false;
		}

		try (FileOutputStream os = FileUtils.openOutputStream(file)) {
			IOUtils.write(documentContent, os, charset);
			os.flush();
			
			return true;
		} catch (IOException ex) {
			log.error("Failed to write document to file '{}'", file.getAbsolutePath(), ex);
		}

		return false;
	}

	@Override
	public boolean saveDocumentStream(String path, InputStream documentStream, List<String> moduleList) {
		log.debug("Save document from stream: '{}'", path);

		File file = buildFilePath(path);
		if (!createParentPath(file)) {
			return false;
		}

		try (FileOutputStream os = FileUtils.openOutputStream(file)) {
			IOUtils.copy(documentStream, os);
			os.flush();
			
			return true;
		} catch (IOException ex) {
			log.error("Failed to write document from stream to file '{}'", file.getAbsolutePath(), ex);
		}

		return false;
	}

	@Override
	public String readDocument(String path, Charset charset) {
		log.debug("Read document: '{}'", path);

		File file = buildFilePath(path);
		if (!createParentPath(file)) {
			return null;
		}

		try {
			return FileUtils.readFileToString(file, charset);
		} catch (IOException ex) {
			log.error("Failed to read document from file '{}'", file.getAbsolutePath(), ex);
		}
		
		return null;
	}

	@Override
	public InputStream readDocumentAsStream(String path) {
		log.debug("Read document as stream: '{}'", path);

		File file = buildFilePath(path);

		try {
			return new BufferedInputStream(FileUtils.openInputStream(file));
		} catch (IOException ex) {
			log.error("Failed to read document as stream from file '{}'", file.getAbsolutePath(), ex);
		}
		
		return null;
	}

	@Override
	public boolean renameDocument(String currentPath, String destinationPath) {
		log.debug("Rename document: '{}' -> '{}'", currentPath, destinationPath);

		File currentFile = buildFilePath(currentPath);
		File destinationFile = buildFilePath(destinationPath);
		
		if (!removeDocument(destinationPath)) {
			log.error("Failed to remove destination file '{}'", destinationFile.getAbsolutePath());
		}

		try {
			currentFile.renameTo(destinationFile);
			return true;
		} catch (Exception ex) {
			log.error("Failed to rename to destination file '{}'", destinationFile.getAbsolutePath(), ex);
		}
		
		return false;
	}

	@Override
	public boolean removeDocument(String path) {
		log.debug("Remove document: '{}'", path);

		if (!hasDocument(path)) {
			return true;
		}

		File file = buildFilePath(path);
		if (!createParentPath(file)) {
			return false;
		}

		return FileUtils.deleteQuietly(file);
	}

	private boolean createParentPath(File file) {
		try {
			FileUtils.forceMkdirParent(file);
			return true;
		} catch (IOException ex) {
			log.error("Failed to create path to file '{}'", file.getAbsolutePath(), ex);
		}

		return false;
	}

	private File buildFilePath(String path) {
		String filePath = baseLocation + File.separator + path;
		return new File(filePath);
	}

}
