/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.model.conf;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.enterprise.inject.Vetoed;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@Vetoed
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessConfiguration {
	
    @JsonProperty("protection_mechanism")
    private ProtectionMechanismType protectionMechanism = null;

	private List<ClientConfiguration> clientConfigurations;

	public List<ClientConfiguration> getClientConfigurations() {
		return clientConfigurations;
	}

	public void setClientConfigurations(List<ClientConfiguration> clientConfigurations) {
		this.clientConfigurations = clientConfigurations;
	}

	public ProtectionMechanismType getProtectionMechanism() {
		return protectionMechanism;
	}

	public void setProtectionMechanism(ProtectionMechanismType protectionMechanism) {
		this.protectionMechanism = protectionMechanism;
	}

}
