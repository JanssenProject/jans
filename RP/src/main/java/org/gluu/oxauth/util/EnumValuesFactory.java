/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.util;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.gluu.oxauth.model.register.ApplicationType;

/**
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public class EnumValuesFactory {

    @Produces
    @Named
    public ResponseType[] getResponseTypes() {
        return ResponseType.values();
    }

    @Produces
    @Named
    public GrantType[] getGrantTypes() {
        return new GrantType[]{
                GrantType.AUTHORIZATION_CODE,
                GrantType.IMPLICIT,
                GrantType.RESOURCE_OWNER_PASSWORD_CREDENTIALS,
                GrantType.CLIENT_CREDENTIALS,
                GrantType.REFRESH_TOKEN,
                GrantType.OXAUTH_UMA_TICKET
        };
    }

    @Produces
    @Named
    public ApplicationType[] getApplicationTypes() {
        return ApplicationType.values();
    }

    @Produces
    @Named
    public AuthenticationMethod[] getAuthenticationMethods() {
        return AuthenticationMethod.values();
    }

    @Produces
    @Named
    public SubjectType[] getSubjectTypes() {
        return SubjectType.values();
    }

    @Produces
    @Named
    public Display[] getDisplays() {
        return Display.values();
    }

    @Produces
    @Named
    public Prompt[] getPrompts() {
        return Prompt.values();
    }

    @Produces
    @Named
    public SignatureAlgorithm[] getSignatureAlgorithms() {
        return SignatureAlgorithm.values();
    }

    @Produces
    @Named
    public KeyEncryptionAlgorithm[] getKeyEncryptionAlgorithms() {
        return KeyEncryptionAlgorithm.values();
    }

    @Produces
    @Named
    public BlockEncryptionAlgorithm[] getBlockEncryptionAlgorithms() {
        return BlockEncryptionAlgorithm.values();
    }
}