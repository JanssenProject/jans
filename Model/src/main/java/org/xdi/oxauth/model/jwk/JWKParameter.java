/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

/**
 * @author Javier Rojas Blum Date: 03.19.2013
 */
public interface JWKParameter {

    public static final String JSON_WEB_KEY_SET = "keys";
    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY = "publicKey";
    public static final String CERTIFICATE = "certificate";
    public static final String JWKS_KEY_ID = "keyId";
    public static final String JWKS_ALGORITHM = "algorithm";

    // Common
    public static final String KEY_TYPE = "kty";
    public static final String KEY_USE = "use";
    public static final String ALGORITHM = "alg";
    public static final String KEY_ID = "kid";
    public static final String EXPIRATION_TIME = "exp";

    // Public Key
    public static final String MODULUS = "n";
    public static final String EXPONENT = "e";
    public static final String CURVE = "crv";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String D = "d";
    public static final String PUBLIC_MODULUS = "modulus";
    public static final String PUBLIC_EXPONENT = "exponent";

    // Private Key
    public static final String PRIVATE_MODULUS = "modulus";
    public static final String PRIVATE_EXPONENT = "privateExponent";

    // Symmetric Key
    public static final String KEY_VALUE = "k";

    // X.509 Certificate Chain
    public static final String X5C = "x5c";
}