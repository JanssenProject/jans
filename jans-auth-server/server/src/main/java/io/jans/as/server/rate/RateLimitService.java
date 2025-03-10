package io.jans.as.server.rate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.jans.as.client.RegisterRequest;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.register.ws.rs.RegisterService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class RateLimitService {

    private static final int DEFAULT_REQUEST_LIMIT = 10;
    private static final int DEFAULT_PERIOD_LIMIT = 60;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private RegisterService registerService;

    public void validateRateLimitForRegister(String key) throws RateLimitedException {
        int requestLimit = getRequestLimit(appConfiguration.getRateLimitRegistrationRequestCount());
        int periodLimit = getPeriodLimit(appConfiguration.getRateLimitRegistrationPeriodInSeconds());

        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(requestLimit, periodLimit));
        if (!bucket.tryConsume(1)) {
            String msg = String.format("Rate limited /register, key %s. Exceeds limit %s requests per %s seconds", key, requestLimit, periodLimit);
            log.debug(msg);
            throw new RateLimitedException(RateLimitType.REGISTRATION, msg);
        }
    }

    private int getRequestLimit(Integer requestLimit) {
        if (requestLimit == null || requestLimit <= 0) {
            return DEFAULT_REQUEST_LIMIT;
        }
        return requestLimit;
    }

    private int getPeriodLimit(Integer periodInSeconds) {
        if (periodInSeconds == null || periodInSeconds <= 0) {
            periodInSeconds = DEFAULT_PERIOD_LIMIT;
        }
        return periodInSeconds;
    }

    private Bucket newBucket(int requestLimit, int periodInSeconds) {
        Bandwidth limit = Bandwidth.builder().capacity(requestLimit).refillGreedy(requestLimit, Duration.ofSeconds(periodInSeconds)).build();
        return Bucket.builder().addLimit(limit).build();
    }

    public RegisterRequest parseRegisterRequest(String body) {
        try {
            final JSONObject requestObject = registerService.parseRequestObjectWithoutValidation(body);
            return RegisterRequest.fromJson(requestObject);
        } catch (Exception e) {
            log.error("Failed to parse registration request.", e);
            return null;
        }
    }
}
