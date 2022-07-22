/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.couchbase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;

import io.jans.orm.couchbase.impl.CouchbaseEntryManager;
import io.jans.orm.couchbase.model.GluuStatus;
import io.jans.orm.couchbase.model.SimpleUser;
import io.jans.orm.model.base.CustomAttribute;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.orm.search.filter.Filter;
import io.jans.orm.util.StringHelper;

/**
 * @author Yuriy Movchan Date: 06/16/2022
 */
public final class CouchbaseAddBulkUserSample {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(CouchbaseAddBulkUserSample.class);
    }

    private static AtomicLong successResult = new AtomicLong(0) ;
    private static AtomicLong errorResult = new AtomicLong(0) ;
    private static AtomicLong totalTime = new AtomicLong(0) ;
    private static AtomicLong activeCount = new AtomicLong(0) ;

    private CouchbaseAddBulkUserSample() {
    }

    public static void main(String[] args) throws InterruptedException {
        // Prepare sample connection details
        CouchbaseEntryManagerSample couchbaseEntryManagerSample = new CouchbaseEntryManagerSample();
        final CouchbaseEntryManager couchbaseEntryManager = couchbaseEntryManagerSample.createCouchbaseEntryManager();
        
        int countUsers = 1000000;
        int threadCount = 200;
        int threadIterationCount = countUsers / threadCount;

    	long totalStart = System.currentTimeMillis();
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount, daemonThreadFactory());
            for (int i = 0; i < threadCount; i++) {
            	activeCount.incrementAndGet();
                final int count = i;
				executorService.execute(new AddUserRunnable(i * threadIterationCount, (i + 1) * threadIterationCount - 1) {
		            @Override
                    public void run() {
                    	long start = System.currentTimeMillis();
                        for (int j = startRange; j <= endRange; j++) {
	                    	long userUid = j;
	                    	String name = String.format("user%06d", userUid);
	                    	String sn = String.format("lastname%06d", userUid);
	                    	String cn = String.format("%s %s", name, sn);
	                    	String mail = String.format("%s@gluu.org", name);

	                        String dn = String.format("inum=%06d,ou=people,o=jans", userUid);

	                        SimpleUser newUser = new SimpleUser();
	                        newUser.setDn(dn);
	                        newUser.setUserId(name);
	                        newUser.setUserPassword("topsecret" + j);
	                        newUser.setDisplayName(cn);
	                        newUser.setMail(mail);
	                        newUser.setStatus(GluuStatus.ACTIVE);

	                        CustomObjectAttribute customAttribute = new CustomObjectAttribute("givenName", name);
	                		newUser.getCustomAttributes().add(customAttribute);
	                		
	                		newUser.setAttributeValue("sn", sn);
	                		newUser.setAttributeValue("cn", cn);

	                		try {
		                        couchbaseEntryManager.persist(newUser);
	                        	successResult.incrementAndGet();
	                        } catch (Throwable e) {
	                        	errorResult.incrementAndGet();
	                            System.out.println("ERROR !!!, thread: " + count + ", uid: " + name + ", error:" + e.getMessage());
	                            e.printStackTrace();
	                        }
                        }
                        
                        long end = System.currentTimeMillis();
                        long duration = end - start; 
                        LOG.info("Thread " + count + " execution time: " + duration);
                        totalTime.addAndGet(duration);
                    	activeCount.decrementAndGet();
                    }
                });
            }

            while (activeCount.get() != 0) {
            	Thread.sleep(1000L);
            }
        } finally {
            couchbaseEntryManager.destroy();
        }
        long totalEnd = System.currentTimeMillis();
        long duration = totalEnd - totalStart; 

        LOG.info("Total execution time: " + duration + " after execution: " + (threadCount * threadIterationCount));

        System.out.println(String.format("successResult: '%d', errorResult: '%d'", successResult.get(), errorResult.get()));
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
    
    public static abstract class AddUserRunnable implements Runnable {
    	
    	protected int startRange;
		protected int endRange;

		public AddUserRunnable(int startRange, int endRange) {
    		this.startRange = startRange;
    		this.endRange = endRange;
    	}
	}

}
