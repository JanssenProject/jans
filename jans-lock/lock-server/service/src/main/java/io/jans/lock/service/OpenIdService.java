/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.lock.service;

import java.io.Serializable;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import io.jans.as.client.OpenIdConfigurationClient;
import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.lock.model.config.AppConfiguration;
import io.jans.util.StringHelper;
import io.jans.util.exception.InvalidConfigurationException;
import io.jans.util.exception.MissingResourceException;
import io.jans.util.init.Initializable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides OpenId configuration
 *
 * @author Yuriy Movchan Date: 12/28/2016
 */
@ApplicationScoped
public class OpenIdService extends Initializable implements Serializable {

    private static final long serialVersionUID = 7875838160379126796L;

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    private OpenIdConfigurationResponse openIdConfiguration;

    @Override
    protected void initInternal() {
        loadOpenIdConfiguration();
    }

	private void loadOpenIdConfiguration() {
		String openIdIssuer = appConfiguration.getOpenIdIssuer();
		if (StringHelper.isEmpty(openIdIssuer)) {
			throw new InvalidConfigurationException("OpenIdIssuer Url is invalid");
		}

		String openIdIssuerEndpoint = openIdIssuer + "/.well-known/openid-configuration";

		OpenIdConfigurationClient client = new OpenIdConfigurationClient(openIdIssuerEndpoint);
		openIdConfiguration = client.execOpenIdConfiguration();

		if ((openIdConfiguration == null) || (openIdConfiguration.getStatus() != HttpStatus.SC_OK)) {
			throw new MissingResourceException("Failed to load OpenID configuration!");
		}


		log.info("Successfully loaded OpenID configuration");
	}

    public OpenIdConfigurationResponse getOpenIdConfiguration() {
        // Call each time to allows retry
        init();

        return openIdConfiguration;
    }

}
