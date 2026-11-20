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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.jans.as.model.util.Util.escapeLog;
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

    private static final int MIN_BUCKET_RETENTION_IN_SECONDS = 120;

    private final AtomicReference<Cache<String, Bucket>> buckets = new AtomicReference<>(newBucketCache(MIN_BUCKET_RETENTION_IN_SECONDS));
    private final AtomicReference<RateLimitState> rateLimitState = new AtomicReference<>();
    private volatile int bucketRetentionInSeconds = MIN_BUCKET_RETENTION_IN_SECONDS;

    public HttpServletRequest validateRateLimit(HttpServletRequest httpRequest) throws RateLimitedException, IOException {
        // if rate_limit flag is disabled immediately return
        if (!errorResponseFactory.isFeatureFlagEnabled(FeatureFlagType.RATE_LIMIT)) {
            return httpRequest;
        }

        // snapshot so a concurrent configuration reload cannot change rules mid-request
        final RateLimitState state = rateLimitState.get();

        // no rate limit configuration -> return
        if (state == null || isEmpty(state.config.getRateLimitRules())) {
            return httpRequest;
        }
        final RateLimitConfig config = state.config;

        String requestPath = canonicalPath(httpRequest.getRequestURI());
        String method = httpRequest.getMethod();

        final List<RateLimitRule> matchedRules = matchRulesByPathAndMethod(state, requestPath, method);

        // no matching rules
        if (matchedRules.isEmpty()) {
            return httpRequest;
        }

        RateLimitContext rateLimitContext = new RateLimitContext(httpRequest, config.isRateLoggingEnabled());
        final List<Pair<String, RateLimitRule>> keyWithRules = buildKeyPerRule(rateLimitContext, matchedRules);
        final Cache<String, Bucket> currentBuckets = buckets.get();

        for (Pair<String, RateLimitRule> keyWithRule : keyWithRules) {
            RateLimitRule rule = keyWithRule.getSecond();

            int requestLimit = getRequestLimit(rule.getRequestCount());
            int periodLimit = getPeriodLimit(rule.getPeriodInSeconds());

            // namespace by rule and its limits so overlapping rules do not share a bucket and edited limits take effect
            // if key is too long -> hash it to reduce amount of space it takes in memory
            String key = saveSpaceIfNeeded(ruleIndex(config, rule) + "_" + requestLimit + "_" + periodLimit + "_" + keyWithRule.getFirst());
            Bucket bucket;
            try {
                bucket = currentBuckets.get(key, () -> newBucket(requestLimit, periodLimit));
            } catch (ExecutionException e) {
                throw new IOException("Failed to evaluate rate limit rule", e);
            }
            if (!bucket.tryConsume(1)) {
                String msg = String.format("Rate limited '%s'. Exceeds limit %s requests per %s seconds.", escapeLog(requestPath), requestLimit, periodLimit);
                if (rateLimitContext.isRateLoggingEnabled()) {
                    log.debug("{} Key: {}", msg, escapeLog(key));
                } else {
                    log.debug(msg);
                }
                throw new RateLimitedException(msg);
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

    private List<Pair<String, RateLimitRule>> buildKeyPerRule(RateLimitContext rateLimitContext, List<RateLimitRule> matchedRules) throws IOException {
        List<Pair<String, RateLimitRule>> keyWithRules = new ArrayList<>();
        for (RateLimitRule rule : matchedRules) {
            keyWithRules.add(new Pair<>(buildKey(rateLimitContext, rule), rule));
        }
        return keyWithRules;
    }

    private static int ruleIndex(RateLimitConfig config, RateLimitRule rule) {
        final List<RateLimitRule> rules = config.getRateLimitRules();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i) == rule) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Normalizes request URI the way routing sees it: strips path parameters (;...), collapses empty
     * segments and percent-decodes, so alternate encodings of one endpoint match the same rule.
     */
    public static String canonicalPath(String requestUri) {
        if (StringUtils.isBlank(requestUri)) {
            return "/";
        }
        if (isAlreadyCanonical(requestUri)) {
            return requestUri;
        }
        StringBuilder path = new StringBuilder();
        for (String segment : requestUri.split("/")) {
            int paramIndex = segment.indexOf(';');
            if (paramIndex >= 0) {
                segment = segment.substring(0, paramIndex);
            }
            if (segment.isEmpty()) {
                continue;
            }
            try {
                segment = URLDecoder.decode(segment.replace("+", "%2B"), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                // keep raw segment if it is not valid percent-encoding
            }
            path.append('/').append(segment);
        }
        return path.length() == 0 ? "/" : path.toString();
    }

    /**
     * Cheap check that skips normalization for the common case: no path parameters, no percent-encoding,
     * no empty segments and no trailing slash.
     */
    private static boolean isAlreadyCanonical(String requestUri) {
        final int length = requestUri.length();
        if (length < 2 || requestUri.charAt(0) != '/' || requestUri.charAt(length - 1) == '/') {
            return false;
        }
        for (int i = 1; i < length; i++) {
            char c = requestUri.charAt(i);
            if (c == ';' || c == '%' || (c == '/' && requestUri.charAt(i - 1) == '/')) {
                return false;
            }
        }
        return true;
    }

    public String buildKey(RateLimitContext rateLimitContext, RateLimitRule rule) throws IOException {
        String requestPath = canonicalPath(rateLimitContext.getRequest().getRequestURI());
        StringBuilder key = new StringBuilder(requestPath + "_");

        for (KeyExtractor keyExtractor : rule.getKeyExtractors()) {
            key.append(extractKey(keyExtractor, rateLimitContext)).append("_");
        }

        String keyString = key.toString();
        if (rateLimitContext.isRateLoggingEnabled() && log.isTraceEnabled()) {
            log.trace("Rate limit key: {}", escapeLog(keyString));
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
                String contentType = StringUtils.lowerCase(rateLimitContext.getRequest().getContentType(), Locale.ROOT);

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
                } else if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                    // Use container parameter parsing (do not drain the entity: downstream needs the form parameters).
                    // Query parameters are mixed in by getParameterValues(), so skip values that came from the query string.
                    Map<String, List<String>> query = parseFormBody(rateLimitContext.getRequest().getQueryString());
                    for (String name : keyExtractor.getParameterNames()) {
                        List<String> values = new ArrayList<>();
                        String[] all = rateLimitContext.getRequest().getParameterValues(name);
                        if (all != null) {
                            values.addAll(Arrays.asList(all));
                        }
                        List<String> queryValues = query.get(name);
                        if (queryValues != null) {
                            queryValues.forEach(values::remove);
                        }
                        if (!values.isEmpty() && StringUtils.isNotBlank(values.get(0))) {
                            key.append(values.get(0)).append("_");
                        }
                    }
                } else {
                    JSONObject jsonObject = parseBody(rateLimitContext.getCachedRequest().getCachedBodyAsString());
                    if (jsonObject != null) {
                        for (String name : keyExtractor.getParameterNames()) {
                            List<String> values = ClientUtil.extractListByKey(jsonObject, name);
                            if (!values.isEmpty()) {
                                key.append(values).append("_");
                            }
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
        return matchRulesByPathAndMethod(rateLimitState.get(), requestPath, method);
    }

    private List<RateLimitRule> matchRulesByPathAndMethod(RateLimitState state, String requestPath, String method) {
        if (state == null) {
            return Collections.emptyList();
        }

        final List<RateLimitRule> rulesForPath = state.rulesByPath.get(requestPath);
        if (rulesForPath == null) {
            return Collections.emptyList();
        }

        List<RateLimitRule> result = new ArrayList<>(rulesForPath.size());
        for (RateLimitRule rule : rulesForPath) {
            if (rule.getMethods().contains(method)) {
                result.add(rule);
            }
        }
        return result;
    }

    private RateLimitState buildState(RateLimitConfig config) {
        Map<String, List<RateLimitRule>> rulesByPath = new HashMap<>();
        if (config.getRateLimitRules() != null) {
            for (RateLimitRule rule : config.getRateLimitRules()) {
                if (!rule.isWellFormed()) {
                    log.error("Invalid rate limit rule: {}", rule);
                    continue;
                }
                rulesByPath.computeIfAbsent(canonicalPath(rule.getPath()), k -> new ArrayList<>()).add(rule);
            }
        }
        return new RateLimitState(config, rulesByPath);
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

    private static Map<String, List<String>> parseFormBody(String body) {
        Map<String, List<String>> result = new HashMap<>();
        if (StringUtils.isBlank(body)) {
            return result;
        }
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            try {
                String name = URLDecoder.decode(eq >= 0 ? pair.substring(0, eq) : pair, StandardCharsets.UTF_8);
                String value = eq >= 0 ? URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8) : "";
                result.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            } catch (IllegalArgumentException e) {
                // skip malformed pair
            }
        }
        return result;
    }

    private static Cache<String, Bucket> newBucketCache(int retentionInSeconds) {
        // expire only after a full refill period without access, otherwise a recreated bucket would reset a deficit
        return CacheBuilder.newBuilder()
                .expireAfterAccess(retentionInSeconds, TimeUnit.SECONDS)
                .build();
    }

    private int maxPeriodInSeconds(RateLimitConfig config) {
        int max = MIN_BUCKET_RETENTION_IN_SECONDS;
        if (config != null && config.getRateLimitRules() != null) {
            for (RateLimitRule rule : config.getRateLimitRules()) {
                max = Math.max(max, getPeriodLimit(rule.getPeriodInSeconds()));
            }
        }
        return max;
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
            final RateLimitConfig config = appConfiguration.getRateLimitConfiguration();
            // keep counters across unrelated config reloads; rebuild only when retention must change
            final int retention = maxPeriodInSeconds(config);
            if (retention != bucketRetentionInSeconds) {
                buckets.set(newBucketCache(retention));
                bucketRetentionInSeconds = retention;
            }
            rateLimitState.set(config != null ? buildState(config) : null);

            if (config == null) {
                log.info("Rate limiting is not configured.");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static final class RateLimitState {
        private final RateLimitConfig config;
        private final Map<String, List<RateLimitRule>> rulesByPath;

        private RateLimitState(RateLimitConfig config, Map<String, List<RateLimitRule>> rulesByPath) {
            this.config = config;
            this.rulesByPath = rulesByPath;
        }
    }
}
