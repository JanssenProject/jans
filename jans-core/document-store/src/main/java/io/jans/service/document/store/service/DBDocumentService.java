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
 * Provides operations with OxDocument
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
	 * Add new OxDocument entry
	 * 
	 * @param OxDocument
	 *            OxDocument
	 */
	public void addOxDocument(OxDocument oxDocument) throws Exception {
		oxDocument.setCreationDate(new Date());
		persistenceEntryManager.persist(oxDocument);
	}

	/**
	 * Remove OxDocument entry
	 * 
	 * @param OxDocument
	 *            OxDocument
	 */
	public void removeOxDocument(OxDocument oxDocument) throws Exception {

		persistenceEntryManager.remove(oxDocument);
	}

	/**
	 * Get OxDocument by inum
	 * 
	 * @param inum
	 *            OxDocument Inum
	 * @return OxDocument
	 */
	public OxDocument getOxDocumentByInum(String inum) throws Exception {
		OxDocument result = null;
		try {
			result = persistenceEntryManager.find(OxDocument.class, getDnForOxDocument(inum));
		} catch (Exception e) {
			logger.error("Not able to find the oxDocument. Here is the exception message ", e);
		}
		return result;
	}

	/**
	 * Build DN string for OxDocument
	 * 
	 * @param inum
	 *            OxDocument Inum
	 * @return DN string for specified scope or DN for OxDocument branch if inum is null
	 * @throws Exception
	 */
	public String getDnForOxDocument(String inum) throws Exception {
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=document,%s", "o=gluu");
		}
		return String.format("inum=%s,ou=document,%s", inum, "o=gluu");
	}

	/**
	 * Update OxDocument entry
	 * 
	 * @param OxDocument
	 *            OxDocument
	 */
	public void updateOxDocument(OxDocument oxDocument) throws Exception {
		persistenceEntryManager.merge(oxDocument);
	}

	/**
	 * Generate new inum for OxDocument
	 * 
	 * @return New inum for OxDocument
	 */
	public String generateInumForNewOxDocument() throws Exception {
		OxDocument oxDocument = new OxDocument();
		String newInum = null;
		String newDn = null;
		newInum = generateInumForNewOxDocumentImpl();
		newDn = getDnForOxDocument(newInum);
		oxDocument.setDn(newDn);
		return newInum;
	}

	/**
	 * Search scopes by pattern
	 * 
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of OxDocument
	 * @throws Exception
	 */
	public List<OxDocument> searchOxDocuments(String pattern, int sizeLimit) {
		Filter searchFilter = null;
		if (StringHelper.isNotEmpty(pattern)) {
			String[] targetArray = new String[] { pattern };
			Filter displayNameFilter = Filter.createSubstringFilter(displayName, null, targetArray,
					null);
			Filter descriptionFilter = Filter.createSubstringFilter(description, null, targetArray,
					null);
			searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter);
		}
		List<OxDocument> result = new ArrayList<>();
		try {
			result = persistenceEntryManager.findEntries(getDnForOxDocument(null), OxDocument.class, searchFilter, sizeLimit);
			return result;
		} catch (Exception e) {
			logger.error("Failed to find OxDocument : ", e);
		}
		return result;
	}

	/**
	 * Generate new inum for oxDocument
	 * 
	 * @return New inum for oxDocument
	 * @throws Exception
	 */
	private String generateInumForNewOxDocumentImpl() throws Exception {
		return UUID.randomUUID().toString();
	}

	public List<OxDocument> getAllOxDocumentsList(int size) {
		try {
			List<OxDocument> oxDocuments = persistenceEntryManager.findEntries(getDnForOxDocument(null), OxDocument.class, null, size);
			return oxDocuments;
		} catch (Exception e) {
			logger.error("Failed to find OxDocument: ", e);
			return new ArrayList<>();
		}
	}

	/**
	 * returns oxDocuments by Dn
	 * 
	 * @return oxDocuments
	 */

	public OxDocument getOxDocumentByDn(String Dn) throws Exception {
		return persistenceEntryManager.find(OxDocument.class, Dn);
	}

	/**
	 * Get oxDocuments by DisplayName
	 * 
	 * @param DisplayName
	 * @return oxDocuments
	 */
	public OxDocument getOxDocumentByDisplayName(String DisplayName) throws Exception {
		OxDocument oxDocument = new OxDocument();
		oxDocument.setDisplayName(DisplayName);
		oxDocument.setDn(getDnForOxDocument(null));;
		List<OxDocument> oxDocuments = persistenceEntryManager.findEntries(oxDocument);
		if ((oxDocuments != null) && (oxDocuments.size() > 0)) {
			return oxDocuments.get(0);
		}
		return null;
	}
}
