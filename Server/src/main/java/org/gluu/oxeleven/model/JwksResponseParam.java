package org.gluu.oxeleven.model;

/**
 * @author Javier Rojas Blum
 * @version March 31, 2016
 */
public interface JwksResponseParam {

    public static final String JSON_WEB_KEY_SET = "keys";
    public static final String KEY_TYPE = "kty";
    public static final String KEY_ID = "kid";
    public static final String KEY_USE = "use";
    public static final String ALGORITHM = "alg";
    public static final String MODULUS = "n";
    public static final String EXPONENT = "e";
    public static final String CURVE = "crv";
    public static final String X = "x";
    public static final String Y = "y";
}
