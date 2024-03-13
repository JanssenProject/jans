package io.jans.service.document.store.provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import io.jans.service.document.store.exception.DocumentException;
import io.jans.service.document.store.exception.WriteDocumentException;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.PersistenceEntryManager;
import io.jans.service.document.store.conf.DBDocumentStoreConfiguration;
import io.jans.service.document.store.conf.DocumentStoreConfiguration;
import io.jans.service.document.store.conf.DocumentStoreType;
import io.jans.service.document.store.service.DBDocumentService;
import io.jans.service.document.store.service.Document;
import io.jans.util.StringHelper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author Shekhar L. on 29/10/2022
 */
@ApplicationScoped
public class DBDocumentStoreProvider extends DocumentStoreProvider<DBDocumentStoreProvider> {

	@Inject
	private Logger log;

	@Inject
	private DBDocumentService documentService;

	@Inject
	private DocumentStoreConfiguration documentStoreConfiguration;

	private DBDocumentStoreConfiguration dbDocumentStoreConfiguration;

	public DocumentStoreType getProviderType() {
		return DocumentStoreType.DB;
	}

	@PostConstruct
	public void init() {
		this.dbDocumentStoreConfiguration = documentStoreConfiguration.getDbConfiguration();
	}

	public void configure(DocumentStoreConfiguration documentStoreConfiguration,
			PersistenceEntryManager persistenceEntryManager) {
		this.log = LoggerFactory.getLogger(DBDocumentStoreProvider.class);
		this.documentStoreConfiguration = documentStoreConfiguration;
		if (documentService == null) {
			this.documentService = new DBDocumentService(persistenceEntryManager);
		}
	}

	@Override
	public void create() {

	}

	@Override
	public void destroy() {

	}

	@Override
	public boolean hasDocument(String path) {
		log.debug("Has document: '{}'", path);
		if (StringHelper.isEmpty(path)) {
			throw new IllegalArgumentException("Specified path should not be empty!");
		}
		Document oxDocument = null;
		try {
			oxDocument = documentService.getDocumentByDisplayName(path);
			if (oxDocument != null) {
				return true;
			}
		} catch (Exception e) {
			log.error("Failed to check if path '" + path + "' exists in repository", e);
			throw new DocumentException(e);
		}

		return false;
	}

	@Override
	public String saveDocument(String path, String description, String documentContent, Charset charset, List<String> moduleList) {
		log.debug("Save document: '{}'", path);
		Document oxDocument = new Document();
		oxDocument.setDocument(documentContent);
		oxDocument.setDisplayName(path);
		try {
				oxDocument.setInum(documentService.generateInumForNewDocument());
				String dn = "inum=" + oxDocument.getInum() + ",ou=document,o=jans";
				oxDocument.setDn(dn);
				oxDocument.setDescription(description);
				oxDocument.setJansEnabled(true);
				oxDocument.setJansModuleProperty(moduleList);
				documentService.addDocument(oxDocument);
				return path;
		} catch (Exception ex) {
			log.error("Failed to write document to file '{}'", path, ex);
			throw new WriteDocumentException(ex);
		}
	}

	@Override
	public String saveDocumentStream(String path, String description, InputStream documentStream, List<String> moduleList) {

		Document oxDocument = new Document();
		oxDocument.setDisplayName(path);

		try {
			String documentContent = new String(documentStream.readAllBytes(), StandardCharsets.UTF_8);
			oxDocument.setDocument(documentContent);
			String inum = documentService.generateInumForNewDocument();
			oxDocument.setInum(inum);
			String dn = "inum=" + oxDocument.getInum() + ",ou=document,o=jans";
			oxDocument.setDn(dn);
			oxDocument.setDescription(description);
			oxDocument.setJansEnabled(true);
			oxDocument.setJansModuleProperty(moduleList);
			documentService.addDocument(oxDocument);
			return path;
		} catch (Exception e) {
			log.error("Failed to write document from stream to file '{}'", path, e);
			throw new WriteDocumentException(e);
		}
	}

	@Override
	public String saveBinaryDocumentStream(String path, String description, InputStream documentStream,
			List<String> moduleList) {
		return saveDocumentStream(path, description, new Base64InputStream(documentStream, true), moduleList);
	}

	@Override
	public String readDocument(String name, Charset charset) {
		log.debug("Read document: '{}'", name);
		Document oxDocument;
		try {
			oxDocument = documentService.getDocumentByDisplayName(name);
			if (oxDocument != null) {
				return oxDocument.getDocument();
			}
		} catch (Exception e) {
			log.error("Failed to read document as stream from file '{}'", name, e);
			throw new DocumentException(e);
		}
		return null;
	}

	@Override
	public InputStream readDocumentAsStream(String name) {
		log.debug("Read document as stream: '{}'", name);
		String filecontecnt = readDocument(name, null);
		if (filecontecnt == null) {
			log.error("Document file '{}' isn't exist", name);
			return null;
		}

		InputStream InputStream = new ByteArrayInputStream(filecontecnt.getBytes());
		return InputStream;
	}

	@Override
	public InputStream readBinaryDocumentAsStream(String path) {
		return new Base64InputStream(readDocumentAsStream(path), false);
	}

	@Override
	public String renameDocument(String currentPath, String destinationPath) {
		log.debug("Rename document: '{}' -> '{}'", currentPath, destinationPath);
		Document oxDocument;
		try {
			oxDocument = documentService.getDocumentByDisplayName(currentPath);
			if (oxDocument == null) {
				log.error("Document doesn't Exist with the name  '{}'", currentPath);
				return null;
			}
			oxDocument.setDisplayName(destinationPath);
			documentService.updateDocument(oxDocument);
			Document oxDocumentDestination = documentService.getDocumentByDisplayName(destinationPath);
			if (oxDocumentDestination == null) {
				log.error("Failed to rename to destination file '{}'", destinationPath);
				return null;
			}
		} catch (Exception e) {
			log.error("Failed to rename to destination file '{}'", destinationPath);
			throw new WriteDocumentException(e);
		}

		return destinationPath;
	}

	@Override
	public boolean removeDocument(String path) {
		log.debug("Remove document: '{}'", path);
		Document oxDocument;
		try {
			oxDocument = documentService.getDocumentByDisplayName(path);
			if (oxDocument == null) {
				log.error(" document not exist file '{}'", path);
				return false;
			}
			documentService.removeDocument(oxDocument);
			return true;
		} catch (Exception e) {
			log.error("Failed to remove document file '{}'", path, e);
			throw new DocumentException(e);
		}
	}

}
