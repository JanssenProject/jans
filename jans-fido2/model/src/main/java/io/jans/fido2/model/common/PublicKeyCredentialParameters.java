package io.jans.fido2.model.common;

public class PublicKeyCredentialParameters {
    private int alg;
    private String type = PublicKeyCredentialType.PUBLIC_KEY.getKeyName();

    public PublicKeyCredentialParameters() {
    }

    public PublicKeyCredentialParameters(int alg, String type) {
        this.alg = alg;
        this.type = type;
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

    public void setType(String type) {
        this.type = type;
    }

    public static PublicKeyCredentialParameters createPublicKeyCredentialParameters(int alg, String type) {
        PublicKeyCredentialParameters instance = new PublicKeyCredentialParameters(alg, type);
        return instance;
    }

    @Override
    public String toString() {
        return "PublicKeyCredentialParameters{" +
                "alg='" + alg + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
