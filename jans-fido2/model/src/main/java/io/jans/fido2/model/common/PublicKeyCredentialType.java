package io.jans.fido2.model.common;

public enum PublicKeyCredentialType {
    PUBLIC_KEY("public-key");

    private String keyName;

    PublicKeyCredentialType(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyName() {
        return this.keyName;
    }

}
