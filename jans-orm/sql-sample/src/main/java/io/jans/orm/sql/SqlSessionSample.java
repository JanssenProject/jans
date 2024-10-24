/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.model.SearchScope;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleSession;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SqlSessionSample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlSessionSample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        String key = UUID.randomUUID().toString();
        final String sessionDn = String.format("jansId=%s,%s", key, "ou=sessions,o=jans");

        int expirationInSeconds = 60;
        Calendar expirationDate = Calendar.getInstance();
		expirationDate.setTime(new Date());
		expirationDate.add(Calendar.SECOND, expirationInSeconds);

		SimpleSession entity = new SimpleSession();
        entity.setDeviceSecrets(Arrays.asList("secret1", "secret2"));
        entity.setId(key);
        entity.setDn(sessionDn);
        entity.setSessionAttributes(new HashMap<String, Object>());
        entity.getSessionAttributes().put("exp", expirationDate.getTimeInMillis());
        entity.getSessionAttributes().put("uid", Math.random() > 0.5 ? "mike" : "jorge");
        entity.getSessionAttributes().put("test", Math.random() > 0.5 ? true : false);

		sqlEntryManager.persist(entity);

		// Try to update
		sqlEntryManager.merge(entity);

		// Try to load
		SimpleSession loadedEntity = sqlEntryManager.find(SimpleSession.class, sessionDn);
		System.out.println(String.format("Loaded entry with uid = %s, type = %s", loadedEntity.getSessionAttributes().get("uid"), loadedEntity.getSessionAttributes().get("uid").getClass()));
		System.out.println(String.format("Loaded entry with exp = %s, type = %s", loadedEntity.getSessionAttributes().get("exp"), loadedEntity.getSessionAttributes().get("exp").getClass()));
		System.out.println(String.format("Loaded entry with test = %s, type = %s", loadedEntity.getSessionAttributes().get("test"), loadedEntity.getSessionAttributes().get("test").getClass()));

        Filter filterAuthenticated = Filter.createEqualityFilter("jansId", key);
        List<SimpleSession> sessions1 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterAuthenticated, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info("Found sessions: " + sessions1.size());

        Filter filterDeviceSecret = Filter.createEqualityFilter("deviceSecret", "secret2");
        List<SimpleSession> sessions2 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterDeviceSecret, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info("Found sessions: " + sessions2.size());

        // Find by json_path uid=mike
        Filter filterSessAttr1 = Filter.createEqualityFilter("jansSessAttr.uid", "mike");
        List<SimpleSession> sessAttr1 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterSessAttr1, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info("Found sessions by uid=mike: " + sessAttr1.size());

        // Find by json_path uid=jorge
        Filter filterSessAttr2 = Filter.createEqualityFilter("jansSessAttr.uid", "jorge");
        List<SimpleSession> sessAttr2 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterSessAttr2, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info("Found sessions by uid=jorge: " + sessAttr2.size());

        // Find by json_path exp
        Filter filterSessAttr3 = Filter.createEqualityFilter("jansSessAttr.exp", expirationDate.getTimeInMillis());
        List<SimpleSession> sessAttr3 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterSessAttr3, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info(String.format("Found sessions by exp == %d: " + sessAttr3.size(), expirationDate.getTimeInMillis()));

        // Find by json_path test=true
        Filter filterSessAttr4 = Filter.createEqualityFilter("jansSessAttr.test", true);
        List<SimpleSession> sessAttr4 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterSessAttr4, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info("Found sessions by test == true: " + sessAttr4.size());

        // Find by json_path test=false
        Filter filterSessAttr5 = Filter.createEqualityFilter("jansSessAttr.test", false);
        List<SimpleSession> sessAttr5 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterSessAttr5, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info("Found sessions by test == false: " + sessAttr5.size());

        // Find by json_path exp
        Filter filterSessAttr6 = Filter.createGreaterOrEqualFilter("jansSessAttr.exp", expirationDate.getTimeInMillis());
        List<SimpleSession> sessAttr6 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterSessAttr6, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info(String.format("Found sessions by exp >= %d: " + sessAttr6.size(), expirationDate.getTimeInMillis()));

        // Find by json_path exp
        Filter filterSessAttr7 = Filter.createLessOrEqualFilter("jansSessAttr.exp", expirationDate.getTimeInMillis());
        List<SimpleSession> sessAttr7 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterSessAttr7, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info(String.format("Found sessions by exp <= %d: " + sessAttr7.size(), expirationDate.getTimeInMillis()));

        // Find by json_path test=false
        Filter filterSessAttr8 = Filter.createSubstringFilter("jansSessAttr.uid", null, new String[] { "rg" }, null);
        List<SimpleSession> sessAttr8 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterSessAttr8, SearchScope.SUB, null, null, 0, 0, 0);
        LOG.info("Found sessions by uid == *rg*: " + sessAttr8.size());

	    // Find by json_path test=false
	    Filter filterSessAttr9 = Filter.createPresenceFilter("jansSessAttr.uid");
	    List<SimpleSession> sessAttr9 = sqlEntryManager.findEntries("o=jans", SimpleSession.class, filterSessAttr9, SearchScope.SUB, null, null, 0, 0, 0);
	    LOG.info("Found sessions by uid == *: " + sessAttr9.size());
    }
}
