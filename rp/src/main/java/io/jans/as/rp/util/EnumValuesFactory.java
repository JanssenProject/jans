/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.rp.util;

import io.jans.as.model.common.*;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.register.ApplicationType;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
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
                GrantType.OXAUTH_UMA_TICKET,
                GrantType.CIBA,
                GrantType.DEVICE_CODE
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

    // CIBA
    @Produces
    @Named
    public BackchannelTokenDeliveryMode[] getBachBackchannelTokenDeliveryModes() {
        return BackchannelTokenDeliveryMode.values();
    }

    @Produces
    @Named
    public AsymmetricSignatureAlgorithm[] getAsymmetricSignatureAlgorithms() {
        return AsymmetricSignatureAlgorithm.values();
    }
}