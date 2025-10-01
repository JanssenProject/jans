package io.jans.saml.metadata.model.enc;

public class EncryptionMethod {

    private String algorithm;
    private Integer keySize;
    private String oaepParams;

    public EncryptionMethod() {

        this.algorithm = null;
        this.keySize = null;
        this.oaepParams = null;
    }

    public String getAlgorithm() {

        return this.algorithm;
    }

    public void setAlgorithm(final String algorithm) {

        this.algorithm = algorithm;
    }

    public Integer getKeySize() {
        
        return this.keySize;
    }

    public void setKeySize(final Integer keySize) {

        this.keySize = keySize;
    }

    public String getOaepParams() {

        return this.oaepParams;
    }

    public void setOaepParams(final String oaepParams) {

        this.oaepParams = oaepParams;
    }
}