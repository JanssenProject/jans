package org.gluu.couchbase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusLogger;
import org.gluu.couchbase.model.SimpleUser;
import org.gluu.log.LoggingHelper;
import org.gluu.persist.couchbase.impl.CouchbaseEntryManager;
import org.gluu.search.filter.Filter;

/**
 * @author Yuriy Movchan Date: 09/18/2019
 */
public final class CouchbaseSampleUserSearchSample {

    private static final Logger LOG;

    static {
        StatusLogger.getLogger().setLevel(Level.OFF);
        LoggingHelper.configureConsoleAppender();
        LOG = Logger.getLogger(CouchbaseSampleUserSearchSample.class);
    }

    private static AtomicLong successResult = new AtomicLong(0) ;
    private static AtomicLong failedResult = new AtomicLong(0) ;
    private static AtomicLong errorResult = new AtomicLong(0) ;
    private static AtomicLong totalTime = new AtomicLong(0) ;
    private static AtomicLong activeCount = new AtomicLong(0) ;

    private CouchbaseSampleUserSearchSample() {
    }

    public static void main(String[] args) throws InterruptedException {
        // Prepare sample connection details
        CouchbaseSampleEntryManager couchbaseSampleEntryManager = new CouchbaseSampleEntryManager();
        final CouchbaseEntryManager couchbaseEntryManager = couchbaseSampleEntryManager.createCouchbaseEntryManager();
        
        int countUsers = 1000000;
        int threadCount = 200;
        int threadIterationCount = 10;

    	long totalStart = System.currentTimeMillis();
        try {
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount, daemonThreadFactory());
            for (int i = 0; i < threadCount; i++) {
            	activeCount.incrementAndGet();
                final int count = i;
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                    	long start = System.currentTimeMillis();
                        for (int j = 0; j < threadIterationCount; j++) {
	                    	long userUid = Math.round(Math.random() * countUsers);
	                    	String uid = String.format("user%06d", userUid);
	                        try {
		                        Filter filter = Filter.createEqualityFilter(Filter.createLowercaseFilter("uid"), uid);
//		                        Filter filter = Filter.createEqualityFilter("uid", uid);
		                        List<SimpleUser> foundUsers = couchbaseEntryManager.findEntries("ou=people,o=gluu", SimpleUser.class, filter);
		                        if (foundUsers.size() > 0) {
		                        	successResult.incrementAndGet();
		                        } else {
		                        	LOG.warn("Failed to find user: " + uid);
		                        	failedResult.incrementAndGet();
		                        }
	                        } catch (Throwable e) {
	                        	errorResult.incrementAndGet();
	                            System.out.println("ERROR !!!, thread: " + count + ", uid: " + uid + ", error:" + e.getMessage());
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

        System.out.println(String.format("successResult: '%d', failedResult: '%d', errorResult: '%d'", successResult.get(), failedResult.get(), errorResult.get()));
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
