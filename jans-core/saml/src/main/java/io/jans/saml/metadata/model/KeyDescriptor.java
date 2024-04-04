package io.jans.saml.metadata.model;

import io.jans.saml.metadata.model.ds.KeyInfo;
import io.jans.saml.metadata.model.enc.EncryptionMethod;

import java.util.ArrayList;
import java.util.List;

public class KeyDescriptor {
    
    private String use;
    private KeyInfo keyInfo;
    private List<EncryptionMethod> encryptionMethods;

    public KeyDescriptor() {

        this.use = null;
        this.keyInfo = null;
        this.encryptionMethods = new ArrayList<>();
    }

    public String getUse() {

        return this.use;
    }

    public boolean isEncryptionKey() {

        return "encryption".equalsIgnoreCase(use);
    }

    public boolean isSigningKey() {

        return "signing".equalsIgnoreCase(use);
    }

    public void setUse(final String use) {

        this.use = use;
    }

    public void setKeyInfo(final KeyInfo keyInfo) {

        this.keyInfo = keyInfo;
    }

    public KeyInfo getKeyInfo() {

        return this.keyInfo;
    }

    public void addEncryptionMethod(final EncryptionMethod encryptionMethod) {

        this.encryptionMethods.add(encryptionMethod);
    }

    public List<EncryptionMethod> getEncryptionMethods() {

        return this.encryptionMethods;
    }


}