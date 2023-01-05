/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.util.StringUtils;
import io.jans.as.model.crypto.signature.RSAKeyFactory;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Identifies the cryptographic algorithm used with the key.
 *
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
@SuppressWarnings("java:S1874")
public enum Algorithm {

    // Signature
    RS256("RS256", "id_token RS256 Sign Key", "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-256", Use.SIGNATURE, AlgorithmFamily.RSA, RSAKeyFactory.DEF_KEYLENGTH),
    RS384("RS384", "id_token RS384 Sign Key", "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-384", Use.SIGNATURE, AlgorithmFamily.RSA, RSAKeyFactory.DEF_KEYLENGTH),
    RS512("RS512", "id_token RS512 Sign Key", "Signature Key: RSA RSASSA-PKCS1-v1_5 using SHA-512", Use.SIGNATURE, AlgorithmFamily.RSA, RSAKeyFactory.DEF_KEYLENGTH),

    ES256("ES256", "id_token ES256 Sign Key", "Signature Key: ECDSA using P-256 (secp256r1) and SHA-256", Use.SIGNATURE, AlgorithmFamily.EC, 256),
    ES256K("ES256K", "id_token ES256K Sign Key", "Signature Key: ECDSA using secp256k1 and SHA-256", Use.SIGNATURE, AlgorithmFamily.EC, 256),
    ES384("ES384", "id_token ES384 Sign Key", "Signature Key: ECDSA using P-384 (secp384r1) and SHA-384", Use.SIGNATURE, AlgorithmFamily.EC, 384),
    ES512("ES512", "id_token ES512 Sign Key", "Signature Key: ECDSA using P-521 (secp521r1) and SHA-512", Use.SIGNATURE, AlgorithmFamily.EC, 528),

    PS256("PS256", "id_token PS256 Sign Key", "Signature Key: RSASSA-PSS using SHA-256 and MGF1 with SHA-256", Use.SIGNATURE, AlgorithmFamily.RSA, RSAKeyFactory.DEF_KEYLENGTH),
    PS384("PS384", "id_token PS384 Sign Key", "Signature Key: RSASSA-PSS using SHA-384 and MGF1 with SHA-384", Use.SIGNATURE, AlgorithmFamily.RSA, RSAKeyFactory.DEF_KEYLENGTH),
    PS512("PS512", "id_token PS512 Sign Key", "Signature Key: RSASSA-PSS using SHA-512 and MGF1 with SHA-512", Use.SIGNATURE, AlgorithmFamily.RSA, RSAKeyFactory.DEF_KEYLENGTH),

    EDDSA("EdDSA", "id_token EdDSA Sign Key", "Signature Key: EdDSA using Ed25519 with SHA-512", Use.SIGNATURE, AlgorithmFamily.ED, 256),

    // Encryption
    RSA1_5("RSA1_5", "id_token RSA1_5 Encryption Key", "Encryption Key: RSAES-PKCS1-v1_5",
            Use.ENCRYPTION, AlgorithmFamily.RSA, RSAKeyFactory.DEF_KEYLENGTH),
    RSA_OAEP("RSA-OAEP", "id_token RSA-OAEP Encryption Key", "Encryption Key: RSAES OAEP using default parameters",
            Use.ENCRYPTION, AlgorithmFamily.RSA, RSAKeyFactory.DEF_KEYLENGTH),
    RSA_OAEP_256("RSA-OAEP-256", "id_token RSA-OAEP-256 Encryption Key", "Encryption Key: RSAES OAEP using SHA-256 and MGF1 with SHA-256 ",
            Use.ENCRYPTION, AlgorithmFamily.RSA, RSAKeyFactory.DEF_KEYLENGTH),

    ECDH_ES("ECDH-ES", "id_token ECDH-ES Encryption Key", "Encryption Key: Elliptic Curve Diffie-Hellman Ephemeral Static key agreement using Concat KDF",
            Use.ENCRYPTION, AlgorithmFamily.EC, 256),
    ECDH_ES_PLUS_A128KW("ECDH-ES+A128KW", "id_token ECDH-ES+A128KW Encryption Key", "Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A128KW",
            Use.ENCRYPTION, AlgorithmFamily.EC, 256),
    ECDH_ES_PLUS_A192KW("ECDH-ES+A192KW", "id_token ECDH-ES+A192KW Encryption Key", "Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A192KW",
            Use.ENCRYPTION, AlgorithmFamily.EC, 256),
    ECDH_ES_PLUS_A256KW("ECDH-ES+A256KW", "id_token ECDH-ES+A256KW Encryption Key", "Encryption Key: ECDH-ES using Concat KDF and CEK wrapped with A256KW",
            Use.ENCRYPTION, AlgorithmFamily.EC, 256),

    A128KW("A128KW", "id_token A128KW Encryption Key", "Encryption Key: AES Key Wrap with default initial value using 128-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES, -1),
    A192KW("A192KW", "id_token A192KW Encryption Key", "Encryption Key: AES Key Wrap with default initial value using 192-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES, -1),
    A256KW("A256KW", "id_token A256KW Encryption Key", "Encryption Key:  AES Key Wrap with default initial value using 192-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES, -1),

    A128GCMKW("A128GCMKW", "id_token A128GCMKW Encryption Key", "Encryption Key: Key wrapping with AES GCM using 128-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES, 128),
    A192GCMKW("A192GCMKW", "id_token A192GCMKW Encryption Key", "Encryption Key: Key wrapping with AES GCM using 192-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES, 192),
    A256GCMKW("A256GCMKW", "id_token A256GCMKW Encryption Key", "Encryption Key: Key wrapping with AES GCM using 256-bit key",
            Use.ENCRYPTION, AlgorithmFamily.AES, 256),

    PBES2_HS256_PLUS_A128KW("PBES2-HS256+A128KW", "id_token PBES2-HS256+A128KW Encryption Key", "Encryption Key: PBES2 with HMAC SHA-256 and A128KW wrapping",
            Use.ENCRYPTION, AlgorithmFamily.PASSW, -1),
    PBES2_HS384_PLUS_A192KW("PBES2-HS384+A192KW", "id_token PBES2-HS384+A192KW Encryption Key", "Encryption Key: PBES2 with HMAC SHA-384 and A192KW wrapping",
            Use.ENCRYPTION, AlgorithmFamily.PASSW, -1),
    PBES2_HS512_PLUS_A256KW("PBES2-HS512+A256KW", "id_token PBES2-HS512+A256KW Encryption Key", "Encryption Key: PBES2 with HMAC SHA-512 and A256KW wrapping",
            Use.ENCRYPTION, AlgorithmFamily.PASSW, -1),

    DIR("dir", "id_token Direct Encryption", "Encryption Key: Direct use of a shared symmetric key as the CEK",
            Use.ENCRYPTION, AlgorithmFamily.DIR, -1);

    private final String paramName;
    private final String outName;
    private final String description;
    private final Use use;
    private final AlgorithmFamily family;
    /**
     *  keyLength
            in bits, for some Algorithms - max key Length;
            keyLength is defined only for algorithms,
            which are used in KeyGenerator,
            i.e. where key is generated in the KeyStorage;
            can be defined for other (AES: A192KW,...); 
     */
    private final int keyLength;

    Algorithm(final String paramName, final String outName, final String description,
              final Use use, final AlgorithmFamily family, int keyLength) {
        this.paramName = paramName;
        this.outName = outName;
        this.description = description;
        this.use = use;
        this.family = family;
        this.keyLength = keyLength;
    }

    public String getParamName() {
        return paramName;
    }

    public String getOutName() {
        return outName;
    }

    public String getDescription() {
        return description;
    }

    public Use getUse() {
        return use;
    }

    public AlgorithmFamily getFamily() {
        return family;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public boolean canGenerateKeys() { // based on currently supported generator, see io.jans.as.model.crypto.AuthCryptoProvider.generateKeyEncryption
        return family == AlgorithmFamily.RSA || family == AlgorithmFamily.EC || family == AlgorithmFamily.ED;
    }

    public void fill(JSONObject jsonObject) {
        jsonObject.put(JWKParameter.NAME, getOutName());
        jsonObject.put(JWKParameter.DESCRIPTION, getDescription());
        jsonObject.put(JWKParameter.KEY_TYPE, getFamily());
        jsonObject.put(JWKParameter.KEY_USE, getUse().getParamName());
        jsonObject.put(JWKParameter.ALGORITHM, getParamName());
    }

    /**
     * Returns the corresponding {@link Algorithm} for a parameter.
     *
     * @param param The use parameter.
     * @return The corresponding algorithm if found, otherwise <code>null</code>.
     */
    @JsonCreator
    public static Algorithm fromString(String param) {
        if (param != null) {
            for (Algorithm algorithm : Algorithm.values()) {
                if (param.equals(algorithm.paramName)) {
                    return algorithm;
                }
            }
        }
        return null;
    }

    public static List<Algorithm> fromString(String[] params, Use use) {
        List<Algorithm> algorithms = new ArrayList<>();

        for (String param : params) {
            Algorithm algorithm = Algorithm.fromString(param);
            if (algorithm != null && algorithm.use == use) {
                algorithms.add(algorithm);
            } else if (StringUtils.equals("RSA_OAEP", param)) {
                algorithms.add(RSA_OAEP);
            }
        }

        return algorithms;
    }


    /**
     * Returns a string representation of the object. In this case the parameter name.
     *
     * @return The string representation of the object.
     */
    @Override
    @JsonValue
    public String toString() {
        return paramName;
    }
}