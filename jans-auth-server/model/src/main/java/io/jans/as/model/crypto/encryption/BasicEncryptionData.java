package io.jans.as.model.crypto.encryption;

public class BasicEncryptionData {
    private String name;
    private String family;
    private String algorithm;
    private int cmkLength;
    private int initVectorLength;

    public BasicEncryptionData(String name, String family, String algorithm, int cmkLength, int initVectorLength) {
        this.name = name;
        this.family = family;
        this.algorithm = algorithm;
        this.cmkLength = cmkLength;
        this.initVectorLength = initVectorLength;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getCmkLength() {
        return cmkLength;
    }

    public void setCmkLength(int cmkLength) {
        this.cmkLength = cmkLength;
    }

    public int getInitVectorLength() {
        return initVectorLength;
    }

    public void setInitVectorLength(int initVectorLength) {
        this.initVectorLength = initVectorLength;
    }
}
