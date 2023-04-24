/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package io.jans.cacherefresh.service;

import io.jans.cacherefresh.model.GluuCustomPerson;
import io.jans.model.GluuAttribute;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.InumEntry;
import io.jans.util.INumGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;

//import javax.inject.Inject;
import java.io.Serializable;

/*

If you update this API, please keep this table in the github docs:
https://github.com/GluuFederation/docs/blob/master/sources/reference/api/id-generation.md

| `prefix` | `type`               | `description`                          |
| -------- | -------------------- | -------------------------------------- |
| 0000     | people               | Person object                          |
| 0001     | organization         | Organization object                    |
| 0002     | configuration            | Configuration object                       |
| 0003     | group                | Group object                           |
| 0004     | server               | Server object                          |
| 0005     | attribute            | User attribute (claim) object          |
| 0006     | tRelationship        | SAML Trust Relationship object         |
| 0008     | client               | OAuth2 Client object                   |
| 0009     | scope                | OAuth2 Scope Object                    |
| 0010     | uma-resource-set     | UMA Resource Set Object                |
| 0011     | interception-script  | Gluu Server interception script object |
| 0012     | sector-identifier    | Managed Sector Identifier URI          |

 */

@ApplicationScoped
@Named
public class InumService implements Serializable {

	public InumService() {
		super();
		// TODO Auto-generated constructor stub
	}

	private static final long serialVersionUID = 6685720517520443399L;

	private static final String PEOPLE = "0000";
	// private static final String ORGANIZATION = "0001";
	// private static final String CONFIGURATION = "0002";
	private static final String GROUP = "0003";
	// private static final String SERVER = "0004";
	private static final String ATTRIBUTE = "0005";
	private static final String TRUST_RELATIONSHIP = "0006";
	private static final String CLIENT = "0008";
	private static final String UMA_SCOPE = "0009";
	private static final String UMA_RESOURCE_SET = "0010";
	private static final String INTERCEPTION_SCRIPT = "0011";
	private static final String SECTOR_IDENTIFIER =  "0012";

	private static final String SEPARATOR = "!";

	private static final int MAX = 10;

	@Inject
	private Logger log;

	@Inject
	PersistenceEntryManager ldapEntryManager;


	public boolean contains(String inum, String type) {
		boolean contains = false;
		if ("attribute".equals(type)) {
			contains = containsAttribute(inum);
		} else if ("people".equals(type)) {
			contains = containsPerson(inum);
//		} else if ("organization".equals(type)) {
//			contains = containsOrganization(inum);
//		} else if ("configuration".equals(type)) {
//			contains = containsConfiguration(inum);
//		} else if ("group".equals(type)) {
//			contains = containsGroup(inum);
//		} else if ("trelationship".equals(type)) {
//			contains = containsTrustRelationship(inum);
//		} else if ("client".equals(type)) {
//			contains = containsClient(inum);
//		} else if ("uma-scope".equals(type)) {
//			contains = containsUmaScope(inum);
//		} else if ("uma-resource-set".equals(type)) {
//			contains = containsUmaResourceSet(inum);
//		} else if ("interception-script".equals(type)) {
//			contains = containsInterceptionScript(inum);
		}
		return contains;
	}


	public boolean containsAttribute(String inum) {
		String dn = "inum=" + inum + ",ou=attributes,o=gluu";
		return ldapEntryManager.contains(dn, GluuAttribute.class);
	}

	public boolean containsPerson(String inum) {
		boolean contains = true;
		String dn = String.format("inum=%s,ou=people,o=jans", inum);
		contains = ldapEntryManager.contains(dn, GluuCustomPerson.class);
		if (contains)
			return true;
		contains = ldapEntryManager.contains(dn, GluuCustomPerson.class);
		return contains;
	}

	/*public boolean containsGroup(String inum) {
		boolean contains = true;
		String dn = String.format("inum=%s,ou=groups,o=gluu", inum);
		contains = ldapEntryManager.contains(dn, GluuGroup.class);
		if (contains)
			return true;
		contains = ldapEntryManager.contains(dn, GluuGroup.class);
		return contains;
	}*/

	/*public boolean containsConfiguration(String inum) {
		String dn = String.format("inum=%s,ou=configurations,o=gluu", inum);
		return ldapEntryManager.contains(dn, GluuConfiguration.class);
	}*/

	/*public boolean containsTrustRelationship(String inum) {
		String dn = String.format("inum=%s,ou=trustRelationships,inum=%s,o=gluu", inum);
		return ldapEntryManager.contains(dn, GluuSAMLTrustRelationship.class);
	}*/

	public boolean containsOrganization(String inum) {
		String dn = String.format("o=%s,o=gluu", inum);
		return ldapEntryManager.contains(dn, InumEntry.class);
	}

	/*public String getDnForInum(String baseDn, String inum) {
		if (baseDn == null || baseDn.trim().equals("")) {
			baseDn = appConfiguration.getBaseDN();
		}
		return String.format("inum=%s,ou=inums,%s", inum, baseDn);
	}*/

	public String generateInums(String type) {
		return generateInums(type, true);
	}

	public String generateInums(String type, boolean checkInDb) {
		String inum = "";
		int counter = 0;
		try {
			while (true) {
				inum = getInum(type);
				if (inum == null || inum.trim().equals(""))
					break;

				if (!checkInDb) {
					break;
				}
				if (!contains(inum, type)) {
					break;
				}
				/* Just to make sure it doesn't get into an infinite loop */
				if (counter > MAX) {
					inum = "";
					log.error("Infinite loop problem while generating new inum");
					break;
				}
				counter++;
			}
		} catch (Exception ex) {
			log.error("Failed to generate inum", ex);
		}
		return inum;
	}

	private String getInum(String type) {
		String inum = "";
		if ("people".equals(type)) {
			inum = PEOPLE + SEPARATOR + generateInum(2);
		} else if ("group".equals(type)) {
			inum = GROUP + SEPARATOR + generateInum(2);
		} else if ("attribute".equals(type)) {
			inum = ATTRIBUTE + SEPARATOR + generateInum(2);
		} else if ("trelationship".equals(type)) {
			inum = TRUST_RELATIONSHIP + SEPARATOR + generateInum(2);
		}
		return inum;
	}

	private String generateInum(int size) {
		String inum = "";
		long value;
		while (true) {
			inum = INumGenerator.generate(size);
			try {
				value = Long.parseLong(inum.replace(".", ""), 16);
				if (value < 7) {
					continue;
				}
			} catch (Exception ex) {
				log.error("Error generating inum: " + ex.getMessage());
			}
			break;
		}
		return inum;
	}

	/**
	 * Add an Inum to the DB by object
	 *
	 * @return True if user exist
	 */
	/*public boolean addInumByObject(EntityManager inumEntryManager, InumSqlEntry inumEntry) {
		boolean successs = false;

		EntityTransaction entityTransaction = inumEntryManager.getTransaction();

		entityTransaction.begin();
		try {

			// add inum
			inumEntryManager.persist(inumEntry);

			successs = true;
		} finally {
			if (successs) {
				// Commit transaction
				entityTransaction.commit();
			} else {
				// Rollback transaction
				entityTransaction.rollback();
			}
		}

		return successs;

	}*/

	/**
	 * Check if inum DB contains an inum
	 *
	 * @return True if user exist
	 */
	/*public boolean containsInum(EntityManager inumEntryManager, String inum) {
		return inumEntryManager.createQuery("select u.id from inumTable u where u.inum = :inum").setParameter("inum", inum)
				.setMaxResults(1).getResultList().size() > 0;
	}*/

	/**
	 * get an inum from inum DB by inum value
	 *
	 * @return InumSqlEntry
	 */
	/*public InumSqlEntry findInum(EntityManager inumEntryManager, String inum) {

		boolean successs = false;

		EntityTransaction entityTransaction = inumEntryManager.getTransaction();

		entityTransaction.begin();

		InumSqlEntry result = null;
		try {

			@SuppressWarnings("unchecked")
			List<InumSqlEntry> inumList = inumEntryManager.createQuery("select u from inumTable u where u.inum = :inum")
					.setParameter("inum", inum).setMaxResults(1).getResultList();

			if (inumList.size() == 0) {
				result = null;
			} else {
				result = inumList.get(0);
				successs = true;
			}

		} finally {
			if (successs) {
				// Commit transaction
				entityTransaction.commit();
			} else {
				// Rollback transaction
				entityTransaction.rollback();
			}
		}
		return result;
	}*/

	/**
	 * get an inum from inum DB by inum value and type
	 *
	 * @return InumSqlEntry
	 */
	/*public InumSqlEntry findInum(EntityManager inumEntryManager, String inum, String type) {
		@SuppressWarnings("unchecked")
		List<InumSqlEntry> inumList = inumEntryManager.createQuery("select u from inumTable u where u.inum = :inum and u.type = :type")
				.setParameter("inum", inum).setParameter("type", type).setMaxResults(1).getResultList();

		if (inumList.size() == 0) {
			return null;
		}

		return inumList.get(0);
	}*/

	/**
	 * get a list of inums from inum DB associated to a specific type
	 *
	 * @return List of InumSqlEntry
	 */
	/*public List<InumSqlEntry> findInums(EntityManager inumEntryManager, String type) {
		@SuppressWarnings("unchecked")
		List<InumSqlEntry> inumList = inumEntryManager.createQuery("select u from inumTable u where u.type = :type")
				.setParameter("type", type).getResultList();

		if (inumList.size() == 0) {
			return null;
		}

		return inumList;
	}*/

	/**
	 * removes an inum from inum DB
	 *
	 * @return List of InumSqlEntry
	 */
	/*public boolean removeInum(EntityManager inumEntryManager, String inum) {

		boolean successs = false;

		EntityTransaction entityTransaction = inumEntryManager.getTransaction();
		InumSqlEntry inumEntry = null;
		try {
			inumEntry = findInum(inumEntryManager, inum);
		} catch (Exception ex) {
			log.error("an error occured : could not find inum", ex);
			ex.printStackTrace();
			return false;
		}

		if (inumEntry == null) {
			return false;
		}

		entityTransaction.begin();

		try {

			inumEntryManager.remove(inumEntry);
			successs = true;

		} finally {
			if (successs) {
				// Commit transaction
				entityTransaction.commit();
			} else {
				// Rollback transaction
				entityTransaction.rollback();
			}
		}

		return successs;

	}*/

	/**
	 * get an inum from inum DB by inum value
	 *
	 * @return InumSqlEntry
	 */
	/*public InumSqlEntry findInumByObject(EntityManager inumEntryManager, String inum) {

		boolean successs = false;

		EntityTransaction entityTransaction = inumEntryManager.getTransaction();

		entityTransaction.begin();
		InumSqlEntry result = null;

		try {

			InumSqlEntry tempInum = new InumSqlEntry();
			tempInum.setInum(inum);

			// find inum
			result = inumEntryManager.find(InumSqlEntry.class, tempInum);
			if (result != null) {
				successs = true;
			}
		} finally {
			if (successs) {
				// Commit transaction
				entityTransaction.commit();
			} else {
				// Rollback transaction
				entityTransaction.rollback();
			}
		}

		return result;

	}*/

	/**
	 * Add an Inum to the DB
	 *
	 * @return True if user exist
	 */
	/*public boolean addInum(EntityManager inumEntryManager, String inum, String type) {
		boolean successs = false;

		EntityTransaction entityTransaction = inumEntryManager.getTransaction();

		entityTransaction.begin();
		try {
			// Prepare inum
			InumSqlEntry inumEntry = new InumSqlEntry();
			inumEntry.setInum(inum);
			inumEntry.setType(type);

			// add inum
			inumEntryManager.persist(inumEntry);

			successs = true;
		} finally {
			if (successs) {
				// Commit transaction
				entityTransaction.commit();
			} else {
				// Rollback transaction
				entityTransaction.rollback();
			}
		}

		return successs;

	}*/

}
