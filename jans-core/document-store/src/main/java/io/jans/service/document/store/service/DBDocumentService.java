package io.jans.service.document.store.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;

import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.search.filter.Filter;
import io.jans.service.document.store.model.Document;
import io.jans.util.StringHelper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Provides operations with Document
 * 
 * @author Shekhar L. Date : 29 Sep 2022
 */
@ApplicationScoped
public class DBDocumentService implements Serializable {

	private static final long serialVersionUID = 65734145678106186L;

	public static final String inum = "inum";
	public static final String displayName = "displayName";
	public static final String description = "description";
	public static final String jansFilePath = "jansFilePath";

	@Inject
	private Logger logger;
	
	@Inject
	private Instance<PersistenceEntryManager> persistenceEntryManagerInstance;

	private PersistenceEntryManager persistenceEntryManager;

	public DBDocumentService() {}

	public DBDocumentService(PersistenceEntryManager persistenceEntryManager) {
		this.persistenceEntryManager = persistenceEntryManager;
	}


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
	 * @param document
	 *            Document
	 */
	public void addDocument(Document document) throws Exception {
		document.setCreationDate(new Date());
		persistenceEntryManager.persist(document);
	}

	/**
	 * Remove Document entry
	 * 
	 * @param document
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
	 * Update Document entry
	 * 
	 * @param document
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
			result = persistenceEntryManager.findEntries(baseDn(), Document.class, searchFilter, sizeLimit);
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
			List<Document> documents = persistenceEntryManager.findEntries(baseDn(), Document.class, null, size);
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

	public Document getDocumentByDn(String dn) throws Exception {
		return persistenceEntryManager.find(Document.class, dn);
	}

	/**
	 * Get documents by DisplayName
	 * 
	 * @param displayName
	 * @return documents
	 */
	public Document getDocumentByDisplayName(String displayName) throws Exception {
        String baseDn = baseDn();
		Filter filter = Filter.createEqualityFilter("displayName", displayName).multiValued();

		List<Document> documents = persistenceEntryManager.findEntries(baseDn, Document.class, filter);
		if ((documents != null) && (documents.size() > 0)) {
			return documents.get(0);
		}

		return null;
	}

    public List<Document> findAllDocuments(String[] returnAttributes) {
        String baseDn = baseDn();

        List<Document> result = persistenceEntryManager.findEntries(baseDn, Document.class, null, returnAttributes);

        return result;
    }

	public List<Document> findDocumentsListByModules(List<String> jansModules, String... returnAttributes) {
        String baseDn = baseDn();

        if ((jansModules == null) || (jansModules.size() == 0)) {
            return findAllDocuments(returnAttributes);
        }

		List<Filter> jansModuleFilters = new ArrayList<Filter>();
		for (String jansModule : jansModules) {
	        Filter filter = Filter.createEqualityFilter("jansService", jansModule);
	        jansModuleFilters.add(filter);
		}
		
		Filter filter = Filter.createORFilter(jansModuleFilters);

		List<Document> documents = persistenceEntryManager.findEntries(baseDn, Document.class, filter, returnAttributes);
		return documents;
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
		String baseDn = baseDn();
		if (StringHelper.isEmpty(inum)) {
			return baseDn;
		}

		return String.format("inum=%s,%s", inum, baseDn);
	}

	public String baseDn() {
		return String.format("ou=document,%s", "o=jans");
    }

	public List<Document> getDocumentsByFilePath(String filePath){
		Filter searchFilter = null;
		if (StringHelper.isNotEmpty(filePath)) {
			String[] targetArray = new String[] { filePath };
			Filter displayNameFilter = Filter.createSubstringFilter(jansFilePath, null, targetArray,
					null);
			searchFilter = Filter.createORFilter(displayNameFilter);
		}
		List<Document> result = new ArrayList<>();
		try {
			result = persistenceEntryManager.findEntries(getDnForDocument(null), Document.class, searchFilter, 100);
			return result;
		} catch (Exception e) {
			logger.error("Failed to find Document : ", e);
		}
		return result;
	}

}
