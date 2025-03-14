package io.jans.as.server.rate;

/**
 * @author Yuriy Z
 */
public class RateLimitedException extends Exception {

    private final RateLimitType type;

    public RateLimitedException(RateLimitType type, String message) {
        super(message);
        this.type = type;
    }

    public RateLimitType getType() {
        return type;
    }
}
