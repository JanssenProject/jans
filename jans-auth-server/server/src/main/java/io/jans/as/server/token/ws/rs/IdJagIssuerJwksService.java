package io.jans.as.server.token.ws.rs;

import io.jans.as.model.configuration.TrustedIssuerConfig;
import io.jans.as.server.service.net.UriService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Resolves the JWKS used to verify the signature of an ID-JAG issued by a trusted external issuer
 * configured in {@code idJagTrustedIdpIssuers}.
 * <p>
 * Inline {@code jwks} takes precedence over {@code jwksUri}. Keys fetched from {@code jwksUri} are
 * cached; a failed fetch is negatively cached and an unknown {@code kid} triggers at most one
 * refetch per {@link #REFETCH_MIN_INTERVAL_MILLIS}. The URI comes only from admin configuration,
 * never from the JWT, and is loaded through {@link UriService} so {@code externalUriWhiteList} applies.
 *
 * @author Yuriy Z
 */
@ApplicationScoped
public class IdJagIssuerJwksService {

    private static final long CACHE_TTL_MILLIS = TimeUnit.SECONDS.toMillis(300);
    private static final long REFETCH_MIN_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static final String HTTPS_SCHEME = "https";

    @Inject
    private Logger log;

    @Inject
    private UriService uriService;

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> fetchLocks = new ConcurrentHashMap<>();

    /**
     * Returns true if the issuer config carries its own keys (inline or by URI).
     */
    public boolean hasKeySource(TrustedIssuerConfig config) {
        return config != null && (StringUtils.isNotBlank(config.getJwks()) || StringUtils.isNotBlank(config.getJwksUri()));
    }

    /**
     * Returns the issuer JWKS, or null (fail closed) if none can be obtained.
     */
    public JSONObject getJwks(TrustedIssuerConfig config, String keyId) {
        if (config == null) {
            return null;
        }

        if (StringUtils.isNotBlank(config.getJwks())) {
            try {
                return new JSONObject(config.getJwks());
            } catch (JSONException e) {
                log.error("Inline jwks of trusted ID-JAG issuer is not valid JSON.", e);
                return null;
            }
        }

        if (StringUtils.isNotBlank(config.getJwksUri())) {
            return getFromUri(config.getJwksUri(), keyId);
        }
        return null;
    }

    private JSONObject getFromUri(String jwksUri, String keyId) {
        if (!isHttps(jwksUri)) {
            log.error("Trusted ID-JAG issuer jwksUri must use https: {}", jwksUri);
            return null;
        }

        final CacheEntry cached = cache.get(jwksUri);
        if (isUsable(cached, keyId, now())) {
            return cached.jwks;
        }

        synchronized (fetchLocks.computeIfAbsent(jwksUri, k -> new Object())) {
            final long now = now();
            final CacheEntry current = cache.get(jwksUri);
            if (isUsable(current, keyId, now)) {
                return current.jwks;
            }

            final JSONObject fetched = uriService.loadJson(jwksUri);
            if (fetched == null || !fetched.has("keys")) {
                log.error("Unable to load jwks of trusted ID-JAG issuer from: {}", jwksUri);
                if (current != null && current.jwks != null && now - current.fetchedAt < CACHE_TTL_MILLIS) {
                    cache.put(jwksUri, new CacheEntry(current.jwks, current.fetchedAt, now));
                    return current.jwks;
                }
                cache.put(jwksUri, new CacheEntry(null, now, now));
                return null;
            }
            cache.put(jwksUri, new CacheEntry(fetched, now, now));
            return fetched;
        }
    }

    long now() {
        return System.currentTimeMillis();
    }

    private static boolean isHttps(String uri) {
        try {
            return HTTPS_SCHEME.equalsIgnoreCase(URI.create(uri).getScheme());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isUsable(CacheEntry entry, String keyId, long now) {
        if (entry == null) {
            return false;
        }
        final long sinceAttempt = now - entry.lastAttemptAt;
        if (entry.jwks == null) {
            return sinceAttempt < REFETCH_MIN_INTERVAL_MILLIS;
        }
        if (now - entry.fetchedAt >= CACHE_TTL_MILLIS) {
            return false;
        }
        return StringUtils.isBlank(keyId) || hasKey(entry.jwks, keyId) || sinceAttempt < REFETCH_MIN_INTERVAL_MILLIS;
    }

    private static boolean hasKey(JSONObject jwks, String keyId) {
        final JSONArray keys = jwks.optJSONArray("keys");
        if (keys == null) {
            return false;
        }
        for (int i = 0; i < keys.length(); i++) {
            final JSONObject key = keys.optJSONObject(i);
            if (key != null && keyId.equals(key.optString("kid"))) {
                return true;
            }
        }
        return false;
    }

    private static final class CacheEntry {
        private final JSONObject jwks;
        private final long fetchedAt;
        private final long lastAttemptAt;

        private CacheEntry(JSONObject jwks, long fetchedAt, long lastAttemptAt) {
            this.jwks = jwks;
            this.fetchedAt = fetchedAt;
            this.lastAttemptAt = lastAttemptAt;
        }
    }
}
