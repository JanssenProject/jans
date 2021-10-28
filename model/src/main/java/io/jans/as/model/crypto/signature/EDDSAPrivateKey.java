/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */
package io.jans.as.model.crypto.signature;

import static io.jans.as.model.jwk.JWKParameter.EXPONENT;
import static io.jans.as.model.jwk.JWKParameter.MODULUS;
import static io.jans.as.model.jwk.JWKParameter.D;
import static io.jans.as.model.jwk.JWKParameter.X;

import java.io.IOException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.json.JSONException;
import org.json.JSONObject;

import io.jans.as.model.crypto.PrivateKey;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.StringUtils;

/**
 * Edwards Curve Digital Signature Algorithm (EDDSA)
 * Private Key
 *
 * @author Sergey Manoylo
 * @version July 23, 2021
 */
public class EDDSAPrivateKey extends PrivateKey {

    private byte[] dEncoded;
    private byte[] xEncoded;

    /**
     * Constructor
     * @param signatureAlgorithm
     * @param dEncoded
     * @param xEncoded
     */
    public EDDSAPrivateKey(final SignatureAlgorithm signatureAlgorithm, final byte[] dEncoded, final byte[] xEncoded) {
        setSignatureAlgorithm(signatureAlgorithm);            
        this.dEncoded = dEncoded.clone();
        this.xEncoded = xEncoded.clone();
    }
    
    /**
     * Copy Constructor
     * @param eddsaPrivateKey
     */
    public EDDSAPrivateKey(final EDDSAPrivateKey eddsaPrivateKey) {
        setSignatureAlgorithm(eddsaPrivateKey.getSignatureAlgorithm());  

        final byte[] inDEncoded = eddsaPrivateKey.getPrivateKeyEncoded();
        final byte[] inXEncoded = eddsaPrivateKey.getPublicKeyEncoded();

        this.dEncoded = inDEncoded != null ? inDEncoded.clone() : null;
        this.xEncoded = inXEncoded != null ? inXEncoded.clone() : null;
        
        setKeyId(eddsaPrivateKey.getKeyId());
    }

    /**
     * Returns public key value array (PKCS8 encoded, Private-Key Information Syntax Standard) 
     * in PKCS8EncodedKeySpec object;
     * PKCS8EncodedKeySpec allows to get encoded array (byte[] getEncoded());
     * 
     * @return public key value array (PKCS8 encoded, Private-Key Information Syntax Standard)
     * in PKCS8EncodedKeySpec object;
     * PKCS8EncodedKeySpec allows to get encoded array (byte[] getEncoded()); 
     */
    public PKCS8EncodedKeySpec getPrivateKeySpec() {
        return new PKCS8EncodedKeySpec(this.dEncoded);
    }
    
    /**
     * Returns public key value array (X509 encoded) in X509EncodedKeySpec object;
     * X509EncodedKeySpec allows to get encoded array (byte[]);
     * 
     * @return public key value array (X509 encoded) in X509EncodedKeySpec object;
     * X509EncodedKeySpec allows to get encoded array (byte[]);
     */
    public X509EncodedKeySpec getPublicKeySpec() {
        if(this.xEncoded == null)
            return null;
        else
            return new X509EncodedKeySpec(this.xEncoded);
    }

    /**
     * Returns original array (decoded) of the public key (ED25519 - 32 byte, ED448 - 56 bytes);
     * 
     * @return original array (decoded) of the public key (ED25519 - 32 byte, ED448 - 56 bytes);
     * @throws IOException
     */
    public byte[] getPrivateKeyDecoded() throws IOException {
        PrivateKeyInfo pki = PrivateKeyInfo.getInstance(new PKCS8EncodedKeySpec(this.dEncoded).getEncoded());
        return ASN1OctetString.getInstance(pki.parsePrivateKey()).getOctets();        
    }
    
    /**
     * Returns encoded private key
     * 
     * @return
     */
    public byte[] getPrivateKeyEncoded() {
        return this.dEncoded != null ? this.dEncoded : new byte[] {};
    }

    /**
     * Returns original array (decoded) of the public key (ED25519 - 32 byte, ED448 - 56 bytes);
     * 
     * @return original array (decoded) of the public key (ED25519 - 32 byte, ED448 - 56 bytes);
     */
    public byte[] getPublicKeyDecoded() {
        if(this.xEncoded == null) {
            return new byte[] {};
        }
        else {
            SubjectPublicKeyInfo subjPubKeyInfo = SubjectPublicKeyInfo.getInstance(this.xEncoded);
            return subjPubKeyInfo.getPublicKeyData().getOctets();       
        }
    }

    /**
     * Returns encoded public key
     * 
     * @return
     */
    public byte[] getPublicKeyEncoded() {
        return this.xEncoded;
    }

    /**
     * Converts EDDSA private key to JSON Object
     */
    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MODULUS, JSONObject.NULL);
        jsonObject.put(EXPONENT, JSONObject.NULL);
        jsonObject.put(D, Base64Util.base64urlencode(this.dEncoded));
        jsonObject.put(X, Base64Util.base64urlencode(this.xEncoded));
        return jsonObject;
    }

    /**
     *  Converts EDDSA private key to String Object
     */
    @Override
    public String toString() {
        try {
            return toJSONObject().toString(4);
        } catch (Exception e) {
            return StringUtils.EMPTY_STRING;
        }
    }

}
