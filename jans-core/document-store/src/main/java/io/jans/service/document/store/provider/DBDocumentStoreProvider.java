package io.jans.service.document.store.provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
	
	public void configure(DocumentStoreConfiguration documentStoreConfiguration, PersistenceEntryManager persistenceEntryManager) {
		this.log = LoggerFactory.getLogger(DBDocumentStoreProvider.class);
		this.documentStoreConfiguration = documentStoreConfiguration;
		if(documentService == null) {
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
	public boolean hasDocument(String DisplayName) {
		log.debug("Has document: '{}'", DisplayName);
		if (StringHelper.isEmpty(DisplayName)) {
			throw new IllegalArgumentException("Specified path should not be empty!");
		}		
		Document oxDocument = null;
		try {
			oxDocument = documentService.getDocumentByDisplayName(DisplayName);
		} catch (Exception e) {
			log.error("Failed to check if path '" + DisplayName + "' exists in repository", e);
		}

		return false;
	}

	@Override
	public boolean saveDocument(String name, String documentContent, Charset charset, List<String> moduleList) {
		log.debug("Save document: '{}'", name);
		Document oxDocument = new Document();
		oxDocument.setDocument(documentContent);
		oxDocument.setDisplayName(name);		
		try {
			try {
				oxDocument.setInum(documentService.generateInumForNewDocument());	
				String dn = "inum="+ oxDocument.getInum() +",ou=document,o=gluu";
				oxDocument.setDn(dn);
				oxDocument.setDescription(name);
				oxDocument.setJansEnabled("true");
				oxDocument.setJansModuleProperty(moduleList);	  
				documentService.addDocument(oxDocument);
				return true;
			} finally {
			}
		} catch (Exception ex) {
			log.error("Failed to write document to file '{}'", name, ex);
		}

		return false;
	}

	@Override
	public boolean saveDocumentStream(String name, InputStream documentStream, List <String> moduleList) {
		
		Document oxDocument = new Document();
		oxDocument.setDisplayName(name);
		
		 try {
			String documentContent = Base64.getEncoder().encodeToString(IOUtils.toByteArray(documentStream));
			oxDocument.setDocument(documentContent);
			String inum = documentService.generateInumForNewDocument();
			oxDocument.setInum(inum);	
			String dn = "inum="+ oxDocument.getInum() +",ou=document,o=gluu";
			oxDocument.setDn(dn);
			oxDocument.setDescription(name);
			oxDocument.setJansEnabled("true");
			oxDocument.setJansModuleProperty(moduleList);
			documentService.addDocument(oxDocument);
			return true;
		} catch (Exception e) {
			log.error("Failed to write document from stream to file '{}'", name, e);
		}	

		return false;
	}


	@Override
	public String readDocument(String name, Charset charset) {
		log.debug("Read document: '{}'", name);		
		Document oxDocument;
		try {
			oxDocument = documentService.getDocumentByDisplayName(name);
			if(oxDocument != null) {
				return oxDocument.getDocument();
			}
		} catch (Exception e) {
			log.error("Failed to read document as stream from file '{}'", name, e);
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

		InputStream InputStream = new ByteArrayInputStream(Base64.getDecoder().decode(filecontecnt));
		return InputStream;
	}

	@Override
	public boolean renameDocument(String currentDisplayName, String destinationDisplayName) {
		log.debug("Rename document: '{}' -> '{}'", currentDisplayName, destinationDisplayName);
		Document oxDocument;
		try {
			oxDocument = documentService.getDocumentByDisplayName(currentDisplayName);
			if (oxDocument == null) {
				log.error("Document doesn't Exist with the name  '{}'", currentDisplayName);
				return false;
			}
			oxDocument.setDisplayName(destinationDisplayName);
			documentService.updateDocument(oxDocument);
			Document oxDocumentDestination = documentService.getDocumentByDisplayName(destinationDisplayName);
			if(oxDocumentDestination == null) {
				log.error("Failed to rename to destination file '{}'", destinationDisplayName);
				return false;
			}
		} catch (Exception e) {
			log.error("Failed to rename to destination file '{}'", destinationDisplayName);
		}
		return true;
	}

	@Override
	public boolean removeDocument(String inum) {
		log.debug("Remove document: '{}'", inum);
		Document oxDocument;
		try {
			oxDocument = documentService.getDocumentByInum(inum);
			if(oxDocument == null) {
				log.error(" document not exist file '{}'", inum);
				return false;
			}
			
			documentService.removeDocument(oxDocument);
			Document checkDocument = documentService.getDocumentByInum(inum);
			if(checkDocument == null) {
				return true;
			}
			return false;
		} catch (Exception e) {
			log.error("Failed to remove document file '{}'", inum, e);
		}
		return false;
	}

}
