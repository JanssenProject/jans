/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.scim.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.gluu.model.GluuAttribute;
import org.gluu.model.GluuStatus;
import org.gluu.model.TrustContact;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.search.filter.Filter;
import org.gluu.service.XmlService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.scim.model.GluuMetadataSourceType;
import io.jans.scim.model.GluuSAMLTrustRelationship;
import io.jans.scim.util.OxTrustConstants;

/**
 * Provides operations with trust relationships
 * 
 * @author Pankaj
 * @author Yuriy Movchan Date: 11.05.2010
 * 
 */
@ApplicationScoped
public class TrustService implements Serializable {

	private static final long serialVersionUID = -8128546040230316737L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager persistenceEntryManager;

	@Inject
	private AttributeService attributeService;

	@Inject
	private OrganizationService organizationService;

	@Inject
	private XmlService xmlService;

	private ObjectMapper objectMapper;
	
	@PostConstruct
	public void init() {
		this.objectMapper = new ObjectMapper();
	}

	public static final String GENERATED_SSL_ARTIFACTS_DIR = "ssl";

	public void addTrustRelationship(GluuSAMLTrustRelationship trustRelationship) {
		log.debug("Adding TR: {}", trustRelationship.getInum());
		String dn = trustRelationship.getDn();

		if (!containsTrustRelationship(dn)) {
			log.debug("Adding TR: {}", dn);
			persistenceEntryManager.persist(trustRelationship);
		} else {
			persistenceEntryManager.merge(trustRelationship);
		}
	}

	public void updateTrustRelationship(GluuSAMLTrustRelationship trustRelationship) {
		String dn = trustRelationship.getDn();
		boolean containsTrustRelationship = trustExist(dn);
		if (containsTrustRelationship) {
			log.info("Updating TR: {}", dn);
			persistenceEntryManager.merge(trustRelationship);
		} else {
			log.info("Adding TR: {}", dn);
			persistenceEntryManager.persist(trustRelationship);
		}
	}

	public void removeTrustRelationship(GluuSAMLTrustRelationship trustRelationship) {
		log.info("Removing TR: {}", trustRelationship.getInum());
		String dn = trustRelationship.getDn();

		if (containsTrustRelationship(dn)) {
			log.debug("Removing TR: {}", dn);
			persistenceEntryManager.remove(trustRelationship);
		}
	}

	public GluuSAMLTrustRelationship getRelationshipByInum(String inum) {
		try {
			return persistenceEntryManager.find(GluuSAMLTrustRelationship.class, getDnForTrustRelationShip(inum));
		} catch (Exception e) {
			log.error(e.getMessage());
			return null;
		}

	}

	public GluuSAMLTrustRelationship getRelationshipByDn(String dn) {
		if (StringHelper.isNotEmpty(dn)) {
			try {
				return persistenceEntryManager.find(GluuSAMLTrustRelationship.class, dn);
			} catch (Exception e) {
				log.info(e.getMessage());
			}

		}
		return null;
	}

	/**
	 * This is a LDAP operation as LDAP and IDP will always be in sync. We can just
	 * call LDAP to fetch all Trust Relationships.
	 */
	public List<GluuSAMLTrustRelationship> getAllTrustRelationships() {
		return persistenceEntryManager.findEntries(getDnForTrustRelationShip(null), GluuSAMLTrustRelationship.class,
				null);
	}

	public List<GluuSAMLTrustRelationship> getAllActiveTrustRelationships() {
		GluuSAMLTrustRelationship trustRelationship = new GluuSAMLTrustRelationship();
		trustRelationship.setBaseDn(getDnForTrustRelationShip(null));
		trustRelationship.setStatus(GluuStatus.ACTIVE);

		return persistenceEntryManager.findEntries(trustRelationship);
	}

	public List<GluuSAMLTrustRelationship> getAllFederations() {
		List<GluuSAMLTrustRelationship> result = new ArrayList<GluuSAMLTrustRelationship>();
		for (GluuSAMLTrustRelationship trust : getAllActiveTrustRelationships()) {
			if (trust.isFederation()) {
				result.add(trust);
			}
		}

		return result;
	}

	public List<GluuSAMLTrustRelationship> getAllOtherFederations(String inum) {
		List<GluuSAMLTrustRelationship> result = getAllFederations();
		result.remove(getRelationshipByInum(inum));
		return result;
	}

	/**
	 * Check if LDAP server contains trust relationship with specified attributes
	 * 
	 * @return True if trust relationship with specified attributes exist
	 */
	public boolean containsTrustRelationship(String dn) {
		return persistenceEntryManager.contains(dn, GluuSAMLTrustRelationship.class);
	}

	public boolean trustExist(String dn) {
		GluuSAMLTrustRelationship trust = null;
		try {
			trust = persistenceEntryManager.find(GluuSAMLTrustRelationship.class, dn);
		} catch (Exception e) {
			trust = null;
		}
		return (trust != null) ? true : false;
	}

	/**
	 * Generate new inum for trust relationship
	 * 
	 * @return New inum for trust relationship
	 */
	public String generateInumForNewTrustRelationship() {
		String newDn = null;
		String newInum = null;
		do {
			newInum = generateInumForNewTrustRelationshipImpl();
			newDn = getDnForTrustRelationShip(newInum);
		} while (containsTrustRelationship(newDn));

		return newInum;
	}

	/**
	 * Generate new inum for trust relationship
	 * 
	 * @return New inum for trust relationship
	 */
	private String generateInumForNewTrustRelationshipImpl() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Get all metadata source types
	 * 
	 * @return Array of metadata source types
	 */
	public GluuMetadataSourceType[] getMetadataSourceTypes() {
		return GluuMetadataSourceType.values();
	}

	/**
	 * Build DN string for trust relationship
	 * 
	 * @param inum
	 *            Inum
	 * @return DN string for specified trust relationship or DN for trust
	 *         relationships branch if inum is null
	 */
	public String getDnForTrustRelationShip(String inum) {
		String organizationDN = organizationService.getDnForOrganization();
		if (StringHelper.isEmpty(inum)) {
			return String.format("ou=trustRelationships,%s", organizationDN);
		}
		return String.format("inum=%s,ou=trustRelationships,%s", inum, organizationDN);
	}

	public List<TrustContact> getContacts(GluuSAMLTrustRelationship trustRelationship) {
		List<String> gluuTrustContacts = trustRelationship.getGluuTrustContact();
		List<TrustContact> contacts = new ArrayList<TrustContact>();
		if (gluuTrustContacts != null) {
			for (String contact : gluuTrustContacts) {
				contacts.add(getTrustContactFromString(contact));
			}
		}
		return contacts;
	}

	public void saveContacts(GluuSAMLTrustRelationship trustRelationship, List<TrustContact> contacts) {
		if (contacts != null && !contacts.isEmpty()) {
			List<String> gluuTrustContacts = new ArrayList<String>();
			for (TrustContact contact : contacts) {
				gluuTrustContacts.add(getStringFromTrustContact(contact));
			}
			trustRelationship.setGluuTrustContact(gluuTrustContacts);
		}
	}

	public List<GluuSAMLTrustRelationship> getDeconstructedTrustRelationships(
			GluuSAMLTrustRelationship trustRelationship) {
		List<GluuSAMLTrustRelationship> result = new ArrayList<GluuSAMLTrustRelationship>();
		for (GluuSAMLTrustRelationship trust : getAllTrustRelationships()) {
			if (trustRelationship.equals(getTrustContainerFederation(trust))) {
				result.add(trust);
			}
		}
		return result;
	}

	public List<GluuSAMLTrustRelationship> getChildTrusts(GluuSAMLTrustRelationship trustRelationship) {
		List<GluuSAMLTrustRelationship> all = getAllTrustRelationships();
		if (all != null && !all.isEmpty()) {
			return all.stream().filter(e -> !e.isFederation())
					.filter(e -> e.getGluuContainerFederation().equalsIgnoreCase(trustRelationship.getDn()))
					.collect(Collectors.toList());
		} else {
			return new ArrayList<GluuSAMLTrustRelationship>();
		}
	}

	public GluuSAMLTrustRelationship getTrustByUnpunctuatedInum(String unpunctuated) {
		for (GluuSAMLTrustRelationship trust : getAllTrustRelationships()) {
			if (StringHelper.removePunctuation(trust.getInum()).equals(unpunctuated)) {
				return trust;
			}
		}
		return null;
	}

	public GluuSAMLTrustRelationship getTrustContainerFederation(GluuSAMLTrustRelationship trustRelationship) {
		GluuSAMLTrustRelationship relationshipByDn = getRelationshipByDn(trustRelationship.getDn());
		return relationshipByDn;
	}

	public GluuSAMLTrustRelationship getTrustContainerFederation(String dn) {
		GluuSAMLTrustRelationship relationshipByDn = getRelationshipByDn(dn);
		return relationshipByDn;
	}

	public List<GluuSAMLTrustRelationship> searchSAMLTrustRelationships(String pattern, int sizeLimit) {
		String[] targetArray = new String[] { pattern };
		Filter displayNameFilter = Filter.createSubstringFilter(OxTrustConstants.displayName, null, targetArray, null);
		Filter descriptionFilter = Filter.createSubstringFilter(OxTrustConstants.description, null, targetArray, null);
		Filter inumFilter = Filter.createSubstringFilter(OxTrustConstants.inum, null, targetArray, null);
		Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);
		return persistenceEntryManager.findEntries(getDnForTrustRelationShip(null), GluuSAMLTrustRelationship.class,
				searchFilter, sizeLimit);

	}

	public List<GluuSAMLTrustRelationship> getAllSAMLTrustRelationships(int sizeLimit) {
		return persistenceEntryManager.findEntries(getDnForTrustRelationShip(null), GluuSAMLTrustRelationship.class,
				null, sizeLimit);
	}

	/**
	 * Remove attribute
	 * 
	 * @param attribute
	 *            Attribute
	 */
	public boolean removeAttribute(GluuAttribute attribute) {
		log.trace("Removing attribute from trustRelationships");
		List<GluuSAMLTrustRelationship> trustRelationships = getAllTrustRelationships();
		log.trace(String.format("Iterating '%d' trustRelationships", trustRelationships.size()));
		for (GluuSAMLTrustRelationship trustRelationship : trustRelationships) {
			log.trace("Analyzing '%s'.", trustRelationship.getDisplayName());
			List<String> customAttrs = trustRelationship.getReleasedAttributes();
			if (customAttrs != null) {
				for (String attrDN : customAttrs) {
					log.trace("'%s' has custom attribute '%s'", trustRelationship.getDisplayName(), attrDN);
					if (attrDN.equals(attribute.getDn())) {
						log.trace("'%s' matches '%s'.  deleting it.", attrDN, attribute.getDn());
						List<String> updatedAttrs = new ArrayList<String>();
						updatedAttrs.addAll(customAttrs);
						updatedAttrs.remove(attrDN);
						if (updatedAttrs.size() == 0) {
							trustRelationship.setReleasedAttributes(null);
						} else {
							trustRelationship.setReleasedAttributes(updatedAttrs);
						}
						updateTrustRelationship(trustRelationship);
						break;
					}
				}
			}
		}
		attributeService.removeAttribute(attribute);
		return true;
	}
    public TrustContact getTrustContactFromString(String data) {
        if (data == null) {
            return null;
        }
        
        // Try to convert from XML first
        if (data.startsWith("<")) {
            Document doc;
    		try {
    			doc = xmlService.getXmlDocument(data, true);

        		String name = xmlService.getNodeValue(doc, "/trustContact/name", null);
                String mail = xmlService.getNodeValue(doc, "/trustContact/mail", null);
                String phone = xmlService.getNodeValue(doc, "/trustContact/phone", null);
                String title = xmlService.getNodeValue(doc, "/trustContact/title", null);

                TrustContact trustContact = new TrustContact();

                trustContact.setName(name);
	        	trustContact.setPhone(mail);
	        	trustContact.setMail(phone);
	        	trustContact.setTitle(title);

	        	return trustContact;
    		} catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException ex) {
                log.error("Failed to create TrustContact from XML {}", ex, data);
            	
                return null;
			}
        } else {	
	        JsonNode rootNode;
	        try {
	        	rootNode = objectMapper.readTree(data);
	        } catch (IOException ex) {
	            log.error("Failed to create TrustContact from JSON {}", ex, data);
	
	            return null;
	        }
	        
	        TrustContact trustContact = new TrustContact();
	        if (rootNode.hasNonNull("name")) {
	        	trustContact.setName(rootNode.get("name").asText());
	        }
	        if (rootNode.hasNonNull("phone")) {
	        	trustContact.setPhone(rootNode.get("phone").asText());
	        }
	        if (rootNode.hasNonNull("mail")) {
	        	trustContact.setMail(rootNode.get("mail").asText());
	        }
	        if (rootNode.hasNonNull("title")) {
	        	trustContact.setTitle(rootNode.get("title").asText());
	        }
	
	        return trustContact;
        }
    }

    public String getStringFromTrustContact(TrustContact contact) {
        if (contact == null) {
            return null;
        }

        ObjectNode rootNode = objectMapper.createObjectNode();
        if (StringHelper.isNotEmpty(contact.getName())) {
        	rootNode.put("name", contact.getName());
        }
        if (StringHelper.isNotEmpty(contact.getPhone())) {
	        rootNode.put("phone", contact.getPhone());
	    }
        if (StringHelper.isNotEmpty(contact.getMail())) {
        	rootNode.put("mail", contact.getMail());
        }
        if (StringHelper.isNotEmpty(contact.getTitle())) {
        	rootNode.put("title", contact.getTitle());
        }

        return rootNode.toString();
    }      

}
