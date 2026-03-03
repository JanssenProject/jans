package io.jans.as.server.rate;

/**
 * @author Yuriy Z
 */
public class RateLimitedException extends Exception {

    public RateLimitedException(String message) {
        super(message);
    }
}
