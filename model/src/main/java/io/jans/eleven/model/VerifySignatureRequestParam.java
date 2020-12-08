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
public class VerifySignatureRequestParam {

    private String signingInput;
    private String signature;
    private String alias;
    private JwksRequestParam jwksRequestParam;
    private String sharedSecret;
    private String signatureAlgorithm;

    public String getSigningInput() {
        return signingInput;
    }

    public void setSigningInput(String signingInput) {
        this.signingInput = signingInput;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public JwksRequestParam getJwksRequestParam() {
        return jwksRequestParam;
    }

    public void setJwksRequestParam(JwksRequestParam jwksRequestParam) {
        this.jwksRequestParam = jwksRequestParam;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }
}
