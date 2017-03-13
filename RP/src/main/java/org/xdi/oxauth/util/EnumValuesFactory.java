/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.util;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.Display;
import org.xdi.oxauth.model.common.GrantType;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.common.SubjectType;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.register.ApplicationType;

/**
 * @author Javier Rojas Blum Date: 09.13.2013
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
        return new GrantType[]{GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT};
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