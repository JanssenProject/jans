package org.xdi.oxauth.model.jwk;

/**
 * @author Javier Rojas Blum Date: 04.26.2012
 */
public class PrivateKey {

    private String modulus;
    private String privateExponent;
    private String d;

    public String getModulus() {
        return modulus;
    }

    public void setModulus(String modulus) {
        this.modulus = modulus;
    }

    public String getPrivateExponent() {
        return privateExponent;
    }

    public void setPrivateExponent(String privateExponent) {
        this.privateExponent = privateExponent;
    }

    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }
}