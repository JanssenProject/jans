/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.interception.CIBARegisterClientMetadataInterception;
import org.gluu.oxauth.interception.CIBARegisterClientMetadataInterceptionInterface;
import org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.crypto.signature.AsymmetricSignatureAlgorithm;
import org.gluu.oxauth.model.registration.Client;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
@Interceptor
@CIBARegisterClientMetadataInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBARegisterClientMetadataInterceptor implements CIBARegisterClientMetadataInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBARegisterClientMetadataInterceptor.class);

    @Inject
    private AppConfiguration appConfiguration;

    public CIBARegisterClientMetadataInterceptor() {
        log.info("CIBA Register Client Metadata Interceptor loaded.");
    }

    @AroundInvoke
    public Object updateClient(InvocationContext ctx) {
        log.debug("CIBA: update client registration");

        boolean valid = false;
        try {
            Client client = (Client) ctx.getParameters()[0];
            BackchannelTokenDeliveryMode backchannelTokenDeliveryMode = (BackchannelTokenDeliveryMode) ctx.getParameters()[1];
            String backchannelClientNotificationEndpoint = (String) ctx.getParameters()[2];
            AsymmetricSignatureAlgorithm backchannelAuthenticationRequestSigningAlg = (AsymmetricSignatureAlgorithm) ctx.getParameters()[3];
            Boolean backchannelUserCodeParameter = (Boolean) ctx.getParameters()[4];
            updateClient(client, backchannelTokenDeliveryMode, backchannelClientNotificationEndpoint,
                    backchannelAuthenticationRequestSigningAlg, backchannelUserCodeParameter);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to retrieve params.", e);
        }

        return valid;
    }

    @Override
    public void updateClient(
            Client client, BackchannelTokenDeliveryMode backchannelTokenDeliveryMode,
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
        if (backchannelUserCodeParameter != null) {
            client.setBackchannelUserCodeParameter(backchannelUserCodeParameter);
        }
    }
}