/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.lock.util;

/**
 * Provides server with basic statistic
 *
 * @author Yuriy Movchan Date: 12/24/2024
 */
public class ApiAccessConstants {

    private ApiAccessConstants() {}

    public static final String MONTH = "month";

    public static final String LOCK_CONFIG_READ_ACCESS = "https://jans.io/oauth/lock-config.readonly";
    public static final String LOCK_CONFIG_ISSUERS_READ_ACCESS = "https://jans.io/lock-server/issuers.read";
    public static final String LOCK_CONFIG_SCHEMA_READ_ACCESS = "https://jans.io/lock-server/schema.read";
    public static final String LOCK_CONFIG_POLICY_READ_ACCESS = "https://jans.io/lock-server/policy.read";

    public static final String LOCK_HEALTH_WRITE_ACCESS = "https://jans.io/oauth/lock/health.write";
    public static final String LOCK_LOG_WRITE_ACCESS = "https://jans.io/oauth/lock/log.write";
    public static final String LOCK_TELEMETRY_WRITE_ACCESS = "https://jans.io/oauth/lock/telemetry.write";
    
    public static final String LOCK_SSE_READ_ACCESS = "https://jans.io/oauth/lock/sse.read";
    
    public static final String LOCK_STAT_READ_ACCESS = "jans_stat";
    

}
