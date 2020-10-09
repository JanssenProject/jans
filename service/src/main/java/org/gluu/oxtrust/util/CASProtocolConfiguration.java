package org.gluu.oxtrust.util;

import org.gluu.config.oxtrust.ShibbolethCASProtocolConfiguration;
import org.gluu.oxtrust.service.CASService;

public class CASProtocolConfiguration {
	private String casBaseURL;
	private ShibbolethCASProtocolConfiguration configuration;

	public CASProtocolConfiguration(String casBaseURL, ShibbolethCASProtocolConfiguration configuration) {
		this.casBaseURL = casBaseURL;
		this.configuration = configuration;
	}

	public String getCasBaseURL() {
		return casBaseURL;
	}

	public void setCasBaseURL(String casBaseURL) {
		this.casBaseURL = casBaseURL;
	}

	public ShibbolethCASProtocolConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(ShibbolethCASProtocolConfiguration configuration) {
		this.configuration = configuration;
	}

	public void save(CASService casService) throws Exception {
		CASProtocolAvailability casProtocolAvailability = CASProtocolAvailability.get();
		if (!casProtocolAvailability.isAvailable()) {
			throw new Exception();
		}
		if (configuration.getInum() == null || configuration.getInum().isEmpty()) {
			casService.addCASConfiguration(configuration);
		} else {
			casService.updateCASConfiguration(configuration);
		}
	}

	public boolean isShibbolethEnabled() {
		return configuration.isEnabled();
	}
}
