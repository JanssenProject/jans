/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service.verifier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Hex;

import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.fido2.model.auth.AuthData;
import io.jans.fido2.service.AuthenticatorDataParser;
import io.jans.orm.model.fido2.UserVerification;

import org.slf4j.Logger;

/**
 * @author Yuriy Movchan
 * @version May 08, 2020
 */
@ApplicationScoped
public class UserVerificationVerifier {

    @Inject
    private Logger log;

    public boolean verifyUserPresent(AuthData authData) {
        if ((authData.getFlags()[0] & AuthenticatorDataParser.FLAG_USER_PRESENT) != 0) {
            return true;
        } else {
            throw new Fido2RuntimeException("User not present");
        }
    }

    public boolean verifyUserVerified(AuthData authData) {
        return (authData.getFlags()[0] & AuthenticatorDataParser.FLAG_USER_VERIFIED) != 0;
    }

	public void verifyUserVerificationOption(UserVerification userVerification, AuthData authData) {
		if (userVerification == UserVerification.required) {
            verifyRequiredUserPresent(authData);
        }
        if (userVerification == UserVerification.preferred) {
            verifyPreferredUserPresent(authData);
        }
        if (userVerification == UserVerification.discouraged) {
            verifyDiscouragedUserPresent(authData);
        }
	}

    public void verifyRequiredUserPresent(AuthData authData) {
        if (log.isDebugEnabled()) {
            log.debug("Required user present {}", Hex.encodeHexString(authData.getFlags()));
        }
        byte flags = authData.getFlags()[0];

        if (!isUserVerified(flags)) {
            throw new Fido2RuntimeException("User required is not present");
        }
    }

    public void verifyPreferredUserPresent(AuthData authData) {
        if (log.isDebugEnabled()) {
            log.debug("Preferred user present {}", Hex.encodeHexString(authData.getFlags()));
        }
    }

    public void verifyDiscouragedUserPresent(AuthData authData) {
        if (log.isDebugEnabled()) {
            log.debug("Discouraged user present {}", Hex.encodeHexString(authData.getFlags()));
        }
    }

    private boolean isUserVerified(byte flags) {
        boolean uv = (flags & AuthenticatorDataParser.FLAG_USER_VERIFIED) != 0;
        log.debug("UV = {}", uv);

        return uv;
    }

}
