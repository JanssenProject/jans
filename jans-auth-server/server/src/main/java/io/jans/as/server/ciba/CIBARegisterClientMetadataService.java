/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ciba;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.BackchannelTokenDeliveryMode;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Stateless
@Named
public class CIBARegisterClientMetadataService {

    @Inject
    private AppConfiguration appConfiguration;

    public void updateClient(Client client, BackchannelTokenDeliveryMode backchannelTokenDeliveryMode,
                             String backchannelClientNotificationEndpoint, AsymmetricSignatureAlgorithm backchannelAuthenticationRequestSigningAlg,
                             Boolean backchannelUserCodeParameter) {
        if (backchannelTokenDeliveryMode != null) {
            client.setBackchannelTokenDeliveryMode(backchannelTokenDeliveryMode);
        }
        if (StringUtils.isNotBlank(backchannelClientNotificationEndpoint)) {
            client.setBackchannelClientNotificationEndpoint(backchannelClientNotificationEndpoint);
        }
        if (backchannelAuthenticationRequestSigningAlg != null) {
            client.setBackchannelAuthenticationRequestSigningAlg(backchannelAuthenticationRequestSigningAlg);
        }
        if (BooleanUtils.isTrue(appConfiguration.getBackchannelUserCodeParameterSupported())
                && backchannelUserCodeParameter != null) {
            client.setBackchannelUserCodeParameter(backchannelUserCodeParameter);
        }
    }
}