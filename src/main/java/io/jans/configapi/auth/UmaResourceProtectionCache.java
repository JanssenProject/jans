package io.jans.configapi.auth;

import io.jans.as.model.uma.persistence.UmaResource;
import io.jans.as.persistence.model.Scope;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
//import com.google.inject.Inject;

import org.slf4j.Logger;

@ApplicationScoped
@Named
public class UmaResourceProtectionCache {

	@Inject
	Logger log;

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
		if (scopeCache.getIfPresent(scope.getDisplayName()) == null) {
			scopeCache.put(scope.getDisplayName(), scope);
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

	public static void putUmaResource(UmaResource umaResource) {
		Preconditions.checkNotNull(umaResource);
		if (umaResourceCache.getIfPresent(umaResource.getName()) == null) {
			umaResourceCache.put(umaResource.getName(), umaResource);
		}
	}

	public static Map<String, UmaResource> getAllUmaResources() {
		return Maps.newHashMap(umaResourceCache.asMap());
	}

}
