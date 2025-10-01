package io.jans.saml.metadata.builder.enc;

import io.jans.saml.metadata.model.enc.EncryptionMethod;

public class EncryptionMethodBuilder {

    private EncryptionMethod encryptionMethod;

    public EncryptionMethodBuilder(final EncryptionMethod encryptionMethod) {

        this.encryptionMethod = encryptionMethod;
    }

    public EncryptionMethodBuilder algorithm(final String algorithm) {

        this.encryptionMethod.setAlgorithm(algorithm);
        return this;
    }

    public EncryptionMethodBuilder keySize(final Integer keySize) {

        this.encryptionMethod.setKeySize(keySize);
        return this;
    }

    public EncryptionMethodBuilder oaepParams(final String oaepParams) {

        this.encryptionMethod.setOaepParams(oaepParams);
        return this;
    }
}