/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.model;

/**
 * @author Javier Rojas Blum
 * @version April 26, 2016
 */
public interface GenerateKeyResponseParam {

    public static final String KEY_TYPE = "kty";
    public static final String KEY_ID = "kid";
    public static final String KEY_USE = "sig";
    public static final String ALGORITHM = "alg";
    public static final String CURVE = "crv";
}
