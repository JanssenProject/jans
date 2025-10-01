/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import io.jans.orm.cloud.spanner.impl.SpannerEntryManager;
import io.jans.orm.cloud.spanner.impl.SpannerEntryManagerFactory;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.ProcessBatchOperation;
import io.jans.orm.model.SearchScope;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.Pair;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public class ManualSpannerEntryManagerTest {
	
	private SpannerEntryManager manager;
	private SessionId persistedSessionId;
	
	private int totalProcessedCount;
	
	@BeforeClass(enabled = false)
	public void init() throws IOException {
        manager = createSpannerEntryManager();
	}

	@AfterClass(enabled = false)
	public void destroy() throws IOException {
		if (manager != null) {
			manager.destroy();
		}
	}

    @Test(enabled = false)
    public void createSessionId() throws IOException {
    	SessionId sessionId = buildSessionId();
        manager.persist(sessionId);
        
        persistedSessionId = sessionId;

        System.out.println(sessionId);
    }

    @Test(dependsOnMethods = "createSessionId", enabled = false)
    public void searchSessionId() throws IOException {
        List<SessionId> sessionIdList = manager.findEntries("o=jans", SessionId.class, null);
        System.out.println(sessionIdList);
    }

    @Test(dependsOnMethods = "searchSessionId", enabled = false)
    public void containsSessionId() throws IOException {
    	SessionId sessionId = persistedSessionId;
    	
        boolean result = manager.contains(sessionId);
        
        assertTrue(result);

        System.out.println(sessionId);
    }

    @Test(dependsOnMethods = "containsSessionId", enabled = false)
    public void updateSessionId() throws IOException {
    	SessionId sessionId = persistedSessionId;

    	sessionId.setAuthenticationTime(new Date());
        sessionId.setLastUsedAt(new Date());

        sessionId.setJwt(null);
        sessionId.setIsJwt(null);

    	Pair<Date, Integer> expirarion = expirationDate(new Date());
        sessionId.setExpirationDate(expirarion.getFirst());
        sessionId.setTtl(expirarion.getSecond());

        manager.merge(sessionId);

        System.out.println(sessionId);
    }

    @Test(dependsOnMethods = "updateSessionId", enabled = false)
    public void countSessionId() throws IOException {
    	SessionId sessionId = persistedSessionId;

    	int countEntries = manager.countEntries(sessionId);
    	
    	assertEquals(countEntries, 1);
    }

    @Test(dependsOnMethods = "countSessionId", enabled = false)
    public void containsSessionIdAfterUpdate() throws IOException {
    	SessionId sessionId = persistedSessionId;
    	
        boolean result = manager.contains(sessionId);
        
        assertTrue(result);

        System.out.println(sessionId);
    }

    @Test(dependsOnMethods = "containsSessionIdAfterUpdate", enabled = false)
    public void deleteSessionId() throws IOException {
    	SessionId sessionId = persistedSessionId;
    	
        manager.remove(sessionId);
        
        boolean result = manager.contains(sessionId);

        assertFalse(result);

        System.out.println(sessionId);
    }

    @Test(dependsOnMethods = "deleteSessionId", enabled = false)
    public void searchSessionIdWithRanges() throws IOException {
		String outsideSid = UUID.randomUUID().toString();
		for (int i = 0; i < 20; i++) {
			SessionId sessionId = buildSessionId();
			sessionId.setOutsideSid(outsideSid);

			manager.persist(sessionId);
		}

		Filter filter = Filter.createEqualityFilter("sid", outsideSid);

		List<SessionId> sessionIdList = manager.findEntries("o=jans", SessionId.class, filter);
		assertNotNull(sessionIdList);
		assertEquals(sessionIdList.size(), 20);

		List<SessionId> sessionIdList2 = manager.findEntries("o=jans", SessionId.class, filter, 5);
		assertNotNull(sessionIdList2);
		assertEquals(sessionIdList2.size(), 5);

		List<SessionId> sessionIdList3 = manager.findEntries("o=jans", SessionId.class, filter, 25);
		assertNotNull(sessionIdList3);
		assertEquals(sessionIdList3.size(), 20);

		List<SessionId> sessionIdList4 = manager.findEntries("o=jans", SessionId.class, filter, null, null, 14, 7, 3);
		assertNotNull(sessionIdList4);
		assertEquals(sessionIdList4.size(), 6);

		List<SessionId> sessionIdList5 = manager.findEntries("o=jans", SessionId.class, filter, null, null, 20, 10, 5);
		assertNotNull(sessionIdList5);
		assertEquals(sessionIdList5.size(), 0);

		List<SessionId> sessionIdList6 = manager.findEntries("o=jans", SessionId.class, filter, null, null, 19, -1, 5);
		assertNotNull(sessionIdList6);
		assertEquals(sessionIdList6.size(), 1);
    }

    @Test(dependsOnMethods = "deleteSessionId", enabled = false)
    public void searchPagedSessionIdWithRanges() throws IOException {
		String outsideSid = UUID.randomUUID().toString();
		for (int i = 0; i < 20; i++) {
			SessionId sessionId = buildSessionId();
			sessionId.setOutsideSid(outsideSid);

			manager.persist(sessionId);
		}

		Filter filter = Filter.createEqualityFilter("sid", outsideSid);

		PagedResult<SessionId> sessionIdList = manager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 0, -1, -1);
		assertNotNull(sessionIdList);
		assertEquals(sessionIdList.getTotalEntriesCount(), 20);

		PagedResult<SessionId> sessionIdList2 = manager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 0, 5, -1);
		assertNotNull(sessionIdList2);
		assertEquals(sessionIdList2.getStart(), 0);
		assertEquals(sessionIdList2.getEntriesCount(), 5);
		assertEquals(sessionIdList2.getTotalEntriesCount(), 20);

		PagedResult<SessionId> sessionIdList4 = manager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 14, 7, 3);
		assertNotNull(sessionIdList4);
		assertEquals(sessionIdList4.getStart(), 14);
		assertEquals(sessionIdList4.getEntriesCount(), 6);
		assertEquals(sessionIdList4.getTotalEntriesCount(), 20);

		PagedResult<SessionId> sessionIdList5 = manager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 20, 10, 5);
		assertNotNull(sessionIdList5);
		assertEquals(sessionIdList5.getStart(), 20);
		assertEquals(sessionIdList5.getEntriesCount(), 0);
		assertEquals(sessionIdList5.getTotalEntriesCount(), 20);

		PagedResult<SessionId> sessionIdList6 = manager.findPagedEntries("o=jans", SessionId.class, filter, null, "sid", SortOrder.DESCENDING, 19, -1, 5);
		assertNotNull(sessionIdList6);
		assertEquals(sessionIdList6.getStart(), 19);
		assertEquals(sessionIdList6.getEntriesCount(), 1);
		assertEquals(sessionIdList6.getTotalEntriesCount(), 20);
    }

    @Test(dependsOnMethods = "deleteSessionId", enabled = false)
    public void deleteSessionIdByFilter() throws IOException {
		String outsideSid = UUID.randomUUID().toString();
		for (int i = 0; i < 20; i++) {
			SessionId sessionId = buildSessionId();
			sessionId.setOutsideSid(outsideSid);

			manager.persist(sessionId);
		}

		Filter filter = Filter.createEqualityFilter("sid", outsideSid);
		
		int removedCount = manager.remove("ou=sessions,o=jans", SessionId.class, filter, 14);
		assertEquals(removedCount, 14);

		int removedCount2 = manager.remove("ou=sessions,o=jans", SessionId.class, filter, 5);
		assertEquals(removedCount2, 5);

		int removedCount3 = manager.remove("ou=sessions,o=jans", SessionId.class, filter, 1);
		assertEquals(removedCount3, 1);
    }

    @Test(dependsOnMethods = "deleteSessionId", enabled = false)
    public void countSessionIdByFilter() throws IOException {
		String outsideSid = UUID.randomUUID().toString();
		for (int i = 0; i < 33; i++) {
			SessionId sessionId = buildSessionId();
			sessionId.setOutsideSid(outsideSid);

			manager.persist(sessionId);
		}

		Filter filter = Filter.createEqualityFilter("sid", outsideSid);

		int countEntries = manager.countEntries("ou=sessions,o=jans", SessionId.class, filter, null);
		assertEquals(countEntries, 33);
    }
    
    @Test(dependsOnMethods = "deleteSessionId", enabled = false)
    public void testBatchJob() {
		String outsideSid = UUID.randomUUID().toString();
		for (int i = 0; i < 200; i++) {
			SessionId sessionId = buildSessionId();
			sessionId.setOutsideSid(outsideSid);

			Pair<Date, Integer> expirarion = expirationDate(new Date());
	        sessionId.setExpirationDate(expirarion.getFirst());
	        sessionId.setTtl(expirarion.getSecond());

			manager.persist(sessionId);
		}

		totalProcessedCount = 0;

		ProcessBatchOperation<SessionId> sessionBatchOperation = new ProcessBatchOperation<SessionId>() {
    		int processedCount = 0;

    		@Override
            public void performAction(List<SessionId> objects) {
                for (SessionId simpleSession : objects) {
                    try {
                        Calendar calendar = Calendar.getInstance();
                        Date jansLastAccessTimeDate = simpleSession.getExpirationDate();
                        calendar.setTime(jansLastAccessTimeDate);
                        calendar.add(Calendar.SECOND, -1);
                        
                        simpleSession.setExpirationDate(calendar.getTime());

                        manager.merge(simpleSession);
                        processedCount++;
                    } catch (EntryPersistenceException ex) {
                    	System.err.println("Failed to update entry: " + ex.getMessage());
                    }
                }

                System.out.println("Total processed: " + processedCount);

                assertEquals(processedCount, 100);
                totalProcessedCount += processedCount;
            }
        };

        Filter filter1 = Filter.createANDFilter(Filter.createPresenceFilter("exp"), Filter.createEqualityFilter("sid", outsideSid));
        manager.findEntries("o=jans", SessionId.class, filter1, SearchScope.SUB, new String[] {"exp"},
        		sessionBatchOperation, 0, 500, 100);

        assertEquals(totalProcessedCount, 200);
    }

    private SessionId buildSessionId() {
        SessionId sessionId = new SessionId();
        sessionId.setId(UUID.randomUUID().toString());
        sessionId.setDn(String.format("jansId=%s,%s", sessionId.getId(), "ou=sessions,o=jans"));
        sessionId.setCreationDate(new Date());
        sessionId.setJwt("{}");
        sessionId.setIsJwt(true);

        return sessionId;
    }

    private Pair<Date, Integer> expirationDate(Date creationDate) {
        int expirationInSeconds = 120;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.SECOND, expirationInSeconds);
        return new Pair<>(calendar.getTime(), expirationInSeconds);
    }

    // MODIFY ACCORDING TO YOUR SERVER
    public static Properties loadProperties() throws IOException {
        Properties properties = new Properties();

        try (InputStream is = new FileInputStream("V://Development//jans//conf/jans-sql.properties")) {
//        try (InputStream is = ManualSpannerEntryManagerTest.class.getResourceAsStream("sql-backend.jans.io.properties")) {
            Properties props = new Properties();
            props.load(is);

            Iterator<?> keys = props.keySet().iterator();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                String value = (String) props.getProperty(key);
                
                if (!key.startsWith("sql")) {
                	key = "sql." + key;
                }
                properties.put(key,  value);
            }
        }

        properties.put("sql.auth.userName", "root");
        properties.put("sql.auth.userPassword", "Secret1!");

        return properties;
    }

    public SpannerEntryManager createSpannerEntryManager() throws IOException {
    	SpannerEntryManagerFactory sqlEntryManagerFactory = new SpannerEntryManagerFactory();
    	sqlEntryManagerFactory.create();

        SpannerEntryManager sqlEntryManager = sqlEntryManagerFactory.createEntryManager(loadProperties());
        System.out.println("Created SpannerEntryManager: " + sqlEntryManager);

        return sqlEntryManager;
    }
}
