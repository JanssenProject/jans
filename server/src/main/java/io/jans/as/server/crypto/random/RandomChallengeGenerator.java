/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.crypto.random;

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
