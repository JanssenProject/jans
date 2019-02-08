package org.gluu.persist.watch;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple duration calculator helper
 *
 * @author Yuriy Movchan Date: 02/07/2019
 */
public class DurationUtil {

    private static final Logger log = LoggerFactory.getLogger(DurationUtil.class);

    public static Instant now() {
        return Instant.now();
    }

    public static Duration duration(Instant start) {
        Instant end = Instant.now();
        return Duration.between(start, end);
    }

    public static Duration duration(Instant start, Instant end) {
        return Duration.between(start, end);
    }

    public static void logDebug(String format, Object... arguments) {
        if (log.isDebugEnabled()) {
            log.debug(format, arguments);
        }
    }

}