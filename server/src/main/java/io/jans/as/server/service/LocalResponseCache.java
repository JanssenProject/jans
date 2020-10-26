package io.jans.as.server.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.cdi.event.AuthConfigurationEvent;
import io.jans.service.cdi.async.Asynchronous;
import io.jans.service.cdi.event.Scheduled;
import org.json.JSONObject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@Named
public class LocalResponseCache {

    public static final int DEFAULT_DISCOVERY_LIFETIME = 60;
    private static final String DISCOVERY_CACHE_KEY = "DISCOVERY_CACHE_KEY";

    @Inject
    private AppConfiguration appConfiguration;

    private final AtomicBoolean rebuilding = new AtomicBoolean(false);
    private Cache<String, JSONObject> discoveryCache = CacheBuilder.newBuilder()
            .expireAfterWrite(DEFAULT_DISCOVERY_LIFETIME, TimeUnit.MINUTES).build();

    private int currentLifetime = DEFAULT_DISCOVERY_LIFETIME;

    @Asynchronous
    public void reloadConfigurationTimerEvent(@Observes @Scheduled AuthConfigurationEvent authConfigurationEvent) {
        if (currentLifetime != appConfiguration.getDiscoveryCacheLifetimeInMinutes()) {
            try {
                if (rebuilding.get())
                    return;

                rebuilding.set(true);
                currentLifetime = appConfiguration.getDiscoveryCacheLifetimeInMinutes();
                discoveryCache = createDiscoveryCache(appConfiguration.getDiscoveryCacheLifetimeInMinutes());
            } finally {
                rebuilding.set(false);
            }
        }
    }

    private static Cache<String, JSONObject> createDiscoveryCache(int lifetimeInMinutes) {
        return CacheBuilder.newBuilder().expireAfterWrite(lifetimeInMinutes, TimeUnit.MINUTES).build();
    }

    public JSONObject getDiscoveryResponse() {
        if (discoveryCache == null || rebuilding.get())
            return null;
        return discoveryCache.getIfPresent(DISCOVERY_CACHE_KEY);
    }

    public void putDiscoveryResponse(JSONObject response) {
        if (discoveryCache == null || rebuilding.get())
            return;

        discoveryCache.put(DISCOVERY_CACHE_KEY, response);
    }
}