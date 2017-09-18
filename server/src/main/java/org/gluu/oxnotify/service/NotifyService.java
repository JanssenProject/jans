/*
 * oxNotify is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */

package org.gluu.oxnotify.service;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Base64;
import org.gluu.oxnotify.model.conf.ClientConfiguration;
import org.gluu.oxnotify.model.conf.Configuration;
import org.gluu.oxnotify.model.sns.ClientData;
import org.slf4j.Logger;

/**
 * @author Yuriy Movchan
 * @version September 15, 2017
 */
@Named
public class NotifyService {

	@Inject
	private Logger log;

	@Inject
	private ApplicationService applicationService;

	@Inject
	private Configuration configuration;

	public ClientConfiguration processAuthorization(String authorization) {
        if ((authorization == null) || !authorization.startsWith("Basic ")) {
			log.error("Client authorization header is invalid");
        	return null;
        }
        String base64Token = authorization.substring(6);
        String token;
		try {
			token = new String(Base64.decodeBase64(base64Token), "utf-8");
		} catch (UnsupportedEncodingException ex) {
			log.error("Failed to parse client authorization header", ex);
			return null;
		}

        String accessKeyId = "";
        String secretAccessKey = "";
        int delim = token.indexOf(":");

        if (delim != -1) {
        	accessKeyId = token.substring(0, delim);
        	secretAccessKey = token.substring(delim + 1);
        }

        ClientConfiguration clientConfiguration = applicationService.getAccessClient(accessKeyId, secretAccessKey);

		return clientConfiguration;
	}

	public ClientData getClientData(ClientConfiguration clientConfiguration) {
		String platformId = clientConfiguration.getPlatformId().toLowerCase();
		ClientData clientData = applicationService.getClientDataByPlatformId(platformId);

		return clientData;
	}

}
