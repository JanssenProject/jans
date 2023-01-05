package io.jans.configapi.security.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import io.jans.as.persistence.model.Scope;
import io.jans.configapi.core.util.ProtectionScopeType;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@Named
public class ApiProtectionCache {

    public static final int CACHE_LIFETIME = 60;

    private static final Cache<String, Scope> scopeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_LIFETIME, TimeUnit.MINUTES).build();

    private static final Cache<String, Scope> groupScopeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_LIFETIME, TimeUnit.MINUTES).build();

    private static final Cache<String, Scope> superScopeCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_LIFETIME, TimeUnit.MINUTES).build();

    private static final Cache<String, Map<ProtectionScopeType, List<Scope>>> resourceCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_LIFETIME, TimeUnit.MINUTES).build();

    ApiProtectionCache() {
    }

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
        if (scopeCache.getIfPresent(scope.getInum()) == null) {
            scopeCache.put(scope.getInum(), scope);
        }
    }

    public static Map<String, Scope> getScopes() {
        return Maps.newHashMap(scopeCache.asMap());
    }

    // Group Scope
    public static Scope getGroupScope(String scopeName) {
        Preconditions.checkNotNull(scopeName);
        Preconditions.checkState(!Strings.isNullOrEmpty(scopeName));
        return groupScopeCache.getIfPresent(scopeName);

    }

    public static void putGroupScope(Scope scope) {
        Preconditions.checkNotNull(scope);
        if (groupScopeCache.getIfPresent(scope.getInum()) == null) {
            groupScopeCache.put(scope.getInum(), scope);
        }
    }

    public static void removeGroupScopes() {
        groupScopeCache.invalidateAll();
    }

    public static Map<String, Scope> getGroupScopes() {
        return Maps.newHashMap(groupScopeCache.asMap());
    }

    // Super Scope
    public static Scope getSuperScope(String scopeName) {
        Preconditions.checkNotNull(scopeName);
        Preconditions.checkState(!Strings.isNullOrEmpty(scopeName));
        return superScopeCache.getIfPresent(scopeName);

    }

    public static void putSuperScope(Scope scope) {
        Preconditions.checkNotNull(scope);
        if (superScopeCache.getIfPresent(scope.getInum()) == null) {
            superScopeCache.put(scope.getInum(), scope);
        }
    }

    public static void removeSuperScopes() {
        superScopeCache.invalidateAll();
    }

    public static Map<String, Scope> getSuperScopes() {
        return Maps.newHashMap(superScopeCache.asMap());
    }

    // All Scope
    public static Map<String, Scope> getAllTypesOfScopes() {
        Map<String, Scope> scopes = Maps.newHashMap(scopeCache.asMap());
        scopes.putAll(Maps.newHashMap(groupScopeCache.asMap()));
        scopes.putAll(Maps.newHashMap(superScopeCache.asMap()));
        return scopes;
    }

    // Resource
    public static void raemoveAllResources() {
        resourceCache.invalidateAll();
    }

    public static void putResource(String resourceName, Map<ProtectionScopeType, List<Scope>> scopeMap) {
        Preconditions.checkNotNull(resourceName);
        resourceCache.put(resourceName, scopeMap);
    }

    public static void putResourceScopeByType(String resourceName, ProtectionScopeType protectionScopeType,
            List<Scope> scopes) {
        Preconditions.checkNotNull(resourceName);
        Preconditions.checkNotNull(protectionScopeType);
        Map<ProtectionScopeType, List<Scope>> scopeMap = resourceCache.getIfPresent(resourceName);
        if (scopeMap == null) {
            scopeMap = new HashMap<>();
        }
        scopeMap.put(protectionScopeType, scopes);
        resourceCache.put(resourceName, scopeMap);
    }

    public static Map<String, Map<ProtectionScopeType, List<Scope>>> getAllResources() {
        return Maps.newHashMap(resourceCache.asMap());
    }

    public static Map<ProtectionScopeType, List<Scope>> getResourceScopes(String resourceName) {
        Preconditions.checkNotNull(resourceName);
        Preconditions.checkState(!Strings.isNullOrEmpty(resourceName));
        return resourceCache.getIfPresent(resourceName);

    }

    public static List<Scope> getResourceScopeByType(String resourceName, ProtectionScopeType protectionScopeType) {
        Preconditions.checkNotNull(resourceName);
        Preconditions.checkNotNull(protectionScopeType);
        Map<ProtectionScopeType, List<Scope>> scopeMap = resourceCache.getIfPresent(resourceName);
        if (scopeMap == null) {
            return Collections.emptyList();
        }
        return scopeMap.get(protectionScopeType);
    }

    public static void addScope(String resourceName, ProtectionScopeType protectionScopeType, Scope scope) {
        Preconditions.checkNotNull(resourceName);
        Preconditions.checkNotNull(protectionScopeType);
        Preconditions.checkNotNull(scope);

        switch (protectionScopeType) {
        case GROUP:
            putGroupScope(scope);
            break;

        case SUPER:
            putSuperScope(scope);
            break;

        default:
            putScope(scope);
            break;

        }
    }
}
