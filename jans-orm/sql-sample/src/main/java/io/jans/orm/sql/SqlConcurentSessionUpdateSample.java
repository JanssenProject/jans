/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;

import io.jans.orm.sql.impl.SqlEntryManager;
import io.jans.orm.sql.model.SimpleSessionState;
import io.jans.orm.sql.persistence.SqlEntryManagerSample;

/**
 * @author Yuriy Movchan Date: 01/15/2020
 */
public final class SqlConcurentSessionUpdateSample {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(SqlConcurentSessionUpdateSample.class);
    }

    private SqlConcurentSessionUpdateSample() {
    }

    public static void main(String[] args) throws InterruptedException {
        // Prepare sample connection details
        SqlEntryManagerSample sqlEntryManagerSample = new SqlEntryManagerSample();

        // Create SQL entry manager
        final SqlEntryManager sqlEntryManager = sqlEntryManagerSample.createSqlEntryManager();

        try {
            String sessionId = UUID.randomUUID().toString();
            final String sessionDn = "uniqueIdentifier=" + sessionId + ",ou=session,o=jans";
            final String userDn =
                    "inum=@!E8F2.853B.1E7B.ACE2!0001!39A4.C163!0000!A8F2.DE1E.D7FB,ou=people,o=jans";

            final SimpleSessionState simpleSessionState = new SimpleSessionState();
            simpleSessionState.setDn(sessionDn);
            simpleSessionState.setId(sessionId);
            simpleSessionState.setLastUsedAt(new Date());

            sqlEntryManager.persist(simpleSessionState);
            System.out.println("Persisted");

            int threadCount = 500;
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount, daemonThreadFactory());
            for (int i = 0; i < threadCount; i++) {
                final int count = i;
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        final SimpleSessionState simpleSessionStateFromSQL = sqlEntryManager.find(SimpleSessionState.class, sessionDn);
                        String beforeUserDn = simpleSessionStateFromSQL.getUserDn();
                        String randomUserDn = count % 2 == 0 ? userDn : "";

                        try {
                            simpleSessionStateFromSQL.setUserDn(randomUserDn);
                            simpleSessionStateFromSQL.setLastUsedAt(new Date());
                            sqlEntryManager.merge(simpleSessionStateFromSQL);
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
            sqlEntryManager.destroy();
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
