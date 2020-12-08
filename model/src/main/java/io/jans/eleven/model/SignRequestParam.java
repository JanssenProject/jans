/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.model;

/**
 * @author Javier Rojas Blum
 * @version April 19, 2016
 */
public class SignRequestParam {

    private String signingInput;
    private String alias;
    private String signatureAlgorithm;
    private String sharedSecret;

    public String getSigningInput() {
        return signingInput;
    }

    public void setSigningInput(String signingInput) {
        this.signingInput = signingInput;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}
