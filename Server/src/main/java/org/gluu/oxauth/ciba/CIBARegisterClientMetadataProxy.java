/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.gluu.oxauth.interception.CIBARegisterClientMetadataInterception;
import org.gluu.oxauth.interception.CIBARegisterClientMetadataInterceptionInterface;
import org.gluu.oxauth.model.common.BackchannelTokenDeliveryMode;
import org.gluu.oxauth.model.crypto.signature.AsymmetricSignatureAlgorithm;
import org.gluu.oxauth.model.registration.Client;

import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Stateless
@Named
public class CIBARegisterClientMetadataProxy implements CIBARegisterClientMetadataInterceptionInterface {

    @Override
    @CIBARegisterClientMetadataInterception
    public void updateClient(Client client, BackchannelTokenDeliveryMode backchannelTokenDeliveryMode,
                             String backchannelClientNotificationEndpoint, AsymmetricSignatureAlgorithm backchannelAuthenticationRequestSigningAlg,
                             Boolean backchannelUserCodeParameter) {
    }
}