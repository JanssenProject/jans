package io.jans.configapi.security.api;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import io.jans.as.persistence.model.Scope;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
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

    private static final Cache<String, List<Scope>> resourceCache = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_LIFETIME, TimeUnit.MINUTES).build();
    
    private static final Cache<String, Map<String,List<Scope>>> resourceScopeCache = CacheBuilder.newBuilder()
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

    // Resource
    public static void removeAllResources() {
        resourceCache.invalidateAll();
    }

    public static List<Scope> getResourceScopes(String resourceName) {
        Preconditions.checkNotNull(resourceName);
        Preconditions.checkState(!Strings.isNullOrEmpty(resourceName));
        return resourceCache.getIfPresent(resourceName);

    }

    public static void putResource(String resourceName, List<Scope> scopeList) {
        Preconditions.checkNotNull(resourceName);
        resourceCache.put(resourceName, scopeList);
    }

    public static Map<String, List<Scope>> getAllResources() {
        return Maps.newHashMap(resourceCache.asMap());
    }
    
    public static void putResource(String resourceName, Map<String,List<Scope>> scopeMap) {
        Preconditions.checkNotNull(resourceName);
        resourceScopeCache.put(resourceName, scopeMap);
    }
    
    public static void putResourceScopeByType(String resourceName, String scopeType, List<Scope> scopes) {
        Preconditions.checkNotNull(resourceName);
        Preconditions.checkNotNull(scopeType);
        Map<String, List<Scope>> scopeMap = resourceScopeCache.getIfPresent(resourceName);
        if(scopeMap==null) {
            scopeMap = new HashMap<>();            
        }           
       scopeMap.put(scopeType, scopes);
    }

    public static List<Scope> getResourceScopeByType(String resourceName, String scopeType) {
        Preconditions.checkNotNull(resourceName);
        Preconditions.checkNotNull(scopeType);
        Map<String, List<Scope>> scopeMap = resourceScopeCache.getIfPresent(resourceName);
        if(scopeMap==null) {
            return null;
        }           
        return scopeMap.get(scopeType);
    }
}
