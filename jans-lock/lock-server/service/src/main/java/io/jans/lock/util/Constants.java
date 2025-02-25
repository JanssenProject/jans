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
public class Constants {

    private Constants() {}

    public static final String MONTH = "month";

    public static final String LOCK_READ_ACCESS = "https://jans.io/oauth/lock/read-all";
    public static final String LOCK_WRITE_ACCESS = "https://jans.io/oauth/lock/write-all";

    public static final String LOCK_CONFIG_READ_ACCESS = "https://jans.io/oauth/lock-config.readonly";
    public static final String LOCK_CONFIG_WRITE_ACCESS = "https://jans.io/oauth/lock-config.write";

    public static final String LOCK_AUDIT_READ_ACCESS = "https://jans.io/oauth/lock/audit.readonly";
    public static final String LOCK_AUDIT_WRITE_ACCESS = "https://jans.io/oauth/lock/audit.write";

    public static final String LOCK_HEALTH_READ_ACCESS = "https://jans.io/oauth/lock/health.readonly";
    public static final String LOCK_HEALTH_WRITE_ACCESS = "https://jans.io/oauth/lock/health.write";

    public static final String LOCK_LOG_READ_ACCESS = "https://jans.io/oauth/lock/log.readonly";
    public static final String LOCK_LOG_WRITE_ACCESS = "https://jans.io/oauth/lock/log.write";

    public static final String LOCK_TELEMETRY_READ_ACCESS = "https://jans.io/oauth/lock/telemetry.readonly";
    public static final String LOCK_TELEMETRY_WRITE_ACCESS = "https://jans.io/oauth/lock/telemetry.write";

}
