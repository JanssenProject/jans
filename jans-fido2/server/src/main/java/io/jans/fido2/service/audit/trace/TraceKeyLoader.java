/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.function.UnaryOperator;

import io.jans.fido2.model.conf.Fido2Configuration;
import io.jans.util.security.SecurityProviderUtility;

/**
 * Loads the Ed25519 TRACE producer key from a BCFKS keystore file.
 *
 * <p>The configuration holds only the keystore path, the alias and an encrypted keystore password. The key
 * material stays in the keystore file, so it is never persisted or returned by the configuration API.
 */
public final class TraceKeyLoader {

    static final String KEYSTORE_TYPE = "BCFKS";
    static final String KEY_ALGORITHM = "Ed25519";

    private TraceKeyLoader() {
    }

    /**
     * Loads the signing key named by the configuration.
     *
     * @param configuration FIDO2 configuration holding the keystore path, alias and encrypted password
     * @param passwordDecryptor decrypts the stored keystore password, for example with the encryption service
     * @return the Ed25519 private key
     * @throws IllegalStateException if a required property is missing or the key cannot be loaded
     */
    public static PrivateKey fromConfiguration(Fido2Configuration configuration, UnaryOperator<String> passwordDecryptor) {
        String path = configuration.getFido2TraceKeyStorePath();
        String alias = configuration.getFido2TraceKeyAlias();
        String storedPassword = configuration.getFido2TraceKeyStorePassword();
        if (isBlank(path) || isBlank(alias) || isBlank(storedPassword)) {
            throw new IllegalStateException("fido2TraceKeyStorePath, fido2TraceKeyAlias and fido2TraceKeyStorePassword are required");
        }
        String password = passwordDecryptor.apply(storedPassword);
        return load(path, password.toCharArray(), alias);
    }

    /**
     * @param path BCFKS keystore file
     * @param password keystore password
     * @param alias key alias
     * @return the Ed25519 private key stored under the alias
     * @throws IllegalStateException if the file, alias or key type is wrong
     */
    public static PrivateKey load(String path, char[] password, String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, SecurityProviderUtility.getBCProvider());
            try (InputStream in = Files.newInputStream(Paths.get(path))) {
                keyStore.load(in, password);
            }
            Key key = keyStore.getKey(alias, password);
            if (!(key instanceof PrivateKey) || !KEY_ALGORITHM.equals(key.getAlgorithm())) {
                throw new IllegalStateException("TRACE signing key '" + alias + "' is not an Ed25519 private key");
            }
            return (PrivateKey) key;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Cannot load TRACE signing key '" + alias + "'", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
