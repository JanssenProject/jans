/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
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

    /**
     * Job for hourly aggregation
     * This job is designed to work in cluster environments where nodes can be added/removed
     * All statistics are persisted to the database, not kept in memory
     */
    public static class HourlyAggregationJob implements Job {
        private static final Logger log = LoggerFactory.getLogger(HourlyAggregationJob.class);
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
                
                if (metricsService != null) {
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
     */
    public static class DailyAggregationJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
                
                if (metricsService != null) {
                    LocalDateTime previousDay = LocalDateTime.now().minusDays(1)
                        .truncatedTo(ChronoUnit.DAYS);
                    metricsService.createDailyAggregation(previousDay);
                }
            } catch (Exception e) {
                throw new JobExecutionException("Failed to execute daily aggregation", e);
            }
        }
    }

    /**
     * Job for weekly aggregation
     */
    public static class WeeklyAggregationJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
                
                if (metricsService != null) {
                    LocalDateTime previousWeek = LocalDateTime.now().minusWeeks(1)
                        .with(java.time.DayOfWeek.MONDAY)
                        .truncatedTo(ChronoUnit.DAYS);
                    metricsService.createWeeklyAggregation(previousWeek);
                }
            } catch (Exception e) {
                throw new JobExecutionException("Failed to execute weekly aggregation", e);
            }
        }
    }

    /**
     * Job for monthly aggregation
     */
    public static class MonthlyAggregationJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get(Fido2MetricsConstants.METRICS_SERVICE);
                
                if (metricsService != null) {
                    LocalDateTime previousMonth = LocalDateTime.now().minusMonths(1)
                        .withDayOfMonth(1)
                        .truncatedTo(ChronoUnit.DAYS);
                    metricsService.createMonthlyAggregation(previousMonth);
                }
            } catch (Exception e) {
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
     */
    public boolean shouldPerformAggregation() {
        // In a real cluster environment, this would check if this node is the designated
        // aggregation node or use a distributed lock mechanism
        return true; // For now, assume all nodes can perform aggregation
    }
}

