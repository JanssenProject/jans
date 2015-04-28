/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.service.ldap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.ldap.model.LdapDummyEntry;

import com.unboundid.ldap.sdk.Filter;

/**
 * Provides operations with LDAP entries
 * 
 * @author Yuriy Movchan Date: 04/28/2014
 */

@Scope(ScopeType.STATELESS)
@Name("ldapEntryService")
@AutoCreate
public class LdapEntryService implements Serializable {

	private static final long serialVersionUID = 7912416422111338984L;

	@In
	private LdapEntryManager ldapEntryManager;

	@Logger
	private Log log;

	public void removeRecursively(String baseDn) {
		List<LdapDummyEntry> entries = ldapEntryManager.findEntries(baseDn, LdapDummyEntry.class, Filter.createPresenceFilter("objectclass"));
		
		List<LdapDummyEntry> sortedEntries = new ArrayList<LdapDummyEntry>(entries);
		Collections.sort(sortedEntries, new Comparator<LdapDummyEntry>() {
			@Override
			public int compare(LdapDummyEntry entry1, LdapDummyEntry entry2) {
				Integer entryDn1Length = entry1.getDn().length();
				Integer entryDn2Length = entry2.getDn().length();

				return entryDn2Length.compareTo(entryDn1Length);
			}

			@Override
			public boolean equals(Object obj) {
				return System.identityHashCode(this) == System.identityHashCode(obj);
			}
		});

		for (LdapDummyEntry entry : sortedEntries) {
			System.out.println(entry.getDn());
		}

		for (LdapDummyEntry entry : sortedEntries) {
			ldapEntryManager.remove(entry);
		}
	}

}
