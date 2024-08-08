package io.jans.fido2.model.common;

import com.google.common.base.Strings;

public class PublicKeyCredentialParameters {
    private int alg;
    private String type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();

    public PublicKeyCredentialParameters() {
    }

    public PublicKeyCredentialParameters(int alg) {
        this.alg = alg;
        this.type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();
    }

    public int getAlg() {
        return alg;
    }

    public void setAlg(int alg) {
        this.alg = alg;
    }

    public String getType() {
        return type;
    }

    public static PublicKeyCredentialParameters createPublicKeyCredentialParameters(int alg) {
        return new PublicKeyCredentialParameters(alg);
    }

    @Override
    public String toString() {
        return "PublicKeyCredentialParameters{" +
                "alg='" + alg + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
