/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;

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
     */
    public static class HourlyAggregationJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                Fido2MetricsService metricsService = (Fido2MetricsService) context.getJobDetail()
                    .getJobDataMap().get("metricsService");
                
                if (metricsService != null) {
                    LocalDateTime previousHour = LocalDateTime.now().minusHours(1)
                        .truncatedTo(ChronoUnit.HOURS);
                    metricsService.createHourlyAggregation(previousHour);
                }
            } catch (Exception e) {
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
                    .getJobDataMap().get("metricsService");
                
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
                    .getJobDataMap().get("metricsService");
                
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
                    .getJobDataMap().get("metricsService");
                
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
                    .getJobDataMap().get("metricsService");
                
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
}

