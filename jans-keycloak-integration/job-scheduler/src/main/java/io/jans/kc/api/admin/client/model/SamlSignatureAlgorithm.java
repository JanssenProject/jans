package io.jans.kc.api.admin.client.model;


public enum SamlSignatureAlgorithm {
    RSA_SHA1("RSA_SHA1"),
    RSA_SHA256("RSA_SHA256"),
    RSA_SHA256_MGF1("RSA_SHA256_MGF1"),
    RSA_SHA512("RSA_SHA512"),
    RSA_SHA512_MGF1("RSA_SHA512_MGF1"),
    DSA_SHA1("DSA_SHA1");

    private final String value;

    private SamlSignatureAlgorithm(final String value) {
        this.value = value;
    }

    public String value() {

        return this.value;
    }

    public static SamlSignatureAlgorithm fromString(final String value) {

        for(SamlSignatureAlgorithm alg : SamlSignatureAlgorithm.values()) {
            if(alg.equals(value)) {
                return alg;
            }
        }
        return null;
    }
}
