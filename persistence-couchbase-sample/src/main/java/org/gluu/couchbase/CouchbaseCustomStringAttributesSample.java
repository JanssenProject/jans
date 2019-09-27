package org.gluu.couchbase;

import java.util.Arrays;
import java.util.Date;

import org.gluu.couchbase.model.SimpleCustomStringUser;
import org.gluu.couchbase.model.SimpleUser;
import org.gluu.couchbase.model.UserRole;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.model.base.CustomAttribute;
import org.gluu.persist.model.base.CustomObjectAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 09/27/2019
 */
public final class CouchbaseCustomStringAttributesSample {

	private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

	private CouchbaseCustomStringAttributesSample() {
	}

	public static void main(String[] args) {
		// Prepare sample connection details
		CouchbaseSampleEntryManager couchbaseSampleEntryManager = new CouchbaseSampleEntryManager();

		// Create Couchbase entry manager
		CouchbaseEntryManager couchbaseEntryManager = couchbaseSampleEntryManager.createCouchbaseEntryManager();

		// Add dummy user
		SimpleUser newUser = new SimpleUser();
		newUser.setDn(String.format("inum=%s,ou=people,o=gluu", System.currentTimeMillis()));
		newUser.setUserId("sample_user_" + System.currentTimeMillis());
		newUser.setUserPassword("test");
		newUser.getCustomAttributes().add(new CustomObjectAttribute("streetAddress", Arrays.asList("London", "Texas", "Kiev")));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("test", "test_value"));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("birthdate", new Date()));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("enabled", false));
		newUser.getCustomAttributes().add(new CustomObjectAttribute("age", 18));

		newUser.setUserRole(UserRole.ADMIN);
		newUser.setNotes(Arrays.asList("note 1", "note 2", "note 3"));

		couchbaseEntryManager.persist(newUser);

		LOG.info("Added User '{}' with uid '{}' and key '{}'", newUser, newUser.getUserId(), newUser.getDn());

		// Find added dummy user but use custom class with String values
		SimpleCustomStringUser foundUser = couchbaseEntryManager.find(SimpleCustomStringUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUser, foundUser.getUserId(), foundUser.getDn());

		LOG.info("Custom attributes '{}'", foundUser.getCustomAttributes());
		for (CustomAttribute customAttribute : foundUser.getCustomAttributes()) {
			LOG.info("Found custom attribute '{}' with value '{}'", customAttribute.getName(), customAttribute.getValue());
		}
	}

}
