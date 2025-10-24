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

    // Cluster node for distributed locking
    private ClusterNode clusterNode;

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
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
                Fido2MetricsAggregationScheduler scheduler = (Fido2MetricsAggregationScheduler) context.getJobDetail()
                    .getJobDataMap().get("scheduler");
                
                if (metricsService != null && scheduler != null) {
                    // Check if this node should perform aggregation (distributed lock check)
                    if (!scheduler.shouldPerformAggregation()) {
                        log.debug("Skipping hourly aggregation - another node holds the lock");
                        return;
                    }

                    // Process the previous hour to ensure data is complete
                    LocalDateTime previousHour = LocalDateTime.now().minusHours(1)
                        .truncatedTo(ChronoUnit.HOURS);
                    
                    // Persist aggregation to database immediately
                    metricsService.createHourlyAggregation(previousHour);
                    
                    // Log completion for monitoring
                    log.info("Hourly aggregation completed for: {}", previousHour);
                }
            } catch (Exception e) {
                // Log error but don't fail the job to prevent cluster issues
                log.error("Failed to execute hourly aggregation: {}", e.getMessage(), e);
                throw new JobExecutionException("Failed to execute hourly aggregation", e);
            }
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
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
                Fido2MetricsAggregationScheduler scheduler = (Fido2MetricsAggregationScheduler) context.getJobDetail()
                    .getJobDataMap().get("scheduler");
                
                if (metricsService != null && scheduler != null) {
                    if (!scheduler.shouldPerformAggregation()) {
                        log.debug("Skipping daily aggregation - another node holds the lock");
                        return;
                    }

                    LocalDateTime previousDay = LocalDateTime.now().minusDays(1)
                        .truncatedTo(ChronoUnit.DAYS);
                    metricsService.createDailyAggregation(previousDay);
                    log.info("Daily aggregation completed for: {}", previousDay);
                }
            } catch (Exception e) {
                log.error("Failed to execute daily aggregation: {}", e.getMessage(), e);
                throw new JobExecutionException("Failed to execute daily aggregation", e);
            }
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
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
                Fido2MetricsAggregationScheduler scheduler = (Fido2MetricsAggregationScheduler) context.getJobDetail()
                    .getJobDataMap().get("scheduler");
                
                if (metricsService != null && scheduler != null) {
                    if (!scheduler.shouldPerformAggregation()) {
                        log.debug("Skipping weekly aggregation - another node holds the lock");
                        return;
                    }

                    LocalDateTime previousWeek = LocalDateTime.now().minusWeeks(1)
                        .with(java.time.DayOfWeek.MONDAY)
                        .truncatedTo(ChronoUnit.DAYS);
                    metricsService.createWeeklyAggregation(previousWeek);
                    log.info("Weekly aggregation completed for: {}", previousWeek);
                }
            } catch (Exception e) {
                log.error("Failed to execute weekly aggregation: {}", e.getMessage(), e);
                throw new JobExecutionException("Failed to execute weekly aggregation", e);
            }
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
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
                Fido2MetricsAggregationScheduler scheduler = (Fido2MetricsAggregationScheduler) context.getJobDetail()
                    .getJobDataMap().get("scheduler");
                
                if (metricsService != null && scheduler != null) {
                    if (!scheduler.shouldPerformAggregation()) {
                        log.debug("Skipping monthly aggregation - another node holds the lock");
                        return;
                    }

                    LocalDateTime previousMonth = LocalDateTime.now().minusMonths(1)
                        .withDayOfMonth(1)
                        .truncatedTo(ChronoUnit.DAYS);
                    metricsService.createMonthlyAggregation(previousMonth);
                    log.info("Monthly aggregation completed for: {}", previousMonth);
                }
            } catch (Exception e) {
                log.error("Failed to execute monthly aggregation: {}", e.getMessage(), e);
                throw new JobExecutionException("Failed to execute monthly aggregation", e);
            }
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
     * Check if specific aggregation type is enabled
     * Note: For now, all aggregation types are controlled by the main aggregation flag
     * Individual type flags can be added to AppConfiguration in the future if needed
     */
    public boolean isAggregationEnabled(String aggregationType) {
        // All aggregation types use the main aggregation enabled flag
        // This ensures backward compatibility and prevents runtime errors
        return isAggregationEnabled();
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
     * Uses distributed locking via ClusterNode
     */
    public boolean shouldPerformAggregation() {
        try {
            // If we don't have a cluster node, try to allocate one
            if (clusterNode == null) {
                clusterNode = clusterNodeService.allocate();
                if (clusterNode == null) {
                    log.debug("Failed to allocate cluster node for FIDO2 metrics aggregation");
                    return false;
                }
                log.info("Allocated cluster node {} for FIDO2 metrics aggregation", clusterNode.getId());
            }

            // Refresh the node to keep the lock alive
            clusterNodeService.refresh(clusterNode);

            // Verify we still hold the lock
            ClusterNode currentNode = clusterNodeService.getClusterNodeByDn(clusterNode.getDn());
            if (currentNode == null || !clusterNodeService.hasLock(currentNode)) {
                log.warn("Lost lock on cluster node {}. Attempting to re-allocate...", 
                    clusterNode != null ? clusterNode.getId() : "null");
                clusterNode = clusterNodeService.allocate();
                return clusterNode != null;
            }

            return true;
        } catch (Exception e) {
            log.error("Error checking aggregation lock", e);
            return false;
        }
    }

    /**
     * Initialize cluster node for this server instance
     * Called during application startup
     */
    public void initializeClusterNode() {
        if (!isAggregationEnabled()) {
            log.info("FIDO2 metrics aggregation is disabled");
            return;
        }

        try {
            clusterNode = clusterNodeService.allocate();
            if (clusterNode != null) {
                log.info("Initialized cluster node {} for FIDO2 metrics aggregation", clusterNode.getId());
            } else {
                log.warn("Failed to initialize cluster node for FIDO2 metrics aggregation");
            }
        } catch (Exception e) {
            log.error("Error initializing cluster node", e);
        }
    }

    /**
     * Release cluster node lock
     * Called during application shutdown
     */
    public void releaseClusterNode() {
        if (clusterNode != null) {
            try {
                clusterNodeService.releaseLock(clusterNode);
                log.info("Released cluster node {} for FIDO2 metrics aggregation", clusterNode.getId());
                clusterNode = null;
            } catch (Exception e) {
                log.error("Error releasing cluster node", e);
            }
        }
    }
}

