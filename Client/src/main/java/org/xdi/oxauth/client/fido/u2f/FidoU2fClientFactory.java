/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.fido.u2f;

import org.jboss.resteasy.client.ProxyFactory;
import org.xdi.oxauth.model.fido.u2f.U2fConfiguration;

/**
 * Helper class which creates proxy FIDO U2F services
 *
 * @author Yuriy Movchan Date: 05/27/2015
 */
public class FidoU2fClientFactory {

    private final static FidoU2fClientFactory instance = new FidoU2fClientFactory();

    private FidoU2fClientFactory() {
    }

    public static FidoU2fClientFactory instance() {
        return instance;
    }

    public U2fConfigurationService createMetaDataConfigurationService(String u2fMetaDataUri) {
        return ProxyFactory.create(U2fConfigurationService.class, u2fMetaDataUri);
    }

    public AuthenticationRequestService createAuthenticationRequestService(U2fConfiguration metadataConfiguration) {
        return ProxyFactory.create(AuthenticationRequestService.class, metadataConfiguration.getAuthenticationEndpoint());
    }

    public RegistrationRequestService createRegistrationRequestService(U2fConfiguration metadataConfiguration) {
        return ProxyFactory.create(RegistrationRequestService.class, metadataConfiguration.getRegistrationEndpoint());
    }

}
