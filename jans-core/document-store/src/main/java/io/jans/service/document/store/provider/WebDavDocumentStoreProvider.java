/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.provider;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.conf.WebDavDocumentStoreConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class WebDavDocumentStoreProvider extends DocumentStoreProvider<String> {

    @Inject
    private Logger log;

    @Inject
    private DocumentStoreConfiguration documentStoreConfiguration;

    private WebDavDocumentStoreConfiguration webDavDocumentStoreConfiguration;

    public WebDavDocumentStoreProvider() {
    }

    @PostConstruct
    public void init() {
        this.webDavDocumentStoreConfiguration = documentStoreConfiguration.getWebDavConfiguration();
    }

    public void create() {
    	log.debug("Starting LocalDocumentStoreProvider ...");
    }

	public void configure(DocumentStoreConfiguration documentStoreConfiguration) {
		this.log = LoggerFactory.getLogger(DocumentStoreConfiguration.class);
		this.documentStoreConfiguration = documentStoreConfiguration;
	}

    @PreDestroy
    public void destroy() {
    	log.debug("Destroying LocalDocumentStoreProvider");

        log.debug("Destroyed LocalDocumentStoreProvider");
    }

    @Override
    public DocumentStoreType getProviderType() {
        return DocumentStoreType.WEB_DAV;
    }

	@Override
	public boolean hasDocument(String path) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String saveDocument(String path, String description, String documentContent, Charset charset, String module) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String saveDocumentStream(String path, String description, InputStream documentStream, String module) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String saveBinaryDocumentStream(String path, String description, InputStream documentStream,
			String module) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String readDocument(String path, Charset charset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream readBinaryDocumentAsStream(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream readDocumentAsStream(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String renameDocument(String currentPath, String destinationPath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeDocument(String path) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> findDocumentsByModules(List<String> moduleList, String ... attributes) {
        throw new RuntimeException("Not yet implemented");
	}

}
