/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package io.jans.notify.model.sns;

import io.jans.notify.model.PushPlatform;

import com.amazonaws.services.sns.AmazonSNS;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
public class ClientData {

	private AmazonSNS snsClient;

	private PushPlatform platform;
	private String platformApplicationArn;

	public ClientData(AmazonSNS amazonSNS, PushPlatform platform, String platformApplicationArn) {
		this.snsClient = amazonSNS;
		this.platform = platform;
		this.platformApplicationArn = platformApplicationArn;
	}

	public AmazonSNS getSnsClient() {
		return snsClient;
	}

	public void setSnsClient(AmazonSNS snsClient) {
		this.snsClient = snsClient;
	}

	public PushPlatform getPlatform() {
		return platform;
	}

	public void setPlatform(PushPlatform platform) {
		this.platform = platform;
	}

	public String getPlatformApplicationArn() {
		return platformApplicationArn;
	}

	public void setPlatformApplicationArn(String platformApplicationArn) {
		this.platformApplicationArn = platformApplicationArn;
	}

}
