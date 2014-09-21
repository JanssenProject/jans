package org.xdi.oxd.licenser.server;

import java.math.BigInteger;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2014
 */

public class LicenseGeneratorInput {

    private BigInteger publicKeyModulus;
    private BigInteger publicKeyExponent;
    private BigInteger privateKeyModulus;
    private BigInteger privateKeyExponent;
    private String privatePassword;
    private String publicPassword;
    private String licensePassword;

    public String getLicensePassword() {
        return licensePassword;
    }

    public void setLicensePassword(String licensePassword) {
        this.licensePassword = licensePassword;
    }

    public BigInteger getPrivateKeyExponent() {
        return privateKeyExponent;
    }

    public void setPrivateKeyExponent(BigInteger privateKeyExponent) {
        this.privateKeyExponent = privateKeyExponent;
    }

    public BigInteger getPrivateKeyModulus() {
        return privateKeyModulus;
    }

    public void setPrivateKeyModulus(BigInteger privateKeyModulus) {
        this.privateKeyModulus = privateKeyModulus;
    }

    public String getPrivatePassword() {
        return privatePassword;
    }

    public void setPrivatePassword(String privatePassword) {
        this.privatePassword = privatePassword;
    }

    public BigInteger getPublicKeyExponent() {
        return publicKeyExponent;
    }

    public void setPublicKeyExponent(BigInteger publicKeyExponent) {
        this.publicKeyExponent = publicKeyExponent;
    }

    public BigInteger getPublicKeyModulus() {
        return publicKeyModulus;
    }

    public void setPublicKeyModulus(BigInteger publicKeyModulus) {
        this.publicKeyModulus = publicKeyModulus;
    }

    public String getPublicPassword() {
        return publicPassword;
    }

    public void setPublicPassword(String publicPassword) {
        this.publicPassword = publicPassword;
    }

}
