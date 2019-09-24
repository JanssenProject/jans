package org.gluu.couchbase;

import java.util.Arrays;

import org.gluu.couchbase.model.SimpleUser;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.persist.couchbase.operation.impl.CouchbaseConnectionProvider;
import org.gluu.persist.model.base.CustomObjectAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Movchan Date: 09/16/2019
 */
public final class CouchbaseCustomMultivaluedTypesSample {

	private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

	private CouchbaseCustomMultivaluedTypesSample() {
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
		newUser.getCustomAttributes().add(new CustomObjectAttribute("fuzzy", "test_value"));
		newUser.setNotes(Arrays.asList("note 1", "note 2", "note 3"));

		couchbaseEntryManager.persist(newUser);

		LOG.info("Added User '{}' with uid '{}' and key '{}'", newUser, newUser.getUserId(), newUser.getDn());
		LOG.info("Persisted custom attributes '{}'", newUser.getCustomAttributes());

		// Find added dummy user
		SimpleUser foundUser = couchbaseEntryManager.find(SimpleUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUser, foundUser.getUserId(), foundUser.getDn());

		LOG.info("Custom attributes '{}'", foundUser.getCustomAttributes());

		// Update custom attributes
		foundUser.setAttributeValues("streetAddress", Arrays.asList("London", "Texas", "Kiev", "Dublin"));
		foundUser.setAttributeValues("test", Arrays.asList("test_value_1", "test_value_2", "test_value_3", "test_value_4"));
		foundUser.setAttributeValues("fuzzy", Arrays.asList("fuzzy_value_1", "fuzzy_value_2"));
		foundUser.setAttributeValue("simple", "simple");
		
		CustomObjectAttribute multiValuedSingleValue = new CustomObjectAttribute("multivalued", "multivalued_single_valued");
		multiValuedSingleValue.setMultiValued(true);
		foundUser.getCustomAttributes().add(multiValuedSingleValue);
		couchbaseEntryManager.merge(foundUser);
		LOG.info("Updated custom attributes '{}'", foundUser.getCustomAttributes());

		// Find updated dummy user
		SimpleUser foundUpdatedUser = couchbaseEntryManager.find(SimpleUser.class, newUser.getDn());
		LOG.info("Found User '{}' with uid '{}' and key '{}'", foundUpdatedUser, foundUpdatedUser.getUserId(), foundUpdatedUser.getDn());

		LOG.info("Cusom attributes '{}'", foundUpdatedUser.getCustomAttributes());
	}

}
