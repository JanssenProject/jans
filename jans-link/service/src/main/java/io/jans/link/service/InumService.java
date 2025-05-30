/*
 * Janssen Project is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Janssen Project
 */

package io.jans.link.service;

import io.jans.link.model.GluuCustomPerson;
import io.jans.model.JansAttribute;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.INumGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.slf4j.Logger;
import java.io.Serializable;

@ApplicationScoped
@Named
public class InumService implements Serializable {

	private static final long serialVersionUID = 6685720517520443399L;

	private static final String PEOPLE = "0000";
	private static final String GROUP = "0003";
	private static final String ATTRIBUTE = "0005";
	private static final String TRUST_RELATIONSHIP = "0006";

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
		}
		return contains;
	}


	public boolean containsAttribute(String inum) {
		String dn = "inum=" + inum + ",ou=attributes,o=jans";
		return ldapEntryManager.contains(dn, JansAttribute.class);
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

}
