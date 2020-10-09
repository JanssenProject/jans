package io.jans.scim.util;

import io.jans.scim.util.ProductInstallationChecker;

public enum CASProtocolAvailability {
	ENABLED, DISABLED;

	public static CASProtocolAvailability get() {
		boolean enabled = !ProductInstallationChecker.isGluuCE()
				|| ProductInstallationChecker.isShibbolethIDP3Installed();
		return from(enabled);
	}

	public static CASProtocolAvailability from(boolean enabled) {
		return enabled ? ENABLED : DISABLED;
	}

	public boolean isAvailable() {
		return this == ENABLED;
	}
}