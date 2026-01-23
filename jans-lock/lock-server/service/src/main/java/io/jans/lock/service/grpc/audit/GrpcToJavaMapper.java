/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.lock.service.grpc.audit;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.google.protobuf.Timestamp;

import io.jans.lock.model.audit.HealthEntry;
import io.jans.lock.model.audit.LogEntry;
import io.jans.lock.model.audit.TelemetryEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Mapper class to convert between gRPC proto messages and Java beans.
 * 
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class GrpcToJavaMapper {
    /**
     * Convert gRPC HealthEntry to Java HealthEntry bean.
     *
     * @param grpcEntry the gRPC health entry
     * @return the Java health entry bean
     */
    public HealthEntry toHealthEntry(io.jans.lock.model.audit.grpc.HealthEntry grpcEntry) {
        if (grpcEntry == null) {
            return null;
        }

        HealthEntry healthEntry = new HealthEntry();
        healthEntry.setCreationDate(toZonedDateTime(grpcEntry.getCreationDate()));
        healthEntry.setEventTime(toZonedDateTime(grpcEntry.getEventTime()));
        healthEntry.setService(grpcEntry.getService());
        healthEntry.setNodeName(grpcEntry.getNodeName());
        healthEntry.setStatus(grpcEntry.getStatus());

        // Convert engine status map
        if (grpcEntry.getEngineStatusCount() > 0) {
            Map<String, String> engineStatus = new HashMap<>();
            grpcEntry.getEngineStatusMap().forEach(engineStatus::put);
            healthEntry.setEngineStatus(engineStatus);
        }

        return healthEntry;
    }

    /**
     * Convert gRPC LogEntry to Java LogEntry bean.
     *
     * @param grpcEntry the gRPC log entry
     * @return the Java log entry bean
     */
    public LogEntry toLogEntry(io.jans.lock.model.audit.grpc.LogEntry grpcEntry) {
        if (grpcEntry == null) {
            return null;
        }

        LogEntry logEntry = new LogEntry();
        logEntry.setCreationDate(toZonedDateTime(grpcEntry.getCreationDate()));
        logEntry.setEventTime(toZonedDateTime(grpcEntry.getEventTime()));
        logEntry.setService(grpcEntry.getService());
        logEntry.setNodeName(grpcEntry.getNodeName());
        logEntry.setEventType(grpcEntry.getEventType());
        logEntry.setSeverityLevel(grpcEntry.getSeverityLevel());
        logEntry.setAction(grpcEntry.getAction());
        logEntry.setDecisionResult(grpcEntry.getDecisionResult());
        logEntry.setRequestedResource(grpcEntry.getRequestedResource());
        logEntry.setPrincipalId(grpcEntry.getPrincipalId());
        logEntry.setClientId(grpcEntry.getClientId());
        logEntry.setJti(grpcEntry.getJti());
        
        // Convert context information map
        if (grpcEntry.getContextInformationCount() > 0) {
            Map<String, String> contextInfo = new HashMap<>();
            grpcEntry.getContextInformationMap().forEach(contextInfo::put);
            logEntry.setContextInformation(contextInfo);
        }

        return logEntry;
    }

    /**
     * Convert gRPC TelemetryEntry to Java TelemetryEntry bean.
     *
     * @param grpcEntry the gRPC telemetry entry
     * @return the Java telemetry entry bean
     */
    public TelemetryEntry toTelemetryEntry(io.jans.lock.model.audit.grpc.TelemetryEntry grpcEntry) {
        if (grpcEntry == null) {
            return null;
        }

        TelemetryEntry telemetryEntry = new TelemetryEntry();
        telemetryEntry.setCreationDate(toZonedDateTime(grpcEntry.getCreationDate()));
        telemetryEntry.setEventTime(toZonedDateTime(grpcEntry.getEventTime()));
        telemetryEntry.setService(grpcEntry.getService());
        telemetryEntry.setNodeName(grpcEntry.getNodeName());
        telemetryEntry.setStatus(grpcEntry.getStatus());
        telemetryEntry.setLastPolicyLoadSize(grpcEntry.getLastPolicyLoadSize());
        telemetryEntry.setPolicySuccessLoadCounter(grpcEntry.getPolicySuccessLoadCounter());
        telemetryEntry.setPolicyFailedLoadCounter(grpcEntry.getPolicyFailedLoadCounter());
        telemetryEntry.setLastPolicyEvaluationTimeNs(grpcEntry.getLastPolicyEvaluationTimeNs());
        telemetryEntry.setAvgPolicyEvaluationTimeNs(grpcEntry.getAvgPolicyEvaluationTimeNs());
        telemetryEntry.setMemoryUsage(grpcEntry.getMemoryUsage());
        telemetryEntry.setEvaluationRequestsCount(grpcEntry.getEvaluationRequestsCount());
        
        // Convert policy stats map
        if (grpcEntry.getPolicyStatsCount() > 0) {
            Map<String, Long> policyStats = new HashMap<>();
            grpcEntry.getPolicyStatsMap().forEach(policyStats::put);
            telemetryEntry.setPolicyStats(policyStats);
        }

        return telemetryEntry;
    }

    /**
     * Convert Protobuf Timestamp to Date.
     *
     * @param timestamp the Protobuf timestamp
     * @return the date
     */
    private Date toZonedDateTime(Timestamp timestamp) {
        if (timestamp == null || (timestamp.getSeconds() == 0 && timestamp.getNanos() == 0)) {
            return null;
        }

        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        
        return Date.from(instant);
    }
}