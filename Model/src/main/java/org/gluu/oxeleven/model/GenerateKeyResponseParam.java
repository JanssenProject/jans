/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.model;

/**
 * @author Javier Rojas Blum
 * @version October 5, 2016
 */
public interface GenerateKeyResponseParam {

    public static final String KEY_TYPE = "kty";
    public static final String KEY_ID = "kid";
    public static final String KEY_USE = "use";
    public static final String ALGORITHM = "alg";
    public static final String CURVE = "crv";
    public static final String EXPIRATION_TIME = "exp";
    public static final String MODULUS = "n";
    public static final String EXPONENT = "e";
    public static final String X = "x";
    public static final String Y = "y";
    public static final String CERTIFICATE_CHAIN = "x5c";
}
