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
public interface JWKParameter {

    String JSON_WEB_KEY_SET = "keys";
    String KEY_TYPE = "kty";
    String KEY_USE = "use";
    String ALGORITHM = "alg";
    String KEY_ID = "kid";
    String EXPIRATION_TIME = "exp";
    String MODULUS = "n";
    String EXPONENT = "e";
    String CURVE = "crv";
    String X = "x";
    String Y = "y";
    String D = "d";
    String KEY_VALUE = "k";
    String CERTIFICATE_CHAIN = "x5c";
    String PRIVATE_KEY = "privateKey";
    String PUBLIC_KEY = "publicKey";
}
