/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.metric;

import io.jans.fido2.model.conf.AppConfiguration;
import io.jans.fido2.model.metric.Fido2MetricsAggregation;
import io.jans.fido2.model.metric.Fido2MetricsConstants;
import io.jans.fido2.model.metric.Fido2UserMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Service for FIDO2 analytics and reporting
 * 
 * @author FIDO2 Team
 */
@ApplicationScoped
@Named("fido2AnalyticsService")
public class Fido2AnalyticsService {

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Fido2MetricsService metricsService;

    @Inject
    private Fido2UserMetricsService userMetricsService;

    // Load configuration from properties file
    private static final ResourceBundle METRICS_CONFIG = ResourceBundle.getBundle("fido2-metrics");
    
    // Constants loaded from properties file
    private static final double STRONG_ADOPTION_RATE_THRESHOLD = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.high.fallback.rate.threshold"));
    private static final double LOW_ADOPTION_RATE_THRESHOLD = Double.parseDouble(METRICS_CONFIG.getString("fido2.user.low.fallback.rate.threshold"));
    private static final double SLOW_REGISTRATION_DURATION_MS = Double.parseDouble(METRICS_CONFIG.getString("fido2.performance.very.slow.operation.threshold.ms"));
    private static final double FAST_REGISTRATION_DURATION_MS = Double.parseDouble(METRICS_CONFIG.getString("fido2.performance.slow.operation.threshold.ms"));
    private static final double LOW_SUCCESS_RATE_THRESHOLD = 0.8;
    private static final double HIGH_SUCCESS_RATE_THRESHOLD = 0.95;
    private static final double LOW_ADOPTION_RATE_RECOMMENDATION_THRESHOLD = 0.2;
    private static final double SLOW_REGISTRATION_RECOMMENDATION_MS = 8000;
    private static final double LOW_SUCCESS_RATE_RECOMMENDATION_THRESHOLD = 0.85;
    private static final double HIGH_MOBILE_USAGE_THRESHOLD = 0.7;
    private static final double GOOD_REGISTRATION_DURATION_MS = 5000;
    private static final double ACCEPTABLE_REGISTRATION_DURATION_MS = 8000;

    /**
     * Generate comprehensive FIDO2 metrics report
     */
    public Map<String, Object> generateComprehensiveReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();
        
        try {
            // Executive Summary
            report.put("executiveSummary", generateExecutiveSummary(startTime, endTime));
            
            // User Adoption Metrics
            report.put("userAdoption", generateUserAdoptionReport(startTime, endTime));
            
            // Performance Metrics
            report.put("performance", generatePerformanceReport(startTime, endTime));
            
            // Device Analytics
            report.put("deviceAnalytics", generateDeviceAnalyticsReport(startTime, endTime));
            
            // Error Analysis
            report.put("errorAnalysis", generateErrorAnalysisReport(startTime, endTime));
            
            // Trends Analysis
            report.put("trends", generateTrendsReport(startTime, endTime));
            
            // Recommendations
            report.put("recommendations", generateRecommendations(startTime, endTime));
            
            // Report metadata
            report.put("reportMetadata", Map.of(
                "generatedAt", LocalDateTime.now(),
                "startTime", startTime,
                "endTime", endTime,
                "reportType", "COMPREHENSIVE"
            ));
            
        } catch (Exception e) {
            log.error("Failed to generate comprehensive report: {}", e.getMessage(), e);
            report.put("error", "Failed to generate report: " + e.getMessage());
        }
        
        return report;
    }

    /**
     * Generate executive summary
     */
    public Map<String, Object> generateExecutiveSummary(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> summary = new HashMap<>();
        
        // Get basic metrics
        Map<String, Object> userAdoption = metricsService.getUserAdoptionMetrics(startTime, endTime);
        Map<String, Object> performance = metricsService.getPerformanceMetrics(startTime, endTime);
        Map<String, Object> errorAnalysis = metricsService.getErrorAnalysis(startTime, endTime);
        
        // Key metrics
        summary.put(Fido2MetricsConstants.TOTAL_UNIQUE_USERS, userAdoption.get(Fido2MetricsConstants.TOTAL_UNIQUE_USERS));
        summary.put(Fido2MetricsConstants.NEW_USERS, userAdoption.get(Fido2MetricsConstants.NEW_USERS));
        summary.put(Fido2MetricsConstants.ADOPTION_RATE, userAdoption.get(Fido2MetricsConstants.ADOPTION_RATE));
        
        // Performance highlights
        summary.put("avgRegistrationDuration", performance.get(Fido2MetricsConstants.REGISTRATION_AVG_DURATION));
        summary.put("avgAuthenticationDuration", performance.get(Fido2MetricsConstants.AUTHENTICATION_AVG_DURATION));
        
        // Success rates
        summary.put("overallSuccessRate", errorAnalysis.get(Fido2MetricsConstants.SUCCESS_RATE));
        summary.put("failureRate", errorAnalysis.get(Fido2MetricsConstants.FAILURE_RATE));
        
        // Top insights
        List<String> insights = new ArrayList<>();
        
        // User adoption insights
        Long totalUsers = (Long) userAdoption.get(Fido2MetricsConstants.TOTAL_UNIQUE_USERS);
        Long newUsers = (Long) userAdoption.get(Fido2MetricsConstants.NEW_USERS);
        if (totalUsers != null && newUsers != null && totalUsers > 0) {
            double adoptionRate = (double) newUsers / totalUsers;
            if (adoptionRate > STRONG_ADOPTION_RATE_THRESHOLD) {
                insights.add("Strong user adoption with " + String.format("%.1f%%", adoptionRate * 100) + " new users");
            } else if (adoptionRate < LOW_ADOPTION_RATE_THRESHOLD) {
                insights.add("Low user adoption - consider user education and onboarding improvements");
            }
        }
        
        // Performance insights
        Double avgRegDuration = (Double) performance.get(Fido2MetricsConstants.REGISTRATION_AVG_DURATION);
        if (avgRegDuration != null) {
            if (avgRegDuration > SLOW_REGISTRATION_DURATION_MS) {
                insights.add("Registration process is slow - consider optimization");
            } else if (avgRegDuration < FAST_REGISTRATION_DURATION_MS) {
                insights.add("Excellent registration performance");
            }
        }
        
        // Error insights
        Double successRate = (Double) errorAnalysis.get("successRate");
        if (successRate != null) {
            if (successRate < LOW_SUCCESS_RATE_THRESHOLD) {
                insights.add("Low success rate - investigate common failure points");
            } else if (successRate > HIGH_SUCCESS_RATE_THRESHOLD) {
                insights.add("Excellent success rate - system performing well");
            }
        }
        
        summary.put("keyInsights", insights);
        
        return summary;
    }

    /**
     * Generate user adoption report
     */
    public Map<String, Object> generateUserAdoptionReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();
        
        // Basic adoption metrics
        Map<String, Object> adoptionMetrics = metricsService.getUserAdoptionMetrics(startTime, endTime);
        report.putAll(adoptionMetrics);
        
        // User analytics
        Map<String, Object> userAnalytics = userMetricsService.getUserAdoptionAnalytics();
        report.putAll(userAnalytics);
        
        // Adoption trends
        List<Fido2MetricsAggregation> dailyAggregations = metricsService.getAggregations(Fido2MetricsConstants.DAILY, startTime, endTime);
        Map<String, Object> adoptionTrends = calculateAdoptionTrends(dailyAggregations);
        report.put("adoptionTrends", adoptionTrends);
        
        // User segments
        List<Fido2UserMetrics> allUsers = userMetricsService.getActiveUsers();
        Map<String, Object> userSegments = analyzeUserSegments(allUsers);
        report.put("userSegments", userSegments);
        
        return report;
    }

    /**
     * Generate performance report
     */
    public Map<String, Object> generatePerformanceReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();
        
        // Basic performance metrics
        Map<String, Object> performanceMetrics = metricsService.getPerformanceMetrics(startTime, endTime);
        report.putAll(performanceMetrics);
        
        // Performance trends
        List<Fido2MetricsAggregation> dailyAggregations = metricsService.getAggregations(Fido2MetricsConstants.DAILY, startTime, endTime);
        Map<String, Object> performanceTrends = calculatePerformanceTrends(dailyAggregations);
        report.put("performanceTrends", performanceTrends);
        
        // Performance benchmarks
        Map<String, Object> benchmarks = calculatePerformanceBenchmarks(performanceMetrics);
        report.put("benchmarks", benchmarks);
        
        return report;
    }

    /**
     * Generate device analytics report
     */
    public Map<String, Object> generateDeviceAnalyticsReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();
        
        // Basic device analytics
        Map<String, Object> deviceAnalytics = metricsService.getDeviceAnalytics(startTime, endTime);
        report.putAll(deviceAnalytics);
        
        // Device trends
        List<Fido2MetricsAggregation> dailyAggregations = metricsService.getAggregations(Fido2MetricsConstants.DAILY, startTime, endTime);
        Map<String, Object> deviceTrends = calculateDeviceTrends(dailyAggregations);
        report.put("deviceTrends", deviceTrends);
        
        // Device performance comparison
        Map<String, Object> devicePerformance = compareDevicePerformance();
        report.put("devicePerformance", devicePerformance);
        
        return report;
    }

    /**
     * Generate error analysis report
     */
    public Map<String, Object> generateErrorAnalysisReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();
        
        // Basic error analysis
        Map<String, Object> errorAnalysis = metricsService.getErrorAnalysis(startTime, endTime);
        report.putAll(errorAnalysis);
        
        // Error trends
        List<Fido2MetricsAggregation> dailyAggregations = metricsService.getAggregations(Fido2MetricsConstants.DAILY, startTime, endTime);
        Map<String, Object> errorTrends = calculateErrorTrends(dailyAggregations);
        report.put("errorTrends", errorTrends);
        
        // Error impact analysis
        Map<String, Object> errorImpact = analyzeErrorImpact();
        report.put("errorImpact", errorImpact);
        
        return report;
    }

    /**
     * Generate trends report
     */
    public Map<String, Object> generateTrendsReport(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> report = new HashMap<>();
        
        List<Fido2MetricsAggregation> dailyAggregations = metricsService.getAggregations(Fido2MetricsConstants.DAILY, startTime, endTime);
        
        // Usage trends
        Map<String, Object> usageTrends = calculateUsageTrends(dailyAggregations);
        report.put("usageTrends", usageTrends);
        
        // Growth trends
        Map<String, Object> growthTrends = calculateGrowthTrends(dailyAggregations);
        report.put("growthTrends", growthTrends);
        
        // Seasonal patterns
        Map<String, Object> seasonalPatterns = analyzeSeasonalPatterns(dailyAggregations);
        report.put("seasonalPatterns", seasonalPatterns);
        
        return report;
    }

    /**
     * Generate recommendations
     */
    public Map<String, Object> generateRecommendations(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> recommendations = new HashMap<>();
        List<Map<String, Object>> recommendationList = new ArrayList<>();
        
        try {
            // Get all metrics for analysis
            Map<String, Object> userAdoption = metricsService.getUserAdoptionMetrics(startTime, endTime);
            Map<String, Object> performance = metricsService.getPerformanceMetrics(startTime, endTime);
            Map<String, Object> errorAnalysis = metricsService.getErrorAnalysis(startTime, endTime);
            Map<String, Object> deviceAnalytics = metricsService.getDeviceAnalytics(startTime, endTime);
            
            // User adoption recommendations
            Double adoptionRate = (Double) userAdoption.get("adoptionRate");
            if (adoptionRate != null && adoptionRate < LOW_ADOPTION_RATE_RECOMMENDATION_THRESHOLD) {
                recommendationList.add(Map.of(
                    Fido2MetricsConstants.CATEGORY, "USER_ADOPTION",
                    Fido2MetricsConstants.PRIORITY, "HIGH",
                    Fido2MetricsConstants.TITLE, "Improve User Adoption",
                    Fido2MetricsConstants.DESCRIPTION, "Low adoption rate detected. Consider improving user education and onboarding process.",
                    Fido2MetricsConstants.ACTIONS, Arrays.asList(
                        "Implement user tutorials",
                        "Add progress indicators",
                        "Provide fallback options",
                        "Send adoption reminders"
                    )
                ));
            }
            
            // Performance recommendations
            Double avgRegDuration = (Double) performance.get(Fido2MetricsConstants.REGISTRATION_AVG_DURATION);
            if (avgRegDuration != null && avgRegDuration > SLOW_REGISTRATION_RECOMMENDATION_MS) {
                recommendationList.add(Map.of(
                    Fido2MetricsConstants.CATEGORY, "PERFORMANCE",
                    Fido2MetricsConstants.PRIORITY, "MEDIUM",
                    Fido2MetricsConstants.TITLE, "Optimize Registration Performance",
                    Fido2MetricsConstants.DESCRIPTION, "Registration process is slower than optimal. Consider performance improvements.",
                    Fido2MetricsConstants.ACTIONS, Arrays.asList(
                        "Optimize database queries",
                        "Implement caching",
                        "Reduce network round trips",
                        "Profile performance bottlenecks"
                    )
                ));
            }
            
            // Error recommendations
            Double successRate = (Double) errorAnalysis.get("successRate");
            if (successRate != null && successRate < LOW_SUCCESS_RATE_RECOMMENDATION_THRESHOLD) {
                recommendationList.add(Map.of(
                    Fido2MetricsConstants.CATEGORY, "RELIABILITY",
                    Fido2MetricsConstants.PRIORITY, "HIGH",
                    Fido2MetricsConstants.TITLE, "Improve Success Rate",
                    Fido2MetricsConstants.DESCRIPTION, "Success rate is below acceptable threshold. Investigate and fix common errors.",
                    Fido2MetricsConstants.ACTIONS, Arrays.asList(
                        "Analyze error patterns",
                        "Improve error handling",
                        "Add retry mechanisms",
                        "Enhance user feedback"
                    )
                ));
            }
            
            // Device recommendations
            @SuppressWarnings("unchecked")
            Map<String, Long> deviceTypes = (Map<String, Long>) deviceAnalytics.get("deviceTypes");
            if (deviceTypes != null && deviceTypes.containsKey("MOBILE") && deviceTypes.get("MOBILE") > deviceTypes.values().stream().mapToLong(Long::longValue).sum() * HIGH_MOBILE_USAGE_THRESHOLD) {
                recommendationList.add(Map.of(
                    Fido2MetricsConstants.CATEGORY, "UX_OPTIMIZATION",
                    Fido2MetricsConstants.PRIORITY, "MEDIUM",
                    Fido2MetricsConstants.TITLE, "Optimize for Mobile",
                    Fido2MetricsConstants.DESCRIPTION, "High mobile usage detected. Ensure mobile experience is optimized.",
                    Fido2MetricsConstants.ACTIONS, Arrays.asList(
                        "Test mobile compatibility",
                        "Optimize touch interactions",
                        "Improve mobile UI",
                        "Add mobile-specific features"
                    )
                ));
            }
            
        } catch (Exception e) {
            log.error("Failed to generate recommendations: {}", e.getMessage(), e);
            recommendationList.add(Map.of(
                Fido2MetricsConstants.CATEGORY, "SYSTEM",
                Fido2MetricsConstants.PRIORITY, "LOW",
                Fido2MetricsConstants.TITLE, "System Error",
                Fido2MetricsConstants.DESCRIPTION, "Unable to generate recommendations due to system error.",
                Fido2MetricsConstants.ACTIONS, Arrays.asList("Check system logs", "Verify data availability")
            ));
        }
        
        recommendations.put("recommendations", recommendationList);
        recommendations.put("totalRecommendations", recommendationList.size());
        recommendations.put("highPriorityCount", recommendationList.stream()
            .mapToInt(r -> "HIGH".equals(r.get(Fido2MetricsConstants.PRIORITY)) ? 1 : 0)
            .sum());
        
        return recommendations;
    }

    // Helper methods for trend calculations
    private Map<String, Object> calculateAdoptionTrends(List<Fido2MetricsAggregation> aggregations) {
        Map<String, Object> trends = new HashMap<>();
        
        if (aggregations.size() < 2) {
            return trends;
        }
        
        // Sort by time
        aggregations.sort(Comparator.comparing(Fido2MetricsAggregation::getStartTime));
        
        // Calculate growth rates
        List<Double> userGrowthRates = new ArrayList<>();
        for (int i = 1; i < aggregations.size(); i++) {
            Long currentUsers = aggregations.get(i).getUniqueUsers();
            Long previousUsers = aggregations.get(i - 1).getUniqueUsers();
            
            if (currentUsers != null && previousUsers != null && previousUsers > 0) {
                double growthRate = (double) (currentUsers - previousUsers) / previousUsers;
                userGrowthRates.add(growthRate);
            }
        }
        
        if (!userGrowthRates.isEmpty()) {
            double avgGrowthRate = userGrowthRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            trends.put("averageUserGrowthRate", avgGrowthRate);
            String trendDirection = getTrendDirection(avgGrowthRate);
            trends.put("userGrowthTrend", trendDirection);
        }
        
        return trends;
    }

    private Map<String, Object> calculatePerformanceTrends(List<Fido2MetricsAggregation> aggregations) {
        Map<String, Object> trends = new HashMap<>();
        
        if (aggregations.size() < 2) {
            return trends;
        }
        
        // Calculate performance trends
        List<Double> regDurationTrends = new ArrayList<>();
        List<Double> authDurationTrends = new ArrayList<>();
        
        for (Fido2MetricsAggregation agg : aggregations) {
            if (agg.getRegistrationAvgDuration() != null) {
                regDurationTrends.add(agg.getRegistrationAvgDuration());
            }
            if (agg.getAuthenticationAvgDuration() != null) {
                authDurationTrends.add(agg.getAuthenticationAvgDuration());
            }
        }
        
        if (!regDurationTrends.isEmpty()) {
            double avgRegDuration = regDurationTrends.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            trends.put("averageRegistrationDuration", avgRegDuration);
        }
        
        if (!authDurationTrends.isEmpty()) {
            double avgAuthDuration = authDurationTrends.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            trends.put("averageAuthenticationDuration", avgAuthDuration);
        }
        
        return trends;
    }

    private Map<String, Object> calculateDeviceTrends(List<Fido2MetricsAggregation> aggregations) {
        Map<String, Object> trends = new HashMap<>();
        
        if (aggregations == null || aggregations.size() < 2) {
            return trends;
        }
        
        // Calculate device type trends over time
        for (Fido2MetricsAggregation agg : aggregations) {
            if (agg.getDeviceTypes() != null) {
                // Parse device type usage and track trends
                // This is a simplified implementation
                trends.put("deviceTrendsAvailable", true);
            }
        }
        
        return trends;
    }

    private Map<String, Object> calculateErrorTrends(List<Fido2MetricsAggregation> aggregations) {
        Map<String, Object> trends = new HashMap<>();
        
        if (aggregations == null || aggregations.size() < 2) {
            return trends;
        }
        
        // Calculate error rate trends
        List<Double> errorRates = new ArrayList<>();
        for (Fido2MetricsAggregation agg : aggregations) {
            long totalAttempts = getTotalAttempts(agg);
            long totalSuccesses = getTotalSuccesses(agg);
            
            if (totalAttempts > 0) {
                double errorRate = 1.0 - (double) totalSuccesses / totalAttempts;
                errorRates.add(errorRate);
            }
        }
        
        if (!errorRates.isEmpty()) {
            double avgErrorRate = errorRates.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            trends.put("averageErrorRate", avgErrorRate);
            trends.put("errorTrend", avgErrorRate > 0.1 ? Fido2MetricsConstants.INCREASING : Fido2MetricsConstants.STABLE);
        }
        
        return trends;
    }

    private Map<String, Object> calculateUsageTrends(List<Fido2MetricsAggregation> aggregations) {
        Map<String, Object> trends = new HashMap<>();
        
        if (aggregations == null || aggregations.size() < 2) {
            return trends;
        }
        
        // Calculate usage growth trends
        List<Long> totalAttempts = aggregations.stream()
            .map(this::getTotalAttempts)
            .collect(Collectors.toList());
        
        if (totalAttempts.size() >= 2) {
            long firstWeek = totalAttempts.get(0);
            long lastWeek = totalAttempts.get(totalAttempts.size() - 1);
            
            if (firstWeek > 0) {
                double growthRate = (double) (lastWeek - firstWeek) / firstWeek;
                trends.put("usageGrowthRate", growthRate);
                trends.put("usageTrend", getTrendDirection(growthRate));
            }
        }
        
        return trends;
    }

    private Map<String, Object> calculateGrowthTrends(List<Fido2MetricsAggregation> aggregations) {
        Map<String, Object> trends = new HashMap<>();
        
        if (aggregations == null || aggregations.size() < 2) {
            return trends;
        }
        
        // Calculate user growth trends
        List<Long> uniqueUsers = aggregations.stream()
            .map(Fido2MetricsAggregation::getUniqueUsers)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (uniqueUsers.size() >= 2) {
            long firstWeek = uniqueUsers.get(0);
            long lastWeek = uniqueUsers.get(uniqueUsers.size() - 1);
            
            if (firstWeek > 0) {
                double userGrowthRate = (double) (lastWeek - firstWeek) / firstWeek;
                trends.put("userGrowthRate", userGrowthRate);
                trends.put("userGrowthTrend", getTrendDirection(userGrowthRate));
            }
        }
        
        return trends;
    }

    private Map<String, Object> analyzeSeasonalPatterns(List<Fido2MetricsAggregation> aggregations) {
        Map<String, Object> patterns = new HashMap<>();
        
        if (aggregations == null || aggregations.size() < 7) {
            return patterns;
        }
        
        // Analyze day-of-week patterns
        Map<String, Long> dayOfWeekUsage = new HashMap<>();
        for (Fido2MetricsAggregation agg : aggregations) {
            String dayOfWeek = agg.getStartTime().getDayOfWeek().name();
            long totalAttempts = getTotalAttempts(agg);
            dayOfWeekUsage.merge(dayOfWeek, totalAttempts, Long::sum);
        }
        
        patterns.put("dayOfWeekPatterns", dayOfWeekUsage);
        
        // Find peak usage day
        String peakDay = dayOfWeekUsage.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("UNKNOWN");
        patterns.put("peakUsageDay", peakDay);
        
        return patterns;
    }

    private Map<String, Object> analyzeUserSegments(List<Fido2UserMetrics> users) {
        Map<String, Object> segments = new HashMap<>();
        
        // Segment by engagement level
        Map<String, Long> engagementSegments = users.stream()
            .filter(u -> u.getEngagementLevel() != null)
            .collect(Collectors.groupingBy(
                Fido2UserMetrics::getEngagementLevel,
                Collectors.counting()
            ));
        segments.put("engagementSegments", engagementSegments);
        
        // Segment by adoption stage
        Map<String, Long> adoptionSegments = users.stream()
            .filter(u -> u.getAdoptionStage() != null)
            .collect(Collectors.groupingBy(
                Fido2UserMetrics::getAdoptionStage,
                Collectors.counting()
            ));
        segments.put("adoptionSegments", adoptionSegments);
        
        return segments;
    }

    private Map<String, Object> calculatePerformanceBenchmarks(Map<String, Object> performanceMetrics) {
        Map<String, Object> benchmarks = new HashMap<>();
        
        Double avgRegDuration = (Double) performanceMetrics.get(Fido2MetricsConstants.REGISTRATION_AVG_DURATION);
        if (avgRegDuration != null) {
            if (avgRegDuration < FAST_REGISTRATION_DURATION_MS) {
                benchmarks.put(Fido2MetricsConstants.REGISTRATION_BENCHMARK, "EXCELLENT");
            } else if (avgRegDuration < GOOD_REGISTRATION_DURATION_MS) {
                benchmarks.put(Fido2MetricsConstants.REGISTRATION_BENCHMARK, "GOOD");
            } else if (avgRegDuration < ACCEPTABLE_REGISTRATION_DURATION_MS) {
                benchmarks.put(Fido2MetricsConstants.REGISTRATION_BENCHMARK, "ACCEPTABLE");
            } else {
                benchmarks.put(Fido2MetricsConstants.REGISTRATION_BENCHMARK, "NEEDS_IMPROVEMENT");
            }
        }
        
        Double avgAuthDuration = (Double) performanceMetrics.get(Fido2MetricsConstants.AUTHENTICATION_AVG_DURATION);
        if (avgAuthDuration != null) {
            if (avgAuthDuration < 1000) {
                benchmarks.put(Fido2MetricsConstants.AUTHENTICATION_BENCHMARK, "EXCELLENT");
            } else if (avgAuthDuration < 2000) {
                benchmarks.put(Fido2MetricsConstants.AUTHENTICATION_BENCHMARK, "GOOD");
            } else if (avgAuthDuration < 3000) {
                benchmarks.put(Fido2MetricsConstants.AUTHENTICATION_BENCHMARK, "ACCEPTABLE");
            } else {
                benchmarks.put(Fido2MetricsConstants.AUTHENTICATION_BENCHMARK, "NEEDS_IMPROVEMENT");
            }
        }
        
        return benchmarks;
    }

    private Map<String, Object> compareDevicePerformance() {
        // Implementation for device performance comparison
        return new HashMap<>();
    }

    private Map<String, Object> analyzeErrorImpact() {
        // Implementation for error impact analysis
        return new HashMap<>();
    }

    // Helper methods to reduce complexity
    private long getTotalAttempts(Fido2MetricsAggregation agg) {
        return (agg.getRegistrationAttempts() != null ? agg.getRegistrationAttempts() : 0) + 
               (agg.getAuthenticationAttempts() != null ? agg.getAuthenticationAttempts() : 0);
    }

    private long getTotalSuccesses(Fido2MetricsAggregation agg) {
        return (agg.getRegistrationSuccesses() != null ? agg.getRegistrationSuccesses() : 0) + 
               (agg.getAuthenticationSuccesses() != null ? agg.getAuthenticationSuccesses() : 0);
    }

    private String getTrendDirection(double value) {
        if (value > 0) {
            return Fido2MetricsConstants.INCREASING;
        } else if (value < 0) {
            return Fido2MetricsConstants.DECREASING;
        } else {
            return Fido2MetricsConstants.STABLE;
        }
    }
}

