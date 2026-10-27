/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.util;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.CryptoProviderFactory;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;

import java.security.KeyStoreException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * @author Javier Rojas Blum
 * @version July 31, 2016
 */
public class SubjectIdentifierGenerator {

    private static final int EPHEMERAL_SUBJECT_IDENTIFIER_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SubjectIdentifierGenerator() {
    }

    public static String generateEphemeralSubjectIdentifier() {
        final byte[] bytes = new byte[EPHEMERAL_SUBJECT_IDENTIFIER_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generatePairwiseSubjectIdentifier(String sectorIdentifier, String localAccountId, String key,
                                                           String salt, AppConfiguration configuration) throws KeyStoreException, CryptoProviderException {
        AbstractCryptoProvider cryptoProvider = CryptoProviderFactory.getCryptoProvider(configuration);

        String signingInput = sectorIdentifier + localAccountId + salt;
        return cryptoProvider.sign(signingInput, null, key, SignatureAlgorithm.HS256);
    }
}
