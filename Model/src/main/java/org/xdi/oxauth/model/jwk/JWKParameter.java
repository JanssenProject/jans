/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.jwk;

/**
 * @author Javier Rojas Blum
 * @version February 17, 2016
 */
public interface JWKParameter {

    public static final String JSON_WEB_KEY_SET = "keys";
    public static final String KEY_TYPE = "kty";
    public static final String KEY_USE = "use";
    public static final String ALGORITHM = "alg";
    public static final String KEY_ID = "kid";
    public static final String EXPIRATION_TIME = "exp";
    public static final String MODULUS = "n";
    public static final String EXPONENT = "e";
    public static final String CURVE = "crv";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String D = "d";
    public static final String KEY_VALUE = "k";
    public static final String CERTIFICATE_CHAIN = "x5c";
    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY = "publicKey";
}