package io.jans.as.server.rate;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class RateLimitService {

    private static final int DEFAULT_REQUEST_LIMIT = 10;
    private static final int DEFAULT_PERIOD_LIMIT = 60;

    private final Cache<String, Bucket> buckets = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .weakKeys()
            .build();

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private RegisterService registerService;

    public void validateRateLimitForRegister(String key) throws RateLimitedException {
        int requestLimit = getRequestLimit(appConfiguration.getRateLimitRegistrationRequestCount());
        int periodLimit = getPeriodLimit(appConfiguration.getRateLimitRegistrationPeriodInSeconds());

        try {
            Bucket bucket = buckets.get(key, () -> newBucket(requestLimit, periodLimit));
            if (!bucket.tryConsume(1)) {
                String msg = String.format("Rate limited '/register', key %s. Exceeds limit %s requests per %s seconds.", key, requestLimit, periodLimit);
                log.debug(msg);
                throw new RateLimitedException(RateLimitType.REGISTRATION, msg);
            }
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
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
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(requestLimit)
                        .refillGreedy(requestLimit, Duration.ofSeconds(periodInSeconds))
                        .build())
                .build();
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
