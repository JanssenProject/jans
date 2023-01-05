/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.model.SimpleSessionState;

/**
 * @author Yuriy Movchan Date: 01/25/2016
 */
public final class CouchbaseConcurentSessionUpdateSample {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(CouchbaseConcurentSessionUpdateSample.class);
    }

    private CouchbaseConcurentSessionUpdateSample() {
    }

    public static void main(String[] args) throws InterruptedException {
        // Prepare sample connection details
        CouchbaseEntryManagerSample couchbaseEntryManagerSample = new CouchbaseEntryManagerSample();
        final CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerSample.createCouchbaseEntryManager();

        try {
            // Create Couchbase entry manager
            String sessionId = "xyzcyzxy-a41a-45ad-8a83-61485dbad561";
            final String sessionDn = "uniqueIdentifier=" + sessionId + ",ou=session,o=jans";
            final String userDn =
                    "inum=sample_concurrent_user_1,ou=people,o=jans";

            final SimpleSessionState simpleSessionState = new SimpleSessionState();
            simpleSessionState.setDn(sessionDn);
            simpleSessionState.setId(sessionId);
            simpleSessionState.setLastUsedAt(new Date());

            couchbaseEntryManager.persist(simpleSessionState);
            System.out.println("Persisted");

            int threadCount = 500;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount, daemonThreadFactory());
            for (int i = 0; i < threadCount; i++) {
                final int count = i;
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        final SimpleSessionState simpleSessionStateFromCouchbase = couchbaseEntryManager.find(SimpleSessionState.class, sessionDn);
                        String beforeUserDn = simpleSessionStateFromCouchbase.getUserDn();
                        String randomUserDn = count % 2 == 0 ? userDn : "";

                        try {
                            simpleSessionStateFromCouchbase.setUserDn(randomUserDn);
                            simpleSessionStateFromCouchbase.setLastUsedAt(new Date());
                            couchbaseEntryManager.merge(simpleSessionStateFromCouchbase);
                            System.out.println("Merged thread: " + count + ", userDn: " + randomUserDn + ", before userDn: " + beforeUserDn);
                        } catch (Throwable e) {
                            System.out.println("ERROR !!!, thread: " + count + ", userDn: " + randomUserDn + ", before userDn: " + beforeUserDn
                                    + ", error:" + e.getMessage());
                            // e.printStackTrace();
                        }
                    }
                });
            }

            Thread.sleep(5000L);
        } finally {
            couchbaseEntryManager.destroy();
        }
    }

    public static ThreadFactory daemonThreadFactory() {
        return new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
            }
        };
    }

}
