/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleCacheEntry;
import io.jans.orm.sql.operation.impl.SqlConnectionProvider;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 03/09/2020
 */
public final class SqlUpateCacheEntrySample {

    private static final Logger LOG = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private SqlUpateCacheEntrySample() {
    }

    public static void main(String[] args) {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        String key = UUID.randomUUID().toString();
        final String cacheDn = String.format("uuid=%s,%s", key, "ou=cache,o=jans");

        int expirationInSeconds = 60;
        Calendar expirationDate = Calendar.getInstance();
		expirationDate.setTime(new Date());
		expirationDate.add(Calendar.SECOND, expirationInSeconds);

        SimpleCacheEntry entity = new SimpleCacheEntry();
        entity.setTtl(expirationInSeconds);
        entity.setData("sample_data");
        entity.setId(key);
        entity.setDn(cacheDn);
        entity.setCreationDate(new Date());
        entity.setExpirationDate(expirationDate.getTime());
        entity.setDeletable(true);

		sqlEntryManager.persist(entity);

		// Try to update
		sqlEntryManager.merge(entity);

		// Try to update with removed value
        entity.setData(null);
		sqlEntryManager.merge(entity);
    }

}
