package org.gluu.oxtrust.service;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.gluu.oxtrust.model.OxAuthClient;
import org.gluu.oxtrust.model.OxAuthSectorIdentifier;
import org.gluu.oxtrust.util.OxTrustConstants;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

/**
 * Provides operations with Sector Identifiers
 *
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
@ApplicationScoped
public class SectorIdentifierService implements Serializable {

	private static final long serialVersionUID = -9167587377957719153L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private OrganizationService organizationService;

	@Inject
	private ClientService clientService;

	/**
	 * Build DN string for sector identifier
	 *
	 * @param oxId
	 *            Sector Identifier oxId
	 * @return DN string for specified sector identifier or DN for sector
	 *         identifiers branch if oxId is null
	 * @throws Exception
	 */
	public String getDnForSectorIdentifier(String oxId) {
		String orgDn = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(oxId)) {
			return String.format("ou=sector_identifiers,%s", orgDn);
		}

		return String.format("oxId=%s,ou=sector_identifiers,%s", oxId, orgDn);
	}

	/**
	 * Search sector identifiers by pattern
	 *
	 * @param pattern
	 *            Pattern
	 * @param sizeLimit
	 *            Maximum count of results
	 * @return List of sector identifiers
	 */
	public List<OxAuthSectorIdentifier> searchSectorIdentifiers(String pattern, int sizeLimit) {
		String[] targetArray = new String[] { pattern };
		Filter searchFilter = Filter.createSubstringFilter(OxTrustConstants.oxId, null, targetArray, null);

		List<OxAuthSectorIdentifier> result = persistenceEntryManager.findEntries(getDnForSectorIdentifier(null),
				OxAuthSectorIdentifier.class, searchFilter, sizeLimit);

		return result;
	}

	public List<OxAuthSectorIdentifier> getAllSectorIdentifiers() {
		return persistenceEntryManager.findEntries(getDnForSectorIdentifier(null), OxAuthSectorIdentifier.class, null);
	}

	/**
	 * Get sector identifier by oxId
	 *
	 * @param oxId
	 *            Sector identifier oxId
	 * @return Sector identifier
	 */
	public OxAuthSectorIdentifier getSectorIdentifierById(String oxId) {
		try {
			return persistenceEntryManager.find(OxAuthSectorIdentifier.class, getDnForSectorIdentifier(oxId));
		} catch (Exception e) {
			log.warn("Failed to find sector identifier by oxId " + oxId, e);
			return null;
		}
	}

	/**
	 * Generate new oxId for sector identifier
	 *
	 * @return New oxId for sector identifier
	 * @throws Exception
	 */
	public String generateIdForNewSectorIdentifier() {
		OxAuthSectorIdentifier sectorIdentifier = new OxAuthSectorIdentifier();
		String newId = null;
		String newDn = null;
		do {
			newId = generateIdForNewSectorIdentifierImpl();
			newDn = getDnForSectorIdentifier(newId);
			sectorIdentifier.setDn(newDn);
		} while (persistenceEntryManager.contains(newDn, OxAuthSectorIdentifier.class));
		return newId;
	}

	/**
	 * Generate new oxId for sector identifier
	 *
	 * @return New oxId for sector identifier
	 */
	private String generateIdForNewSectorIdentifierImpl() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Add new sector identifier entry
	 *
	 * @param sectorIdentifier
	 *            Sector identifier
	 */
	public void addSectorIdentifier(OxAuthSectorIdentifier sectorIdentifier) {
		persistenceEntryManager.persist(sectorIdentifier);
	}

	/**
	 * Update sector identifier entry
	 *
	 * @param sectorIdentifier
	 *            Sector identifier
	 */
	public void updateSectorIdentifier(OxAuthSectorIdentifier sectorIdentifier) {
		persistenceEntryManager.merge(sectorIdentifier);
	}

	/**
	 * Remove sector identifier entry
	 *
	 * @param sectorIdentifier
	 *            Sector identifier
	 */
	public void removeSectorIdentifier(OxAuthSectorIdentifier sectorIdentifier) {
		if (sectorIdentifier.getClientIds() != null) {
			List<String> clientDNs = sectorIdentifier.getClientIds();

			// clear references in Client entries
			for (String clientDN : clientDNs) {
				OxAuthClient client = clientService.getClientByDn(clientDN);
				client.setSectorIdentifierUri(null);
				clientService.updateClient(client);
			}
		}

		persistenceEntryManager.remove(sectorIdentifier);
	}

}
