package io.jans.as.server.rate;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.jans.as.client.util.ClientUtil;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.configuration.rate.KeyExtractor;
import io.jans.as.model.configuration.rate.RateLimitConfig;
import io.jans.as.model.configuration.rate.RateLimitRule;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.util.Pair;
import io.jans.service.cdi.event.ConfigurationUpdate;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * @author Yuriy Z
 */
@ApplicationScoped
public class RateLimitService {

    private static final int DEFAULT_REQUEST_LIMIT = 10;
    private static final int DEFAULT_PERIOD_LIMIT = 60;
    public static final int KEY_LENGTH_LIMIT_FOR_DIGEST = 300;

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    private final Cache<String, Bucket> buckets = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            //.weakKeys()
            .build();
    private RateLimitConfig rateLimitConfiguration;

    public HttpServletRequest validateRateLimit(HttpServletRequest httpRequest) throws RateLimitedException, IOException {
        // if rate_limit flag is disabled immediately return
        if (!errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.RATE_LIMIT)) {
            return httpRequest;
        }

        // no rate limit configuration -> return
        if (rateLimitConfiguration == null || isEmpty(rateLimitConfiguration.getRateLimitRules())) {
            return httpRequest;
        }

        String requestPath = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        final List<RateLimitRule> matchedRules = matchRulesByPathAndMethod(requestPath, method);

        // no matching rules
        if (matchedRules.isEmpty()) {
            return httpRequest;
        }

        RateLimitContext rateLimitContext = new RateLimitContext(httpRequest, rateLimitConfiguration.isRateLoggingEnabled());
        final List<Pair<String, RateLimitRule>> keyWithRules = buildKeyPerRule(rateLimitContext, matchedRules);

        for (Pair<String, RateLimitRule> keyWithRule : keyWithRules) {
            String key = keyWithRule.getFirst();
            RateLimitRule rule = keyWithRule.getSecond();

            int requestLimit = getRequestLimit(rule.getRequestCount());
            int periodLimit = getPeriodLimit(rule.getPeriodInSeconds());

            // if key is too long -> hash it to reduce amount of space it takes in memory
            key = saveSpaceIfNeeded(key);
            try {
                Bucket bucket = buckets.get(key, () -> newBucket(requestLimit, periodLimit));
                if (!bucket.tryConsume(1)) {
                    String msg = String.format("Rate limited '%s'. Exceeds limit %s requests per %s seconds. Key: %s", requestPath, requestLimit, periodLimit, key);
                    log.debug(msg);
                    throw new RateLimitedException(RateLimitType.REGISTRATION, msg);
                }
            } catch (ExecutionException e) {
                log.error(e.getMessage(), e);
            }
        }

        if (rateLimitContext.isCachedRequestAvailable()) {
            return rateLimitContext.getCachedRequest();
        }

        return httpRequest;
    }

    public static @NotNull String saveSpaceIfNeeded(String key) {
        if (key.length() > KEY_LENGTH_LIMIT_FOR_DIGEST) {
            key = DigestUtils.sha256Hex(key);
        }
        return key;
    }

    private List<Pair<String, RateLimitRule>> buildKeyPerRule(RateLimitContext rateLimitContext, List<RateLimitRule> matchedRules) {
        List<Pair<String, RateLimitRule>> keyWithRules = new ArrayList<>();
        for (RateLimitRule rule : matchedRules) {
            try {
                keyWithRules.add(new Pair<>(buildKey(rateLimitContext, rule), rule));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return keyWithRules;
    }

    public String buildKey(RateLimitContext rateLimitContext, RateLimitRule rule) throws IOException {
        String requestPath = rateLimitContext.getRequest().getRequestURI();
        StringBuilder key = new StringBuilder(requestPath + "_");

        for (KeyExtractor keyExtractor : rule.getKeyExtractors()) {
            key.append(extractKey(keyExtractor, rateLimitContext)).append("_");
        }

        String keyString = key.toString();
        if (rateLimitContext.isRateLoggingEnabled() && log.isTraceEnabled()) {
            log.trace("Rate limit key: {}", keyString);
        }
        return keyString;
    }

    protected String extractKey(KeyExtractor keyExtractor, RateLimitContext rateLimitContext) throws IOException {
        StringBuilder key = new StringBuilder();
        switch (keyExtractor.getSource()) {
            case HEADER:
                for (String header : keyExtractor.getParameterNames()) {
                    String value = rateLimitContext.getRequest().getHeader(header);
                    if (StringUtils.isNotBlank(value)) {
                        key.append(value).append("_");
                    }
                }
                return key.toString();
            case BODY:
                String contentType = rateLimitContext.getRequest().getContentType();

                // Note: Use .contains() rather than .equals() because the header often includes character encoding (e.g., application/json; charset=UTF-8).
                if (contentType != null && contentType.contains("application/json")) {
                    String bodyAsString = rateLimitContext.getCachedRequest().getCachedBodyAsString();
                    JSONObject jsonObject = parseBody(bodyAsString);
                    if (jsonObject != null) {
                        for (String name : keyExtractor.getParameterNames()) {
                            List<String> values = ClientUtil.extractListByKey(jsonObject, name);
                            if (!values.isEmpty()) {
                                key.append(values).append("_");
                            }
                        }
                    }
                } else {
                    for (String name : keyExtractor.getParameterNames()) {
                        String value = rateLimitContext.getRequest().getParameter(name);
                        if (StringUtils.isNotBlank(value)) {
                            key.append(value).append("_");
                        }
                    }
                }

                return key.toString();
            case QUERY:
                for (String name : keyExtractor.getParameterNames()) {
                    String value = rateLimitContext.getRequest().getParameter(name);
                    if (StringUtils.isNotBlank(value)) {
                        key.append(value).append("_");
                    }
                }
                return key.toString();
        }

        log.error("Invalid key extractor source: {}", keyExtractor.getSource());
        return "null";
    }

    public List<RateLimitRule> matchRulesByPathAndMethod(String requestPath, String method) {
        List<RateLimitRule> result = new ArrayList<>();

        for (RateLimitRule rule : rateLimitConfiguration.getRateLimitRules()) {
            if (!rule.isWellFormed()) {
                log.error("Invalid rate limit rule: {}", rule);
                continue;
            }

            if (rule.getPath().equals(requestPath) && rule.getMethods().contains(method)) {
                result.add(rule);
            }
        }
        return result;
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

    public JSONObject parseBody(String body) {
        try {
            return new JSONObject(body);
        } catch (Exception e) {
            try {
                return Jwt.parseOrThrow(body).getClaims().toJsonObject();
            } catch (InvalidJwtException ex) {
                return null;
            }
        }
    }

    @PostConstruct
    public void init() {
        updateConfiguration(appConfiguration);
    }

    public void updateConfiguration(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
        try {
            rateLimitConfiguration = appConfiguration.getRateLimitConfiguration();

            if (rateLimitConfiguration == null) {
                log.info("Rate limiting is not configured.");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
