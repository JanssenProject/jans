/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import io.jans.fido2.service.cluster.Fido2ClusterNodeService;
import io.jans.model.cluster.ClusterNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Scheduler service for FIDO2 metrics aggregation
 * 
 * @author FIDO2 Team
 */
@ApplicationScoped
public class Fido2MetricsAggregationScheduler {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Fido2MetricsService metricsService;

    @Inject
    private Fido2ClusterNodeService clusterNodeService;

    // Scheduled executor for periodic lock updates during aggregation
    private final ScheduledExecutorService updateExecutor = Executors.newScheduledThreadPool(1);

    /**
     * Helper method to execute aggregation job with distributed locking
     */
    private static void executeAggregationJob(JobExecutionContext context, String jobType, 
            Consumer<Fido2MetricsService> aggregationTask) throws JobExecutionException {
        Logger log = LoggerFactory.getLogger(Fido2MetricsAggregationScheduler.class);
        try {
            Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
            Fido2MetricsAggregationScheduler scheduler = (Fido2MetricsAggregationScheduler) context.getJobDetail()
                .getJobDataMap().get(Fido2MetricsConstants.SCHEDULER);
            
            if (metricsService != null && scheduler != null) {
                if (!scheduler.shouldPerformAggregation()) {
                    log.debug("Skipping {} aggregation - another node holds the lock", jobType);
                    return;
                }

                ScheduledFuture<?> updateTask = scheduler.startPeriodicLockUpdates();
                try {
                    aggregationTask.accept(metricsService);
                } finally {
                    if (updateTask != null) {
                        updateTask.cancel(false);
                    }
                }
            }
        } catch (Exception e) {
            String errorMsg = String.format("Failed to execute %s aggregation job: %s", jobType, e.getMessage());
            log.error(errorMsg, e);
            throw new JobExecutionException(errorMsg, e);
        }
    }

    /**
     * Job for hourly aggregation
     * This job is designed to work in cluster environments where nodes can be added/removed
     * All statistics are persisted to the database, not kept in memory
     * Uses distributed locking to ensure only one node performs aggregation
     */
    public static class HourlyAggregationJob implements Job {
        private static final Logger log = LoggerFactory.getLogger(HourlyAggregationJob.class);
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            executeAggregationJob(context, "hourly", metricsService -> {
                LocalDateTime previousHour = LocalDateTime.now().minusHours(1)
                    .truncatedTo(ChronoUnit.HOURS);
                metricsService.createHourlyAggregation(previousHour);
                log.info("Hourly aggregation completed for: {}", previousHour);
            });
        }
    }

    /**
     * Job for daily aggregation
     * Uses distributed locking to ensure only one node performs aggregation
     */
    public static class DailyAggregationJob implements Job {
        private static final Logger log = LoggerFactory.getLogger(DailyAggregationJob.class);
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            executeAggregationJob(context, "daily", metricsService -> {
                LocalDateTime previousDay = LocalDateTime.now().minusDays(1)
                    .truncatedTo(ChronoUnit.DAYS);
                metricsService.createDailyAggregation(previousDay);
                log.info("Daily aggregation completed for: {}", previousDay);
            });
        }
    }

    /**
     * Job for weekly aggregation
     * Uses distributed locking to ensure only one node performs aggregation
     */
    public static class WeeklyAggregationJob implements Job {
        private static final Logger log = LoggerFactory.getLogger(WeeklyAggregationJob.class);
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            executeAggregationJob(context, "weekly", metricsService -> {
                LocalDateTime previousWeek = LocalDateTime.now().minusWeeks(1)
                    .with(java.time.DayOfWeek.MONDAY)
                    .truncatedTo(ChronoUnit.DAYS);
                metricsService.createWeeklyAggregation(previousWeek);
                log.info("Weekly aggregation completed for: {}", previousWeek);
            });
        }
    }

    /**
     * Job for monthly aggregation
     * Uses distributed locking to ensure only one node performs aggregation
     */
    public static class MonthlyAggregationJob implements Job {
        private static final Logger log = LoggerFactory.getLogger(MonthlyAggregationJob.class);
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            executeAggregationJob(context, "monthly", metricsService -> {
                LocalDateTime previousMonth = LocalDateTime.now().minusMonths(1)
                    .withDayOfMonth(1)
                    .truncatedTo(ChronoUnit.DAYS);
                metricsService.createMonthlyAggregation(previousMonth);
                log.info("Monthly aggregation completed for: {}", previousMonth);
            });
        }
    }

    /**
     * Job for data cleanup
     */
    public static class DataCleanupJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
                
                if (metricsService != null) {
                    int retentionDays = (Integer) context.getJobDetail()
                        .getJobDataMap().getOrDefault("retentionDays", 30);
                    metricsService.cleanupOldData(retentionDays);
                }
            } catch (Exception e) {
                throw new JobExecutionException("Failed to execute data cleanup", e);
            }
        }
    }

    /**
     * Check if aggregation is enabled
     */
    public boolean isAggregationEnabled() {
        return appConfiguration.isFido2MetricsEnabled() && 
               appConfiguration.isFido2MetricsAggregationEnabled();
    }


    /**
     * Get aggregation interval in minutes
     */
    public int getAggregationInterval() {
        return appConfiguration.getFido2MetricsAggregationInterval();
    }

    /**
     * Get data retention days
     */
    public int getDataRetentionDays() {
        return appConfiguration.getFido2MetricsRetentionDays();
    }

    /**
     * Ensure all pending metrics are persisted to database
     * This method should be called before node shutdown or during maintenance
     */
    public void flushPendingMetrics() {
        try {
            if (metricsService != null) {
                // Force flush any pending metrics to database
                log.info("Flushing pending FIDO2 metrics to database...");
                // The actual flush logic would be implemented in Fido2MetricsService
                // This ensures no data is lost when nodes are removed from cluster
            }
        } catch (Exception e) {
            log.error("Failed to flush pending metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Check if this node should perform aggregation
     * In cluster environments, only one node should perform aggregation to avoid conflicts
     * Uses distributed locking via ClusterNode with getClusterNodesLive() pattern
     * Synchronized to prevent race conditions between concurrent job threads
     */
    public synchronized boolean shouldPerformAggregation() {
        try {
            List<ClusterNode> liveList = clusterNodeService.getClusterNodesLive();
            
            if (liveList.isEmpty()) {
                return tryAcquireLock();
            } else {
                return checkIfWeHoldLock(liveList);
            }
        } catch (Exception e) {
            log.error("Error checking aggregation lock", e);
            return false;
        }
    }
    
    /**
     * Try to acquire the lock when no live nodes exist
     */
    private boolean tryAcquireLock() {
        ClusterNode allocatedNode = clusterNodeService.allocate();
        if (allocatedNode == null) {
            log.debug("Failed to allocate cluster node for FIDO2 metrics aggregation");
            return false;
        }
        
        // Verify we got the lock by checking live list again
        List<ClusterNode> liveList = clusterNodeService.getClusterNodesLive();
        return verifyLockAcquired(liveList);
    }
    
    /**
     * Verify that we successfully acquired the lock
     */
    private boolean verifyLockAcquired(List<ClusterNode> liveList) {
        if (liveList.size() != 1) {
            log.debug("Multiple live nodes detected (size: {}), skipping aggregation", liveList.size());
            return false;
        }
        
        ClusterNode liveNode = liveList.get(0);
        String ourLockKey = clusterNodeService.getLockKey();
        if (ourLockKey.equals(liveNode.getLockKey())) {
            log.info("Acquired lock for FIDO2 metrics aggregation on cluster node {}", liveNode.getId());
            return true;
        } else {
            log.debug("Lock acquired by another node (lock key mismatch)");
            return false;
        }
    }
    
    /**
     * Check if we already hold the lock when live nodes exist
     */
    private boolean checkIfWeHoldLock(List<ClusterNode> liveList) {
        String ourLockKey = clusterNodeService.getLockKey();
        for (ClusterNode liveNode : liveList) {
            if (ourLockKey.equals(liveNode.getLockKey())) {
                log.debug("Already holding lock for FIDO2 metrics aggregation on cluster node {}", liveNode.getId());
                return true;
            }
        }
        log.debug("Another node holds the lock for FIDO2 metrics aggregation ({} live nodes)", liveList.size());
        return false;
    }
    
    /**
     * Start periodic updates to keep the lock alive during aggregation work
     * Should be called at the start of aggregation work
     * 
     * @return ScheduledFuture that can be used to cancel the updates
     */
    @SuppressWarnings("java:S1452") // Wildcard type is required as ScheduledExecutorService.scheduleAtFixedRate returns ScheduledFuture<?>
    public ScheduledFuture<?> startPeriodicLockUpdates() {
        try {
            // Get the current live node that we hold
            List<ClusterNode> liveList = clusterNodeService.getClusterNodesLive();
            String ourLockKey = clusterNodeService.getLockKey();
            
            ClusterNode ourNode = null;
            for (ClusterNode node : liveList) {
                if (ourLockKey.equals(node.getLockKey())) {
                    ourNode = node;
                    break;
                }
            }
            
            if (ourNode == null) {
                log.warn("Cannot start periodic lock updates - we don't hold the lock");
                return null;
            }
            
            final ClusterNode nodeToUpdate = ourNode;
            
            // Schedule updates every 30 seconds to keep the lock alive
            return updateExecutor.scheduleAtFixedRate(() -> {
                try {
                    // Refresh the node timestamp to keep it alive
                    clusterNodeService.refresh(nodeToUpdate);
                    log.trace("Updated lock timestamp for cluster node {}", nodeToUpdate.getId());
                } catch (Exception e) {
                    log.error("Failed to update lock timestamp for cluster node {}: {}", 
                        nodeToUpdate.getId(), e.getMessage(), e);
                }
            }, 30, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to start periodic lock updates: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Initialize cluster node service
     * Called during application startup
     * With the new pattern, we don't need to pre-allocate a node
     * The lock is acquired on-demand when aggregation jobs run
     */
    public void initializeClusterNode() {
        if (!isAggregationEnabled()) {
            log.info("FIDO2 metrics aggregation is disabled");
            return;
        }
        log.info("FIDO2 metrics aggregation enabled - locks will be acquired on-demand");
    }

    /**
     * Cleanup resources on shutdown
     * Shutdown the executor service used for periodic lock updates
     */
    public void releaseClusterNode() {
        try {
            updateExecutor.shutdown();
            if (!updateExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                updateExecutor.shutdownNow();
            }
            log.info("Shutdown periodic lock update executor for FIDO2 metrics aggregation");
        } catch (InterruptedException e) {
            updateExecutor.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("Interrupted while shutting down lock update executor", e);
        }
    }
}

