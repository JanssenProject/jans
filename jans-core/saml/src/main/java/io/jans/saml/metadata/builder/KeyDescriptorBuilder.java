package io.jans.saml.metadata.builder;

import io.jans.saml.metadata.model.KeyDescriptor;
import io.jans.saml.metadata.model.ds.KeyInfo;
import io.jans.saml.metadata.model.enc.EncryptionMethod;

import io.jans.saml.metadata.builder.ds.KeyInfoBuilder;
import io.jans.saml.metadata.builder.enc.EncryptionMethodBuilder;

public class KeyDescriptorBuilder {

    private KeyDescriptor keyDescriptor;

    public KeyDescriptorBuilder(final KeyDescriptor keyDescriptor) {

        this.keyDescriptor = keyDescriptor;
    }

    public KeyDescriptorBuilder use(final String use) {

        this.keyDescriptor.setUse(use);
        return this;
    }

    public KeyInfoBuilder keyInfo() {

        KeyInfo keyInfo = new KeyInfo();
        this.keyDescriptor.setKeyInfo(keyInfo);
        return new KeyInfoBuilder(keyInfo);
    }

    public EncryptionMethodBuilder encryptionMethod() {

        EncryptionMethod encmethod = new EncryptionMethod();
        this.keyDescriptor.addEncryptionMethod(encmethod);
        return new EncryptionMethodBuilder(encmethod);
    }
}