package org.xdi.oxauth.action;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Name;
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
@Name("enumValuesFactory")
@AutoCreate
public class EnumValuesFactory {

    @Factory("responseTypes")
    public ResponseType[] responseTypes() {
        return ResponseType.values();
    }

    @Factory("grantTypes")
    public GrantType[] grantTypes() {
        return new GrantType[]{GrantType.AUTHORIZATION_CODE, GrantType.IMPLICIT};
    }

    @Factory("applicationTypes")
    public ApplicationType[] applicationTypes() {
        return ApplicationType.values();
    }

    @Factory("authenticationMethods")
    public AuthenticationMethod[] authenticationMethods() {
        return AuthenticationMethod.values();
    }

    @Factory("subjectTypes")
    public SubjectType[] subjectTypes() {
        return SubjectType.values();
    }

    @Factory("displays")
    public Display[] displays() {
        return Display.values();
    }

    @Factory("prompts")
    public Prompt[] prompts() {
        return Prompt.values();
    }

    @Factory("signatureAlgorithms")
    public SignatureAlgorithm[] signatureAlgorithms() {
        return SignatureAlgorithm.values();
    }

    @Factory("keyEncryptionAlgorithms")
    public KeyEncryptionAlgorithm[] keyEncryptionAlgorithms() {
        return KeyEncryptionAlgorithm.values();
    }

    @Factory("blockEncryptionAlgorithms")
    public BlockEncryptionAlgorithm[] blockEncryptionAlgorithms() {
        return BlockEncryptionAlgorithm.values();
    }
}