package io.jans.configapi.auth;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;

import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Named
public class UmaResourceProtectionCache {

    public static final int CACHE_LIFETIME = 60;

    private static final Cache<String, Scope> scopeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_LIFETIME, TimeUnit.MINUTES).build();

    private static final Cache<String, UmaResource> umaResourceCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_LIFETIME, TimeUnit.MINUTES).build();

    // Scope
    public static void removeAllScopes() {
        scopeCache.invalidateAll();
    }

    public static Scope getScope(String scopeName) {
        Preconditions.checkNotNull(scopeName);
        Preconditions.checkState(!Strings.isNullOrEmpty(scopeName));
        return scopeCache.getIfPresent(scopeName);

    }

    public static void putScope(Scope scope) {
        Preconditions.checkNotNull(scope);
        if (scopeCache.getIfPresent(scope.getId()) == null) {
            scopeCache.put(scope.getId(), scope);
        }
    }

    public static Map<String, Scope> getAllScopes() {
        return Maps.newHashMap(scopeCache.asMap());
    }

    // UmaResource
    public static void removeAllUmaResources() {
        umaResourceCache.invalidateAll();
    }

    public static UmaResource getUmaResource(String umaResourceName) {
        Preconditions.checkNotNull(umaResourceName);
        Preconditions.checkState(!Strings.isNullOrEmpty(umaResourceName));
        return umaResourceCache.getIfPresent(umaResourceName);

    }

    public static void putUmaResource(String umaResourceName, UmaResource umaResource) {
        Preconditions.checkNotNull(umaResource);
        if (umaResourceCache.getIfPresent(umaResourceName) == null) {
            umaResourceCache.put(umaResourceName, umaResource);
        }
    }

    public static Map<String, UmaResource> getAllUmaResources() {
        return Maps.newHashMap(umaResourceCache.asMap());
    }

}
