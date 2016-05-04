/*
 * oxEleven is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2016, Gluu
 */

package org.gluu.oxeleven.model;

/**
 * @author Javier Rojas Blum
 * @version May 4, 2016
 */
public interface SignatureAlgorithm {

    public static final String NONE = "none";
    public static final String HS256 = "HS256";
    public static final String HS384 = "HS384";
    public static final String HS512 = "HS512";
    public static final String RS256 = "RS256";
    public static final String RS384 = "RS384";
    public static final String RS512 = "RS512";
    public static final String ES256 = "ES256";
    public static final String ES384 = "ES384";
    public static final String ES512 = "ES512";
}
