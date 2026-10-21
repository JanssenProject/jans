/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.testkit;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.jans.core.cedarling.service.policy.PolicyDownloadService;
import io.jans.core.cedarling.service.policy.PolicyDownloadService.LoadedPolicySource;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.trace.correlation.PolicyMismatchCheck;

/**
 * Builds a {@link PolicyMismatchCheck} over a fixed set of served policy stores.
 */
public final class PolicyMismatchChecks {

	private PolicyMismatchChecks() {
	}

	public static PolicyMismatchCheck none(AppConfiguration appConfiguration) {
		return serving(appConfiguration, new StaticPolicyDownloadService());
	}

	public static PolicyMismatchCheck serving(AppConfiguration appConfiguration, StaticPolicyDownloadService served) {
		PolicyMismatchCheck check = new PolicyMismatchCheck();
		setField(check, "policyDownloadService", served);
		setField(check, "appConfiguration", appConfiguration);
		return check;
	}

	public static final class StaticPolicyDownloadService extends PolicyDownloadService {

		private final Map<String, LoadedPolicySource> policies = new HashMap<>();

		public StaticPolicyDownloadService archive(String policyStoreId, String version, String bundleHash) {
			LoadedPolicySource source = new LoadedPolicySource(policyStoreId, new byte[] { 1 }, bundleHash,
					"https://policy.example.org/" + policyStoreId);
			source.setPolicyStoreVersion(version);
			policies.put(policyStoreId, source);
			return this;
		}

		public StaticPolicyDownloadService json(String policyStoreUri) {
			policies.put(policyStoreUri, new LoadedPolicySource(policyStoreUri, "{}"));
			return this;
		}

		@Override
		public Map<String, LoadedPolicySource> getLoadedPolicies() {
			return Collections.unmodifiableMap(policies);
		}

	}

	private static void setField(Object target, String fieldName, Object value) {
		try {
			Field field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}

}
