/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version February 17, 2016
 */
public class JSONWebKey implements Comparable<JSONWebKey> {

    private KeyType kty;
    private Use use;
    private String alg;
    private String kid;
    private Long exp;
    private String crv;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private List<String> x5c;

    public JSONWebKey() {
        privateKey = new PrivateKey();
        publicKey = new PublicKey();
    }

    public KeyType getKty() {
        return kty;
    }

    public void setKty(KeyType kty) {
        this.kty = kty;
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

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    /**
     * Returns the Key ID. The Key ID member can be used to match a specific key. This can be used, for instance,
     * to choose among a set of keys within the JWK during key rollover.
     *
     * @return The Key ID.
     */
    public String getKid() {
        return kid;
    }

    /**
     * Sets the Key ID.
     *
     * @param kid The Key ID.
     */
    public void setKid(String kid) {
        this.kid = kid;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    /**
     * Returns the curve member that identifies the cryptographic curve used with the key.
     *
     * @return The curve member that identifies the cryptographic curve used with the key.
     */
    public String getCrv() {
        return crv;
    }

    /**
     * Sets the curve member that identifies the cryptographic curve used with the key.
     *
     * @param crv The curve member that identifies the cryptographic curve used with the key.
     */
    public void setCrv(String crv) {
        this.crv = crv;
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

    public List<String> getX5c() {
        return x5c;
    }

    public void setX5c(List<String> x5c) {
        this.x5c = x5c;
    }

    @Override
    public int compareTo(JSONWebKey o) {
        if (this.getExp() == null || o.getExp() == null) {
            return 0;
        }

        return getExp().compareTo(o.getExp());
    }
}