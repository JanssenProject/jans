/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.document.store.provider;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.WebDavDocumentStoreConfiguration;
import io.jans.service.document.store.conf.DocumentStoreType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class WebDavDocumentStoreProvider extends DocumentStoreProvider<WebDavDocumentStoreProvider> {

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
	public boolean saveDocument(String path, String documentContent, Charset charset, List<String> moduleList) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean saveDocumentStream(String path, InputStream documentStream, List<String> moduleList) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String readDocument(String path, Charset charset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream readDocumentAsStream(String path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean renameDocument(String currentPath, String destinationPath) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeDocument(String path) {
		// TODO Auto-generated method stub
		return false;
	}

}
