/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ciba;

import io.jans.as.model.configuration.AppConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_AUTHENTICATION_ENDPOINT;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG_VALUES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_TOKEN_DELIVERY_MODES_SUPPORTED;
import static io.jans.as.model.configuration.ConfigurationResponseClaim.BACKCHANNEL_USER_CODE_PAREMETER_SUPPORTED;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Stateless
@Named
public class CIBAConfigurationService {

    private final static Logger log = LoggerFactory.getLogger(CIBAConfigurationService.class);

    @Inject
    private AppConfiguration appConfiguration;

    public void processConfiguration(JSONObject jsonConfiguration) {
        try {
            jsonConfiguration.put(BACKCHANNEL_AUTHENTICATION_ENDPOINT, appConfiguration.getBackchannelAuthenticationEndpoint());

            JSONArray backchannelTokenDeliveryModesSupported = new JSONArray();
            for (String item : appConfiguration.getBackchannelTokenDeliveryModesSupported()) {
                backchannelTokenDeliveryModesSupported.put(item);
            }
            if (backchannelTokenDeliveryModesSupported.length() > 0) {
                jsonConfiguration.put(BACKCHANNEL_TOKEN_DELIVERY_MODES_SUPPORTED, backchannelTokenDeliveryModesSupported);
            }

            JSONArray backchannelAuthenticationRequestSigningAlgValuesSupported = new JSONArray();
            for (String item : appConfiguration.getBackchannelAuthenticationRequestSigningAlgValuesSupported()) {
                backchannelAuthenticationRequestSigningAlgValuesSupported.put(item);
            }
            if (backchannelAuthenticationRequestSigningAlgValuesSupported.length() > 0) {
                jsonConfiguration.put(BACKCHANNEL_AUTHENTICATION_REQUEST_SIGNING_ALG_VALUES_SUPPORTED, backchannelAuthenticationRequestSigningAlgValuesSupported);
            }

            jsonConfiguration.put(BACKCHANNEL_USER_CODE_PAREMETER_SUPPORTED, appConfiguration.getBackchannelUserCodeParameterSupported());
        } catch (Exception e) {
            log.error("Failed to process CIBA configuration.", e);
        }
    }
}