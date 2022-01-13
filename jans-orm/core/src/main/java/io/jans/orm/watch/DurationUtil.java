/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.watch;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple duration calculator helper
 *
 * @author Yuriy Movchan Date: 02/07/2019
 */
public abstract class DurationUtil {

	protected static final Logger log = LoggerFactory.getLogger(DurationUtil.class);

    public Instant now() {
        return Instant.now();
    }

    public Duration duration(Instant start) {
        Instant end = Instant.now();
        return Duration.between(start, end);
    }

    public Duration duration(Instant start, Instant end) {
        return Duration.between(start, end);
    }

    public abstract void logDebug(String format, Object... arguments);

}