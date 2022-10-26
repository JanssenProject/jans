/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.crypto.random;

import jakarta.inject.Named;
import java.security.SecureRandom;

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
