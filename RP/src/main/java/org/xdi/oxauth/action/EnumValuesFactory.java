/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.action;

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
	@Named("responseTypes")
    public ResponseType[] responseTypes() {
        return ResponseType.values();
    }

    @Produces
	@Named
    public GrantType[] getGrantTypes() {
        return new GrantType[]{GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT};
    }

    @Produces
	@Named("applicationTypes")
    public ApplicationType[] applicationTypes() {
        return ApplicationType.values();
    }

    @Produces
	@Named("authenticationMethods")
    public AuthenticationMethod[] authenticationMethods() {
        return AuthenticationMethod.values();
    }

    @Produces
	@Named("subjectTypes")
    public SubjectType[] subjectTypes() {
        return SubjectType.values();
    }

    @Produces
	@Named("displays")
    public Display[] displays() {
        return Display.values();
    }

    @Produces
	@Named("prompts")
    public Prompt[] prompts() {
        return Prompt.values();
    }

    @Produces
	@Named("signatureAlgorithms")
    public SignatureAlgorithm[] signatureAlgorithms() {
        return SignatureAlgorithm.values();
    }

    @Produces
	@Named("keyEncryptionAlgorithms")
    public KeyEncryptionAlgorithm[] keyEncryptionAlgorithms() {
        return KeyEncryptionAlgorithm.values();
    }

    @Produces
	@Named("blockEncryptionAlgorithms")
    public BlockEncryptionAlgorithm[] blockEncryptionAlgorithms() {
        return BlockEncryptionAlgorithm.values();
    }
}