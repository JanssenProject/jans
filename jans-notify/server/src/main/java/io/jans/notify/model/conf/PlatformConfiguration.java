/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.notify.model.conf;

import jakarta.enterprise.inject.Vetoed;

import io.jans.notify.model.PushPlatform;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@Vetoed
public class PlatformConfiguration {

	private String platformId;
	private PushPlatform platform;
	private String platformArn;

	private String accessKeyId;
	private String secretAccessKey;

	private String region;

	private boolean enabled;

	public String getPlatformId() {
		return platformId;
	}

	public void setPlatformId(String platformId) {
		this.platformId = platformId;
	}

	public PushPlatform getPlatform() {
		return platform;
	}

	public void setPlatform(PushPlatform platform) {
		this.platform = platform;
	}

	public String getPlatformArn() {
		return platformArn;
	}

	public void setPlatformArn(String platformArn) {
		this.platformArn = platformArn;
	}

	public String getAccessKeyId() {
		return accessKeyId;
	}

	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	public String getSecretAccessKey() {
		return secretAccessKey;
	}

	public void setSecretAccessKey(String secretAccessKey) {
		this.secretAccessKey = secretAccessKey;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
