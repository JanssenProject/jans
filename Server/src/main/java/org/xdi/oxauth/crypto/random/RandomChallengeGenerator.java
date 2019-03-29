/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.crypto.random;

import java.security.SecureRandom;

import javax.inject.Named;

@Named("randomChallengeGenerator")
public class RandomChallengeGenerator implements ChallengeGenerator {

    private final SecureRandom random = new SecureRandom();

    @Override
    public byte[] generateChallenge() {
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);

        return randomBytes;
    }
}
