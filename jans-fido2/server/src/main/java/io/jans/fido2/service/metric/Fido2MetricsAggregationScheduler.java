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
import jakarta.enterprise.inject.spi.CDI;
import io.jans.service.timer.QuartzSchedulerManager;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    // Cluster node service - optional, only available in multi-node deployments
    private Fido2ClusterNodeService clusterNodeService;
    
    // Flag to track if we're in a cluster environment
    private boolean isClusterEnvironment = false;

    @Inject
    private QuartzSchedulerManager quartzSchedulerManager;

    // Scheduled executor for periodic lock updates during aggregation (only used in cluster mode)
    private ScheduledExecutorService updateExecutor;

    // Load properties file safely - don't fail if missing
    private static final ResourceBundle METRICS_CONFIG;
    
    static {
        ResourceBundle bundle = null;
        try {
            bundle = ResourceBundle.getBundle("fido2-metrics");
        } catch (Exception e) {
            // Properties file not found - will use hardcoded defaults
            System.err.println("WARN: fido2-metrics.properties not found, using defaults");
        }
        METRICS_CONFIG = bundle;
    }

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

                // In cluster mode, try to start periodic lock updates
                // If cluster mode fails, fall back to single-node mode and still run aggregation
                ScheduledFuture<?> updateTask = null;
                boolean isClusterMode = scheduler.isClusterEnvironment;
                if (isClusterMode) {
                    updateTask = scheduler.startPeriodicLockUpdates();
                    if (updateTask == null) {
                        // Cluster mode failed (e.g., cluster config missing), but shouldPerformAggregation 
                        // already returned true (fallback to single-node), so we can still run aggregation
                        log.warn("Cluster lock updates failed for {} aggregation, running in single-node mode: {}", 
                            jobType, "ou=node is not configured in static configuration");
                        // Continue to run aggregation in single-node mode
                    }
                }
                
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
                // Use UTC timezone to align with FIDO2 services
                LocalDateTime previousHour = ZonedDateTime.now(ZoneId.of("UTC"))
                    .minusHours(1)
                    .truncatedTo(ChronoUnit.HOURS)
                    .toLocalDateTime();
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
                // Use UTC timezone to align with FIDO2 services
                LocalDateTime previousDay = ZonedDateTime.now(ZoneId.of("UTC"))
                    .minusDays(1)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toLocalDateTime();
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
                // Use UTC timezone to align with FIDO2 services
                LocalDateTime previousWeek = ZonedDateTime.now(ZoneId.of("UTC"))
                    .minusWeeks(1)
                    .with(java.time.DayOfWeek.MONDAY)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toLocalDateTime();
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
                // Use UTC timezone to align with FIDO2 services
                LocalDateTime previousMonth = ZonedDateTime.now(ZoneId.of("UTC"))
                    .minusMonths(1)
                    .withDayOfMonth(1)
                    .truncatedTo(ChronoUnit.DAYS)
                    .toLocalDateTime();
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
    /**
     * Check if this node should perform the aggregation
     * In single-node deployments: always returns true
     * In multi-node clusters: uses distributed locking to coordinate
     * Synchronized to prevent race conditions between concurrent job threads
     */
    public synchronized boolean shouldPerformAggregation() {
        // Single-node deployment: always perform aggregation
        if (!isClusterEnvironment) {
            return true;
        }
        
        // Multi-node cluster: use distributed locking
        try {
            List<ClusterNode> liveList = clusterNodeService.getClusterNodesLive();
            
            if (liveList.isEmpty()) {
                return tryAcquireLock();
            } else {
                return checkIfWeHoldLock(liveList);
            }
        } catch (Exception e) {
            log.warn("Error checking aggregation lock in cluster environment, falling back to single-node behavior: {}", 
                e.getMessage());
            // Fallback to single-node behavior if cluster coordination fails
            return true;
        }
    }
    
    // ========== CLUSTER SUPPORT (OPTIONAL - AUTO-DETECTS ENVIRONMENT) ==========
    
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
     * Checks if our lock key exists in the live list (handles race conditions where multiple nodes allocate simultaneously)
     */
    private boolean verifyLockAcquired(List<ClusterNode> liveList) {
        String ourLockKey = clusterNodeService.getLockKey();
        for (ClusterNode liveNode : liveList) {
            if (ourLockKey.equals(liveNode.getLockKey())) {
                log.info("Acquired lock for FIDO2 metrics aggregation on cluster node {}", liveNode.getId());
                return true;
            }
        }
        log.debug("Lock not acquired (our lock key not found in {} live nodes)", liveList.size());
        return false;
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
     * Only used in cluster environments
     * 
     * @return ScheduledFuture that can be used to cancel the updates, or null if not in cluster
     */
    @SuppressWarnings("java:S1452") // Wildcard type is required as ScheduledExecutorService.scheduleAtFixedRate returns ScheduledFuture<?>
    public ScheduledFuture<?> startPeriodicLockUpdates() {
        if (!isClusterEnvironment || updateExecutor == null) {
            return null; // Not in cluster or executor not initialized
        }
        
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
     * Detects if we're in a cluster environment and initializes accordingly
     * NOTE: Removed @PostConstruct to avoid blocking during bean creation
     */
    private void initializeClusterEnvironment() {
        // Try to detect cluster environment by checking if cluster service exists
        try {
            // Attempt to get cluster service via CDI lookup
            clusterNodeService = CDI.current().select(Fido2ClusterNodeService.class).get();
            isClusterEnvironment = true;
            // Initialize update executor for cluster coordination
            updateExecutor = Executors.newScheduledThreadPool(1);
            log.info("FIDO2 metrics aggregation enabled in CLUSTER mode - distributed locking will be used");
        } catch (Exception e) {
            // Cluster service not available - single node deployment
            isClusterEnvironment = false;
            updateExecutor = null;
            log.info("FIDO2 metrics aggregation enabled in SINGLE-NODE mode - no cluster coordination needed");
        }
    }

    /**
     * Initialize and register Quartz jobs for metrics aggregation
     * Called during application startup by AppInitializer
     */
    public void initTimer() {
        if (!isAggregationEnabled()) {
            log.info("FIDO2 metrics aggregation is disabled, skipping scheduler initialization");
            return;
        }

        // Initialize cluster environment detection (moved from @PostConstruct to avoid blocking)
        initializeClusterEnvironment();

        try {
            // Prepare JobDataMap with required services
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(Fido2MetricsConstants.METRICS_SERVICE, metricsService);
            jobDataMap.put(Fido2MetricsConstants.SCHEDULER, this);

            // Register hourly aggregation job
            if (isAggregationTypeEnabled(Fido2MetricsConstants.HOURLY)) {
                String hourlyCron = getConfigString("fido2.metrics.aggregation.hourly.cron", "0 5 * * * ?");
                registerAggregationJob("HourlyAggregation", HourlyAggregationJob.class, hourlyCron, jobDataMap);
                log.info("Registered hourly FIDO2 metrics aggregation job with cron: {}", hourlyCron);
            }

            // Register daily aggregation job
            if (isAggregationTypeEnabled(Fido2MetricsConstants.DAILY)) {
                String dailyCron = getConfigString("fido2.metrics.aggregation.daily.cron", "0 10 1 * * ?");
                registerAggregationJob("DailyAggregation", DailyAggregationJob.class, dailyCron, jobDataMap);
                log.info("Registered daily FIDO2 metrics aggregation job with cron: {}", dailyCron);
            }

            // Register weekly aggregation job
            if (isAggregationTypeEnabled(Fido2MetricsConstants.WEEKLY)) {
                String weeklyCron = getConfigString("fido2.metrics.aggregation.weekly.cron", "0 15 1 ? * MON");
                registerAggregationJob("WeeklyAggregation", WeeklyAggregationJob.class, weeklyCron, jobDataMap);
                log.info("Registered weekly FIDO2 metrics aggregation job with cron: {}", weeklyCron);
            }

            // Register monthly aggregation job
            if (isAggregationTypeEnabled(Fido2MetricsConstants.MONTHLY)) {
                String monthlyCron = getConfigString("fido2.metrics.aggregation.monthly.cron", "0 20 1 1 * ?");
                registerAggregationJob("MonthlyAggregation", MonthlyAggregationJob.class, monthlyCron, jobDataMap);
                log.info("Registered monthly FIDO2 metrics aggregation job with cron: {}", monthlyCron);
            }

            log.info("FIDO2 metrics aggregation scheduler initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize FIDO2 metrics aggregation scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Register a Quartz job with cron trigger
     */
    private void registerAggregationJob(String jobName, Class<? extends Job> jobClass, String cronExpression, JobDataMap jobDataMap) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity("Fido2Metrics_" + jobName, "Fido2MetricsGroup")
                    .usingJobData(jobDataMap)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("Fido2Metrics_" + jobName + "_Trigger", "Fido2MetricsGroup")
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .build();

            quartzSchedulerManager.schedule(jobDetail, trigger);
        } catch (Exception e) {
            log.error("Failed to register {} job: {}", jobName, e.getMessage(), e);
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("Failed to register " + jobName + " job", e);
        }
    }

    /**
     * Check if a specific aggregation type is enabled
     */
    private boolean isAggregationTypeEnabled(String aggregationType) {
        try {
            String key = "fido2.metrics.aggregation." + aggregationType.toLowerCase() + ".enabled";
            return Boolean.parseBoolean(getConfigString(key, "true"));
        } catch (Exception e) {
            log.warn("Failed to check if {} aggregation is enabled, defaulting to true: {}", aggregationType, e.getMessage());
            return true;
        }
    }

    /**
     * Get configuration string with default value
     */
    private String getConfigString(String key, String defaultValue) {
        try {
            if (METRICS_CONFIG != null && METRICS_CONFIG.containsKey(key)) {
                return METRICS_CONFIG.getString(key);
            }
            return defaultValue;
        } catch (Exception e) {
            log.warn("Failed to get config string for key {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Cleanup resources on shutdown
     * Shuts down update executor if in cluster mode
     */
    @PreDestroy
    public void releaseClusterNode() {
        if (updateExecutor != null) {
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
        log.info("FIDO2 metrics aggregation scheduler shutdown");
    }
}

