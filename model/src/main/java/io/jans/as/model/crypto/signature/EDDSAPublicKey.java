/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2021, Janssen Project
 */
package io.jans.as.model.crypto.signature;

import static io.jans.as.model.jwk.JWKParameter.EXPONENT;
import static io.jans.as.model.jwk.JWKParameter.MODULUS;
import static io.jans.as.model.jwk.JWKParameter.X;

import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.json.JSONException;
import org.json.JSONObject;

import io.jans.as.model.crypto.PublicKey;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.StringUtils;

/**
 * Edwards Curve Digital Signature Algorithm (EDDSA)
 * Public Key
 *
 * @author Sergey Manoylo
 * @version July 23, 2021
 */
public class EDDSAPublicKey extends PublicKey {

    private byte[] xEncoded;

    /**
     * Constructor
     * 
     * @param signatureAlgorithm
     * @param publicKeyData
     */
    public EDDSAPublicKey(final SignatureAlgorithm signatureAlgorithm, byte[] xEncoded) {
        setSignatureAlgorithm(signatureAlgorithm);
        this.xEncoded = xEncoded.clone();
    }

    /**
     * Copy Constructor
     * 
     * @param eddsaPublicKey
     */
    public EDDSAPublicKey(final EDDSAPublicKey eddsaPublicKey) {
        setSignatureAlgorithm(eddsaPublicKey.getSignatureAlgorithm());
        final byte[] inXEncoded = eddsaPublicKey.getPublicKeyEncoded();        
        this.xEncoded = inXEncoded != null ? inXEncoded.clone() : null;
        setKeyId(eddsaPublicKey.getKeyId());
        setCertificate(eddsaPublicKey.getCertificate());
    }    

    /**
     * get public key value array (X509 encoded) in X509EncodedKeySpec object;
     * X509EncodedKeySpec allows to get encoded array (byte[] getEncoded())
     * 
     * @return public key value array (X509 encoded) in X509EncodedKeySpec object;
     *         X509EncodedKeySpec allows to get encoded array (byte[] getEncoded());
     */
    public X509EncodedKeySpec getPublicKeySpec() {
        return new X509EncodedKeySpec(this.xEncoded);
    }

    /**
     * Returns original array (decoded) of the public key
     * (ED25519 - 32 byte, ED448 - 56 bytes)
     * 
     * @return original array (decoded) of the public key;
     * 
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
        return this.xEncoded != null ? this.xEncoded : new byte[] {};
    }

    /**
     * Converts EDDSA public key to JSON Object
     */
    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(MODULUS, JSONObject.NULL);
        jsonObject.put(EXPONENT, JSONObject.NULL);
        jsonObject.put(X, Base64Util.base64urlencode(this.xEncoded));
        return jsonObject;
    }

    /**
     * Converts EDDSA public key to String Object
     */
    @Override
    public String toString() {
        try {
            return toJSONObject().toString(4);
        } catch (Exception e) {
            return StringUtils.EMPTY_STRING;
        }
    }
    
    /**
     * Overriding Object.equals() function
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj) {
            return true;            
        }
        if(obj == null) {
            return false;
        }
        if(this.getClass() != obj.getClass()) {
            return false;
        }
        EDDSAPublicKey objTyped = (EDDSAPublicKey) obj;

        if(!Arrays.equals(this.xEncoded, objTyped.xEncoded)) {
            return false;
        }

        String thisKeyId = getKeyId();
        String objKeyId = objTyped.getKeyId();        

        SignatureAlgorithm thisSignAlg = this.getSignatureAlgorithm();        
        SignatureAlgorithm objSignAlg = objTyped.getSignatureAlgorithm();
        
        boolean keysEquals = (thisKeyId == null && objKeyId == null) ||
                (thisKeyId != null && thisKeyId.equals(objKeyId));

        boolean signAlgEquals = (thisSignAlg == null && objSignAlg == null) ||
                (thisSignAlg != null && thisSignAlg.equals(objSignAlg));

        return keysEquals && signAlgEquals; 
    }
    
    /**
     * Overriding Object.hashCode() function
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(this.xEncoded);         
    }
}
