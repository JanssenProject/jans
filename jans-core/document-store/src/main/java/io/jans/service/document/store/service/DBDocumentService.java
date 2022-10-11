package io.jans.service.document.store.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Provides operations with Document
 * 
 * @author Shekhar L. Date : 29 Sep 2022
 */
@ApplicationScoped
@Named("DBDocumentService")
public class DBDocumentService implements Serializable {

	private static final long serialVersionUID = 65734145678106186L;

	@Inject
	private Logger logger;
	
	@Inject
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

	private PersistenceEntryManager persistenceEntryManager;

	public DBDocumentService() {}

	public DBDocumentService(PersistenceEntryManager persistenceEntryManager) {
		this.persistenceEntryManager = persistenceEntryManager;
	}
	
	public static final String inum = "inum";
	public static final String displayName = "displayName";
	public static final String description = "description";

	@PostConstruct
	public void init() {
		if (persistenceEntryManagerInstance.isResolvable()) {
			this.persistenceEntryManager = persistenceEntryManagerInstance.get();
		} else {
			this.persistenceEntryManager = null;
		}
	}

	/**
	 * Add new Document entry
	 * 
	 * @param Document
	 *            Document
	 */
	public void addDocument(Document document) throws Exception {
		document.setCreationDate(new Date());
		persistenceEntryManager.persist(document);
	}

	/**
	 * Remove Document entry
	 * 
	 * @param Document
	 *            Document
	 */
	public void removeDocument(Document document) throws Exception {

		persistenceEntryManager.remove(document);
	}

	/**
	 * Get Document by inum
	 * 
	 * @param inum
	 *            Document Inum
	 * @return Document
	 */
	public Document getDocumentByInum(String inum) throws Exception {
		Document result = null;
		try {
			result = persistenceEntryManager.find(Document.class, getDnForDocument(inum));
		} catch (Exception e) {
			logger.error("Not able to find the document. Here is the exception message ", e);
		}
		return result;
	}

	/**
	 * Build DN string for Document
	 * 
	 * @param inum
	 *            Document Inum
	 * @return DN string for specified scope or DN for Document branch if inum is null
	 * @throws Exception
	 */
	public String getDnForDocument(String inum) throws Exception {
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=document,%s", "o=gluu");
		}
		return String.format("inum=%s,ou=document,%s", inum, "o=gluu");
	}

	/**
	 * Update Document entry
	 * 
	 * @param Document
	 *            Document
	 */
	public void updateDocument(Document document) throws Exception {
		persistenceEntryManager.merge(document);
	}

	/**
	 * Generate new inum for Document
	 * 
	 * @return New inum for Document
	 */
	public String generateInumForNewDocument() throws Exception {
		Document document = new Document();
		String newInum = null;
		String newDn = null;
		newInum = generateInumForNewDocumentImpl();
		newDn = getDnForDocument(newInum);
		document.setDn(newDn);
		return newInum;
	}

	/**
	 * Search scopes by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of Document
	 * @throws Exception
	 */
	public List<Document> searchDocuments(String pattern, int sizeLimit) {
		Filter searchFilter = null;
		if (StringHelper.isNotEmpty(pattern)) {
			String[] targetArray = new String[] { pattern };
			Filter displayNameFilter = Filter.createSubstringFilter(displayName, null, targetArray,
					null);
			Filter descriptionFilter = Filter.createSubstringFilter(description, null, targetArray,
					null);
			searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
		}
		List<Document> result = new ArrayList<>();
		try {
			result = persistenceEntryManager.findEntries(getDnForDocument(null), Document.class, searchFilter, sizeLimit);
			return result;
		} catch (Exception e) {
			logger.error("Failed to find Document : ", e);
		}
		return result;
	}

	/**
	 * Generate new inum for document
	 * 
	 * @return New inum for document
	 * @throws Exception
	 */
	private String generateInumForNewDocumentImpl() throws Exception {
		return UUID.randomUUID().toString();
	}

	public List<Document> getAllDocumentsList(int size) {
		try {
			List<Document> documents = persistenceEntryManager.findEntries(getDnForDocument(null), Document.class, null, size);
			return documents;
		} catch (Exception e) {
			logger.error("Failed to find Document: ", e);
			return new ArrayList<>();
		}
	}

	/**
	 * returns documents by Dn
	 * 
	 * @return documents
	 */

	public Document getDocumentByDn(String Dn) throws Exception {
		return persistenceEntryManager.find(Document.class, Dn);
	}

	/**
	 * Get documents by DisplayName
	 * 
	 * @param DisplayName
	 * @return documents
	 */
	public Document getDocumentByDisplayName(String DisplayName) throws Exception {
		Document document = new Document();
		document.setDisplayName(DisplayName);
		document.setDn(getDnForDocument(null));;
		List<Document> documents = persistenceEntryManager.findEntries(document);
		if ((documents != null) && (documents.size() > 0)) {
			return documents.get(0);
		}
		return null;
	}
}
