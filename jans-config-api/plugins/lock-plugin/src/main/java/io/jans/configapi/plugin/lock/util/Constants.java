/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.configapi.plugin.lock.util;

public class Constants {

    private Constants() {
    }

    public static final String LOCK = "/lock";
    public static final String LOCK_CONFIG = "/lockConfig";
    public static final String AUDIT = "/audit";
    public static final String LOCK_STAT = "/lockStat";
    public static final String HEALTH = "/health";
    public static final String LOG = "/log";
    public static final String TELEMETRY = "/telemetry";
    public static final String SEARCH = "/search";
    public static final String BULK = "/bulk";

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