/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.model.SimpleUser;
import io.jans.orm.couchbase.operation.impl.CouchbaseConnectionProvider;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.model.base.CustomEntry;

/**
 * @author Yuriy Movchan Date: 11/03/2016
 */
public final class CouchbaseUpdateAttributeSample {

    private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);

    private CouchbaseUpdateAttributeSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        CouchbaseEntryManagerSample couchbaseEntryManagerSample = new CouchbaseEntryManagerSample();

        // Create Couchbase entry manager
        CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerSample.createCouchbaseEntryManager();

        String uid = "sample_user_" + System.currentTimeMillis();
        String dn = String.format("inum=%s,ou=people,o=jans", System.currentTimeMillis());

        SimpleUser newUser = new SimpleUser();
        newUser.setDn(dn);
        newUser.setUserId(uid);
        newUser.setUserPassword("test");
        couchbaseEntryManager.persist(newUser);

        SimpleUser user = couchbaseEntryManager.find(SimpleUser.class, dn);
        LOG.info("Found user '{}'", user);

        CustomEntry customEntry = new CustomEntry();
		customEntry.setDn(user.getDn());
		customEntry.setCustomObjectClasses(new String[] { "jansPerson" });

		Date now = new GregorianCalendar(TimeZone.getTimeZone("UTC")).getTime();
		String nowDateString = couchbaseEntryManager.encodeTime(customEntry.getDn(), now);
		CustomAttribute customAttribute = new CustomAttribute("jansLastLogonTime", nowDateString);
		customEntry.getCustomAttributes().add(customAttribute);

		couchbaseEntryManager.merge(customEntry);

        SimpleUser userAfterUpdate = couchbaseEntryManager.find(SimpleUser.class, dn);
        LOG.info("Found user after update '{}'", userAfterUpdate);
        LOG.info("jansLastLogonTime after update '{}'", userAfterUpdate.getAttribute("jansLastLogonTime"));
    }

}
