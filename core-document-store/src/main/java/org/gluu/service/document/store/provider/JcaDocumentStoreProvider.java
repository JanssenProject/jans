package org.gluu.service.document.store.provider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.service.document.store.conf.DocumentStoreConfiguration;
import org.gluu.service.document.store.conf.DocumentStoreType;
import org.gluu.service.document.store.conf.JcaDocumentStoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan on 04/10/2020
 */
@ApplicationScoped
public class JcaDocumentStoreProvider extends DocumentStoreProvider<JcaDocumentStoreProvider> {

    @Inject
    private Logger log;

    @Inject
    private DocumentStoreConfiguration documentStoreConfiguration;

    private JcaDocumentStoreConfiguration jcaDocumentStoreConfiguration;

    public JcaDocumentStoreProvider() {
    }

    @PostConstruct
    public void init() {
        this.jcaDocumentStoreConfiguration = documentStoreConfiguration.getJcaConfiguration();
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

    public DocumentStoreType getProviderType() {
        return DocumentStoreType.LOCAL;
    }

	public boolean hasDocument(String path) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean saveDocument(String path, String documentContent, Charset charset) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean saveDocumentStream(String path, InputStream documentStream) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String readDocument(String path, Charset charset) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream readDocumentAsStream(String path) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean renameDocument(String currentPath, String destinationPath) throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean removeDocument(String path) {
		// TODO Auto-generated method stub
		return false;
	}

}
