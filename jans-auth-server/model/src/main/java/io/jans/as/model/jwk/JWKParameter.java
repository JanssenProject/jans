/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwk;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class JWKParameter {

    private JWKParameter() {
    }

    public static final String NAME = "name";
    public static final String DESCRIPTION = "descr";
    public static final String JSON_WEB_KEY_SET = "keys";
    public static final String KEY_TYPE = "kty";
    public static final String KEY_USE = "use";
    public static final String KEY_OPS = "key_ops";
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
