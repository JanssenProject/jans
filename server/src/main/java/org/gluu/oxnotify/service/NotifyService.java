/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.service;

import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxnotify.model.PushPlatform;
import org.gluu.oxnotify.model.conf.Configuration;
import org.gluu.oxnotify.model.sns.ClientData;
import org.slf4j.Logger;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@Named
public class NotifyService {

	@Inject
	private Logger log;

	@Inject
	private Configuration configuration;

	public boolean processAuthorization(String authorization) {
		return true;
	}

	public ClientData getClientData(String authorization) {
		BasicAWSCredentials credentials = new BasicAWSCredentials("", "");
		AmazonSNSAsyncClientBuilder snsClientBuilder = AmazonSNSAsyncClientBuilder.standard();

		AmazonSNSAsync snsClient = snsClientBuilder.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();

		return new ClientData(snsClient, PushPlatform.GCM, "");
	}

}
