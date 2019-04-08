package org.gluu.persist.watch;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;

/**
 * Simple duration calculator helper
 *
 * @author Yuriy Movchan Date: 02/07/2019
 */
public abstract class DurationUtil {

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

    public void logDebug(String format, Object... arguments) {
    	Logger log = getLog();
        if (log.isDebugEnabled()) {
            log.debug(format, arguments);
        }
    }
    
    public abstract Logger getLog();

}