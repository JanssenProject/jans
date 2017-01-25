package org.gluu.ldap;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.xdi.ldap.model.CustomAttribute;
import org.xdi.ldap.model.SearchScope;
import org.xdi.ldap.model.VirtualListViewResponse;
import org.xdi.log.LoggingHelper;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchResult;

/**
 * @author Yuriy Movchan
 * Date: 01/25/2016
 */
public class LdapSampleSimpleSessionSample {

	private static final Logger log;

	static {
		StatusLogger.getLogger().setLevel(Level.OFF);
		LoggingHelper.configureConsoleAppender();
		log = Logger.getLogger(LdapSampleSimpleSessionSample.class);
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		LdapSampleEntryManager ldapSampleEntryManager = new LdapSampleEntryManager();

		// Create LDAP entry manager
		LdapEntryManager ldapEntryManager = ldapSampleEntryManager.createLdapEntryManager();
		String sessionId = "xyzcyzxy-a41a-45ad-8a83-61485dbad550";
		String sessionDn = "uniqueIdentifier=" + sessionId + ",ou=session,o=@!E8F2.853B.1E7B.ACE2!0001!39A4.C163,o=gluu";
		String userDn = "inum=@!E8F2.853B.1E7B.ACE2!0001!39A4.C163!0000!A8F2.DE1E.D7FB,ou=people,o=@!E8F2.853B.1E7B.ACE2!0001!39A4.C163,o=gluu"; 

		SimpleSessionState simpleSessionState = new SimpleSessionState();
		simpleSessionState.setDn(sessionDn);
		simpleSessionState.setId(sessionId);
		simpleSessionState.setLastUsedAt(new Date());

		ldapEntryManager.persist(simpleSessionState);
		
		SimpleSessionState simpleSessionStateFromLdap = ldapEntryManager.find(SimpleSessionState.class, sessionDn);
		
		simpleSessionStateFromLdap.setUserDn(userDn);
		simpleSessionState.setLastUsedAt(new Date());
		ldapEntryManager.merge(simpleSessionState);
	}


}
