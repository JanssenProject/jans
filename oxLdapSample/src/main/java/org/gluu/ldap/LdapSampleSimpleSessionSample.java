package org.gluu.ldap;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.xdi.log.LoggingHelper;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

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

	public static void main(String[] args) throws InterruptedException {
		// Prepare sample connection details
		LdapSampleEntryManager ldapSampleEntryManager = new LdapSampleEntryManager();
		final LdapEntryManager ldapEntryManager = ldapSampleEntryManager.createLdapEntryManager();

		try {

			// Create LDAP entry manager
			String sessionId = "xyzcyzxy-a41a-45ad-8a83-61485dbad553";
			final String sessionDn = "uniqueIdentifier=" + sessionId + ",ou=session,o=@!E8F2.853B.1E7B.ACE2!0001!39A4.C163,o=gluu";
			final String userDn = "inum=@!E8F2.853B.1E7B.ACE2!0001!39A4.C163!0000!A8F2.DE1E.D7FB,ou=people,o=@!E8F2.853B.1E7B.ACE2!0001!39A4.C163,o=gluu";

			final SimpleSessionState simpleSessionState = new SimpleSessionState();
			simpleSessionState.setDn(sessionDn);
			simpleSessionState.setId(sessionId);
			simpleSessionState.setLastUsedAt(new Date());

			ldapEntryManager.persist(simpleSessionState);
			System.out.println("Persisted");

			int threadCount = 500;
			ExecutorService executorService = Executors.newFixedThreadPool(threadCount, daemonThreadFactory());
			for (int i = 0; i < threadCount; i++) {
				final int count = i;
				executorService.execute(new Runnable() {
					@Override
					public void run() {
						SimpleSessionState simpleSessionStateFromLdap = ldapEntryManager.find(SimpleSessionState.class, sessionDn);

						String randomUserDn = count % 2 == 0 ? userDn : "";

						simpleSessionStateFromLdap.setUserDn(randomUserDn);
						simpleSessionState.setLastUsedAt(new Date());
						ldapEntryManager.merge(simpleSessionState);
						System.out.println("Merged thread: " + count + ", userDn: " + randomUserDn);
					}
				});
			}

			Thread.sleep(5000L);
		} finally {
			ldapEntryManager.getLdapOperationService().getConnectionPool().close();
		}
	}

	public static ThreadFactory daemonThreadFactory() {
		return new ThreadFactory() {
			public Thread newThread(Runnable p_r) {
				Thread thread = new Thread(p_r);
				thread.setDaemon(true);
				return thread;
			}
		};
	}


}
