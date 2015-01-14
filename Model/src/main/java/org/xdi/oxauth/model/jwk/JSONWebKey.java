/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version 0.9 December 9, 2014
 */
public class JSONWebKey implements Comparable<JSONWebKey> {

    private KeyType keyType;
    private Use use;
    private String algorithm;
    private String keyId;
    private Long expirationTime;
    private String curve;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private List<String> certificateChain;

    public JSONWebKey() {
        privateKey = new PrivateKey();
        publicKey = new PublicKey();
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public void setKeyType(KeyType keyType) {
        this.keyType = keyType;
    }

    /**
     * Returns the intended use of the key: signature or encryption.
     *
     * @return The intended use of the key.
     */
    public Use getUse() {
        return use;
    }

    /**
     * Sets the intended use of the key: signature or encryption.
     *
     * @param use The intended use of the key.
     */
    public void setUse(Use use) {
        this.use = use;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * Returns the Key ID. The Key ID member can be used to match a specific key. This can be used, for instance,
     * to choose among a set of keys within the JWK during key rollover.
     *
     * @return The Key ID.
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Sets the Key ID.
     *
     * @param keyId The Key ID.
     */
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     * Returns the curve member that identifies the cryptographic curve used with the key.
     *
     * @return The curve member that identifies the cryptographic curve used with the key.
     */
    public String getCurve() {
        return curve;
    }

    /**
     * Sets the curve member that identifies the cryptographic curve used with the key.
     *
     * @param curve The curve member that identifies the cryptographic curve used with the key.
     */
    public void setCurve(String curve) {
        this.curve = curve;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public List<String> getCertificateChain() {
        return certificateChain;
    }

    public void setCertificateChain(List<String> certificateChain) {
        this.certificateChain = certificateChain;
    }

    @Override
    public int compareTo(JSONWebKey o) {
        if (this.getExpirationTime() == null || o.getExpirationTime() == null) {
            return 0;
        }

        return getExpirationTime().compareTo(o.getExpirationTime());
    }
}